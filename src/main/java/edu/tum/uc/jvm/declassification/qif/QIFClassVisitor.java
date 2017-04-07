package edu.tum.uc.jvm.declassification.qif;

import java.util.List;

import org.apache.log4j.Logger;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.commons.LocalVariablesSorter;
import org.objectweb.asm.tree.ClassNode;

import edu.tum.uc.jvm.utility.analysis.Flow.Chop;
import edu.tum.uc.jvm.utility.analysis.StaticAnalysis;

public class QIFClassVisitor extends ClassVisitor {
	private static Logger _logger = Logger.getLogger(QIFClassVisitor.class.getName());

	private ClassNode cn;

	private int clVersion;
	private int clAccess;
	private String clName;
	private String clSignature;
	private String clSupername;
	private String[] clInterfaces;
	private ClassWriter cw;

	public static final String DELEGATECLASS = QIFTracker.class.getName().replace(".", "/");

	public QIFClassVisitor(int version, ClassVisitor p_cv, ClassNode p_cn) {
		super(version, p_cv);
		this.cn = p_cn;
		this.cw = (ClassWriter) p_cv;
	}

	@Override
	public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
		cv.visit(version, access, name, signature, superName, interfaces);
		this.clVersion = version;
		this.clAccess = access;
		this.clName = name;
		this.clSignature = signature;
		this.clSupername = superName;
		this.clInterfaces = interfaces;
	}

	/**
	 * Visits a method of the class.
	 * 
	 * @param access
	 *            The method's acccess flags.
	 * @param name
	 *            The method name.
	 * @param desc
	 *            The descriptor of the method.
	 * @param signature
	 *            The signature of the method.
	 * @param p_exceptions
	 *            The internal names of the method's exception classes.
	 */
	public MethodVisitor visitMethod(int access, String name, String desc, String signature, String[] p_exceptions) {
		// _logger.info("Analyzing method "+this.clName+"."+name);
		MethodVisitor _return = cv.visitMethod(access, name, desc, signature, p_exceptions);

		if ((access & Opcodes.ACC_NATIVE) != Opcodes.ACC_NATIVE) {
			String chopsParent = this.clName.replace("/", ".") + "." + name + desc;
			List<Chop> chopNodes = StaticAnalysis.getChop(chopsParent);
			_return = new QIFMethodVisitor(Opcodes.ASM5, _return, access, name, desc, signature, this.clName, chopNodes,
					this.cw);
//			((QIFMethodVisitor) _return).setLvs(new LocalVariablesSorter(access, desc, _return));
//			_return = ((QIFMethodVisitor) _return).getLvs();
		}

		return _return;
	}

}
