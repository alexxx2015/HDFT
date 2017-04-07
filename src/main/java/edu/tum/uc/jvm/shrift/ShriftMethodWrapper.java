package edu.tum.uc.jvm.shrift;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;

import edu.tum.uc.jvm.UcTransformer;
import edu.tum.uc.jvm.utility.ConfigProperties;
import edu.tum.uc.jvm.utility.Utility;
import edu.tum.uc.jvm.utility.analysis.SinkSource;

public class ShriftMethodWrapper {

	static Map<String, String> METHODS = new HashMap<String, String>();

	/**
	 * Creates a dummy method that extracts the method for file descriptor
	 * 
	 * @param p_opcode
	 *            Java Bytecode opcode to invoke instruction
	 * @param p_owner_classname
	 *            Owner class where of invoked method p_name
	 * @param p_owner_methodname
	 *            Method name of invoked method that has to be replaced
	 * @param p_desc
	 *            Java bytecode method signature
	 * @param cv
	 *            ClassVisitor
	 * @param classname
	 *            The classname where the method is invoked
	 * @param sors
	 *            Contains sink or source specification
	 * @return
	 */
	public static String createASMHelperMethod(int p_opcode, String p_owner_classname, String p_owner_methodname,
			String p_desc, ClassWriter cv, String classname, List<SinkSource> sors) {
		StringBuilder desc = new StringBuilder();
		try {
			// Class<?> clazz = DummyHelperClass.class;
			// String className = clazz.getName().replace(".",
			// System.getProperty("file.separator")) + ".class";
			// InputStream is =
			// clazz.getClassLoader().getResourceAsStream(className);
			// byte[] b = IOUtils.toByteArray(is);
	
			// Generate new method signature
			Type[] argT = Type.getArgumentTypes(p_desc);
	
			// Create method description of the to be invoked methods
			desc.append("(");
			// desc.append("Ljava/lang/Object;");
			desc.append("L" + p_owner_classname + ";");
	
			// Helper variable to store the correct parameter index within the
			// local variable table
			int paramIndex = 0;
			if (argT.length > 0) {
				for (Type t : argT) {
					desc.append(t.getDescriptor());
					paramIndex++;
					if (Type.DOUBLE == t.getSort() || Type.LONG == t.getSort()) {
						paramIndex++;
					}
				}
			}
			desc.append("Ljava/lang/String;");
			paramIndex++;
			desc.append(")");
			Type retT = Type.getReturnType(p_desc);
			if (retT != null) {
				desc.append(retT.getDescriptor());
			}
			Type[] myArgT = Type.getArgumentTypes(desc.toString());
	
			if (p_owner_methodname.equals("<init>")) {
				// p_owner_methodname = "Lpackage_class_init"
				p_owner_methodname = p_owner_classname.replace("/", "_") + "_init";
			}
			String id = classname + "." + p_owner_methodname + ":" + desc.toString();
			if (METHODS.containsKey(id)) {
				return desc.toString();
			} else {
				METHODS.put(id, id);
			}
	
			// MyClassReader cr = new MyClassReader(b);
			// ClassReader cr = new ClassReader(b);
			// ClassWriter cw = new ClassWriter(cv, ClassWriter.COMPUTE_MAXS |
			// ClassWriter.COMPUTE_FRAMES);
	
			// Create a new asm-method instance
			MethodVisitor mv = cv.visitMethod(Opcodes.ACC_PUBLIC + Opcodes.ACC_STATIC, p_owner_methodname,
					desc.toString(), null, null);
			mv.visitCode();
	
			// Set<Integer> ks = sors.keySet();
			// Iterator<Integer> ksIt = ks.iterator();
			// while (ksIt.hasNext()) {
			// int parameter = ksIt.next();
			// if (sors.get(parameter).equals(StaticAnalysis.nodeType.SOURCE)) {
			// if (myArgT[parameter].getSort() == Type.OBJECT) {
			// mv.visitVarInsn(Opcodes.ALOAD, parameter);
			// } else if (myArgT[parameter].getSort() == Type.ARRAY) {
			// mv.visitVarInsn(Opcodes.ALOAD, parameter);
			// } else if (myArgT[parameter].getSort() == Type.DOUBLE) {
			// mv.visitVarInsn(Opcodes.DLOAD, parameter);
			// } else if (myArgT[parameter].getSort() == Type.FLOAT) {
			// mv.visitVarInsn(Opcodes.FLOAD, parameter);
			// } else if (myArgT[parameter].getSort() == Type.LONG) {
			// mv.visitVarInsn(Opcodes.LLOAD, parameter);
			// } else if (myArgT[parameter].getSort() == Type.INT) {
			// mv.visitVarInsn(Opcodes.ILOAD, parameter);
			// }
			// } else {
			mv.visitVarInsn(Opcodes.ALOAD, 0);// ALOAD_0 -> the first parameter
												// is the object reference }
			mv.visitTypeInsn(Opcodes.CHECKCAST, "java/lang/Object");
			mv.visitVarInsn(Opcodes.ALOAD, paramIndex);
			mv.visitMethodInsn(Opcodes.INVOKESTATIC, UcTransformer.HOOKMETHOD, "methodInvoked",
					"(Ljava/lang/Object;Ljava/lang/String;)Z", false);
			mv.visitInsn(Opcodes.POP);
			// }
	
			// mv.visitVarInsn(Opcodes.ALOAD, 0);
			// mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/lang/Object",
			// "getClass", "()Ljava/lang/Class;",false);
			// mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/lang/Class",
			// "toString", "()Ljava/lang/String;",false);
			mv.visitVarInsn(Opcodes.ALOAD, 0);
			int i = 1;
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
					;
					i++;
				} else if ((t.getSort() == Type.INT) || (t.getSort() == Type.CHAR)) {
					mv.visitVarInsn(Opcodes.ILOAD, i);
				}
				i++;
			}
			Boolean timer4 = new Boolean(ConfigProperties.getProperty(ConfigProperties.PROPERTIES.TIMER_T4));
			if (timer4) {
				mv.visitMethodInsn(Opcodes.INVOKESTATIC, UcTransformer.HOOKMETHOD, "timerT4Start", "()V", false);
			}
	
			mv.visitMethodInsn(p_opcode, p_owner_classname, p_owner_methodname, p_desc, false);
	
			if (timer4) {
				mv.visitVarInsn(Opcodes.ALOAD, paramIndex);// myArgT.length -
															// 1);
				mv.visitMethodInsn(Opcodes.INVOKESTATIC, UcTransformer.HOOKMETHOD, "timerT4Stop",
						"(Ljava/lang/String;)V", false);
			}
	
			// mv.visitVarInsn(Opcodes.ALOAD, 0);// ALOAD_0 -> the first
			// parameter is the object reference
			// mv.visitTypeInsn(Opcodes.CHECKCAST, "java/lang/Object");
			// mv.visitVarInsn(Opcodes.ALOAD, paramIndex);// myArgT.length - 1);
			// mv.visitMethodInsn(Opcodes.INVOKESTATIC,
			// UcTransformer.HOOKMETHOD,"methodExited",
			// "(Ljava/lang/Object;Ljava/lang/String;)V",false);
	
			// Add return
			if (retT.getSort() == Type.OBJECT || retT.getSort() == Type.ARRAY) {
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
	
			mv.visitMaxs(myArgT.length, myArgT.length);
			mv.visitEnd();
			cv.visitEnd();
	
			// cr.accept(cw, 0);
			// ClassLoader parent = DummyHelperClass.class.getClassLoader();
			// DummyHelperClassLoader mcl = new
			// DummyHelperClassLoader(parent.getParent());
			// Class<?> myTest2Clazz =
			// mcl.define("edu.tum.uc.jvm.utility.DummyHelperClass",
			// cw.toByteArray());
	
			// Method[] methods = ClassLoader.class.getDeclaredMethods();
			// for(Method m : methods){
			// if(m.getName().toLowerCase().equals("defineclass") &&
			// (m.getParameterTypes().length == 4)){
			// m.setAccessible(true);
			// m.invoke(parent,
			// "edu.tum.uc.jvm.utility.DummyHelperClass",cw.toByteArray(),0,cw.toByteArray().length);
			// }
			// }
			// scl.setAccessible(true);
			// scl.set(null, mcl);
			// Object obj = myTest2Clazz.newInstance();
	
			// File f = new
			// File("/home/alex/instrumented/DummyHelperClass.class");
			// if (!f.exists()) {
			// f.createNewFile();
			// }
			// DataOutputStream dos = new DataOutputStream(
			// new FileOutputStream(f));
			// dos.write(cw.toByteArray());
		} catch (Exception e) {
		}
	
		return desc.toString();
	}

}
