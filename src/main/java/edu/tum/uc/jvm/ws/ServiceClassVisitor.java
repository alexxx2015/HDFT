package edu.tum.uc.jvm.ws;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

import edu.tum.uc.jvm.utility.analysis.StaticAnalysis.NODETYPE;

public class ServiceClassVisitor extends ClassVisitor {
	static Map<String, String> METHODS = new HashMap<String, String>();
	static List<SinkSourceSpec> SOURCELIST = new LinkedList<SinkSourceSpec>();
	static List<SinkSourceSpec> SINKLIST = new LinkedList<SinkSourceSpec>();

	private String classname;
	private ServiceClassAnalyzer wca;
	private List<ServiceClassAnalyzer> sei;
	private ClassVisitor deepClassWriter;

	public ServiceClassVisitor(int arg0) {
		super(arg0);
		// TODO Auto-generated constructor stub
	}

	public ServiceClassVisitor(int arg0, ClassVisitor cv,
			ServiceClassAnalyzer wca, List<ServiceClassAnalyzer> sei) {
		super(arg0, cv);
		this.wca = wca;
		this.sei = sei;
		this.deepClassWriter = cv;
	}

	public MethodVisitor visitMethod(int access, String name, String desc,
			String signature, String[] exceptions) {
		
//		SinkSourceSpec source = new SinkSourceSpec(NODETYPE.SOURCE);
//		source.setClazz(this.classname);
//		source.setSelector(name+desc);
//		source.setParams();
//		ServiceClassVisitor.SOURCELIST.add(source);

		MethodVisitor mv = cv.visitMethod(access, name, desc, signature,
				exceptions);

		mv = new ServiceMethodVisitor(Opcodes.ASM5, mv, this.wca, name,
				this.sei, this.deepClassWriter);
		return mv;
	}
	
	public static class SinkSourceSpec{
		private String clazz;
		private String selector;
		private String params;
		private NODETYPE type;
		
		public SinkSourceSpec(NODETYPE type){
			this.type = type;
		}

		public String getClazz() {
			return clazz;
		}

		public void setClazz(String clazz) {
			this.clazz = clazz;
		}

		public String getSelector() {
			return selector;
		}

		public void setSelector(String selector) {
			this.selector = selector;
		}

		public String getParams() {
			return params;
		}

		public void setParams(String params) {
			this.params = params;
		}
	}

}
