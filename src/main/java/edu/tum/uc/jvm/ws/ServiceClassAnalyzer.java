package edu.tum.uc.jvm.ws;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.LinkedList;
import java.util.List;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.AnnotationNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodNode;

/**
 * This class searches and detects if a class or a methods correspond to a service method or class
 * @author alex
 *
 */
public class ServiceClassAnalyzer {

	public static final String JAXWSMETHOD = "Ljavax/jws/WebMethod;";
	public static final String JAXWSSERVICE = "Ljavax/jws/WebService;";
	public static final String JAXRSPATH = "Ljavax/ws/rs/Path;";
	public static final String JAXRSGET = "Ljavax/ws/rs/GET;";
	

	private String fileName;
	private ClassReader cr;
	private ClassNode cn;
	private byte[] bytecode;

	public ServiceClassAnalyzer(File f) {
		this(f.getAbsolutePath());
	}

	public ServiceClassAnalyzer(String fileName) {
		this.fileName = fileName;
		File f = new File(fileName);
		bytecode = new byte[(int) f.length()];
		try {
			FileInputStream fis = new FileInputStream(f);
			fis.read(bytecode);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		this.cr = new ClassReader(bytecode);
		this.cn = new ClassNode();
		this.cr.accept(this.cn, 0);
	}

	//Check whether class has jax-ws annotation
	public boolean hasJaxWSAnnotation() {
		boolean _return = false;
		if (this.cn.visibleAnnotations != null) {
			for (Object o : this.cn.visibleAnnotations) {
				AnnotationNode an = (AnnotationNode) o;
				if (ServiceClassAnalyzer.JAXWSSERVICE.equals(an.desc)) {
					_return = true;
					break;
				}
			}
		}
		return _return;
	}
	
	//Check whether class jax-rs annotation
	public boolean hasJaxRSAnnotation(){			
		boolean _return = false;
		if(this.cn.visibleAnnotations != null){
			for(Object o: this.cn.visibleAnnotations){
				AnnotationNode an = (AnnotationNode)o;
				if(ServiceClassAnalyzer.JAXRSPATH.equals(an.desc)){
					_return = true;
					break;
				}
			}
		}
		return _return;
	}

	public List<MethodNode> getWebMethods() {
		List<MethodNode> _return = new LinkedList<MethodNode>();
		if (this.cn.methods != null) {
			for (Object o1 : this.cn.methods) {
				MethodNode mn = (MethodNode) o1;
				if (mn.visibleAnnotations != null) {
					for (Object o2 : mn.visibleAnnotations) {
						AnnotationNode an = (AnnotationNode) o2;
						if (JAXWSMETHOD.equals(an.desc)) {
							_return.add(mn);
							break;
						}
					}
				}
			}
		}
		return _return;
	}
	
	public boolean isInterface(){
		return (this.cn.access & Opcodes.ACC_INTERFACE) == Opcodes.ACC_INTERFACE ? true : false;
	}
	
	public List<String> getInterfaces(){
		return this.cn.interfaces;
	}
	
	public String getClassName(){
		return this.cn.name;
	}
	
	public boolean hasInterface(ServiceClassAnalyzer wca){
		boolean _return = false;
		for(Object o : this.cn.interfaces){
			String myintf = (String) o;
			String classname = wca.getClassName();
			if(myintf.equals(classname)){
				_return = true;
				break;
			}
		}
		return _return;
	}
	
	public boolean hasMethod(String name, String desc){
		boolean _return = false;
		for(Object o : this.cn.methods){
			MethodNode mn = (MethodNode) o;
			if(mn.name.equals(name) && mn.desc.equals(desc)){
				_return = true;
				break;
			}
		}
		return _return;
	}
	
	public ClassReader getClassReader(){
		return this.cr;
	}
	public byte[] getBytecode(){
		return this.bytecode;
	}
	public String getFilename(){
		return this.fileName;
	}
}
