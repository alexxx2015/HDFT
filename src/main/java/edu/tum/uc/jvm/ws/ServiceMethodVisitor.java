package edu.tum.uc.jvm.ws;

import java.util.List;

import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;

import edu.tum.uc.jvm.utility.analysis.StaticAnalysis;
import edu.tum.uc.jvm.ws.ServiceClassVisitor.SinkSourceSpec;

/**
 * This class search and rewrites service interface methods
 * 
 * @author alex
 *
 */
public class ServiceMethodVisitor extends MethodVisitor {

	private MethodVisitor parentMv;
	private String parentMethodName;
	private ServiceClassAnalyzer parentClass;
	private List<ServiceClassAnalyzer> sei;
	private ClassVisitor cv;

	public ServiceMethodVisitor(int arg0) {
		super(arg0);
	}

	public ServiceMethodVisitor(int arg0, MethodVisitor parentMv,
			ServiceClassAnalyzer parentClass, String parentMethodName,
			List<ServiceClassAnalyzer> sei, ClassVisitor cv) {
		super(arg0, parentMv);
		this.parentMv = parentMv;
		this.parentClass = parentClass;
		this.parentMethodName = parentMethodName;
		this.sei = sei;
		this.cv = cv;
	}

	@Override
	public void visitMethodInsn(int opcode, String owner, String name,
			String desc, boolean intf) {

		// skip non-interface methods
		if (opcode != Opcodes.INVOKEINTERFACE) {
			mv.visitMethodInsn(opcode, owner, name, desc, intf);
			return;
		}

		// skip non-webservice interfaces
		if (!isWebServiceInterface(owner, name, desc)) {
			mv.visitMethodInsn(opcode, owner, name, desc, intf);
			return;
		}

		String[] signature = generateWrapper(opcode, owner, name, desc, intf);
		mv.visitMethodInsn(Opcodes.INVOKESTATIC,
				this.parentClass.getClassName(), signature[0], signature[1],
				false);
	}

	private String[] generateWrapper(int opcode, String owner, String name,
			String desc, boolean intf) {

		boolean isConstructor = opcode == Opcodes.INVOKESPECIAL
				&& owner.equals("<init>");
		boolean isStatic = opcode == Opcodes.INVOKESTATIC;
		String[] _return = new String[2];
		String wrapperName = "_" + name + "_";
		StringBuilder wrapperDesc = new StringBuilder();
		String wrapperSig = null;
		String[] wrapperExceptions = null;

		// ==> Preprocessing, create required method description, indizes,
		// Create method description of the to be invoked methods
		wrapperDesc.append("(");
		int argIndex = -1;
		if (!isConstructor && !isStatic) {
			wrapperDesc.append("L" + owner + ";");// append("Ljava/lang/Object;");
			++argIndex;
		} // count the number of parameters the wrapper method has,
			// (0)=object,(1...n-1)=parameters,(n)=source-id

		// Helper variable to store the correct parameter index within the
		// local variable table
		int paramStartIndex = 0;
		Type[] argT = Type.getArgumentTypes(desc);
		if (argT.length > 0) {
			paramStartIndex = argIndex + 1;
			for (Type t : argT) {
				wrapperDesc.append(t.getDescriptor());
				argIndex++;
				if (Type.DOUBLE == t.getSort() || Type.LONG == t.getSort()) {
					argIndex++;
				}
			}
		}

		wrapperDesc.append(")");

		Type retT = Type.getReturnType(desc);
		if (isConstructor) {
			wrapperDesc.append("L" + owner + ";");
		} else if (retT != null) {
			wrapperDesc.append(retT.getDescriptor());
		}

		_return[0] = wrapperName;
		_return[1] = wrapperDesc.toString();

		// Check if method invocation was already wrapped
		// String id = this.parentClass.getClassName() + "."+owner+"." + name +
		// ":" + desc.toString();
		String key = this.parentClass.getClassName() + ":" + owner + ":" + name
				+ ":" + desc.toString();
		String value = _return[0] + ":" + _return[1];
		if (ServiceClassVisitor.METHODS.containsKey(key)) {
			return _return;
		} else {
			ServiceClassVisitor.METHODS.put(key, value);
		}

		// Add wrapper method to list of sinks, but only when it's not already
		// in it.
		if (wrapperName.contains("updateStream")) {
			int a = 0;
			a++;
		}
		Type[] types = Type.getArgumentTypes(wrapperDesc.toString());
		for (int i = 1; i < types.length; i++) {
			SinkSourceSpec sink = new SinkSourceSpec(
					StaticAnalysis.NODETYPE.SINK);
			sink.setClazz(this.parentClass.getClassName());
			sink.setSelector(wrapperName + wrapperDesc);
			sink.setParams(String.valueOf(i));
			ServiceClassVisitor.SINKLIST.add(sink);
		}

		// Generate wrapper method
		MethodVisitor mmv = this.cv.visitMethod(Opcodes.ACC_PUBLIC
				+ Opcodes.ACC_STATIC, wrapperName, wrapperDesc.toString(),
				wrapperSig, null);
		mmv.visitCode();
		// Send an event to the pdp
		if (isConstructor) {
			mmv.visitInsn(Opcodes.ACONST_NULL);
		} else if (!isStatic) {
			mmv.visitVarInsn(Opcodes.ALOAD, 0);// first parameter is the
												// ownerobject
		}
		int i = paramStartIndex; // local variable index counter
		for (Type argType : argT) {
			if (argType.getSort() == Type.OBJECT) {
				mmv.visitVarInsn(Opcodes.ALOAD, i);
			} else if (argType.getSort() == Type.ARRAY) {
				mmv.visitVarInsn(Opcodes.ALOAD, i);
			} else {
				if (argType.getSort() == Type.DOUBLE) {
					mmv.visitVarInsn(Opcodes.DLOAD, i);
					i++;
				} else if (argType.getSort() == Type.FLOAT) {
					mmv.visitVarInsn(Opcodes.FLOAD, i);
				} else if (argType.getSort() == Type.LONG) {
					mmv.visitVarInsn(Opcodes.LLOAD, i);
					i++;
				} else if (argType.getSort() == Type.INT) {
					mmv.visitVarInsn(Opcodes.ILOAD, i);
				} else if (argType.getSort() == Type.CHAR) {
					mmv.visitVarInsn(Opcodes.ILOAD, i);
				} else if (argType.getSort() == Type.BYTE) {
					mmv.visitVarInsn(Opcodes.ILOAD, i);
				} else if (argType.getSort() == Type.BOOLEAN) {
					mmv.visitVarInsn(Opcodes.ILOAD, i);
				} else if (argType.getSort() == Type.SHORT) {
					mmv.visitVarInsn(Opcodes.ILOAD, i);
				}
			}
			i++;
		}
		mmv.visitMethodInsn(Opcodes.INVOKEINTERFACE, owner, name, desc, intf);
		// Add return
		if (retT.getSort() == Type.OBJECT || retT.getSort() == Type.ARRAY
				|| isConstructor) {
			mmv.visitInsn(Opcodes.ARETURN);
		} else if (retT.getSort() == Type.DOUBLE) {
			mmv.visitInsn(Opcodes.DRETURN);
		} else if (retT.getSort() == Type.FLOAT) {
			mmv.visitInsn(Opcodes.FRETURN);
		} else if (retT.getSort() == Type.LONG) {
			mmv.visitInsn(Opcodes.LRETURN);
		} else if (retT.getSort() == Type.INT || retT.getSort() == Type.BOOLEAN) {
			mmv.visitInsn(Opcodes.IRETURN);
		} else {
			mmv.visitInsn(Opcodes.RETURN);
		}

		Type[] myArgT = Type.getArgumentTypes(desc.toString());
		mmv.visitMaxs(myArgT.length + 2, myArgT.length + 2);
		mmv.visitEnd();

		return _return;
	}

	private boolean isWebServiceInterface(String owner, String name, String desc) {
		boolean _return = false;

		for (ServiceClassAnalyzer sca : this.sei) {
			if (sca.getClassName().equals(owner) && sca.hasMethod(name, desc)) {
				_return = true;
				break;
			}
		}

		return _return;
	}
}
