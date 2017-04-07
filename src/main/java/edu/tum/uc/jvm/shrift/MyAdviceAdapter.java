package edu.tum.uc.jvm.shrift;

import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.commons.AdviceAdapter;
import org.objectweb.asm.tree.MethodNode;

import edu.tum.uc.jvm.UcException;
import edu.tum.uc.jvm.UcTransformer;
import edu.tum.uc.jvm.utility.ConfigProperties;

public class MyAdviceAdapter extends AdviceAdapter {
	private String methodName;
	private String className;
	private Label tcStart, tcEnd, tcHandler;

	protected MyAdviceAdapter(int p_api, MethodVisitor p_mv, int p_access,
			String p_name, String p_desc, String p_signature,
			String p_className, MethodNode p_methNode) {
		super(p_api, p_mv, p_access, p_name, p_desc);

		this.methodName = p_name;
		this.className = p_className;
	}

	protected void onMethodEnter() {
		if (this.methodName.equals("main") || this.methodName.equals("getData")) {
			Boolean b = new Boolean(
					ConfigProperties
							.getProperty(ConfigProperties.PROPERTIES.TIMER_T1));
			if (b) {
				mv.visitMethodInsn(Opcodes.INVOKESTATIC,
						UcTransformer.HOOKMETHOD, "timerT1Start", "()V", false);
			}
		}
	}

	protected void onMethodExit(int opcode) {
		if (this.methodName.equals("main") || this.methodName.equals("getData") || this.methodName.equals("refresh")) {
			Boolean b = new Boolean(
					ConfigProperties
							.getProperty(ConfigProperties.PROPERTIES.TIMER_T1));
			if (b) {
				mv.visitMethodInsn(Opcodes.INVOKESTATIC,
						UcTransformer.HOOKMETHOD, "timerT1Stop", "()V", false);
			}
			mv.visitMethodInsn(Opcodes.INVOKESTATIC, UcTransformer.HOOKMETHOD,
					"endMain", "()V", false);
		}
	}
	
	//The next enter-exit-pair wraps the whole method into a try-catch-block
	//In case a policy vialotion is detected a specific UcException is thrown and catch by the injected try-catch-block 
	protected void _onMethodEnter() {
		this.tcStart = new Label();
		this.tcEnd = new Label();
		this.tcHandler = new Label();

		this.mv.visitTryCatchBlock(this.tcStart, this.tcEnd, this.tcHandler,
				"edu/tum/uc/jvm/UcException");
		this.mv.visitLabel(this.tcStart);
	}
	protected void _onMethodExit(int opcode) {
		this.mv.visitLabel(this.tcEnd);
		Label end = new Label();
		this.mv.visitJumpInsn(Opcodes.GOTO, end);
		this.mv.visitLabel(this.tcHandler);
		this.mv.visitVarInsn(Opcodes.ASTORE, 1);
		this.mv.visitVarInsn(Opcodes.ALOAD, 1);
		this.mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, UcException.class
				.getName().replace(".", "/"), "printStackTrace", "()V", false);
		this.mv.visitLabel(end);
	}
}
