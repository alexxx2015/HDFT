package edu.tum.uc.jvm;

import java.io.DataOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.IllegalClassFormatException;
import java.security.ProtectionDomain;

import org.apache.log4j.Logger;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.ClassNode;

import edu.tum.uc.jvm.declassification.qif.QIFClassVisitor;
import edu.tum.uc.jvm.instrum.InstrumDelegate;
import edu.tum.uc.jvm.instrum.MyClassReader;
import edu.tum.uc.jvm.instrum.MyClassWriter;
import edu.tum.uc.jvm.utility.ConfigProperties;
import edu.tum.uc.jvm.utility.EventRepository;
import edu.tum.uc.jvm.utility.StatisticsWriter;
import edu.tum.uc.jvm.utility.Utility;
/**
 * Transforms a single class. Takes a class as a bytecodestream. Uses internally visitor classses for quantitative information flow tracking
 * @author alex
 *
 */
public class QIFUCTransformer implements ClassFileTransformer {
	
	private static Logger _logger = Logger.getLogger(QIFUCTransformer.class.getName());
	
	private static TomcatClassLoader myClassLoader;

	private static ProtectionDomain myProtDom;

	// true if running instrumentation in a webservice
	private boolean instrument_webservice;

	{
		// Initialize pdp communication
//		UcCommunicator.getInstance().initPDP();

		// Populate PIP
		// Utility.populatePip(ConfigProperties.getProperty(ConfigProperties.PROPERTIES.ANALYSIS_REPORT));

		// Create for all sinks and sources a corresponding object in the event
		// repository
		EventRepository.createEventObjects(ConfigProperties
				.getProperty(ConfigProperties.PROPERTIES.ANALYSIS_REPORT));
	}

	public QIFUCTransformer() {
		this(false);
	}

	public QIFUCTransformer(boolean p_instrument_webservice) {
		this.instrument_webservice = p_instrument_webservice;
	}

	// Run instruction: java -javaagent:uc4jvm.jar -Djava.security.manager
	// -Djava.security.policy=mypolicy.txt -Xverify:none helloWorld

	private void setClassLoader(ClassLoader p_myClassLoader) {
		QIFUCTransformer.myClassLoader = (TomcatClassLoader) p_myClassLoader;
	}

	public static ClassLoader getMyClassLoader() {
		return QIFUCTransformer.myClassLoader;
	}

	private void setProtectionDomain(ProtectionDomain p_protectionDomain) {
		QIFUCTransformer.myProtDom = p_protectionDomain;
	}

	@Override
	public byte[] transform(ClassLoader loader, String className,
			Class<?> classBeingRedefined, ProtectionDomain protectionDomain,
			byte[] classfileBuffer) throws IllegalClassFormatException {

		if (this.instrument_webservice) {
			this.setClassLoader(loader);
			this.setProtectionDomain(protectionDomain);
		}
		
		if(!Utility.isWhitelisted(className)){
			if (Utility.isBlacklisted(className)) {
				return classfileBuffer;
			}
			return classfileBuffer;
		}

		String statistic = ConfigProperties
				.getProperty(ConfigProperties.PROPERTIES.STATISTICS);
		// log time for instrumentation
		long start_instrumentation = 0;
		if (!"".equals(statistic)) {
			start_instrumentation = System.nanoTime();
		}

		MyClassReader cr = new MyClassReader(classfileBuffer);
		ClassNode cn = new ClassNode();
		cr.accept(cn, 0);

		MyClassWriter cw = new MyClassWriter(cr, ClassWriter.COMPUTE_MAXS
				| ClassWriter.COMPUTE_FRAMES);// ClassWriter cw = new
												// ClassWriter(cr,
												// ClassWriter.COMPUTE_MAXS |
												// ClassWriter.COMPUTE_FRAMES);
		ClassVisitor cv = new QIFClassVisitor(Opcodes.ASM5, cw, cn);
		cr.accept(cv, ClassReader.EXPAND_FRAMES);
		if (!"".equals(statistic)) {
			StatisticsWriter.logInstrumentation(cn, cw.toByteArray(),
					System.nanoTime() - start_instrumentation);
		}

		// Dump instrumented bytecode if INSTRUMENTED_CLASS_PATH is set in
		// configuration file
		String s = ConfigProperties
				.getProperty(ConfigProperties.PROPERTIES.INSTRUMENTED_CLASS_PATH);
		if ((s != null) && !s.equals("")) {			
			try {
				File f = new File(s + cr.getClassName().replace("/", "_")
						+ ".class");
				if (!f.getParentFile().exists()) {
				    f.getParentFile().mkdirs();
				}
				if (!f.exists()) {
					f.createNewFile();
				}
				DataOutputStream dos = new DataOutputStream(
						new FileOutputStream(f));
				dos.write(cw.toByteArray());
			} catch (FileNotFoundException e) {
				e.printStackTrace();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}

		// Add class name to list of instrumented classes in the instrumentation
		// delegate
		InstrumDelegate.addInstrumentedClassName(cr.getClassName());

		// return the instrumented class
		return cw.toByteArray();
	}
}
