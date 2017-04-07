package edu.tum.uc.jvm;

import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStream;
import java.lang.instrument.IllegalClassFormatException;

import org.apache.commons.io.IOUtils;
import org.apache.log4j.Logger;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.ClassNode;

import edu.tum.uc.jvm.instrum.MyClassReader;
import edu.tum.uc.jvm.instrum.MyClassVisitor;
import edu.tum.uc.jvm.instrum.MyClassWriter;
import edu.tum.uc.jvm.utility.ConfigProperties;
import edu.tum.uc.jvm.utility.Utility;

/**
 * Main Instrumentor class for offline instrumentation. 
 * @author alex
 *
 */

//JVM property to format loggin output: -Djava.util.logging.SimpleFormatter.format='%1$tY-%1$tm-%1$td %1$tH:%1$tM:%1$tS %4$s %2$s %5$s%6$s%n'
public class Instrumentor {

	private static Logger _logger = Logger.getLogger(Instrumentor.class.getName());

	// arg[0] = Source-Directory of to be instrumented files
	// arg[1] = Destination-Directory of instrumented files
	// arg[2] = uc.config
	public static void main(String[] arg) throws IOException, IllegalClassFormatException {
		// arg = new
		// String[]{"/home/osn/ws_securibench/JavaFTP/bin","/home/osn/instrumented","/home/osn/uc-reports/uc-jftp.config"};
//		arg = new String[] { "/home/alex/git_repos/io-flow-analyzer/examples/classes/bin", "/home/alex/instrumented",
//				"/home/alex/git_repos/uc4java/src/test/resources/uc-config/uc-userauth.config" };
		if (arg.length != 3)
			throw new IllegalArgumentException(
					"Number of commandline parameters insufficient. arg[0]=SourceDir, arg[1]=DestDir, arg[2]=uc.config");

		// Check if passed directories exist
		File sourceDir = new File(arg[0]);
		File destDir = new File(arg[1]);
		File ucConfig = new File(arg[2]);
		if (!ucConfig.exists()) {
			throw new FileNotFoundException("File " + ucConfig.getName() + " does not exist or cannot be found");
		}
		if (!sourceDir.exists()) {
			_logger.info("Creating source dir " + sourceDir.getName());
			sourceDir.mkdirs();
		}
		if (!destDir.exists()) {
			_logger.info("Creating destination dir " + destDir.getName());
			destDir.mkdirs();
		}
		_logger.info("Reading configuration file: " + ucConfig.getAbsolutePath());
		ConfigProperties.setConfigFile(ucConfig.getAbsolutePath());

		_logger.info("Search for classfiles in " + sourceDir);
		traverseDir(sourceDir, destDir, sourceDir, ucConfig);
	}

	private static void traverseDir(File sourceDir, File destDir, File currentDir, File ucConfig)
			throws IllegalClassFormatException, IOException {
		// Do instrumentation for each JavaClassFile
		for (File f : currentDir.listFiles()) {
			if (f.isDirectory()) {
				traverseDir(sourceDir, destDir, f, ucConfig);
				continue;
			}
			if (!f.getName().endsWith(".class")) {
				// TODO: just copy the file before continue
				continue;
			}

			InputStream is = new FileInputStream(f);
			byte[] raw_bytecode = IOUtils.toByteArray(is);
			byte[] instrumentedClass = transform(raw_bytecode);

			String sourceClassName = f.getAbsolutePath();
			String destClassName = sourceClassName.replace(sourceDir.getAbsolutePath(), destDir.getAbsolutePath());
			// Dump instrumented Class
			try {
				File dumpFile = new File(destClassName);
				if (!dumpFile.getParentFile().exists()) {
					dumpFile.getParentFile().mkdirs();
				}
				if (!dumpFile.exists()) {
					dumpFile.createNewFile();
				}
				DataOutputStream dos = new DataOutputStream(new FileOutputStream(dumpFile));
				dos.write(instrumentedClass);
			} catch (FileNotFoundException e) {
				e.printStackTrace();
			} catch (IOException e) {
				e.printStackTrace();
			}

		}
	}

	private static byte[] transform(byte[] classfileBuffer) throws IllegalClassFormatException {
		// Read class
		MyClassReader cr = new MyClassReader(classfileBuffer);
		ClassNode cn = new ClassNode();
		cr.accept(cn, 0);
		String className = cr.getClassName();

		// Only instrument whitelisted classes and they are not allowed to be in
		// the blacklist
		if (!Utility.isWhitelisted(className)) {
			if (Utility.isBlacklisted(className)) {
				return classfileBuffer;
			}
			return classfileBuffer;
		}

		// _logger.info("Start instrumenting " + className);
		MyClassWriter cw = new MyClassWriter(cr, ClassWriter.COMPUTE_MAXS | ClassWriter.COMPUTE_FRAMES);

		ClassVisitor cv = new MyClassVisitor(Opcodes.ASM5, cw, cn);
		// ClassVisitor cv = new QIFClassVisitor(Opcodes.ASM5, cw, cn);
		cr.accept(cv, ClassReader.EXPAND_FRAMES);
		// _logger.info("Done instrumenting " + className);
		return cw.toByteArray();
	}
	/*
	static class JavaClassFileFilter implements FilenameFilter {
		@Override
		public boolean accept(File dir, String name) {
			return name.endsWith("\\.class");
		}
	}
	*/

}
