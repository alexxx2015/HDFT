package edu.tum.uc.jvm.instrum.opt;

import java.util.List;

import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.ClassNode;

import edu.tum.uc.jvm.instrum.MyAdviceAdapter;
import edu.tum.uc.jvm.instrum.TimerAdviceAdapter;
import edu.tum.uc.jvm.utility.ConfigProperties;
import edu.tum.uc.jvm.utility.analysis.Flow.Chop;
import edu.tum.uc.jvm.utility.analysis.StaticAnalysis;

/**
 * This class is responsible to provide a MethodVisitor object to ASM in order
 * to instrument a method.
 * 
 * @author alex
 *
 */
public class MyClassVisitorOpt extends ClassVisitor {
	private String superName;
	/**
	 * The name of the class.
	 */
	private String className;
	/**
	 * A ClassNode object used to access the super class name.
	 */
	private ClassNode classNode;
	/**
	 * The ClassWriter being the next in the ASM-ish class-event processing
	 * chain.
	 */
	private ClassWriter classWriter;

	public MyClassVisitorOpt(int p_api, ClassVisitor p_cv, ClassNode p_cn) {
		super(p_api, p_cv);
		this.classNode = p_cn;
		this.className = this.classNode.name;
		this.classWriter = (ClassWriter) p_cv;
	}

	/**
	 * Visits a method of the class.
	 * 
	 * @param p_access
	 *            The method's acccess flags.
	 * @param p_name
	 *            The method name.
	 * @param p_desc
	 *            The descriptor of the method.
	 * @param p_signature
	 *            The signature of the method.
	 * @param p_exceptions
	 *            The internal names of the method's exception classes.
	 */
	public MethodVisitor visitMethod(int p_access, String p_name, String p_desc, String p_signature,
			String[] p_exceptions) {
		
		// Get chop nodes list from analysis report
		String k = this.className.replace("/", ".") + "." + p_name + p_desc;
		List<Chop> chopNodes = StaticAnalysis.getChop(k);

		// Forward the method to next ClassVisitor in chain
		MethodVisitor mv = cv.visitMethod(p_access, p_name, p_desc, p_signature, p_exceptions);

		// If the method is not native, switch in the instrumenting
		// MyMethodVisitor
		// We cannot instrument native machine code
		if ((p_access & Opcodes.ACC_NATIVE) != Opcodes.ACC_NATIVE) {
			// only provide instrumentation if the corresponding flag was set in
			// the configuration file
			if (ConfigProperties.getProperty(ConfigProperties.PROPERTIES.INSTRUMENTATION) != null
					&& ConfigProperties.getProperty(ConfigProperties.PROPERTIES.INSTRUMENTATION).equals("true")) {
				// timers for tracking time spent in a method
				TimerAdviceAdapter timeAa = new TimerAdviceAdapter(Opcodes.ASM4, mv, p_access, p_name, p_desc,
						p_signature, this.className, this.classNode.superName);
				// general stuff to do regarding instrumentation (e.g. generate
				// return-main-method event)
				MyAdviceAdapter myAa = new MyAdviceAdapter(Opcodes.ASM4, timeAa, p_access, p_name, p_desc, p_signature,
						this.className);
				// actual instrumentation for tracking events
				mv = new MyMethodVisitorOptimized(Opcodes.ASM4, myAa, p_access, p_name, p_desc, p_signature, this.className,
						chopNodes, this.classWriter, this.superName);
//				mv = new MyMethodVisitor(Opcodes.ASM4, myAa, p_access, p_name, p_desc, p_signature, this.className,
//						chopNodes, this.classWriter, this.superName);
//				mv = new MyMethodVisitorSAP(Opcodes.ASM4, myAa, p_access, p_name, p_desc, p_signature, this.className,
//						chopNodes, this.classWriter, this.superName);
			} else {
				// only timers for methods, no other additional bytecode
				TimerAdviceAdapter timeAa = new TimerAdviceAdapter(Opcodes.ASM4, mv, p_access, p_name, p_desc,
						p_signature, this.className, this.classNode.superName);
				mv = timeAa;
			}
		}
		return mv;
	}

	@Override
	public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
		this.superName = superName;
		cv.visit(version, access, name, signature, superName, interfaces);
	}
}
