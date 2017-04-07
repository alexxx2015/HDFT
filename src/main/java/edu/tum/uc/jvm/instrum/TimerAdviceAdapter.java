package edu.tum.uc.jvm.instrum;

import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.commons.AdviceAdapter;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import edu.tum.uc.jvm.MyUcTransformer;
import edu.tum.uc.jvm.instrum.opt.InstrumDelegateOpt;
import edu.tum.uc.jvm.utility.ConfigProperties;

/**
 * This class is responsible to insert delegate calls for starting and stopping
 * method timers based on the method name.
 * 
 * @author alex
 *
 */
public class TimerAdviceAdapter extends AdviceAdapter {

	/**
	 * The method name.
	 */
	private String methodName;
	/**
	 * The name of the class this method belongs to.
	 */
	private String className;
	/**
	 * The descriptor of the method.
	 */
	private String descriptor;
	/**
	 * The fully qualified name of this method consisting of the class and
	 * method names and the descriptor.
	 */
	private String fqName;
	/**
	 * The name of the superclass of this method's class.
	 */
	private String superClassName;

	public TimerAdviceAdapter(int p_api, MethodVisitor p_mv, int p_access, String p_name, String p_desc,
			String p_signature, String p_className, String p_superClassName) {
		super(p_api, p_mv, p_access, p_name, p_desc);

		this.methodName = p_name;
		this.descriptor = p_desc;
		this.className = p_className;
		this.superClassName = p_superClassName;
		this.fqName = this.className.replace("/", ".") + "|" + this.methodName + this.descriptor;
	}

	/**
	 * Called at the beginning of the method or after super class class call in
	 * the constructor. Inserts a StartMethodTimer call on the delegate if the
	 * method is eligible.
	 */
	protected void onMethodEnter() {
		if (shouldAddTimer()) {
			if (!InstrumDelegateOpt.eventBasicRepoAdded) {
				mv.visitMethodInsn(Opcodes.INVOKESTATIC, InstrumDelegateOpt.class.getName().replace(".", "/"),
						"populateMyEventBasic", "()V", false);
				InstrumDelegateOpt.eventBasicRepoAdded = true;
			}
			mv.visitLdcInsn(fqName);
			mv.visitMethodInsn(Opcodes.INVOKESTATIC, MyUcTransformer.DELEGATECLASS, "startMethodTimer",
					"(Ljava/lang/String;)V", false);
		}
	}

	/**
	 * Called before explicit exit from the method using either return or throw.
	 * Inserts a delegate call of DumpStatistics if the method is called "main".
	 * Inserts a StopMethodTimer call on the delegate if the method is eligible.
	 * 
	 * @param opcode
	 *            A return opcode like IRETURN.
	 */
	protected void onMethodExit(int opcode) {
		if (shouldAddTimer()) {
			mv.visitLdcInsn(fqName);
			mv.visitMethodInsn(Opcodes.INVOKESTATIC, MyUcTransformer.DELEGATECLASS, "stopMethodTimer",
					"(Ljava/lang/String;)V", false);
		}

		if (this.methodName.equals("main")) {
			mv.visitMethodInsn(Opcodes.INVOKESTATIC, MyUcTransformer.DELEGATECLASS, "dumpStatistics", "()V", false);
		}
	}

	/**
	 * Checks whether this method is Servlet.doGet(), Servlet.doPost() or
	 * JZip.start().
	 * 
	 * @return A boolean value indicating that a timer should be added.
	 */
	private boolean shouldAddTimer() {
		String jsonString = ConfigProperties.getProperty(ConfigProperties.PROPERTIES.TIMERMETHODS);
		if (!"".equals(jsonString) && jsonString != null) {
			Map<String, List<Map<String, String>>> json = new Gson().fromJson(jsonString,
					new TypeToken<Map<String, List<Map<String, String>>>>() {
					}.getType());
			List<Map<String, String>> methods = json.get("methods");
			for (Map<String, String> method : methods) {
				boolean match = true;
				for (Entry<String, String> keyValuePair : method.entrySet()) {
					if (keyValuePair.getKey().equals("superClassName"))
						match = match && this.superClassName.matches(keyValuePair.getValue());
					else if (keyValuePair.getKey().equals("argNum"))
						match = match && Type.getArgumentTypes(this.descriptor).length == Integer
								.parseInt(keyValuePair.getValue());
					else if (keyValuePair.getKey().equals("methodName"))
						match = match && this.methodName.matches(keyValuePair.getValue());
					else if (keyValuePair.getKey().equals("className"))
						match = match && this.className.matches(keyValuePair.getValue());
				}
				if (match)
					return true;
			}
		}
		return false;
		// return (this.superClassName.contains("Servlet") &&
		// Type.getArgumentTypes(this.descriptor).length == 2
		// && (this.methodName.equals("doPost") || this.methodName
		// .equals("doGet")))
		// || (this.className.contains("JZip") &&
		// this.methodName.equals("start"))
		// || (this.className.contains("Action") &&
		// this.methodName.equals("execute"));
	}
}
