package edu.tum.uc.jvm.instrum;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;

import edu.tum.uc.jvm.MyUcTransformer;
import edu.tum.uc.jvm.checker.RequestCheck;
import edu.tum.uc.jvm.declassification.Declassifier;
import edu.tum.uc.jvm.utility.ConfigProperties;
import edu.tum.uc.jvm.utility.Utility;
import edu.tum.uc.jvm.utility.analysis.SinkSource;

public class InstrumMethodWrapper {

	private static String CHECKERCLASS = RequestCheck.class.getName().replaceAll(".", "/");
	
	static Map<String, String> METHODS = new HashMap<String, String>();

//	public static String[] createSourceWrapper(int p_opcode, String p_ownerclass, String p_ownermethod,
//			String p_descownermethod, ClassWriter cv, String p_parentclass, List<SinkSource> p_sources) {
//		return createSourceWrapper(p_opcode, p_ownerclass, p_ownermethod, p_descownermethod, cv, p_parentclass,
//				p_sources, null);
//	}

	public static String[] createSourceWrapper(int p_opcode, String p_ownerclass, String p_ownermethod,
			String p_descownermethod, ClassWriter cv, String p_parentclass, List<SinkSource> p_sources//,List<MethodLabel> methodLabel
			) {
		boolean isConstructor = p_opcode == Opcodes.INVOKESPECIAL && p_ownermethod.equals("<init>");
		boolean isStatic = p_opcode == Opcodes.INVOKESTATIC;
		String[] _return = new String[2];
		StringBuilder wrapperMethodDesc = new StringBuilder();
		try {
			// ==> Preprocessing, create required method description, indizes,
			// Create method description of the to be invoked methods
			wrapperMethodDesc.append("(");
			int argIndex = -1;
			if (!isConstructor && !isStatic) {
				wrapperMethodDesc.append("L" + p_ownerclass + ";");// append("Ljava/lang/Object;");
				++argIndex;
			} // count the number of parameters the wrapper method has,
				// (0)=object,(1...n-1)=parameters,(n)=source-id

			// Helper variable to store the correct parameter index within the
			// local variable table
			int paramStartIndex = 0;
			Type[] argT = Type.getArgumentTypes(p_descownermethod);
			if (argT.length > 0) {
				paramStartIndex = argIndex + 1;
				for (Type t : argT) {
					wrapperMethodDesc.append(t.getDescriptor());
					argIndex++;
					if (Type.DOUBLE == t.getSort() || Type.LONG == t.getSort()) {
						argIndex++;
					}
				}
			}
			// Parent-object index
			wrapperMethodDesc.append("Ljava/lang/Object;");
			int parentObjIndex = ++argIndex;

			// Parent-method index
			wrapperMethodDesc.append("Ljava/lang/String;");
			int parentMethodIndex = ++argIndex;

			// sourceindex
			wrapperMethodDesc.append("Ljava/lang/String;");
			int sinksourceIndex = ++argIndex;

			// chopLabel index
			wrapperMethodDesc.append("Ljava/lang/String;");
			int chopLabelIndex = ++argIndex;

			wrapperMethodDesc.append(")");

			Type retT = Type.getReturnType(p_descownermethod);
			if (isConstructor) {
				wrapperMethodDesc.append("L" + p_ownerclass + ";");
			} else if (retT != null) {
				wrapperMethodDesc.append(retT.getDescriptor());
			}

			// Constructors are renamed
			String wrapperMethodName = p_ownermethod;
			if (isConstructor) {
				wrapperMethodName = p_ownerclass.replace("/", "_") + "_init";
			}
			_return[0] = wrapperMethodName;
			_return[1] = wrapperMethodDesc.toString();

			// Check if method invocation was already wrapped
			String id = p_parentclass + "." + wrapperMethodName + ":" + wrapperMethodDesc.toString();
			if (METHODS.containsKey(id)) {
				return _return;
			} else {
				METHODS.put(id, id);
			}

			// ==> Create wrapper method content
			MethodVisitor mv = cv.visitMethod(Opcodes.ACC_PUBLIC + Opcodes.ACC_STATIC, wrapperMethodName,
					wrapperMethodDesc.toString(), null, null);
			mv.visitCode();

			// --> Load all parameters into an array
			mv.visitLdcInsn(argT.length);
			mv.visitTypeInsn(Opcodes.ANEWARRAY, "java/lang/Object");
			// Store it after all method params
			int paramArrayIndex = ++argIndex;
			mv.visitVarInsn(Opcodes.ASTORE, paramArrayIndex);
			int i = paramStartIndex; // local variable index counter
			int j = 0; // array entry counter
			for (Type argType : argT) {
				mv.visitVarInsn(Opcodes.ALOAD, paramArrayIndex);
				mv.visitLdcInsn(j);
				if (argType.getSort() == Type.OBJECT) {
					mv.visitVarInsn(Opcodes.ALOAD, i);
				} else if (argType.getSort() == Type.ARRAY) {
					mv.visitVarInsn(Opcodes.ALOAD, i);
				} else {
					if (argType.getSort() == Type.DOUBLE) {
						mv.visitVarInsn(Opcodes.DLOAD, i);
						i++;
					} else if (argType.getSort() == Type.FLOAT) {
						mv.visitVarInsn(Opcodes.FLOAD, i);
					} else if (argType.getSort() == Type.LONG) {
						mv.visitVarInsn(Opcodes.LLOAD, i);
						i++;
					} else if (argType.getSort() == Type.INT) {
						mv.visitVarInsn(Opcodes.ILOAD, i);
					} else if (argType.getSort() == Type.CHAR) {
						mv.visitVarInsn(Opcodes.ILOAD, i);
					} else if (argType.getSort() == Type.BYTE) {
						mv.visitVarInsn(Opcodes.ILOAD, i);
					} else if (argType.getSort() == Type.BOOLEAN) {
						mv.visitVarInsn(Opcodes.ILOAD, i);
					} else if (argType.getSort() == Type.SHORT) {
						mv.visitVarInsn(Opcodes.ILOAD, i);
					}
					Utility.boxTopStackValue(mv, argType);
				}
				mv.visitInsn(Opcodes.AASTORE);
				i++;
				j++;
			}
			// <-- Load all parameters into an array

			// --> Execute the original method
			if (isConstructor) {
				mv.visitTypeInsn(Opcodes.NEW, p_ownerclass);
				mv.visitInsn(Opcodes.DUP);
			} else if (!isStatic) {
				mv.visitVarInsn(Opcodes.ALOAD, 0);
			}
			i = paramStartIndex;
			for (Type t : argT) {
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

			mv.visitMethodInsn(p_opcode, p_ownerclass, p_ownermethod, p_descownermethod,
					p_opcode == Opcodes.INVOKEINTERFACE);
			// <-- Execute the original method
			
			if (!isConstructor && !isStatic) {
				mv.visitVarInsn(Opcodes.ALOAD, 0);
				mv.visitVarInsn(Opcodes.ALOAD, paramArrayIndex);
				mv.visitMethodInsn(Opcodes.INVOKESTATIC, CHECKERCLASS, "parseObjectBool",
						"(Ljava/lang/Object;[Ljava/lang/Object;)Z", false);
				mv.visitInsn(Opcodes.POP);
				// mv.visitInsn(Opcodes.ICONST_0);
				// mv.visitMethodInsn(Opcodes.INVOKESTATIC,
				// System.class.getName().replace(".", "/"), "exit", "(I)V",
				// false);
			}
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

			// Send event to pdp
			if (p_sources.size() > 0) {
				for (SinkSource s : p_sources) {
					if (s.isReturn()) {
						if (isConstructor) {
							mv.visitInsn(Opcodes.DUP);
						} else if (retT.getSort() != Type.VOID) {
							boolean retValueIsBig = retT.getSize() == 2;
							if (retValueIsBig) {
								mv.visitInsn(Opcodes.DUP2);
							} else {
								mv.visitInsn(Opcodes.DUP);
							}
							Utility.boxTopStackValue(mv, retT);
						} else {
							mv.visitInsn(Opcodes.ACONST_NULL);
						}
					} else {
						int param = s.getParam();
						if (param > 0) {
							i = paramStartIndex + param - 1;
							Type t = argT[param - 1];
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
							break;
						} else {
							mv.visitInsn(Opcodes.ACONST_NULL);
						}
					}
				}
			} else {
				mv.visitInsn(Opcodes.ACONST_NULL);
			}

			if (isConstructor || isStatic)
				mv.visitInsn(Opcodes.ACONST_NULL);
			else
				mv.visitVarInsn(Opcodes.ALOAD, 0);// first parameter is the
													// ownerobject
			mv.visitLdcInsn(p_ownerclass.replace("/", "."));
			mv.visitLdcInsn(p_ownermethod);
			mv.visitVarInsn(Opcodes.ALOAD, parentObjIndex);// Load parentObj ref
			// mv.visitTypeInsn(Opcodes.CHECKCAST, "java/lang/Object");
			mv.visitLdcInsn(p_parentclass.replace("/", ".")); // Load parent
																// class name
			mv.visitVarInsn(Opcodes.ALOAD, parentMethodIndex); // Load parent
																// method name
			mv.visitVarInsn(Opcodes.ALOAD, sinksourceIndex); // Load
																// sinksource-ids
			mv.visitVarInsn(Opcodes.ALOAD, chopLabelIndex); // Load choplabel
															// onto the stack
			mv.visitVarInsn(Opcodes.ALOAD, paramArrayIndex);// Load method
															// params array
			String label = "";
//			if (methodLabel != null && methodLabel.size() > 0) {
//				label = methodLabel.get(0).idText;
//			}
			mv.visitLdcInsn(label);

			mv.visitMethodInsn(Opcodes.INVOKESTATIC, MyUcTransformer.DELEGATECLASS, "sourceInvoked",
					"(Ljava/lang/Object;Ljava/lang/Object;Ljava/lang/String;Ljava/lang/String;Ljava/lang/Object;Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;[Ljava/lang/Object;Ljava/lang/String;)Z",
					false);
			mv.visitInsn(Opcodes.POP);

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
		}
		return _return;
	}

//	public static String[] createSinkWrapper(int p_opcode, String p_ownerclass, String p_ownermethod,
//			String p_descownermethod, ClassWriter cv, String p_parentclass, List<SinkSource> p_sinks) {
//		return createSinkWrapper(p_opcode, p_ownerclass, p_ownermethod, p_descownermethod, cv, p_parentclass, p_sinks,null
//				);
//	}

	public static String[] createSinkWrapper(int p_opcode, String p_ownerclass, String p_ownermethod,
			String p_descownermethod, ClassWriter cv, String p_parentclass, List<SinkSource> p_sinks//,List<MethodLabel> methodLabel
			) {

		String[] _return = new String[2];
		boolean isConstructor = p_opcode == Opcodes.INVOKESPECIAL && p_ownermethod.equals("<init>");
		boolean isStatic = p_opcode == Opcodes.INVOKESTATIC;
		StringBuilder wrapperMethodDesc = new StringBuilder();
		try {
			// Preprocessing, create required method description, indizes, etc
			// Create method description of the to be invoked methods
			wrapperMethodDesc.append("(");
			int argIndex = -1;
			if (!isConstructor && !isStatic) {
				wrapperMethodDesc.append("L" + p_ownerclass + ";");// append("Ljava/lang/Object;");
				++argIndex;
			}
			// count the number of parameters the wrapper method has,

			// Helper variable to store the correct parameter index within the
			// local variable table
			int paramStartIndex = 0;
			Type[] argT = Type.getArgumentTypes(p_descownermethod);
			if (argT.length > 0) {
				paramStartIndex = argIndex + 1;
				for (Type t : argT) {
					wrapperMethodDesc.append(t.getDescriptor());
					if (Type.DOUBLE == t.getSort() || Type.LONG == t.getSort()) {
						argIndex++;
					}
					++argIndex;
				}
			}

			// Parent-object index
			wrapperMethodDesc.append("Ljava/lang/Object;");
			int parentObjIndex = ++argIndex;

			// Parent-method index
			wrapperMethodDesc.append("Ljava/lang/String;");
			int parentMethodIndex = ++argIndex;

			// sinksourceindex
			wrapperMethodDesc.append("Ljava/lang/String;");
			int sinksourceIndex = ++argIndex;

			// chopLabel index
			wrapperMethodDesc.append("Ljava/lang/String;");
			int chopLabelIndex = ++argIndex;

			wrapperMethodDesc.append(")");

			Type retT = Type.getReturnType(p_descownermethod);
			if (isConstructor) {
				wrapperMethodDesc.append("L" + p_ownerclass + ";");
			} else if (retT != null) {
				wrapperMethodDesc.append(retT.getDescriptor());
			}

			// Constructors are renamed
			String wrapperMethodName = p_ownermethod;
			if (isConstructor) {
				wrapperMethodName = p_ownerclass.replace("/", "_") + "_init";
			}
			_return[0] = wrapperMethodName;
			_return[1] = wrapperMethodDesc.toString();

			// Check if method invocation was already wrapped
			String id = p_parentclass + "." + wrapperMethodName + ":" + wrapperMethodDesc.toString();
			if (METHODS.containsKey(id)) {
				return _return;
			} else {
				METHODS.put(id, id);
			}

			// ==> Create wrapper method content
			MethodVisitor mv = cv.visitMethod(Opcodes.ACC_PUBLIC + Opcodes.ACC_STATIC, wrapperMethodName,
					wrapperMethodDesc.toString(), null, null);
			mv.visitCode();

			// Create array to fit all arguments
			mv.visitLdcInsn(argT.length);
			mv.visitTypeInsn(Opcodes.ANEWARRAY, "java/lang/Object");
			// Store it after all method params
			int paramArrayIndex = ++argIndex;
			mv.visitVarInsn(Opcodes.ASTORE, paramArrayIndex);

			int i = paramStartIndex; // local variable index counter
			int j = 0; // array entry counter
			for (Type argType : argT) {
				mv.visitVarInsn(Opcodes.ALOAD, paramArrayIndex);
				mv.visitLdcInsn(j);
				if (argType.getSort() == Type.OBJECT) {
					mv.visitVarInsn(Opcodes.ALOAD, i);
				} else if (argType.getSort() == Type.ARRAY) {
					mv.visitVarInsn(Opcodes.ALOAD, i);
				} else {
					if (argType.getSort() == Type.DOUBLE) {
						mv.visitVarInsn(Opcodes.DLOAD, i);
						i++;
					} else if (argType.getSort() == Type.FLOAT) {
						mv.visitVarInsn(Opcodes.FLOAD, i);
					} else if (argType.getSort() == Type.LONG) {
						mv.visitVarInsn(Opcodes.LLOAD, i);
						i++;
					} else if (argType.getSort() == Type.INT) {
						mv.visitVarInsn(Opcodes.ILOAD, i);
					} else if (argType.getSort() == Type.CHAR) {
						mv.visitVarInsn(Opcodes.ILOAD, i);
					} else if (argType.getSort() == Type.BYTE) {
						mv.visitVarInsn(Opcodes.ILOAD, i);
					} else if (argType.getSort() == Type.BOOLEAN) {
						mv.visitVarInsn(Opcodes.ILOAD, i);
					} else if (argType.getSort() == Type.SHORT) {
						mv.visitVarInsn(Opcodes.ILOAD, i);
					}
					Utility.boxTopStackValue(mv, argType);
				}

				mv.visitInsn(Opcodes.AASTORE);
				i++;
				j++;
			}

			// Send an event to the pdp
			if (isConstructor) {
				mv.visitInsn(Opcodes.ACONST_NULL);
			} else if (!isStatic) {
				mv.visitVarInsn(Opcodes.ALOAD, 0);// first parameter is the
													// ownerobject
			}
			mv.visitLdcInsn(p_ownerclass.replace("/", "."));
			mv.visitLdcInsn(p_ownermethod);
			mv.visitVarInsn(Opcodes.ALOAD, paramArrayIndex);// Load method
															// params
			mv.visitVarInsn(Opcodes.ALOAD, parentObjIndex);// Load parentObj ref
			mv.visitLdcInsn(p_parentclass.replace("/", ".")); // Load parent
																// class name
			mv.visitVarInsn(Opcodes.ALOAD, parentMethodIndex); // Load parent
																// method name
			mv.visitVarInsn(Opcodes.ALOAD, sinksourceIndex); // Load
																// sinksource-ids
			mv.visitVarInsn(Opcodes.ALOAD, chopLabelIndex);

			String label = "";
//			if (methodLabel != null && methodLabel.size() > 0) {
//				label = methodLabel.get(0).idText;
//			}
			mv.visitLdcInsn(label);

			mv.visitMethodInsn(Opcodes.INVOKESTATIC, MyUcTransformer.DELEGATECLASS, "sinkInvoked",
					"(Ljava/lang/Object;Ljava/lang/String;Ljava/lang/String;[Ljava/lang/Object;Ljava/lang/Object;Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;)Z",
					false);
			mv.visitInsn(Opcodes.POP);
			// Label elseLab = new Label();
			// mv.visitJumpInsn(Opcodes.IFNE, elseLab);
			//// in case invoked sink is not allowed overwrite the respective
			// sind parameter
			// boolean issink = false;
			// i = paramStartIndex;
			// for (Type t : argT) {
			// // Check if sink parameter must be desclassified
			// for (SinkSource s : p_sinks) {
			// if (s.getParam() == i) {
			// issink = true;
			// }
			// }
			// if(!issink){
			// i++;
			// continue;
			// }
			// if (t.getSort() == Type.OBJECT) {
			// mv.visitVarInsn(Opcodes.ALOAD, i);
			// mv.visitMethodInsn(Opcodes.INVOKESTATIC,
			// Declassifier.class.getName().replace(".", "/"), "declassify",
			// "(Ljava/lang/Object;)Ljava/lang/Object;", false);
			// mv.visitInsn(Opcodes.POP);
			// } else if (t.getSort() == Type.ARRAY) {
			// mv.visitIntInsn(Opcodes.BIPUSH, 0);
			// mv.visitTypeInsn(Opcodes.ANEWARRAY,
			// Object.class.getName().replace(".", "/"));
			// mv.visitVarInsn(Opcodes.ASTORE, i);
			// } else if (t.getSort() == Type.DOUBLE) {
			// mv.visitLdcInsn(0D);
			// mv.visitVarInsn(Opcodes.DSTORE, i);
			// i++;
			// } else if (t.getSort() == Type.FLOAT) {
			// mv.visitLdcInsn(0f);
			// mv.visitVarInsn(Opcodes.FSTORE, i);
			// } else if (t.getSort() == Type.LONG) {
			// mv.visitLdcInsn(0L);
			// mv.visitVarInsn(Opcodes.LSTORE, i);
			// i++;
			// } else if ((t.getSort() == Type.INT) || (t.getSort() ==
			// Type.CHAR)) {
			// mv.visitIntInsn(Opcodes.BIPUSH, 0);
			// mv.visitVarInsn(Opcodes.ISTORE, i);
			// }
			// i++;
			// break;
			// }
			// mv.visitLabel(elseLab);

			// Preprocessing, to execute the original method
			if (isConstructor) {
				mv.visitTypeInsn(Opcodes.NEW, p_ownerclass);
				mv.visitInsn(Opcodes.DUP);
			} else if (!isStatic) {
				mv.visitVarInsn(Opcodes.ALOAD, 0);
			}

			boolean issink = false;
			i = paramStartIndex;
			for (Type t : argT) {
				// Check if sink parameter must be desclassified
				for (SinkSource s : p_sinks) {
					if (s.getParam() == i && t.getSort() == Type.OBJECT
							&& t.getClassName().equals(String.class.getName())) {
						issink = true;
					}
				}

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

				if (issink == true) {
					boolean declassify = Boolean
							.valueOf(ConfigProperties.getProperty(ConfigProperties.PROPERTIES.DECLSSIFY));
					if (declassify) {
						mv.visitMethodInsn(Opcodes.INVOKESTATIC, Declassifier.class.getName().replace(".", "/"),
								"declassify", "(Ljava/lang/String;)Ljava/lang/String;", false);
					}
					issink = false;
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

			// Execute original method
			mv.visitMethodInsn(p_opcode, p_ownerclass, p_ownermethod, p_descownermethod,
					p_opcode == Opcodes.INVOKEINTERFACE);

			// if (timer4) {
			// mv.visitVarInsn(Opcodes.ALOAD, paramIndex);// myArgT.length -
			// // 1);
			// mv.visitMethodInsn(Opcodes.INVOKESTATIC,
			// UcTransformer.HOOKMETHOD, "timerT4Stop",
			// "(Ljava/lang/String;)V", false);
			// }

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
			mv.visitMaxs(myArgT.length + 5, myArgT.length + 5);
			mv.visitEnd();
		} catch (Exception e) {
		}

		return _return;
	}

}
