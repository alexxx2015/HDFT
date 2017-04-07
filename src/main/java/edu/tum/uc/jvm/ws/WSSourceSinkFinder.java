package edu.tum.uc.jvm.ws;

import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.LinkedList;
import java.util.List;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;

import edu.tum.uc.jvm.ws.ServiceClassVisitor.SinkSourceSpec;

/**
 * This class find sinks and sources in
 * 
 * @author alex
 *
 */
public class WSSourceSinkFinder {
	private static String classFolderPath = "/home/alex/git_repos/streamapp";
	private static String dumpPath = "/home/alex/instrumented/";

	public static void main(String[] args) throws IOException {
		// Fetch WSDL file
		// String wsdlFilePath = "servicemain.wsdl";// "sadd.wsdl";
		// URL wsdlFile = ClassLoader.getSystemClassLoader().getResource(
		// wsdlFilePath);
		// if (wsdlFile == null) {
		// System.err.println("Provide a valid WSDL file, " + wsdlFilePath
		// + " not found or does not exist");
		// System.exit(1);
		// }

		// Create WSDL parser
		// WSDLParser parser = new WSDLParser();
		// Definitions def = parser.parse(wsdlFile.getPath());
		// for (Service s : def.getServices()) {
		// System.out.println(s.getName());
		// }
		// for (PortType p : def.getPortTypes()) {
		// for (Operation o : p.getOperations())
		// System.out.println(p.getName() + ": " + o.getName());
		// }
		
		if(args.length > 0 && !"".equals(args[0])){
			classFolderPath = args[0];
		}
		if(args.length > 0 && !"".equals(args[1])){
			dumpPath = args[1];
		}
		
		File classFolder = new File(classFolderPath);
		if (!classFolder.exists() || !classFolder.isDirectory()) {
			System.err.println("Add a valid class folder, " + classFolder
					+ " does not exist");
			System.exit(1);
		}

		// Find possible webservice classes and methods
		List<File> toBeAnalyzedFiles = new LinkedList<File>();
		Path p = Files.walkFileTree(classFolder.toPath(),
				new SimpleFileVisitor<Path>() {
					@Override
					public FileVisitResult visitFile(Path file,
							BasicFileAttributes attrs) {
						String fileName = file.getFileName().toString();
						if (fileName.endsWith(".class"))
							toBeAnalyzedFiles.add(file.toFile());
						return FileVisitResult.CONTINUE;
					}
				});
		List<ServiceClassAnalyzer> poi = new LinkedList<ServiceClassAnalyzer>();
		if (toBeAnalyzedFiles != null) {
			ServiceClassAnalyzer ca;
			List<ServiceClassAnalyzer> sei = new LinkedList<ServiceClassAnalyzer>();
			List<ServiceClassAnalyzer> classes = new LinkedList<ServiceClassAnalyzer>();
			for (File f : toBeAnalyzedFiles) {
				ca = new ServiceClassAnalyzer(f);
				if (ca.hasJaxWSAnnotation() && ca.isInterface()) {
					// List<MethodNode> webmethods = ca.getWebMethods();
					// if(webmethods != null && webmethods.size() > 0){
					sei.add(ca);
					
					//Dump interfaces to the same folder as instrumented files
					FileInputStream fis = new FileInputStream(ca.getFilename());
					byte[] b = new byte[(int)f.length()];
					fis.read(b);
					dumpModifiedClass(b,ca.getClassName(),ca.getFilename());
					// }
				}
				else if (ca.hasJaxRSAnnotation()){
//					classes.add(ca);
					poi.add(ca);
				}
				else if (ca.getInterfaces().size() > 0) {
					classes.add(ca);
//					poi.add(ca);
				}
				else{
					System.out.println("Nothing to do for "+f.getAbsoluteFile());
					ClassWriter cw = new ClassWriter(ca.getClassReader(),
							ClassWriter.COMPUTE_FRAMES | ClassWriter.COMPUTE_MAXS);
					ca.getClassReader().accept(cw, ClassReader.EXPAND_FRAMES);
					dumpModifiedClass(cw.toByteArray(), ca.getClassName(), ca.getFilename());
				}
			}
			// Check if classes implements sei
			for (ServiceClassAnalyzer wca1 : sei) {
				for (ServiceClassAnalyzer wca2 : classes) {
					if (wca2.hasInterface(wca1) && !poi.contains(wca2)) {
						poi.add(wca2);
					}
				}
			}
			
			// If we found some points of interest, i.e. classes that implements some service endpoint interfaces, than rewrite those classes.
			if (poi.size() > 0) {
				rewriteServiceInterfaceInvocations(poi, sei);
			}
			if(ServiceClassVisitor.SINKLIST.size() > 0){
				for(SinkSourceSpec sink : ServiceClassVisitor.SINKLIST){
					StringBuilder s = new StringBuilder();
					s.append("<sink ");
					s.append("class=\"L"+sink.getClazz()+"\" ");
					s.append("selector=\""+sink.getSelector()+"\" ");
					s.append("params=\""+sink.getParams()+"\" ");
					s.append("/>");
					System.out.println(s.toString());
				}
			}
		}
	}

	private static void rewriteServiceInterfaceInvocations(
			List<ServiceClassAnalyzer> poi, List<ServiceClassAnalyzer> sei) {
		for (ServiceClassAnalyzer wca : poi) {
			ClassWriter cw = new ClassWriter(wca.getClassReader(),
					ClassWriter.COMPUTE_FRAMES | ClassWriter.COMPUTE_MAXS);
			ServiceClassVisitor scv = new ServiceClassVisitor(Opcodes.ASM5, cw,
					wca, sei);
			wca.getClassReader().accept(scv, ClassReader.EXPAND_FRAMES);
			dumpModifiedClass(cw.toByteArray(), wca.getClassName(), wca.getFilename());
		}
	}

	private static void dumpModifiedClass(byte[] bytecode, String classname, String filepath) {
		try {
			String prefix = filepath.substring(0, filepath.indexOf(classname)).substring(classFolderPath.length()+1);
			
			File f = new File(dumpPath+prefix+classname + ".class");//.replace("/", "_")
			if (!f.getParentFile().exists()) {
				f.getParentFile().mkdirs();
			}
			if (!f.exists()) {
				f.createNewFile();
			}
			DataOutputStream dos = new DataOutputStream(new FileOutputStream(f));
			dos.write(bytecode);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
}
