package edu.tum.uc.jvm.declassification.qif;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Handle;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.commons.LocalVariablesSorter;
import org.objectweb.asm.tree.LabelNode;
import org.objectweb.asm.tree.LocalVariableNode;

import edu.tum.uc.jvm.utility.Mnemonic;
import edu.tum.uc.jvm.utility.Utility;
import edu.tum.uc.jvm.utility.analysis.Flow;
import edu.tum.uc.jvm.utility.analysis.Flow.Chop;
import edu.tum.uc.jvm.utility.analysis.SinkSource;
import edu.tum.uc.jvm.utility.analysis.StaticAnalysis;

public class QIFMethodVisitor extends AbstractMethodVisitor {

	private static Logger _logger = Logger.getLogger(QIFMethodVisitor.class.getName());

	private int mAccess;
	private String mName;
	private String mDesc;
	private String mSignature;
	private String clName;
	private String fqName;
	private ClassWriter cw;

	private LocalVariablesSorter lvs = null;
	private Map<Integer, LocalVariableNode> localVarTmp;
	private Label endLabel;

	public QIFMethodVisitor(int p_version, MethodVisitor p_mv, int access, String name, String desc, String signature,
			String classname, List<Chop> chopNodes, ClassWriter cw) {
		super(p_version, p_mv, chopNodes);
		this.mAccess = access;
		this.mName = name;
		this.mDesc = desc;
		this.mSignature = signature;
		this.clName = classname;
		this.fqName = this.clName.replace("/", ".") + "|" + this.mName + this.mDesc;
		this.cw = cw;
		this.endLabel = new Label();
		this.localVarTmp = new HashMap<Integer, LocalVariableNode>();

	}

	@Override
	public void visitFieldInsn(int opcode, String owner, String name, String desc) {
		Label lab = this.getCurrentLabel();
		Chop chopNode = checkChopNode(lab);
		if (chopNode != null)
			_logger.debug("Label found for " + chopNode.getLabel() + ", at offset " + lab.getOffset());

		mv.visitFieldInsn(opcode, owner, name, desc);
	}

	@Override
	public void visitIincInsn(int var, int increment) {
		Label lab = this.getCurrentLabel();
		Chop chopNode = checkChopNode(lab);
		if (chopNode != null)
			_logger.debug("Label found for " + chopNode.getLabel() + ", at offset " + lab.getOffset());

		mv.visitIincInsn(var, increment);
	}

	@Override
	public void visitInsn(int opcode) {
		Label lab = this.getCurrentLabel();
		Chop chopNode = checkChopNode(lab);
		if (chopNode != null) {
			_logger.debug("Label found for " + chopNode.getLabel() + ", at offset " + lab.getOffset() + ", "
					+ Mnemonic.OPCODE[opcode]);

			String[] wrapperDesc = null;
			boolean convertOp = false;
			int dupCmd = -1;
			Class<?> boxClass = null;
			Type t = null;
			// Convert instructions reduce the number of information to the max
			// value of the target type
			// Convert instructions operate only on one stacke element
			switch (opcode) {
			case Opcodes.I2B:
			case Opcodes.I2C:
			case Opcodes.I2S:
			case Opcodes.I2L:
			case Opcodes.I2F:
			case Opcodes.I2D:
				convertOp = true;
				dupCmd = Opcodes.DUP;
				boxClass = Integer.class;
				t = Type.INT_TYPE;
				break;
			case Opcodes.F2I:
			case Opcodes.F2L:
			case Opcodes.F2D:
				convertOp = true;
				dupCmd = Opcodes.DUP;
				boxClass = Float.class;
				t = Type.FLOAT_TYPE;
				break;
			case Opcodes.L2I:
			case Opcodes.L2F:
			case Opcodes.L2D:
				convertOp = true;
				dupCmd = Opcodes.DUP;
				boxClass = Long.class;
				t = Type.LONG_TYPE;
				break;
			case Opcodes.D2I:
			case Opcodes.D2F:
			case Opcodes.D2L:
				convertOp = true;
				dupCmd = Opcodes.DUP2;
				boxClass = Double.class;
				t = Type.DOUBLE_TYPE;
				break;
			default:
				// an arithmetic binary instruction first pops two stack
				// elements and pushes the result back
				// therefore we wrap arithmetic binary instructions in a special
				// wrap such instructions in a special wraper method
				convertOp = false;
				wrapperDesc = QIFMethodWrapper.createArithWrapper(opcode, cw, lab, chopNode, clName);
				break;
			}

			// If we analyze a convert operation then do not wrap instruction
			if (convertOp) {
				/*
				 * List<Flow> flows =
				 * StaticAnalysis.getFlowsByChopNode(chopNode); if (flows.size()
				 * > 0) { for (Flow f : flows) { for (String source :
				 * f.getSource()) { // Load operand parameter on the stack
				 * mv.visitInsn(dupCmd);
				 * mv.visitMethodInsn(Opcodes.INVOKESTATIC,
				 * boxClass.getName().replace(".", "/"), "valueOf", "(" +
				 * t.getDescriptor() + ")L" + boxClass.getName().replace(".",
				 * "/") + ";", false); mv.visitLdcInsn(source);
				 * mv.visitLdcInsn(opcode);
				 * mv.visitMethodInsn(Opcodes.INVOKESTATIC,
				 * QIFClassVisitor.DELEGATECLASS, "decConvQty",
				 * "(Ljava/lang/Object;Ljava/lang/String;I)V", false); } } }
				 */
				// Load operand parameter on the stack
				mv.visitInsn(dupCmd);
				mv.visitMethodInsn(Opcodes.INVOKESTATIC, boxClass.getName().replace(".", "/"), "valueOf",
						"(" + t.getDescriptor() + ")L" + boxClass.getName().replace(".", "/") + ";", false);
				mv.visitLdcInsn(chopNode.getSourceId());
				mv.visitLdcInsn(opcode);
				mv.visitMethodInsn(Opcodes.INVOKESTATIC, QIFClassVisitor.DELEGATECLASS, "decConvQty",
						"(Ljava/lang/Object;Ljava/lang/String;I)V", false);
				mv.visitInsn(opcode);
			}
			// Wrap an arithmetic instruction
			else if (wrapperDesc != null && wrapperDesc.length > 0 && wrapperDesc[0] != null
					&& wrapperDesc[1] != null) {
				String wrapperMethodName = wrapperDesc[0];
				String wrapperMethodDesc = wrapperDesc[1];
				mv.visitMethodInsn(Opcodes.INVOKESTATIC, clName, wrapperMethodName, wrapperMethodDesc, false);
			} else {
				mv.visitInsn(opcode);
			}
		} else {
			mv.visitInsn(opcode);
		}
	}

	@Override
	public void visitIntInsn(int opcode, int operand) {
		Label lab = this.getCurrentLabel();
		Chop chopNode = checkChopNode(lab);
		if (chopNode != null)
			_logger.debug("Label found for " + chopNode.getLabel() + ", at offset " + lab.getOffset());

		mv.visitIntInsn(opcode, operand);
	}

	@Override
	public void visitInvokeDynamicInsn(String name, String desc, Handle bsm, Object... bsmArgs) {
		Label lab = this.getCurrentLabel();
		Chop chopNode = checkChopNode(lab);
		if (chopNode != null)
			_logger.debug("Label found for " + chopNode.getLabel() + ", at offset " + lab.getOffset());

		mv.visitInvokeDynamicInsn(name, desc, bsm, bsmArgs);
	}

	@Override
	public void visitJumpInsn(int opcode, Label label) {
		Label lab = this.getCurrentLabel();
		Chop chopNode = checkChopNode(lab);
		if (chopNode != null)
			_logger.debug("Label found for " + chopNode.getLabel() + ", at offset " + lab.getOffset());

		mv.visitJumpInsn(opcode, label);
	}

	@Override
	public void visitLdcInsn(Object cst) {
		Label lab = this.getCurrentLabel();
		Chop chopNode = checkChopNode(lab);
		if (chopNode != null)
			_logger.debug("Label found for " + chopNode.getLabel() + ", at offset " + lab.getOffset());

		mv.visitLdcInsn(cst);
	}

	@Override
	public void visitLookupSwitchInsn(Label dflt, int[] keys, Label[] labels) {
		Label lab = this.getCurrentLabel();
		Chop chopNode = checkChopNode(lab);
		if (chopNode != null)
			_logger.debug("Label found for " + chopNode.getLabel() + ", at offset " + lab.getOffset());

		mv.visitLookupSwitchInsn(dflt, keys, labels);
	}

	@Override
	public void visitMethodInsn(int opcode, String owner, String name, String desc, boolean itf) {
		boolean addOriginalMethod = true;
		Chop chopNode = null;
		List<SinkSource> sources = null;
		List<SinkSource> sinks = null;

		Label lab = this.getCurrentLabel();
		if (lab != null) {
			int offset = lab.getOffset();
			sources = StaticAnalysis.isSource(fqName, offset);
			sinks = StaticAnalysis.isSinkWithFlow(fqName, offset);

			if (sources != null && sources.size() > 0) {
				_logger.debug(sources.size() + " sources detected at offset " + offset + " in method " + this.mName);
				// execution of a single source reset the amount of read
				// information to 100%
				for (SinkSource source : sources) {
					// System.out.println(source.getId()+",
					// "+source.getOffset()+", "+source.getParam()+",
					// "+source.isReturn());

					Type[] argT = null;
					Type retT = null;
					for (String z : source.getPossibleSignatures()) {
						String signature = Utility.extractMethodSignature(z);
						argT = Type.getArgumentTypes(signature);
						retT = Type.getReturnType(signature);
						break;
					}
					String[] wrapperDesc = QIFMethodWrapper.createSourceWrapper(opcode, owner, name, desc, cw, clName,
							sources, offset);
					if (wrapperDesc.length == 2) {
						String wrapperMethodName = wrapperDesc[0];
						String wrapperMethodDesc = wrapperDesc[1];

						mv.visitMethodInsn(Opcodes.INVOKESTATIC, clName, wrapperMethodName, wrapperMethodDesc, false);
						addOriginalMethod = !addOriginalMethod;
					}

					// }
				}
			} else if (sinks != null && sinks.size() > 0) {
				_logger.debug(sinks.size() + " sinks detected at offset " + offset + " in method " + this.mName);
				for (SinkSource sink : sinks) {
					List<SinkSource> depSources = StaticAnalysis.getDepSources(sink);
					if (depSources.size() > 0) {
						mv.visitLdcInsn(sink.getId());
						mv.visitLdcInsn(depSources.size());
						mv.visitTypeInsn(Opcodes.ANEWARRAY, String.class.getName().replace(".", "/"));
						for (int i = 0; i < depSources.size(); i++) {
							mv.visitInsn(Opcodes.DUP);// Duplicate the
														// array-reference
							mv.visitLdcInsn(i);// Push array-index
							mv.visitLdcInsn(depSources.get(i).getId());// Push
																		// to-be-added
																		// sourceid
							mv.visitInsn(Opcodes.AASTORE);// Store previously
															// pushed sourceid
															// into the array at
															// position
															// array-index
						}
						mv.visitMethodInsn(Opcodes.INVOKESTATIC, QIFClassVisitor.DELEGATECLASS, "check",
								"(Ljava/lang/String;[Ljava/lang/String;)V", false);
					}
				}
			} else {
				if (Utility.hasQtyInfo(owner, name)) {
					chopNode = checkChopNode(lab);
					if (chopNode != null) {
						_logger.debug("Label found for " + chopNode.getLabel() + ", at offset " + lab.getOffset());
						String[] wrapperDesc = QIFMethodWrapper.createMethodWrapper(opcode, owner, name, desc, cw,
								clName, chopNode);
						if (wrapperDesc != null && !"".equals(wrapperDesc[0]) && !"".equals(wrapperDesc[1])) {
							String sourceId = chopNode.getSourceId();
							String wrapperMethodName = wrapperDesc[0];
							String wrapperMethodDesc = wrapperDesc[1];
							mv.visitLdcInsn(sourceId);
							mv.visitMethodInsn(Opcodes.INVOKESTATIC, clName, wrapperMethodName, wrapperMethodDesc,
									false);
							addOriginalMethod = !addOriginalMethod;
						}
					}
				}
			}
		}

		if (addOriginalMethod)
			mv.visitMethodInsn(opcode, owner, name, desc, itf);
	}

	@Override
	public void visitMultiANewArrayInsn(String desc, int dims) {
		Label lab = this.getCurrentLabel();
		Chop chopNode = checkChopNode(lab);
		if (chopNode != null)
			_logger.debug("Label found for " + chopNode.getLabel() + ", at offset " + lab.getOffset());

		mv.visitMultiANewArrayInsn(desc, dims);
	}

	@Override
	public void visitTableSwitchInsn(int min, int max, Label dftl, Label rftl, Label... labels) {
		Label lab = this.getCurrentLabel();
		Chop chopNode = checkChopNode(lab);
		if (chopNode != null)
			_logger.debug("Label found for " + chopNode.getLabel() + ", at offset " + lab.getOffset());

		mv.visitTableSwitchInsn(min, max, dftl, rftl, labels);
	}

	@Override
	public void visitTypeInsn(int opcode, String type) {
		Label lab = this.getCurrentLabel();
		Chop chopNode = checkChopNode(lab);
		if (chopNode != null)
			_logger.debug("Label found for " + chopNode.getLabel() + ", at offset " + lab.getOffset());

		mv.visitTypeInsn(opcode, type);
	}

	@Override
	public void visitVarInsn(int opcode, int var) {
		Label lab = this.getCurrentLabel();
		Chop chopNode = checkChopNode(lab);
		if (chopNode != null)
			_logger.debug("Label found for " + chopNode.getLabel() + ", at offset " + lab.getOffset());

		mv.visitVarInsn(opcode, var);
	}

	public void setLvs(LocalVariablesSorter lvs) {
		this.lvs = lvs;
	}

	public LocalVariablesSorter getLvs() {
		return this.lvs;
	}

	// private int createTmpVar(Type type) {
	// int index = -1;
	// index = this.lvs.newLocal(type);
	// String locVarName = "__" + index + "__";
	// Label startLabel = new Label();
	// mv.visitLabel(startLabel);
	// LocalVariableNode tmpLocVar = new LocalVariableNode(locVarName,
	// type.getDescriptor(), null,
	// new LabelNode(startLabel), new LabelNode(endLabel), index);
	// this.localVarTmp.put(index, tmpLocVar);
	// return index;
	// }

	// @Override
	// public void visitEnd() {
	// if (this.localVarTmp.size() > 0) {
	// for (int key : this.localVarTmp.keySet()) {
	// LocalVariableNode n = this.localVarTmp.get(key);
	// mv.visitLocalVariable(n.name, n.desc, n.signature, n.start.getLabel(),
	// n.end.getLabel(), n.index);
	// }
	// this.localVarTmp.clear();
	// }
	// mv.visitLabel(endLabel);
	// mv.visitEnd();
	// }

}
