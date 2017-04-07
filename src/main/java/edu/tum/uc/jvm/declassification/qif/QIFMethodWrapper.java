package edu.tum.uc.jvm.declassification.qif;

import java.io.PrintStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;

import edu.tum.uc.jvm.utility.Mnemonic;
import edu.tum.uc.jvm.utility.Utility;
import edu.tum.uc.jvm.utility.analysis.Flow.Chop;
import edu.tum.uc.jvm.utility.analysis.Flow;
import edu.tum.uc.jvm.utility.analysis.SinkSource;
import edu.tum.uc.jvm.utility.analysis.StaticAnalysis;

public class QIFMethodWrapper {
	private static Logger _logger = Logger.getLogger(QIFMethodWrapper.class.getName());
	static Map<String, String> METHODS = new HashMap<String, String>();

	public static String[] createSourceWrapper(int opcode, String ownerclass, String ownermethod,
			String descOwnermethod, ClassWriter cv, String parentclass, List<SinkSource> sources, int offset) {
		boolean isConstructor = opcode == Opcodes.INVOKESPECIAL && ownermethod.equals("<init>");
		boolean isStatic = opcode == Opcodes.INVOKESTATIC;
		String[] _return = new String[2];
		StringBuilder wrapperMethodDesc = new StringBuilder();
		try {
			// Create the wrapper method signature description as follows:
			// (0)=object,(1...n-1)=parameters,(n)=parent-object,
			// (n+1)=parent-method, (n+2)=source-id, (n+3)=chop-label
			// Create method description of the to be invoked methods
			wrapperMethodDesc.append("(");
			int argIndex = -1;
			// if wrapped method is an instance method invocation, then add an
			// additional parameter for the object reference
			if (!isConstructor && !isStatic) {
				wrapperMethodDesc.append("L" + ownerclass + ";");// append("Ljava/lang/Object;");
				++argIndex;
			}

			// Helper variable to store the correct parameter index within the
			// local variable table
			int paramStartIndex = 0;
			Type[] oriArgT = Type.getArgumentTypes(descOwnermethod);
			if (oriArgT.length > 0) {
				paramStartIndex = argIndex + 1;
				for (Type t : oriArgT) {
					wrapperMethodDesc.append(t.getDescriptor());
					argIndex++;
					if (Type.DOUBLE == t.getSort() || Type.LONG == t.getSort()) {
						argIndex++;
					}
				}
			}
			// Parent-object index
			// wrapperMethodDesc.append("Ljava/lang/Object;");
			// int parentObjIndex = ++argIndex;
			//
			// // Parent-method index
			// wrapperMethodDesc.append("Ljava/lang/String;");
			// int parentMethodIndex = ++argIndex;
			//
			// // sourceindex
			// wrapperMethodDesc.append("Ljava/lang/String;");
			// int sinksourceIndex = ++argIndex;
			//
			// // chopLabel index
			// wrapperMethodDesc.append("Ljava/lang/String;");
			// int chopLabelIndex = ++argIndex;

			wrapperMethodDesc.append(")");

			Type retT = Type.getReturnType(descOwnermethod);
			if (isConstructor) {
				wrapperMethodDesc.append("L" + ownerclass + ";");
			} else if (retT != null) {
				wrapperMethodDesc.append(retT.getDescriptor());
			}

			// Constructors are renamed
			String wrapperMethodName = "_" + ownermethod + "_" + offset;
			if (isConstructor) {
				wrapperMethodName = ownerclass.replace("/", "_") + "_init";
			}
			_return[0] = wrapperMethodName;
			_return[1] = wrapperMethodDesc.toString();

			// Check if method invocation was already wrapped
			String id = parentclass + "." + wrapperMethodName + ":" + wrapperMethodDesc.toString();
			if (METHODS.containsKey(id)) {
				return _return;
			} else {
				METHODS.put(id, id);
			}

			// ==> Create wrapper method content
			MethodVisitor mv = cv.visitMethod(Opcodes.ACC_PRIVATE + Opcodes.ACC_STATIC, wrapperMethodName,
					wrapperMethodDesc.toString(), null, null);
			mv.visitCode();

			// --> Load all parameters into an array
//			mv.visitLdcInsn(oriArgT.length);
//			mv.visitTypeInsn(Opcodes.ANEWARRAY, "java/lang/Object");
//			// Store the array element at the end of parameter's list
//			int locArrayIndex = ++argIndex;
//			mv.visitVarInsn(Opcodes.ASTORE, locArrayIndex);
//			int i = paramStartIndex; // local variable index counter
//			int j = 0; // array index counter
//			for (Type argType : oriArgT) {
//				mv.visitVarInsn(Opcodes.ALOAD, locArrayIndex);
//				mv.visitLdcInsn(j);
//				if (argType.getSort() == Type.OBJECT) {
//					mv.visitVarInsn(Opcodes.ALOAD, i);
//				} else if (argType.getSort() == Type.ARRAY) {
//					mv.visitVarInsn(Opcodes.ALOAD, i);
//				} else {
//					if (argType.getSort() == Type.DOUBLE) {
//						mv.visitVarInsn(Opcodes.DLOAD, i);
//						i++;
//					} else if (argType.getSort() == Type.FLOAT) {
//						mv.visitVarInsn(Opcodes.FLOAD, i);
//					} else if (argType.getSort() == Type.LONG) {
//						mv.visitVarInsn(Opcodes.LLOAD, i);
//						i++;
//					} else if (argType.getSort() == Type.INT) {
//						mv.visitVarInsn(Opcodes.ILOAD, i);
//					} else if (argType.getSort() == Type.CHAR) {
//						mv.visitVarInsn(Opcodes.ILOAD, i);
//					} else if (argType.getSort() == Type.BYTE) {
//						mv.visitVarInsn(Opcodes.ILOAD, i);
//					} else if (argType.getSort() == Type.BOOLEAN) {
//						mv.visitVarInsn(Opcodes.ILOAD, i);
//					} else if (argType.getSort() == Type.SHORT) {
//						mv.visitVarInsn(Opcodes.ILOAD, i);
//					}
//					Utility.boxTopStackValue(mv, argType);
//				}
//				mv.visitInsn(Opcodes.AASTORE);
//				i++;
//				j++;
//			}
			// <-- Load all parameters into an array

			// --> Execute the original method
			if (isConstructor) {
				mv.visitTypeInsn(Opcodes.NEW, ownerclass);
				mv.visitInsn(Opcodes.DUP);
			} else if (!isStatic) {
				mv.visitVarInsn(Opcodes.ALOAD, 0);
			}
			int i = paramStartIndex;
			for (Type t : oriArgT) {
				if (t.getSort() == Type.OBJECT) {
					mv.visitVarInsn(Opcodes.ALOAD, i);
				} else if (t.getSort() == Type.ARRAY) {
					mv.visitVarInsn(Opcodes.ALOAD, i);
				} else if (t.getSort() == Type.DOUBLE) {
					mv.visitVarInsn(Opcodes.DLOAD, i);
					i++;
				} else if (t.getSort() == Type.FLOAT) {
					mv.visitVarInsn(Opcodes.FLOAD, i);
				} else if (t.getSort() == Type.LONG) {
					mv.visitVarInsn(Opcodes.LLOAD, i);
					i++;
				} else if ((t.getSort() == Type.INT) || (t.getSort() == Type.CHAR)) {
					mv.visitVarInsn(Opcodes.ILOAD, i);
				}
				i++;
			}
			// Timer4: log native method execution
			// Boolean timer4 = new
			// Boolean(ConfigProperties.getProperty(ConfigProperties.PROPERTIES.TIMER_T4));
			// if (timer4) {
			// mv.visitMethodInsn(Opcodes.INVOKESTATIC,
			// UcTransformer.HOOKMETHOD, "timerT4Start", "()V", false);
			// }

			mv.visitMethodInsn(opcode, ownerclass, ownermethod, descOwnermethod, opcode == Opcodes.INVOKEINTERFACE);
			// <-- Execute the original method

			// int constructorIndex = -1;
			// if(isConstructor){
			// constructorIndex = ++argIndex;
			// mv.visitVarInsn(Opcodes.ASTORE, constructorIndex);
			// }

			// if (timer4) {
			// mv.visitVarInsn(Opcodes.ALOAD, paramIndex);// myArgT.length -
			// // 1);
			// mv.visitMethodInsn(Opcodes.INVOKESTATIC,
			// UcTransformer.HOOKMETHOD, "timerT4Stop",
			// "(Ljava/lang/String;)V", false);
			// }

			// Compute the size of the source. A source could be the return
			// value or one of the paramtere of a method invocation.
			if (sources.size() > 0) {
				for (SinkSource s : sources) {
					if (s.isReturn() && !isConstructor) {
						mv.visitInsn(Opcodes.DUP);
						mv.visitLdcInsn(s.getId());
						mv.visitMethodInsn(Opcodes.INVOKESTATIC, QIFClassVisitor.DELEGATECLASS, "addQty",
								"(Ljava/lang/Object;Ljava/lang/String;)V", false);
					} else {
						int param = s.getParam();
						if (param > 0 && retT.getSize() > 0) {
							i = paramStartIndex + param - 1;
							Type t = oriArgT[param - 1];
							if (t.getSort() == Type.OBJECT) {
								mv.visitVarInsn(Opcodes.ALOAD, i);
							} else if (t.getSort() == Type.ARRAY) {
								mv.visitVarInsn(Opcodes.ALOAD, i);
							} else if (t.getSort() == Type.DOUBLE) {
								mv.visitVarInsn(Opcodes.DLOAD, i);
							} else if (t.getSort() == Type.FLOAT) {
								mv.visitVarInsn(Opcodes.FLOAD, i);
							} else if (t.getSort() == Type.LONG) {
								mv.visitVarInsn(Opcodes.LLOAD, i);
							} else if ((t.getSort() == Type.INT) || (t.getSort() == Type.CHAR)) {
								mv.visitVarInsn(Opcodes.ILOAD, i);
							}
							mv.visitLdcInsn(s.getId());
							mv.visitMethodInsn(Opcodes.INVOKESTATIC, QIFClassVisitor.DELEGATECLASS, "addQty",
									"(Ljava/lang/Object;Ljava/lang/String;)V", false);
						}
					}
				}
			}

			// if (isConstructor || isStatic)
			// mv.visitInsn(Opcodes.ACONST_NULL);
			// else
			// mv.visitVarInsn(Opcodes.ALOAD, 0);// first parameter is the
			// ownerobject

			// mv.visitLdcInsn(ownerclass.replace("/", "."));
			// mv.visitLdcInsn(ownermethod);
			// mv.visitVarInsn(Opcodes.ALOAD, parentObjIndex);// Load parentObj
			// ref
			// // mv.visitTypeInsn(Opcodes.CHECKCAST, "java/lang/Object");
			// mv.visitLdcInsn(parentclass.replace("/", ".")); // Load parent
			// // class name
			// mv.visitVarInsn(Opcodes.ALOAD, parentMethodIndex); // Load parent
			// // method name
			// mv.visitVarInsn(Opcodes.ALOAD, sinksourceIndex); // Load
			// // sinksource-ids
			// mv.visitVarInsn(Opcodes.ALOAD, chopLabelIndex); // Load choplabel
			// // onto the stack
			// mv.visitVarInsn(Opcodes.ALOAD, locArrayIndex);// Load method
			// // params array
			//
			// mv.visitMethodInsn(Opcodes.INVOKESTATIC,
			// MyUcTransformer.DELEGATECLASS, "sourceInvoked",
			// "(Ljava/lang/Object;Ljava/lang/Object;Ljava/lang/String;Ljava/lang/String;Ljava/lang/Object;Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;[Ljava/lang/Object;)Z",
			// false);
			// mv.visitInsn(Opcodes.POP);

			// Add return
			if (retT.getSort() == Type.OBJECT || retT.getSort() == Type.ARRAY || isConstructor) {
				mv.visitInsn(Opcodes.ARETURN);
			} else if (retT.getSort() == Type.DOUBLE) {
				mv.visitInsn(Opcodes.DRETURN);
			} else if (retT.getSort() == Type.FLOAT) {
				mv.visitInsn(Opcodes.FRETURN);
			} else if (retT.getSort() == Type.LONG) {
				mv.visitInsn(Opcodes.LRETURN);
			} else if (retT.getSort() == Type.INT || retT.getSort() == Type.BOOLEAN) {
				mv.visitInsn(Opcodes.IRETURN);
			} else {
				mv.visitInsn(Opcodes.RETURN);
			}

			Type[] myArgT = Type.getArgumentTypes(wrapperMethodDesc.toString());
			mv.visitMaxs(myArgT.length + 6, myArgT.length + 5);
			mv.visitEnd();
			// cv.visitEnd();
		} catch (Exception e) {
			_logger.error(e.getMessage());
		}
		return _return;
	}	

	public static String[] createArithWrapper(int opcode, ClassWriter cv, Label lab, Chop chopNode,
			String parentClass) {
		String[] _return = new String[2];
		StringBuilder wrapperMethodDesc = new StringBuilder();
		try {
//			System.out.println(Mnemonic.OPCODE[opcode]);
			// Create the wrapper method signature description as follows:
			// (0)=OP1,(2)=OP2 => OP3
			Type t = null;
			int returnOp = -1;
			int loadOp = -1;
			int subOp = -1;
			Class<?> boxClass = null;
			int sndVarSlot = 1;
			switch (opcode) {
			case Opcodes.IADD:
			case Opcodes.ISUB:
			case Opcodes.IMUL:
			case Opcodes.IDIV:
			case Opcodes.IREM:
			case Opcodes.ISHL:
			case Opcodes.ISHR:
			case Opcodes.IAND:
			case Opcodes.IOR:
			case Opcodes.IXOR:
				t = Type.INT_TYPE;
				returnOp = Opcodes.IRETURN;
				loadOp = Opcodes.ILOAD;
				subOp = Opcodes.ISUB;
				boxClass = Integer.class;
				break;
			case Opcodes.LADD:
			case Opcodes.LSUB:
			case Opcodes.LMUL:
			case Opcodes.LDIV:
			case Opcodes.LREM:
			case Opcodes.LSHL:
			case Opcodes.LSHR:
			case Opcodes.LAND:
			case Opcodes.LOR:
			case Opcodes.LXOR:
				t = Type.LONG_TYPE;
				returnOp = Opcodes.LRETURN;
				loadOp = Opcodes.LLOAD;
				subOp = Opcodes.LSUB;
				boxClass = Long.class;
				sndVarSlot = 2;
				break;
			case Opcodes.FADD:
			case Opcodes.FSUB:
			case Opcodes.FMUL:
			case Opcodes.FDIV:
			case Opcodes.FREM:
				t = Type.FLOAT_TYPE;
				returnOp = Opcodes.FRETURN;
				loadOp = Opcodes.FLOAD;
				subOp = Opcodes.FSUB;
				boxClass = Float.class;
				break;
			case Opcodes.DADD:
			case Opcodes.DSUB:
			case Opcodes.DMUL:
			case Opcodes.DDIV:
			case Opcodes.DREM:
				t = Type.DOUBLE_TYPE;
				returnOp = Opcodes.DRETURN;
				loadOp = Opcodes.DLOAD;
				subOp = Opcodes.DSUB;
				boxClass = Double.class;
				sndVarSlot = 2;
				break;
			}
			
			if(t==null) return _return;

//			assert (t != null); assert(returnOp != -1); assert(loadOp != -1); assert(subOp != -1);
			// Construct the wrapper method signature
			wrapperMethodDesc.append("(").append(t.getDescriptor()).append(t.getDescriptor()).append(")")
					.append(t.getDescriptor());
			String wrapperMethodName = Mnemonic.OPCODE[opcode] + "_" + lab.getOffset();

			_return[0] = wrapperMethodName;
			_return[1] = wrapperMethodDesc.toString();

			// Check if method invocation was already wrapped
			String id = parentClass + "." + wrapperMethodName + ":" + wrapperMethodDesc.toString();
			if (METHODS.containsKey(id)) {
				return _return;
			} else {
				METHODS.put(id, id);
			}

			// ==> Create wrapper method content
			MethodVisitor mv = cv.visitMethod(Opcodes.ACC_PRIVATE + Opcodes.ACC_STATIC, wrapperMethodName,
					wrapperMethodDesc.toString(), null, null);
			mv.visitCode();
			
			List<Flow> flows = StaticAnalysis.getFlowsByChopNode(chopNode);
			if (flows.size() > 0) {
				for (Flow f : flows) {
					for (String source : f.getSource()) {
//						Load operand parameter on the stack
						mv.visitVarInsn(loadOp, 0);
						mv.visitMethodInsn(Opcodes.INVOKESTATIC, boxClass.getName().replace(".", "/"), "valueOf", "("+t.getDescriptor()+")L"+boxClass.getName().replace(".", "/")+";",false);
						mv.visitVarInsn(loadOp, sndVarSlot);			
						mv.visitMethodInsn(Opcodes.INVOKESTATIC, boxClass.getName().replace(".", "/"), "valueOf", "("+t.getDescriptor()+")L"+boxClass.getName().replace(".", "/")+";",false);
						mv.visitLdcInsn(source);
						mv.visitLdcInsn(opcode);
						mv.visitMethodInsn(Opcodes.INVOKESTATIC, QIFClassVisitor.DELEGATECLASS, "decArithQty",
								"(Ljava/lang/Object;Ljava/lang/Object;Ljava/lang/String;I)V", false);
					}
				}
			}
			
//			Load operands on the stack and execute the original instruction
			mv.visitVarInsn(loadOp, 0);
			mv.visitVarInsn(loadOp, sndVarSlot);			
			mv.visitInsn(opcode);
			mv.visitInsn(returnOp);
			
			Type[] myArgT = Type.getArgumentTypes(wrapperMethodDesc.toString());
			mv.visitMaxs(myArgT.length + 6, myArgT.length + 5);
			mv.visitEnd();
		} catch (Exception e) {
			e.printStackTrace();
			_logger.error(e.getMessage());
		}
		return _return;
	}
	
	public static String[] createMethodWrapper(int opcode, String ownerclass, String ownermethod,
			String descOwnermethod, ClassWriter cv, String parentclass, Chop chopNode) {
		boolean isConstructor = opcode == Opcodes.INVOKESPECIAL && ownermethod.equals("<init>");
		boolean isStatic = opcode == Opcodes.INVOKESTATIC;
		String[] _return = new String[2];
		StringBuilder wrapperMethodDesc = new StringBuilder();
		try {
			// Create the wrapper method signature description as follows:
			// (0)=object,(1...n-1)=parameters,(n)=source-id
			// Create method description of the to be invoked methods
			wrapperMethodDesc.append("(");
			int argIndex = -1;
			// if wrapped method is an instance method invocation, then add an
			// additional parameter for the object reference
			if (!isConstructor && !isStatic) {
				wrapperMethodDesc.append("L" + ownerclass + ";");// append("Ljava/lang/Object;");
				++argIndex;
			}

			// Helper variable to store the correct parameter index within the
			// local variable table
			int paramStartIndex = 0;
			Type[] oriArgT = Type.getArgumentTypes(descOwnermethod);
			if (oriArgT.length > 0) {
				paramStartIndex = argIndex + 1;
				for (Type t : oriArgT) {
					wrapperMethodDesc.append(t.getDescriptor());
					argIndex++;
					if (Type.DOUBLE == t.getSort() || Type.LONG == t.getSort()) {
						argIndex++;
					}
				}
			}
			// Parent-object index
			 wrapperMethodDesc.append("Ljava/lang/String;");
			 int srcIndex = ++argIndex;

			// Parent-method index
			// wrapperMethodDesc.append("Ljava/lang/String;");
			// int parentMethodIndex = ++argIndex;
			 
			// sourceindex
			// wrapperMethodDesc.append("Ljava/lang/String;");
			// int sinksourceIndex = ++argIndex;
			 
			// chopLabel index
			// wrapperMethodDesc.append("Ljava/lang/String;");
			// int chopLabelIndex = ++argIndex;

			wrapperMethodDesc.append(")");

			Type retT = Type.getReturnType(descOwnermethod);
			if (isConstructor) {
				wrapperMethodDesc.append("L" + ownerclass + ";");
			} else if (retT != null) {
				wrapperMethodDesc.append(retT.getDescriptor());
			}

			// Constructors are renamed
			String wrapperMethodName = "_" + ownermethod + "_" + chopNode.getByteCodeIndex();
			if (isConstructor) {
				wrapperMethodName = ownerclass.replace("/", "_") + "_init";
			}
			_return[0] = wrapperMethodName;
			_return[1] = wrapperMethodDesc.toString();

			// Check if method invocation was already wrapped
			String id = parentclass + "." + wrapperMethodName + ":" + wrapperMethodDesc.toString();
			if (METHODS.containsKey(id)) {
				return _return;
			} else {
				METHODS.put(id, id);
			}

			// ==> Create wrapper method content
			MethodVisitor mv = cv.visitMethod(Opcodes.ACC_PRIVATE + Opcodes.ACC_STATIC, wrapperMethodName,
					wrapperMethodDesc.toString(), null, null);
			mv.visitCode();

			int tmpString1= -1;
			int nextLVSlot = 0;
			if(ownerclass.toLowerCase().equals("java/lang/stringbuilder") && ownermethod.toLowerCase().equals("append")){
				if(!isConstructor){
					for(Type t : Type.getType(wrapperMethodDesc.toString()).getArgumentTypes()){
						nextLVSlot += t.getSize();
					}
					mv.visitVarInsn(Opcodes.ALOAD,0);
					mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, StringBuilder.class.getName().replace(".", "/"), "toString", "()Ljava/lang/String;", false);					
					mv.visitVarInsn(Opcodes.ASTORE, (tmpString1 = ++nextLVSlot));
				}
			}

			// --> Execute the original method
			if (isConstructor) {
				mv.visitTypeInsn(Opcodes.NEW, ownerclass);
				mv.visitInsn(Opcodes.DUP);
			} else if (!isStatic) {
				mv.visitVarInsn(Opcodes.ALOAD, 0);
			}
			int i = paramStartIndex;
			for (Type t : oriArgT) {
				if (t.getSort() == Type.OBJECT) {
					mv.visitVarInsn(Opcodes.ALOAD, i);
				} else if (t.getSort() == Type.ARRAY) {
					mv.visitVarInsn(Opcodes.ALOAD, i);
				} else if (t.getSort() == Type.DOUBLE) {
					mv.visitVarInsn(Opcodes.DLOAD, i);
					i++;
				} else if (t.getSort() == Type.FLOAT) {
					mv.visitVarInsn(Opcodes.FLOAD, i);
				} else if (t.getSort() == Type.LONG) {
					mv.visitVarInsn(Opcodes.LLOAD, i);
					i++;
				} else if ((t.getSort() == Type.INT) || (t.getSort() == Type.CHAR)) {
					mv.visitVarInsn(Opcodes.ILOAD, i);
				}
				i++;
			}

			mv.visitMethodInsn(opcode, ownerclass, ownermethod, descOwnermethod, opcode == Opcodes.INVOKEINTERFACE);
			// <-- Execute the original method			
			
			if(ownerclass.toLowerCase().equals("java/lang/stringbuilder") && ownermethod.toLowerCase().equals("append")){
				if(!isConstructor && tmpString1>0){
					mv.visitInsn(Opcodes.DUP);
					mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, StringBuilder.class.getName().replace(".", "/"), "toString", "()Ljava/lang/String;", false);					
					mv.visitVarInsn(Opcodes.ALOAD, tmpString1);
					mv.visitVarInsn(Opcodes.ALOAD, srcIndex);
					mv.visitMethodInsn(Opcodes.INVOKESTATIC, QIFClassVisitor.DELEGATECLASS, "decStringQty", "(Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;)V", false);
				}
			}
			else if(ownerclass.toLowerCase().equals("java/lang/string") && ownermethod.toLowerCase().contains("replace")){
				mv.visitInsn(Opcodes.DUP);
				mv.visitVarInsn(Opcodes.ALOAD, 0);
				mv.visitVarInsn(Opcodes.ALOAD, srcIndex);
				mv.visitMethodInsn(Opcodes.INVOKESTATIC, QIFClassVisitor.DELEGATECLASS, "decStringQty", "(Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;)V", false);
			}
			else if(ownerclass.toLowerCase().equals("java/lang/string") && (ownermethod.toLowerCase().contains("subsequence") || ownermethod.toLowerCase().contains("substr"))){
				mv.visitInsn(Opcodes.DUP);
				mv.visitTypeInsn(Opcodes.CHECKCAST, String.class.getName().replace(".", "/"));
				mv.visitVarInsn(Opcodes.ALOAD, 0);
				mv.visitVarInsn(Opcodes.ALOAD, srcIndex);
				mv.visitMethodInsn(Opcodes.INVOKESTATIC, QIFClassVisitor.DELEGATECLASS, "decSubSequQty", "(Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;)V", false);
			}			
			else if(ownerclass.toLowerCase().equals("java/lang/string") && ownermethod.toLowerCase().contains("split")){
				mv.visitInsn(Opcodes.DUP);
				mv.visitVarInsn(Opcodes.ALOAD, srcIndex);
				mv.visitMethodInsn(Opcodes.INVOKESTATIC, QIFClassVisitor.DELEGATECLASS, "decSplitQty", "([Ljava/lang/String;Ljava/lang/String;)V", false);
			}

			// Add return
			if (retT.getSort() == Type.OBJECT || retT.getSort() == Type.ARRAY || isConstructor) {
				mv.visitInsn(Opcodes.ARETURN);
			} else if (retT.getSort() == Type.DOUBLE) {
				mv.visitInsn(Opcodes.DRETURN);
			} else if (retT.getSort() == Type.FLOAT) {
				mv.visitInsn(Opcodes.FRETURN);
			} else if (retT.getSort() == Type.LONG) {
				mv.visitInsn(Opcodes.LRETURN);
			} else if (retT.getSort() == Type.INT || retT.getSort() == Type.BOOLEAN) {
				mv.visitInsn(Opcodes.IRETURN);
			} else {
				mv.visitInsn(Opcodes.RETURN);
			}

			Type[] myArgT = Type.getArgumentTypes(wrapperMethodDesc.toString());
			mv.visitMaxs(myArgT.length + 6, myArgT.length + 5);
			mv.visitEnd();
			// cv.visitEnd();
		} catch (Exception e) {
			_logger.error(e.getMessage());
		}
		return _return;
	}	

}
