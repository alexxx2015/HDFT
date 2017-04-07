package edu.tum.uc.jvm.deprecated.misc;
//package edu.tum.uc.jvm.asm;
//
//import java.io.File;
//import java.io.FileWriter;
//import java.io.IOException;
//import java.util.HashMap;
//import java.util.Iterator;
//import java.util.Map;
//
//import org.objectweb.asm.Label;
//import org.objectweb.asm.MethodVisitor;
//import org.objectweb.asm.Opcodes;
//import org.objectweb.asm.Type;
//import org.objectweb.asm.tree.LocalVariableNode;
//import org.objectweb.asm.tree.MethodNode;
//
//import edu.tum.uc.jvm.MirrorStack;
//import edu.tum.uc.jvm.UcTransformer;
//import edu.tum.uc.jvm.utility.Mnemonic;
//import edu.tum.uc.jvm.utility.Utility;
//
//public class MyMethodVisitor_V1 extends MethodVisitor {
//	private String methodName;
//	private String className;
//	private MethodNode methNode;
//	private String signature;
//	private boolean isInstance;
//
//	protected MyMethodVisitor_V1(int p_api, MethodVisitor p_mv, int p_access, String p_name, String p_desc, String p_signature, String p_className, MethodNode p_methNode) {
//		super(p_api, p_mv);
//		
//		this.methodName = p_name;
//		this.className = p_className;
//		this.methNode = p_methNode;
//		this.signature = p_signature;
//		
//		this.isInstance = false;
//		
//		if (this.methodName.equals("<init>")){
//			this.isInstance = false;
//		}
//		else if( ((this.methNode.access & Opcodes.ACC_STATIC) != Opcodes.ACC_STATIC)
//				&& ((this.methNode.access & Opcodes.ACC_ABSTRACT) != Opcodes.ACC_ABSTRACT)){
//			this.isInstance = true;
//		}
////		System.out.println("METHODVISITOR: "+this.className+", "+this.methodName+", "+this.methNode.access+", "+this.isInstance+", "+this.methNode.name);
////		if(this.className.contains("DefaultSystemMessagesProvider") && this.methodName.equals("get"))
////			this.isInstance = false;
//	}
//	
//	public void visitVarInsn(int p_opcode, int p_var){
//		String varInsStr = this.getFullName();
////		System.out.println("UCAPT LOADVAR 1: "+p_var+", "+p_opcode);
//		LocalVariableNode lvn = this.getLocVarByIdx(p_var);
//		if(lvn != null){
//			varInsStr += "/"+lvn.name;
//		}else{
//			varInsStr += "/"+p_var;
//		}
////		System.out.println("UCAPT LOADVAR 2: "+varInsStr);
////		Label lab = this.insertChecks(mv, Disassembler.class.getName(), null, null);
//		
//		if((p_opcode >= Opcodes.ILOAD) && (p_opcode <= Opcodes.ALOAD)){
//			if(this.isInstance == false){
//				mv.visitLdcInsn(varInsStr+":"+p_opcode);
//				mv.visitMethodInsn(Opcodes.INVOKESTATIC, UcTransformer.HOOKMETHOD, "loadVar", "(Ljava/lang/String;)V");
//			}else{
//				mv.visitVarInsn(Opcodes.ALOAD, 0);
////				mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, Object.class.getName().replace(".", "/"), "hashCode", "()I");
//				mv.visitLdcInsn(varInsStr+":"+p_opcode);
//				mv.visitMethodInsn(Opcodes.INVOKESTATIC, UcTransformer.HOOKMETHOD, "loadVar", "(Ljava/lang/Object;Ljava/lang/String;)V");
//			}
//		}
//		else if ((p_opcode >= Opcodes.ISTORE) && (p_opcode <= Opcodes.ASTORE)){
//			if(this.isInstance == false){
//				mv.visitLdcInsn(varInsStr+":"+p_opcode);
//				mv.visitMethodInsn(Opcodes.INVOKESTATIC, UcTransformer.HOOKMETHOD, "storeVar", "(Ljava/lang/String;)V");
//			}else{
//				mv.visitVarInsn(Opcodes.ALOAD, 0);
////				mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, Object.class.getName().replace(".", "/"), "hashCode", "()I");
//				mv.visitLdcInsn(varInsStr+":"+p_opcode);
//				mv.visitMethodInsn(Opcodes.INVOKESTATIC, UcTransformer.HOOKMETHOD, "storeVar", "(Ljava/lang/Object;Ljava/lang/String;)V");
//			}
//		}
////		mv.visitLabel(lab);		
//		mv.visitVarInsn(p_opcode, p_var);
//	}
//	
//	public void visitInsn(int p_opcode){
//		String ucaHAStr = this.getFullName();
////		Label lab = this.insertChecks(mv, Disassembler.class.getName(), null, null);
//		
//		if((p_opcode >= Opcodes.ACONST_NULL) && (p_opcode <= Opcodes.SIPUSH)){
//			ucaHAStr += "/"+Mnemonic.OPCODE[p_opcode];
//			if(this.isInstance == false){
//				mv.visitLdcInsn(ucaHAStr+":"+p_opcode);
//				mv.visitMethodInsn(Opcodes.INVOKESTATIC, UcTransformer.HOOKMETHOD, "constLoad", "(Ljava/lang/String;)V");
//			}else{
//				mv.visitVarInsn(Opcodes.ALOAD, 0);
////				mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, Object.class.getName().replace(".", "/"), "hashCode", "()I");
//				mv.visitLdcInsn(ucaHAStr+":"+p_opcode);
//				mv.visitMethodInsn(Opcodes.INVOKESTATIC, UcTransformer.HOOKMETHOD, "constLoad", "(Ljava/lang/Object;Ljava/lang/String;)V");
//			}
//		}
//		else if (p_opcode == Opcodes.POP){
//			mv.visitMethodInsn(Opcodes.INVOKESTATIC, UcTransformer.HOOKMETHOD, "popInstr", "()V");
//		}
//		else if( ((p_opcode >= Opcodes.IADD) && (p_opcode <= Opcodes.DREM))
//				|| ((p_opcode >= Opcodes.ISHL) && (p_opcode <= Opcodes.LXOR)) ){
//			if(this.isInstance == false){
//				mv.visitLdcInsn(ucaHAStr+":"+p_opcode);
//				mv.visitMethodInsn(Opcodes.INVOKESTATIC, UcTransformer.HOOKMETHOD, "arithInstr", "(Ljava/lang/String;)V");
//			}else{
//				mv.visitVarInsn(Opcodes.ALOAD, 0);
////				mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, Object.class.getName().replace(".", "/"), "hashCode", "()I");
//				mv.visitLdcInsn(ucaHAStr+":"+p_opcode);
//				mv.visitMethodInsn(Opcodes.INVOKESTATIC, UcTransformer.HOOKMETHOD, "arithInstr", "(Ljava/lang/Object;Ljava/lang/String;)V");
//			}
//		}
//		else if(p_opcode == Opcodes.ARRAYLENGTH){
//			mv.visitMethodInsn(Opcodes.INVOKESTATIC, UcTransformer.HOOKMETHOD, "lengthArray", "()V");
//		}
//		else if ((p_opcode >= Opcodes.IALOAD) && (p_opcode <= Opcodes.SALOAD)){
////			ucaHAStr += "/"+Mnemonic.OPCODE[p_opcode];
//			if(this.isInstance == false){
//				mv.visitLdcInsn(ucaHAStr+":"+p_opcode);
//				mv.visitMethodInsn(Opcodes.INVOKESTATIC, UcTransformer.HOOKMETHOD, "loadArrayVar", "(Ljava/lang/String;)V");
//			}else{
//				mv.visitVarInsn(Opcodes.ALOAD, 0);
////				mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, Object.class.getName().replace(".", "/"), "hashCode", "()I");
//				mv.visitLdcInsn(ucaHAStr+":"+p_opcode);
//				mv.visitMethodInsn(Opcodes.INVOKESTATIC, UcTransformer.HOOKMETHOD, "loadArrayVar", "(Ljava/lang/Object;Ljava/lang/String;)V");
//			}
//		}
//		else if ((p_opcode >= Opcodes.IASTORE) && (p_opcode <= Opcodes.SASTORE)){
////			ucaHAStr += "/"+Mnemonic.OPCODE[p_opcode];
//			if(this.isInstance == false){
//				mv.visitLdcInsn(ucaHAStr+":"+p_opcode);
//				mv.visitMethodInsn(Opcodes.INVOKESTATIC, UcTransformer.HOOKMETHOD, "storeArrayVar", "(Ljava/lang/String;)V");
//			}else{
//				mv.visitVarInsn(Opcodes.ALOAD, 0);
////				mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, Object.class.getName().replace(".", "/"), "hashCode", "()I");
//				mv.visitLdcInsn(ucaHAStr+":"+p_opcode);
//				mv.visitMethodInsn(Opcodes.INVOKESTATIC, UcTransformer.HOOKMETHOD, "storeArrayVar", "(Ljava/lang/Object;Ljava/lang/String;)V");
//			}
//		}
//		else if (p_opcode == Opcodes.SWAP){
//			mv.visitMethodInsn(Opcodes.INVOKESTATIC, UcTransformer.HOOKMETHOD, "swapInstr", "()V");
//		}
//		else if(p_opcode == Opcodes.DUP){
//			mv.visitMethodInsn(Opcodes.INVOKESTATIC, UcTransformer.HOOKMETHOD, "dupInstr", "()V");
//		}
//		else if(p_opcode == Opcodes.DUP_X1){
//			mv.visitMethodInsn(Opcodes.INVOKESTATIC, UcTransformer.HOOKMETHOD, "dupX1Instr", "()V");
//		}
//		else if(p_opcode == Opcodes.DUP_X2){
//			mv.visitMethodInsn(Opcodes.INVOKESTATIC, UcTransformer.HOOKMETHOD, "dupX2Instr", "()V");
//		}
//		else if(p_opcode == Opcodes.DUP2){
//			mv.visitMethodInsn(Opcodes.INVOKESTATIC, UcTransformer.HOOKMETHOD, "dup2Instr", "()V");
//		}
//		else if(p_opcode == Opcodes.DUP2_X1){
//			mv.visitMethodInsn(Opcodes.INVOKESTATIC, UcTransformer.HOOKMETHOD, "dup2X1Instr", "()V");				
//		}
//		else if(p_opcode == Opcodes.DUP2_X2){
//			mv.visitMethodInsn(Opcodes.INVOKESTATIC, UcTransformer.HOOKMETHOD, "dup2X2Instr", "()V");	
//		}
////		mv.visitLabel(lab);
//		mv.visitInsn(p_opcode);
//	}
//	
//	public void visitFieldInsn(int p_opcode, String p_owner, String p_name, String p_desc){
////		if(!p_owner.equals("java/lang/Object") && ucaPremainTransformer.checkClassExist(p_owner)){
////			p_owner = ucaPremainTransformer.myrep(p_owner);
////		}
////		if(p_desc != null){
////			Type t = Type.getType(p_desc);
////			while(t.getSort() == Type.ARRAY)
////				t = t.getElementType();
////			if( ((t.getSort() == Type.OBJECT) || ((t.getSort() == Type.ARRAY) && (t.getElementType().getSort() == Type.OBJECT))) 
////				&& !t.getClassName().contains("java.lang.Object") && ucaPremainTransformer.checkClassExist(t.getClassName()) ){
////				p_desc = ucaPremainTransformer.myrep(p_desc);
////			}
////		}
//		
////		Label lab = this.insertChecks(mv, Disassembler.class.getName(), null, null);	
//		String ldcInsn = p_owner+"/"+p_name;
//		
////		while(type.getSort() == Type.ARRAY){
////			type = type.getElementType();
////		}
//		
//		ldcInsn = ldcInsn+":"+p_desc;
////		System.out.println("VISITFIELDINS, "+this.isInstance+", "+this.methodName+", "+ldcInsn);
//		if(this.isInstance == false){
//			switch(p_opcode){
//				case Opcodes.GETFIELD:
//					mv.visitLdcInsn(ldcInsn);
//					mv.visitMethodInsn(Opcodes.INVOKESTATIC, UcTransformer.HOOKMETHOD, "getField", "(Ljava/lang/String;)V");break;
//				case Opcodes.GETSTATIC: 
//					mv.visitLdcInsn(ldcInsn);
//					mv.visitMethodInsn(Opcodes.INVOKESTATIC, UcTransformer.HOOKMETHOD, "getStaticField", "(Ljava/lang/String;)V");break;
//				case Opcodes.PUTFIELD:
//					mv.visitLdcInsn(ldcInsn);
//					mv.visitMethodInsn(Opcodes.INVOKESTATIC, UcTransformer.HOOKMETHOD, "putField", "(Ljava/lang/String;)V");break;
//				case Opcodes.PUTSTATIC: 
//					mv.visitLdcInsn(ldcInsn);
//					mv.visitMethodInsn(Opcodes.INVOKESTATIC, UcTransformer.HOOKMETHOD, "putStaticField", "(Ljava/lang/String;)V");break;
//				default: mv.visitInsn(Opcodes.POP);
//			}
//		}
//		else{
//			mv.visitVarInsn(Opcodes.ALOAD, 0);
////			mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, Object.class.getName().replace(".", "/"), "hashCode", "()I");
//			switch(p_opcode){
//				case Opcodes.GETFIELD:
//					mv.visitLdcInsn(ldcInsn);
//					mv.visitMethodInsn(Opcodes.INVOKESTATIC, UcTransformer.HOOKMETHOD, "getField", "(Ljava/lang/Object;Ljava/lang/String;)V");break;
//				case Opcodes.GETSTATIC: 
//					mv.visitLdcInsn(ldcInsn);
//					mv.visitMethodInsn(Opcodes.INVOKESTATIC, UcTransformer.HOOKMETHOD, "getStaticField", "(Ljava/lang/Object;Ljava/lang/String;)V");break;
//				case Opcodes.PUTFIELD:
//					mv.visitLdcInsn(ldcInsn);
//					mv.visitMethodInsn(Opcodes.INVOKESTATIC, UcTransformer.HOOKMETHOD, "putField", "(Ljava/lang/Object;Ljava/lang/String;)V");break;
//				case Opcodes.PUTSTATIC: 
//					mv.visitLdcInsn(ldcInsn);
//					mv.visitMethodInsn(Opcodes.INVOKESTATIC, UcTransformer.HOOKMETHOD, "putStaticField", "(Ljava/lang/Object;Ljava/lang/String;)V");break;
//				default: mv.visitInsn(Opcodes.POP);
//			}
//		}
//			
//		
////		mv.visitLabel(lab);			
//		mv.visitFieldInsn(p_opcode, p_owner, p_name, p_desc);
//	}
//	
//	public void visitJumpInsn(int p_opcode, Label p_label){
//		if( ((p_opcode >= Opcodes.IFEQ) && (p_opcode <= Opcodes.IF_ACMPNE))
//			|| (p_opcode == Opcodes.IFNULL) || (p_opcode == Opcodes.IFNONNULL)){
////			Label lab = this.insertChecks(mv, Disassembler.class.getName(), null, null);
//			
//			String jumpStr = this.getFullName()+"/"+Mnemonic.OPCODE[p_opcode]+":"+p_opcode;
//			try{
//				jumpStr += ":"+p_label.getOffset();
//			} catch(Exception e){}
//			if(this.isInstance == false){
//				mv.visitLdcInsn(jumpStr);
//				mv.visitMethodInsn(Opcodes.INVOKESTATIC, UcTransformer.HOOKMETHOD, "ifInstr", "(Ljava/lang/String;)V");
//			}else{
//				mv.visitVarInsn(Opcodes.ALOAD, 0);
////				mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, Object.class.getName().replace(".", "/"), "hashCode", "()I");
//				mv.visitLdcInsn(jumpStr);
//				mv.visitMethodInsn(Opcodes.INVOKESTATIC, UcTransformer.HOOKMETHOD, "ifInstr", "(Ljava/lang/Object;Ljava/lang/String;)V");
//			}
////			mv.visitLabel(lab);
//		}
//		mv.visitJumpInsn(p_opcode, p_label);
//	}
//	
//	public void visitIntInsn(int p_opcode, int p_operand){
////		Label lab = this.insertChecks(mv, Disassembler.class.getName(), null, null);
//		String ucaHAStr;
//		if(p_opcode == Opcodes.NEWARRAY){
//			ucaHAStr = this.getFullName()+":"+p_operand+":"+p_opcode;
//			if(this.isInstance == false){
//				mv.visitLdcInsn(ucaHAStr);
//				mv.visitMethodInsn(Opcodes.INVOKESTATIC, UcTransformer.HOOKMETHOD, "newArray", "(Ljava/lang/String;)V");
//			}else{
//				mv.visitVarInsn(Opcodes.ALOAD, 0);
////				mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, Object.class.getName().replace(".", "/"), "hashCode", "()I");
//				mv.visitLdcInsn(ucaHAStr);
//				mv.visitMethodInsn(Opcodes.INVOKESTATIC, UcTransformer.HOOKMETHOD, "newArray", "(Ljava/lang/Object;Ljava/lang/String;)V");
//			}
//		}
//		else if((p_opcode >= Opcodes.ACONST_NULL) && (p_opcode <= Opcodes.SIPUSH)){
//			ucaHAStr = this.getFullName()+"/"+Mnemonic.OPCODE[p_opcode]+"_"+p_operand+":"+p_opcode;
//			if(this.isInstance == false){
//				mv.visitLdcInsn(ucaHAStr);
//				mv.visitMethodInsn(Opcodes.INVOKESTATIC, UcTransformer.HOOKMETHOD, "constLoad", "(Ljava/lang/String;)V");
//			}else{
//				mv.visitVarInsn(Opcodes.ALOAD, 0);
////				mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, Object.class.getName().replace(".", "/"), "hashCode", "()I");
//				mv.visitLdcInsn(ucaHAStr);
//				mv.visitMethodInsn(Opcodes.INVOKESTATIC, UcTransformer.HOOKMETHOD, "constLoad", "(Ljava/lang/Object;Ljava/lang/String;)V");
//			}
//		}	
////		mv.visitLabel(lab);
//		mv.visitIntInsn(p_opcode, p_operand);
//	}
//	
//	public void visitTypeInsn(int p_opcode, String p_type){
////		Label lab = this.insertChecks(mv, Disassembler.class.getName(), null, null);
//		String ucaHAStr = "";
//		if(p_opcode == Opcodes.ANEWARRAY){
//			ucaHAStr = this.getFullName()+":"+p_type+":"+p_opcode;
//			if(this.isInstance == false){
//				mv.visitLdcInsn(ucaHAStr);
//				mv.visitMethodInsn(Opcodes.INVOKESTATIC, UcTransformer.HOOKMETHOD, "newArray", "(Ljava/lang/String;)V");
//			}else{
//				mv.visitVarInsn(Opcodes.ALOAD, 0);
////				mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, Object.class.getName().replace(".", "/"), "hashCode", "()I");
//				mv.visitLdcInsn(ucaHAStr);
//				mv.visitMethodInsn(Opcodes.INVOKESTATIC, UcTransformer.HOOKMETHOD, "newArray", "(Ljava/lang/Object;Ljava/lang/String;)V");
//			}
//		}
//		else if (p_opcode == Opcodes.NEW){
//			ucaHAStr = this.getFullName()+":"+p_type+":"+p_opcode;
//			if(this.isInstance == false){
//				mv.visitLdcInsn(ucaHAStr);
//				mv.visitMethodInsn(Opcodes.INVOKESTATIC, UcTransformer.HOOKMETHOD, "newInstr", "(Ljava/lang/String;)V");
//			}else{
//				mv.visitVarInsn(Opcodes.ALOAD, 0);
////				mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, Object.class.getName().replace(".", "/"), "hashCode", "()I");
//				mv.visitLdcInsn(ucaHAStr);
//				mv.visitMethodInsn(Opcodes.INVOKESTATIC, UcTransformer.HOOKMETHOD, "newInstr", "(Ljava/lang/Object;Ljava/lang/String;)V");
//			}
//		}
//		
////		mv.visitLabel(lab);
//		mv.visitTypeInsn(p_opcode, p_type);
//	}
//	
//	public void visitMultiANewArrayInsn(String p_desc, int p_dim){
////		Label lab = this.insertChecks(mv, Disassembler.class.getName(), null, null);
//
//		String ucaHAStr = this.getFullName()+"::"+Opcodes.MULTIANEWARRAY;
//		if(this.isInstance == false){
//			mv.visitLdcInsn(ucaHAStr);
//			mv.visitMethodInsn(Opcodes.INVOKESTATIC, UcTransformer.HOOKMETHOD, "newArray", "(Ljava/lang/String;)V");
//		}else{
//			mv.visitVarInsn(Opcodes.ALOAD, 0);
////			mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, Object.class.getName().replace(".", "/"), "hashCode", "()I");
//			mv.visitLdcInsn(ucaHAStr);
//			mv.visitMethodInsn(Opcodes.INVOKESTATIC, UcTransformer.HOOKMETHOD, "newArray", "(Ljava/lang/Object;Ljava/lang/String;)V");
//		}
//		
////		mv.visitLabel(lab);
//		mv.visitMultiANewArrayInsn(p_desc, p_dim);
//	}
//	
//	public void visitLdcInsn(Object cst){		
//		String ucaHAStr = this.getFullName()+":"+cst.toString().replace(":", "&#58;");
////		Label lab = this.insertChecks(mv, Disassembler.class.getName(), null, null);
//		if(this.isInstance == false){
//			mv.visitLdcInsn(ucaHAStr);
//			mv.visitMethodInsn(Opcodes.INVOKESTATIC, UcTransformer.HOOKMETHOD, "ldcConstLoad", "(Ljava/lang/String;)V");
//		}else{
//			mv.visitVarInsn(Opcodes.ALOAD, 0);
////			mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, Object.class.getName().replace(".", "/"), "hashCode", "()I");
//			mv.visitLdcInsn(ucaHAStr);
//			mv.visitMethodInsn(Opcodes.INVOKESTATIC, UcTransformer.HOOKMETHOD, "ldcConstLoad", "(Ljava/lang/Object;Ljava/lang/String;)V");
//		}
////		mv.visitLabel(lab);
//		mv.visitLdcInsn(cst);
//	}
//
//	public void visitMethodInsn(int p_opcode, String p_owner, String p_name, String p_desc){
//		MyLabel label = (MyLabel)this.getCurrentLabel();
//		if(label != null){
//			System.out.println(this.methodName+", "+p_owner+", "+p_name+", "+label.getOffset());
//		}
//		
//		if((p_opcode == Opcodes.INVOKEVIRTUAL) || (p_opcode == Opcodes.INVOKEINTERFACE) || (p_opcode == Opcodes.INVOKESPECIAL) || (p_opcode == Opcodes.INVOKESTATIC)){				
//			Type[] argT = Type.getArgumentTypes(p_desc);
//			Type retT = Type.getReturnType(p_desc);
//			
////			Label elseLab = this.insertChecks(mv, Disassembler.class.getName(), null, null);
//			if(this.isInstance == false){
//				mv.visitLdcInsn(this.getFullName()+":"+p_owner+"/"+p_name+":"+p_desc+":"+p_opcode);
//				mv.visitMethodInsn(Opcodes.INVOKESTATIC, UcTransformer.HOOKMETHOD, "methodInvoked", "(Ljava/lang/String;)Z");
//			} else {
//				mv.visitVarInsn(Opcodes.ALOAD, 0);
////				mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, Object.class.getName().replace(".", "/"), "hashCode", "()I");
//				mv.visitLdcInsn(this.getFullName()+":"+p_owner+"/"+p_name+":"+p_desc+":"+p_opcode);
//				mv.visitMethodInsn(Opcodes.INVOKESTATIC, UcTransformer.HOOKMETHOD, "methodInvoked", "(Ljava/lang/Object;Ljava/lang/String;)Z");
//			}
//
////			mv.visitInsn(Opcodes.POP);
////			mv.visitInsn(Opcodes.ICONST_3);
//			
//			Label elseLab = new Label();
//			mv.visitJumpInsn(Opcodes.IFNE, elseLab);
//			
////			Remove all argument frames
//			if(argT.length > 0){
//				for(int k = 0; k < argT.length; k++){
////					System.out.println(p_owner+", "+p_name+", "+argT.length);
//					mv.visitInsn(Opcodes.POP);
//				}
//			}
//			
////			Remove a reference frame if method invocation is type of INVOKEVIRTUAL
//			if(p_opcode != Opcodes.INVOKESTATIC){
//				mv.visitInsn(Opcodes.POP);
//			}
//			
////			Add a return frame on the stack
//			if(retT.getSort() != Type.VOID){
//				int isArray = 0;
//				
//				while(retT.getSort() == Type.ARRAY){
//					isArray++;
//					retT = retT.getElementType();
//				}
//
//				if(isArray > 0){
//					if(retT.getSort() == Type.OBJECT){
////						mv.visitTypeInsn(Opcodes.ANEWARRAY, retT.toString());
//						mv.visitInsn(Opcodes.ACONST_NULL);
//						mv.visitTypeInsn(Opcodes.CHECKCAST, "Ljava/lang/Object");
//					}
//					else{
//						int retType;				
//						switch(retT.getSort()){
//							case Type.BOOLEAN: retType = Opcodes.T_BOOLEAN;break;
//							case Type.CHAR: retType = Opcodes.T_CHAR; break;
//							case Type.FLOAT: retType = Opcodes.T_FLOAT; break;
//							case Type.DOUBLE: retType = Opcodes.T_DOUBLE; break;
//							case Type.BYTE: retType = Opcodes.T_BYTE; break;
//							case Type.SHORT: retType = Opcodes.T_SHORT; break;
//							case Type.INT: retType = Opcodes.T_INT; break;
//							case Type.LONG: retType = Opcodes.T_LONG; break;
//							default: retType = Opcodes.T_CHAR;
//						}
//						mv.visitIntInsn(Opcodes.NEWARRAY, retType);
//					}
//				} else {
//					if(retT.getSort() == Type.OBJECT){
////						mv.visitTypeInsn(Opcodes.NEW, "java/lang/Object");//retT.toString());
////						mv.visitInsn(Opcodes.DUP);
////						mv.visitMethodInsn(Opcodes.INVOKESPECIAL, "java/lang/Object", "<init>", "()V");
//						mv.visitInsn(Opcodes.ACONST_NULL);
//						mv.visitTypeInsn(Opcodes.CHECKCAST, "java/lang/Object");
//					}
//					else{
//						
//						switch(retT.getSort()){
//							case Type.BOOLEAN: mv.visitInsn(Opcodes.ICONST_0);break;
//							case Type.CHAR: mv.visitIntInsn(Opcodes.BIPUSH, 0); break;
//							case Type.FLOAT: mv.visitLdcInsn(1.0f); break;
//							case Type.DOUBLE: mv.visitLdcInsn(1.0d); break;
//							case Type.BYTE: mv.visitInsn(Opcodes.ICONST_0); break;
//							case Type.SHORT: mv.visitIntInsn(Opcodes.SIPUSH, 0); break;
//							case Type.INT: mv.visitIntInsn(Opcodes.SIPUSH, 0); break;
//							case Type.LONG: mv.visitLdcInsn(1l); break;
//						}
//					}
//				}
//			}
////			mv.visitMethodInsn(p_opcode, p_owner, p_name, p_desc);
//
//			Label endLab = new Label();
//			mv.visitJumpInsn(Opcodes.GOTO, endLab);				
//			mv.visitLabel(elseLab);
//			mv.visitMethodInsn(p_opcode, p_owner, p_name, p_desc);
//			
////			Maybe some return values 
////			this.insertChecks(mv, Disassembler.class.getName(), null, endLab);	
//			if(this.isInstance == false){
//				mv.visitLdcInsn(this.getFullName()+":"+p_owner+"/"+p_name+":"+p_desc+":"+p_opcode);	
//				mv.visitMethodInsn(Opcodes.INVOKESTATIC, UcTransformer.HOOKMETHOD, "methodExited", "(Ljava/lang/String;)V");
//			} else {
//				mv.visitVarInsn(Opcodes.ALOAD, 0);
////				mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, Object.class.getName().replace(".", "/"), "hashCode", "()I");	
//				mv.visitLdcInsn(this.getFullName()+":"+p_owner+"/"+p_name+":"+p_desc+":"+p_opcode);	
//				mv.visitMethodInsn(Opcodes.INVOKESTATIC, UcTransformer.HOOKMETHOD, "methodExited", "(Ljava/lang/Object;Ljava/lang/String;)V");				
//			}
//			mv.visitLabel(endLab);				
//		}else{
//			mv.visitMethodInsn(p_opcode, p_owner, p_name, p_desc);
//		}
//	}		
////	public void visitMethodInsn(int p_opcode, String p_owner, String p_name, String p_desc){
////		if((p_opcode == Opcodes.INVOKESPECIAL) && p_name.equals("<init>") && p_owner.equals("java/lang/String")){
////			p_owner = PREFIX+p_owner;
////		}
////		if(ucaPremainTransformer.checkClassExist(p_owner) && !p_owner.equals("java/lang/Object")){
////			p_owner = ucaPremainTransformer.myrep(p_owner);
////			if((p_desc != null)){
////				StringBuilder tmpDesc = new StringBuilder();
////				Type[] argType = Type.getArgumentTypes(p_desc);
////				tmpDesc.append("(");
////				if(argType.length > 0){
////					for(Type t1: argType){
////						while(t1.getSort() == Type.ARRAY)
////							t1 = t1.getElementType();
////						
////						if(t1.getSort() != Type.OBJECT){
////							tmpDesc.append(t1.toString());
////						}
////						else if(!ucaPremainTransformer.checkClassExist(t1.getClassName())){
////							tmpDesc.append(t1.toString());
////						}else{
////							tmpDesc.append(ucaPremainTransformer.myrep(t1.toString()));
////						}
////					}
////				}				
////				tmpDesc.append(")");
////				Type retType = Type.getReturnType(p_desc);
////				if((retType.getSort() == Type.OBJECT) && (ucaPremainTransformer.checkClassExist(retType.getClassName()))){
////					tmpDesc.append(ucaPremainTransformer.myrep(retType.toString()));
////				}else{
////					tmpDesc.append(retType.toString());
////				}
////				
////				p_desc = tmpDesc.toString();//p_desc.replace("java/", MyASM.PREFIX+"java/");
////			}
////		}
////		mv.visitMethodInsn(p_opcode, p_owner, p_name, p_desc);
////	}
//	
////	Return full name of a method, i.e. classname + methodname
//	private String getFullName(){
//		return this.className+"/"+this.methodName;
//	}
//	
//	//Return the local variable node of a method by index
//	private LocalVariableNode getLocVarByIdx(int p_indx){
//		LocalVariableNode lvn = null;
//		Iterator locVarIt = this.methNode.localVariables.iterator();
//		while(locVarIt.hasNext()){
//			lvn = (LocalVariableNode)locVarIt.next();
////			System.out.println("UCAPT GLVBI: "+this.methodName+", "+this.methNode.name+", "+lvn.name+", "+lvn.index);
//			if(lvn.index == p_indx){
//				break;
//			}
//		}
//		return lvn;
//	}
//	
//	//Return the local variable node of a method by index
//	private LocalVariableNode getLocVarByName(String p_name){
//		LocalVariableNode lvn = null;
//		Iterator locVarIt = this.methNode.localVariables.iterator();
//		while(locVarIt.hasNext()){
//			lvn = (LocalVariableNode)locVarIt.next();
//			if(lvn.name.equals(p_name)){
//				break;
//			}
//		}
//		return lvn;
//	}
//	
//	private Label insertChecks(MethodVisitor p_mv, String p_class, String p_method, Label p_lab){
//		if(p_mv == null){
//			p_mv = mv;
//		}
//		if(p_class == null){
//			p_class = Utility.notSpecified;
//		}
//		if(p_method == null){
//			p_method = Utility.notSpecified;
//		}
//		if(p_lab == null){
//			p_lab = new Label();
//		}			
////		p_mv.visitLdcInsn(p_class);
////		p_mv.visitLdcInsn(p_method);
////		p_mv.visitMethodInsn(Opcodes.INVOKESTATIC, ucaUtility.class.getName(), "checkInvocationOnStack", "(Ljava/lang/String;Ljava/lang/String;)Z");			
////		p_mv.visitFieldInsn(Opcodes.GETSTATIC, Disassembler.class.getName(), "ucaUsed", "Z");
//		
//		p_mv.visitMethodInsn(Opcodes.INVOKESTATIC, UcTransformer.HOOKMETHOD, "getUcaUsed", "()Z");
//		p_mv.visitJumpInsn(Opcodes.IFNE, p_lab);			
//		return p_lab;
//	}
//
////	public void visitLocalVariable(String p_name, String p_desc, String p_signature, Label p_start, Label p_end, int p_index){
////		if(this.className.toLowerCase().contains("helloworld")){
////			System.out.println("LV "+this.methodName+", "+p_name+", "+p_desc+", "+p_signature+", "+p_start.toString()+", "+p_end.toString()+", "+p_index);
////		}
////	}
//
//	private void printStr2File(String p_str){
//		try {
//			File f = new File("/Users/ladmin/LOGTRANS.txt");
//			FileWriter fo = new FileWriter(f, true);
//			fo.append(p_str);
//			fo.append(System.getProperty("line.separator"));
//			fo.flush();
//			fo.close();
//		} catch (IOException e) {
//			// TODO Auto-generated catch block
//			e.printStackTrace();
//		}
//	}
//	
//	private static String myrep(String p_replaceable){		
//		if(p_replaceable != null){
//			if(!p_replaceable.contains(";")){
//				if(!p_replaceable.contains("java/lang/Object") && !p_replaceable.contains("java/lang/Class") && !p_replaceable.contains("sun/misc/Unsafe")){
//					if(p_replaceable.startsWith("apple/")){
//						p_replaceable = p_replaceable.replaceFirst("apple/", UcTransformer.PREFIX+"apple/");
//					}
//					else if(p_replaceable.startsWith("com/")){
//						p_replaceable = p_replaceable.replaceFirst("com/", UcTransformer.PREFIX+"com/");
//					}
//					else if(p_replaceable.startsWith("java/")){
//						p_replaceable = p_replaceable.replaceFirst("java/", UcTransformer.PREFIX+"java/");
//					}
//					else if(p_replaceable.startsWith("javax/")){
//						p_replaceable = p_replaceable.replaceFirst("javax/", UcTransformer.PREFIX+"javax/");
//					}
//					else if(p_replaceable.startsWith("org/")){
//						p_replaceable = p_replaceable.replaceFirst("org/", UcTransformer.PREFIX+"org/");
//					}
//					else if(p_replaceable.startsWith("sun/")){
//						p_replaceable = p_replaceable.replaceFirst("sun/", UcTransformer.PREFIX+"sun/");
//					}
//					else if(p_replaceable.startsWith("sunw/")){
//						p_replaceable = p_replaceable.replaceFirst("sunw/", UcTransformer.PREFIX+"sunw/");
//					}
//					else if(p_replaceable.startsWith("oracle/")){
//						p_replaceable = p_replaceable.replaceFirst("sunw/", UcTransformer.PREFIX+"oracle/");
//					}
//				}
//			}
//			else{
//				String[] str = p_replaceable.split(";");
//				StringBuilder sb = new StringBuilder();
//				Map<String, Integer> occurence = new HashMap<String,Integer>();
//				for(int i = 0; i< str.length; i++){
//					if(!str[i].contains("java/lang/Object") && !str[i].contains("java/lang/Class") && !str[i].contains("sun/misc/Unsafe")){
//						String minString = "";
//						int minInt = 100000000;
//						if((str[i].lastIndexOf("apple/") < minInt) && (str[i].lastIndexOf("apple/") != -1)){
//							minString = "apple/";
//							minInt = str[i].lastIndexOf("apple/");
//						}
//						
//						if((str[i].lastIndexOf("com/") < minInt) && (str[i].lastIndexOf("com/") != -1)){
//							minString = "com/";
//							minInt = str[i].lastIndexOf("com/");
//						}
//						
//						if((str[i].lastIndexOf("java/") < minInt) && (str[i].lastIndexOf("java/") != -1)){
//							minString = "java/";
//							minInt = str[i].lastIndexOf("java/");
//						}
//						
//						if((str[i].lastIndexOf("javax/") < minInt) && (str[i].lastIndexOf("javax/") != -1)){
//							minString = "javax/";
//							minInt = str[i].lastIndexOf("javax/");
//						}
//						
//						if((str[i].lastIndexOf("org/") < minInt) && (str[i].lastIndexOf("org/") != -1)){
//							minString = "org/";
//							minInt = str[i].lastIndexOf("org/");
//						}
//						
//						if((str[i].lastIndexOf("sun/") < minInt) && (str[i].lastIndexOf("sun/") != -1)){
//							minString = "sun/";
//							minInt = str[i].lastIndexOf("sun/");
//						}
//						
//						if((str[i].lastIndexOf("sunw/") < minInt) && (str[i].lastIndexOf("sunw/") != -1)){
//							minString = "sunw/";
//							minInt = str[i].lastIndexOf("sunw/");
//						}
//						
//						if((str[i].lastIndexOf("oracle/") < minInt) && (str[i].lastIndexOf("oracle/") != -1)){
//							minString = "oracle/";
//							minInt = str[i].lastIndexOf("oracle/");
//						}
//						
//						if(!minString.equals("")){
//							str[i] = str[i].replaceFirst(minString, UcTransformer.PREFIX+minString);	
//						}
//					}
//					
//					sb.append(str[i]);
//					if(str[i].contains("L"+UcTransformer.PREFIX) || str[i].contains("java/lang/Object") || str[i].contains("java/lang/Class") || str[i].contains("sun/misc/Unsafe")){
//						sb.append(";");
//					}
//				}				
//				p_replaceable = sb.toString();
//			}
//		}
//		return p_replaceable;
//	}
//}
