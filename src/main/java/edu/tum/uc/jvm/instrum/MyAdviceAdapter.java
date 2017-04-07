package edu.tum.uc.jvm.instrum;

import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.commons.AdviceAdapter;

import edu.tum.uc.jvm.MyUcTransformer;
import edu.tum.uc.jvm.UcTransformer;
import edu.tum.uc.jvm.utility.ConfigProperties;

/**
 * This class is responsible for adding bytecode at the end of a method during instrumentation.
 * 
 * @author alex
 *
 */
public class MyAdviceAdapter extends AdviceAdapter {
    /**
     * The method name.
     */
    private String methodName;
    /**
     * The descriptor of the method.
     */
    private String descriptor;
    /**
     * The name of the class this method belongs to.
     */
    private String className;
    /**
     * The fully qualified name of this method consisting of the class and method names and the descriptor.
     */
    private String fqName;

    public MyAdviceAdapter(int p_api, MethodVisitor p_mv, int p_access, String p_name, String p_desc,
	    String p_signature, String p_className) {
	super(p_api, p_mv, p_access, p_name, p_desc);

	this.methodName = p_name;
	this.descriptor = p_desc;
	this.className = p_className;
	this.fqName = this.className.replace("/", ".") + "|" + this.methodName + this.descriptor;
    }

    /**
     * Called before explicit exit from the method using either return or throw. Inserts a delegate call of
     * MainMethodReturned if the method is called "main".
     * 
     * @param opcode
     *            A return opcode like IRETURN.
     */
    protected void onMethodExit(int opcode) {
	if (this.methodName.equals("main")) {
	    mv.visitLdcInsn(fqName);
	    mv.visitMethodInsn(Opcodes.INVOKESTATIC, MyUcTransformer.DELEGATECLASS, "mainMethodReturned",
		    "(Ljava/lang/String;)V", false);
	}
    }
    
    protected void onMethodEnter(){
    	if (this.methodName.equals("main")){// && !MyUcTransformer.ingestrument_webservice) {
    	    mv.visitMethodInsn(Opcodes.INVOKESTATIC, MyUcTransformer.DELEGATECLASS, "mainMethodInvoked",
    		    "()V", false);
    	}
    }
}
