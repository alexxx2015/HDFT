package edu.tum.uc.jvm.shrift;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.EmptyStackException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Stack;
import java.util.UUID;

import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;

import de.tum.in.i22.uc.cm.datatypes.interfaces.IEvent;
import edu.tum.uc.jvm.UcCommunicator;
import edu.tum.uc.jvm.deprecated.container.ArithResult;
import edu.tum.uc.jvm.deprecated.container.ArrayField;
import edu.tum.uc.jvm.deprecated.container.ArrayRef;
import edu.tum.uc.jvm.deprecated.container.ConstInstr;
import edu.tum.uc.jvm.deprecated.container.Container;
import edu.tum.uc.jvm.deprecated.container.Field;
import edu.tum.uc.jvm.deprecated.container.LocalVariable;
import edu.tum.uc.jvm.deprecated.container.ObjectReference;
import edu.tum.uc.jvm.extractor.FileDescriptorExtractor;
import edu.tum.uc.jvm.utility.ConfigProperties;
import edu.tum.uc.jvm.utility.EventRepository;
import edu.tum.uc.jvm.utility.MethEvent;
import edu.tum.uc.jvm.utility.Mnemonic;
import edu.tum.uc.jvm.utility.StatisticsWriter;
import edu.tum.uc.jvm.utility.Utility;
import edu.tum.uc.jvm.utility.analysis.CreationSite;
import it.uniroma1.dis.wsngroup.gexf4j.core.EdgeType;
import it.uniroma1.dis.wsngroup.gexf4j.core.Gexf;
import it.uniroma1.dis.wsngroup.gexf4j.core.Graph;
import it.uniroma1.dis.wsngroup.gexf4j.core.Mode;
import it.uniroma1.dis.wsngroup.gexf4j.core.Node;
import it.uniroma1.dis.wsngroup.gexf4j.core.data.Attribute;
import it.uniroma1.dis.wsngroup.gexf4j.core.data.AttributeClass;
import it.uniroma1.dis.wsngroup.gexf4j.core.data.AttributeList;
import it.uniroma1.dis.wsngroup.gexf4j.core.data.AttributeType;
import it.uniroma1.dis.wsngroup.gexf4j.core.impl.GexfImpl;
import it.uniroma1.dis.wsngroup.gexf4j.core.impl.StaxGraphWriter;
import it.uniroma1.dis.wsngroup.gexf4j.core.impl.data.AttributeListImpl;
import it.uniroma1.dis.wsngroup.gexf4j.core.impl.viz.ColorImpl;

public class MirrorStack {
	// private static Logger _logger =
	// LoggerFactory.getLogger(MirrorStack.class);


	private static Stack<Container> Stack = new Stack<Container>();

	private static boolean ucaUsed = false;
	public static boolean runnable = true; // preventing the execution of
											// instrumenting code before not all
											// classes are loaded. This variable
											// is only needed for instrumenting
											// Java system classes

	private static long TIMER_T1;// START-END of MAIN-Method
	private static long TIMER_T2;// Complete Source/Sink execution
	protected static long TIMER_T4;

	public static enum COMP_TYPE_CAT {
		COMP_TYPE_CAT_1, COMP_TYPE_CAT_2
	};

	private static HashMap<String, ArrayList<Container>> methodParam = new HashMap<String, ArrayList<Container>>();
	private static HashMap<String, Container> methodInvoker = new HashMap<String, Container>();

	private static UcCommunicator ucCom = UcCommunicator.getInstance();

	private static Stack<CreationSite> creationSiteScopes = new Stack<CreationSite>();
	protected static Map<String, String> contextToObject = new HashMap<String, String>();

	private static Gexf gexf;
	private static Graph graph;
	private static int nodeId = 0;
	private static int attrId = 0;
	private static Attribute fqName;
	private static Attribute offset;
	private static Attribute opcode;
	private static Attribute misc;
	private static Node lastNode;
	private static Map<String, Node> createdNodes = new HashMap<String, Node>();
	

	public static void logChopNode(String chop) {
		if (gexf == null) {
			gexf = new GexfImpl();
			Calendar date = Calendar.getInstance();

			gexf.getMetadata().setLastModified(date.getTime())
					.setCreator("Java Pep")
					.setDescription("Visualizing program's taken runtime path");
			gexf.setVisualization(true);

			graph = gexf.getGraph();
			graph.setDefaultEdgeType(EdgeType.UNDIRECTED).setMode(Mode.STATIC);

			AttributeList attrList = new AttributeListImpl(AttributeClass.NODE);
			graph.getAttributeLists().add(attrList);

			fqName = attrList.createAttribute(String.valueOf(++attrId),
					AttributeType.STRING, "FQName");
			offset = attrList.createAttribute(String.valueOf(++attrId),
					AttributeType.STRING, "Offset");
			opcode = attrList.createAttribute(String.valueOf(++attrId),
					AttributeType.STRING, "Opcode");
			misc = attrList.createAttribute(String.valueOf(++attrId),
					AttributeType.STRING, "Misc");

			Node n = graph.createNode(String.valueOf(++nodeId));
			n.setLabel("Start").setColor(new ColorImpl(0, 153, 0));
			lastNode = n;
		}
		// Structure of each parameter 'chop'
		// this.fqName+"|"+label.getOffset()+"|"+Mnemonic.OPCODE[p_opcode]+"|"+p_owner
		// + "." + p_name + ":" + p_desc+"| -- visitFieldInsn";
		String[] s = chop.split("\\|");
		String label = s[1] + ":" + s[0];

		Node n = null;
		if (createdNodes.containsKey(label)) {
			n = createdNodes.get(label);
		} else {
			n = graph.createNode(String.valueOf(++nodeId));
			n.setLabel(label).getAttributeValues().addValue(fqName, s[0])
					.addValue(offset, s[1]).addValue(opcode, s[2])
					.addValue(misc, s[3]);
			createdNodes.put(label, n);
		}

		if (lastNode != null) {
			if(!lastNode.hasEdgeTo(n.getId())){
				lastNode.connectTo(n);
			}
			lastNode = n;
		} else {
			lastNode = n;
		}
	}

	public static void timerT1Start() {
		TIMER_T1 = System.nanoTime();
	}

	public static void timerT1Stop() {
		StatisticsWriter.logExecutionTimerT1(TIMER_T1, System.nanoTime());
	}

	public static void timerT2Start() {
		TIMER_T2 = System.nanoTime();
	}

	public static void timerT2Stop() {
		StatisticsWriter.logExectionTimerT2(TIMER_T2, System.nanoTime());
	}

	public static void timerT4Start() {
		TIMER_T4 = System.nanoTime();
	}

	public static void timerT4Stop(String p_methName) {
		StatisticsWriter.logExecutionTimerT4(p_methName, System.nanoTime()
				- MirrorStack.TIMER_T4);
	}

	public static void pushCreationSiteScope(String creationSite) {
		// [0] = id
		// [1] = location
		// [2] = offset
		// [3] = classtype
		String[] creationSiteCmp = creationSite.split(":");
		CreationSite cs = new CreationSite();
		cs.setId(creationSiteCmp[0]);
		cs.setLocation(creationSiteCmp[1]);
		cs.setType(creationSiteCmp[3]);
		cs.setOffset(Integer.parseInt(creationSiteCmp[2]));
		creationSiteScopes.push(cs);
	}

	public static void popCreationSiteScope(Object obj, String creationSite) {
		// [0] = id
		// [1] = location
		// [2] = offset
		// [3] = classtype
		String[] creationSiteCmp = creationSite.split(":");
		CreationSite cs = new CreationSite();
		cs.setId(creationSiteCmp[0]);
		cs.setLocation(creationSiteCmp[1]);
		cs.setType(creationSiteCmp[3]);
		cs.setOffset(Integer.parseInt(creationSiteCmp[2]));

		try {
			CreationSite cs_old = creationSiteScopes.pop();
			if (cs_old.getLocation().equals(cs.getLocation())
					&& cs_old.getOffset() <= cs.getOffset()
					&& cs_old.getId().equals(cs.getId())) {
				contextToObject.put(cs.getId(), String.valueOf(obj.hashCode()));
				// ucCom.sendContextEvent2PDP(obj, cs);
			}
		} catch (Exception e) {
		}
	}

	// *** Load ***
	public static void loadVar(String p_varName) {
		if (MirrorStack.lock()) {
			try {
				// Container c = Stack.pop();
				// if(c.getName().equals(p_varName)){
				// Stack.push(c);
				// }else{
				// _logger.info("LOAD {}", p_varName);
				String[] varNameComp = p_varName.split(":");
				if (varNameComp.length >= 2) {
					LocalVariable lv = new LocalVariable(varNameComp[0]);
					lv.setOpcode(Integer.parseInt(varNameComp[1]));
					Stack.push(lv);
				}
				// }
			} catch (EmptyStackException e) {
				Stack.push(new LocalVariable(p_varName));
			}
			MirrorStack.unlock();
		}
	}

	public static void loadVar(Object p_obj, String p_varName) {
		int objId = p_obj.hashCode();
		MirrorStack.loadVar(objId + "/" + p_varName);
	}

	// *** Store primitive data type***
	public static void storeVar(String p_var) {
		if (MirrorStack.lock()) {
			try {
				// _logger.info("STORE {}", p_var);
				String[] varComp = p_var.split(":");
				if (varComp.length >= 2) {
					Container c = Stack.pop();
					p_var = varComp[0];
					int opcode = Integer.parseInt(varComp[1]);
					// LocalVariable lv = new LocalVariable(p_var);
					// lv.addContainer(c);
					// lv.setOpcode(opcode);
					// Stack.push(lv);
					String event = "assignPrim";
					if (opcode == Opcodes.ASTORE) {
						event = "assignRef";
					}

					event += ",name=lftVar:value=" + p_var
							+ ",name=rgtVar:value=" + c.getName();
					// if(sendEvent2PDP(event)){
					// ucCom.sendEvent2PIP(event);
					// }
				}
			} catch (Exception e) {
				e.printStackTrace();
			} finally {

			}
			MirrorStack.unlock();
		}
	}

	public static void storeVar(Object p_obj, String p_varName)
			throws IOException {
		if (MirrorStack.ucaUsed == false) {
			MirrorStack.ucaUsed = true;
			int objId = p_obj.hashCode();
			MirrorStack.ucaUsed = false;
			MirrorStack.storeVar(objId + "/" + p_varName);
		}
	}

	// *** Field ***
	public static void putStaticField(String p_field) {
		if (MirrorStack.lock()) {
			try {
				// _logger.info("PutStaticField {}", p_field);
				String[] fieldComp = p_field.split(":");
				if (fieldComp.length >= 2) {
					Container c = Stack.pop();
					// Field f = new Field(fieldComp[1], true);
					// f.addContainer(c);
					int p_type = Integer.parseInt(fieldComp[1]);
					// f.setOpcode(p_type);

					String event = "assignPrim";
					if ((p_type == Type.ARRAY) || (p_type == Type.OBJECT)) {
						event = "assignRef";
					}
					event += ",name=lftVar:value=" + fieldComp[0]
							+ ",name=rgtVar:value=" + c.getName();
					// if(sendEvent2PDP(event)){
					// ucCom.sendEvent2PIP(event);
					// }
				}
			} catch (Exception e) {
			}
			MirrorStack.unlock();
		}
	}

	public static void putStaticField(Object p_obj, String p_field) {
		if (MirrorStack.ucaUsed == false) {
			MirrorStack.ucaUsed = true;
			int objId = p_obj.hashCode();
			MirrorStack.ucaUsed = false;
			MirrorStack.putStaticField(objId + "/" + p_field);
		}
	}

	public static void getStaticField(String p_field) {
		if (MirrorStack.lock()) {
			// _logger.info("GetStaticField {}", p_field);
			String[] fieldComp = p_field.split(":");
			if (fieldComp.length >= 2) {
				Field f = new Field(fieldComp[0], true);
				f.setOpcode(Opcodes.GETSTATIC);
				Stack.push(new Field(p_field, true));
			}
			MirrorStack.unlock();
		}
	}

	public static void getStaticField(Object p_obj, String p_field) {
		if (MirrorStack.ucaUsed == false) {
			MirrorStack.ucaUsed = true;
			int objId = p_obj.hashCode();
			MirrorStack.ucaUsed = false;
			MirrorStack.getStaticField(objId + "/" + p_field);
		}
	}

	public static void putField(String p_field) {
		if (MirrorStack.lock()) {
			// _logger.info("PutField {}", p_field);
			try {
				String[] fieldComp = p_field.split(":");
				if (fieldComp.length >= 2) {
					Type type = Type.getType(fieldComp[1]);
					Container c = Stack.pop(), refCont = Stack.pop();
					// Field f = new Field(fieldComp[1], false);
					// f.addContainer(c);
					int p_type = type.getSort();
					// f.setOpcode(p_type);

					String event = "assignPrim";
					if (((p_type == Type.ARRAY) || (p_type == Type.OBJECT))
							&& !type.getClassName().equals("java.lang.String")) {
						event = "assignRef";
					}
					event += ",name=lftVar:value=" + fieldComp[0]
							+ ",name=rgtVar:value=" + c.getName();
					// if(sendEvent2PDP(event)){
					// ucCom.sendEvent2PIP(event);
					// }
				}
			} catch (Exception e) {
			}
			MirrorStack.unlock();
		}
	}

	public static void putField(Object p_obj, String p_field) {
		if (MirrorStack.ucaUsed == false) {
			MirrorStack.ucaUsed = true;
			int objId = p_obj.hashCode();
			MirrorStack.ucaUsed = false;
			MirrorStack.putField(objId + "/" + p_field);
		}
	}

	public static void getField(String p_field) {
		if (MirrorStack.lock()) {
			// _logger.info("GetField {}", p_field);
			String[] fieldComp = p_field.split(":");
			if (fieldComp.length >= 2) {
				// Pop "this"-reference from mirror stack
				Stack.pop();
				Field f = new Field(fieldComp[0], false);
				f.setOpcode(Opcodes.GETFIELD);
				Stack.push(f);
			}
			MirrorStack.unlock();
		}
	}

	public static void getField(Object p_obj, String p_field) {
		if (MirrorStack.ucaUsed == false) {
			MirrorStack.ucaUsed = true;
			int objId = p_obj.hashCode();
			MirrorStack.ucaUsed = false;
			MirrorStack.getField(objId + "/" + p_field);
		}
	}

	// *** Const ***
	public static void constLoad(String p_const) {
		if (MirrorStack.lock()) {
			// _logger.info("LoadConst {}", p_const);
			String[] constComp = p_const.split(":");
			if (constComp.length >= 2) {
				// ConstInstr c = new ConstInstr(constComp[1]);
				ConstInstr c = new ConstInstr("");
				c.setOpcode(Integer.parseInt(constComp[1].trim()));
				c.setValue(constComp[0]);
				Stack.push(c);
			}
			MirrorStack.unlock();
		}
	}

	public static void constLoad(Object p_obj, String p_const) {
		if (MirrorStack.ucaUsed == false) {
			MirrorStack.ucaUsed = true;
			int objId = p_obj.hashCode();
			MirrorStack.ucaUsed = false;
			MirrorStack.constLoad(objId + "/" + p_const);
		}
	}

	public static void ldcConstLoad(String p_const) {
		if (MirrorStack.lock()) {
			// _logger.info("LDC {}", p_const);
			String[] constComp = p_const.split(":");
			if (constComp.length >= 2) {
				ConstInstr c = new ConstInstr(constComp[1]);
				c.setOpcode(Opcodes.LDC);
				c.setValue(constComp[1]);
				Stack.push(c);
			}
			MirrorStack.unlock();
		}
	}

	public static void ldcConstLoad(Object p_obj, String p_const) {
		if (MirrorStack.ucaUsed == false) {
			MirrorStack.ucaUsed = true;
			int objId = p_obj.hashCode();
			MirrorStack.ucaUsed = false;
			MirrorStack.ldcConstLoad(objId + "/" + p_const);
		}
	}

	// *** Method ***
	public static boolean methodEntered(Object p_obj, String p_methName) {
		if (MirrorStack.ucaUsed == false) {
			MirrorStack.ucaUsed = true;
			int objId = p_obj.hashCode();
			MirrorStack.ucaUsed = false;
			return MirrorStack.methodEntered(objId + "/" + p_methName);
		}
		return true;
	}

	public static boolean methodEntered(String p_methName) {
		boolean _return = true;
		if (MirrorStack.lock()) {
			// _logger.info("MethodEntered {}", p_methName);
			try {
				// p_opcode+":"+this.getFullName()+":"+p_owner+"/"+p_name+":"+argT.length+":"+retT
				String[] s = p_methName.split(":");
				if (s.length >= 3) {
					String invokee = s[0], methDesc = s[1], methParams = s[2], paramName, pipEv;
					String[] methParam = methParams.split("\\|"), methParamVal;

					Type[] argType = Type.getArgumentTypes(methDesc);
					// Type retType = Type.getReturnType(methDesc);
					ArrayList<Container> paramList = MirrorStack.methodParam
							.get(invokee + "_" + argType.length);
					// System.out.println("UCAHA METHENT: "+p_methName+"; "+invokee+"_"+argType.length);
					ArrayList<String> pipEvs = new ArrayList<String>();
					if ((methParam.length > 0) && (paramList != null)) {
						for (int i = methParam.length - 1; i >= 0; i--) {
							methParamVal = methParam[i].split("#");
							if (methParamVal.length >= 2) {
								if (!methParamVal[0].equals("")) {
									paramName = methParamVal[0];
								} else {
									paramName = methParamVal[1];
								}
								int paramPos = Integer
										.parseInt(methParamVal[1]);// -1;
								Container c = null;
								// try{
								c = paramList.get(paramPos);

								pipEv = "assignPrim";
								if ((argType[i].getSort() == Type.OBJECT)
										|| (argType[i].getSort() == Type.ARRAY)) {
									pipEv = "assignRef";
								}
								pipEv += ",name=lftVar:value=" + invokee + "/"
										+ paramName + ",name=rgtVar:value="
										+ c.getName();
								pipEvs.add(pipEv);
								// }catch(Exception e){
								// System.out.println("UCAHAERR: "+p_methName+"; "+paramPos+"; "+paramList.size());
								// }
							}
						}
						Iterator<String> it = pipEvs.iterator();
						while (it.hasNext()) {
							pipEv = it.next();
							if ((pipEv != null) && !pipEv.equals("")) {
								// System.out.println("PIPEV: "+pipEv);
								// ucCom.sendEvent2PIP(pipEv);
							}
						}
					}
				}
			} catch (EmptyStackException e) {
			}
			MirrorStack.unlock();
		}
		return _return;
	}

	public static void methodExit(String p_methName) {
		if (MirrorStack.lock()) {
			// _logger.info("MethodExit {}", p_methName);
			final String delim = Utility.STRDELIM;
			MethEvent event = new MethEvent(MethEvent.Type.END);
			String[] methNameCmp = p_methName.split(delim);
			// Extract invoker method
			if ((methNameCmp.length >= 1) && (methNameCmp[0] != null)) {
				event.setMethodInvoker(methNameCmp[0]);
			}
			if ((methNameCmp.length >= 2) && (methNameCmp[1] != null)) {
				event.setMethodInvoker(methNameCmp[1]);
			}
			// Extract invokee method
			if ((methNameCmp.length >= 3) && (methNameCmp[2] != null)) {
				event.setMethodInvokee(methNameCmp[2]);
			}
			if ((methNameCmp.length >= 4) && (methNameCmp[3] != null)) {
				event.setMethodInvokeeSig(methNameCmp[3]);
			}
			// Extract offset
			if ((methNameCmp.length >= 5) && (methNameCmp[4] != null)) {
				event.setOffset(Integer.parseInt(methNameCmp[4]));
			}
			// Extract opcode
			if ((methNameCmp.length >= 6) && (methNameCmp[5] != null)) {
				event.setOpcode(Integer.parseInt(methNameCmp[5]));
			}

			// ucCom.sendEvent2PIP(event);
			MirrorStack.unlock();
		}

		// if(MirrorStack.lock() == true){
		// System.out.println("UCAHA methodexit: "+p_methName);
		// try{
		// //p_opcode+":"+this.getFullName()+":"+p_owner+"/"+p_name+":"+argT.length+":"+retT
		// String[] s = p_methName.split(":");
		// if(s.length >= 4){
		// int opcode = Integer.parseInt(s[0]);
		// String invoker = s[1], invokee = s[2];
		// String methDesc = s[3];
		//
		// Type[] argType = Type.getArgumentTypes(methDesc);
		// Type retType = Type.getReturnType(methDesc);
		//
		// if(retType.getSort() != Type.VOID){
		// Container c = Stack.pop();
		// String locVarName = c.getName()+"[RET_"+invokee+"]";
		//
		// LocalVariable lv = new LocalVariable(locVarName);
		// Stack.push(lv);
		// }
		// // if(opcode == Opcodes.INVOKEVIRTUAL){
		// // Stack.pop();
		// // }
		// // if(retType.getSort() != Type.VOID){
		// // LocalVariable lv = new
		// LocalVariable(retType.toString()+"[RET_"+invokee+"]");
		// // Stack.push(lv);
		// // }
		// }
		// }catch(EmptyStackException e){}
		// MirrorStack.unlock();
		// }
	}

	/**
	 * Takes a bytecode method signature description and return a
	 * MethEvent-Object
	 * 
	 * @param p_methName
	 * @return
	 */
	private static MethEvent createMethEventFromString(String p_methName) {
		final String delim = Utility.STRDELIM;
		MethEvent event = new MethEvent(MethEvent.Type.START);
		String[] methNameCmp = p_methName.split(delim);
		// Extract invoker method
		if ((methNameCmp.length >= 1) && (methNameCmp[0] != null)) {
			event.setMethodInvoker(methNameCmp[0]);
		}
		if ((methNameCmp.length >= 2) && (methNameCmp[1] != null)) {
			event.setMethodInvokerSig(methNameCmp[1]);
		}
		// Extract invokee method
		if ((methNameCmp.length >= 3) && (methNameCmp[2] != null)) {
			event.setMethodInvokee(methNameCmp[2]);
		}
		if ((methNameCmp.length >= 4) && (methNameCmp[3] != null)) {
			event.setMethodInvokeeSig(methNameCmp[3]);
		}
		// Extract offset
		if ((methNameCmp.length >= 5) && (methNameCmp[4] != null)) {
			event.setOffset(Integer.parseInt(methNameCmp[4]));
		}
		// Extract opcode
		if ((methNameCmp.length >= 6) && (methNameCmp[5] != null)) {
			event.setOpcode(Integer.parseInt(methNameCmp[5]));
		}
		// Extract sinkOrSource
		if ((methNameCmp.length >= 7) && (methNameCmp[6] != null)) {
			event.setSinkSource(methNameCmp[6]);
		}
		// Context ids
		if ((methNameCmp.length >= 8) && (methNameCmp[7] != null)) {
			event.addContextIds(methNameCmp[7]);
		}
		// Extract file descriptor
		if ((methNameCmp.length >= 9) && (methNameCmp[8] != null)) {
			event.setFileDescriptor(methNameCmp[8]);
		}
		return event;
	}

	// public static boolean methodInvoked(Object p_obj, String p_methName)
	// throws UcException {
	// boolean _return = true;
	// if (MirrorStack.ucaUsed == false) {
	// MirrorStack.ucaUsed = true;
	// int objId = p_obj.hashCode();
	// MirrorStack.ucaUsed = false;
	// _return = MirrorStack.methodInvoked(objId + "/" + p_methName);
	// }
	// return _return;
	// }

	public static void endMain() {
		String statistic = ConfigProperties
				.getProperty(ConfigProperties.PROPERTIES.STATISTICS);
		if (!"".equals(statistic)) {
			StatisticsWriter.dumpFile(statistic);
		}
		try {
			Node n = graph.createNode();
			n.setLabel("End").setColor(new ColorImpl(255,51,0));
			lastNode.connectTo(n);			
			StaxGraphWriter graphWriter = new StaxGraphWriter();
			File f = new File("my_gexf.gexf");
			Writer out = new FileWriter(f);
			graphWriter.writeToStream(gexf, out, "UTF-8");
			System.out.println("Graph written to : " + f.getAbsolutePath());
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		ucCom.sendKillProcessEvent2Pdp();
	}

	public static void systemExit(int status) {
		Boolean b = new Boolean(
				ConfigProperties
						.getProperty(ConfigProperties.PROPERTIES.TIMER_T1));
		if (b) {
			timerT1Stop();
		}
		endMain();
		System.exit(status);
	}

	public static boolean methodInvoked(Object o, String p_methName) {
		IEvent event = EventRepository.getEvent(p_methName);
		if (event == null)
			return false;

		boolean _return;

		FileDescriptorExtractor fdExt = new FileDescriptorExtractor();
		Map<String,String> fileDescriptor = (Map<String, String>) fdExt.extract(o);
		event.getParameters().put("fileDescriptor", fileDescriptor.get("fd"));

		Boolean timer3 = new Boolean(
				ConfigProperties
						.getProperty(ConfigProperties.PROPERTIES.TIMER_T3));
		if (timer3) {
			// System.out.println("MIRROR STACK " + p_methName);
			long start = System.nanoTime();
			// _return = methodInvoked(event);
			_return = ucCom.sendEvent2Pdp(event, p_methName);
			StatisticsWriter.logExecutionTimerT3(p_methName, System.nanoTime()
					- start);
		} else {
			// System.out.println("MIRROR STACK " + p_methName);
			// _return = methodInvoked(event);
			_return = ucCom.sendEvent2Pdp(event, p_methName);
		}

		// Timer T4
		// b = new Boolean(
		// ConfigProperties
		// .getProperty(ConfigProperties.PROPERTIES.TIMER_T4));
		// if (b) {
		// StatisticsWriter.logExecutionTimerT4(p_methName,
		// MirrorStack.TIMER_T4);
		// }

		return _return;
	}

	private static boolean methodInvoked(IEvent event, String p_methName) {
		boolean _return = false;
		;
		String e = ConfigProperties
				.getProperty(ConfigProperties.PROPERTIES.ENFORCEMENT);
		boolean enforcement = new Boolean(e);
		if (enforcement) {
			_return = ucCom.sendEvent2Pdp(event, p_methName);

			if (_return == true) {
				_return = ucCom.sendEvent2Pdp(event, p_methName);
			}
		} else {
			_return = ucCom.sendEvent2Pdp(event, p_methName);
		}
		return _return;
	}

	public static boolean methodInvoked(String p_methName) {
		boolean _return = true;
		if (MirrorStack.lock()) {
			IEvent event = EventRepository.getEvent(p_methName);
			if (event == null)
				return false;
			_return = methodInvoked(event, p_methName);
			MirrorStack.unlock();
		}
		return _return;
	}

	public static boolean methodInvokedOld(String p_methName) {
		// System.out.println("METHODINVOKED: "+p_methName);
		boolean _return = true;
		if (MirrorStack.lock()) {
			// _logger.info("MethodInvoked {}", p_methName);
			MethEvent event = MirrorStack.createMethEventFromString(p_methName);
			String e = ConfigProperties
					.getProperty(ConfigProperties.PROPERTIES.ENFORCEMENT);
			boolean enforcement = new Boolean(e);
			if (enforcement) {
				event.setActual(false);
				_return = ucCom.sendEvent2Pdp(event, p_methName);

				if (_return == true) {
					event.setActual(true);
					_return = ucCom.sendEvent2Pdp(event, p_methName);
				}
			} else {
				event.setActual(true);
				_return = ucCom.sendEvent2Pdp(event, p_methName);
			}
			MirrorStack.unlock();
			// if (_return != true) {
			// throw new UcException("Method invokation not allowed");
			// }
		}
		return _return;
	}

	// public static void methodExited(Object p_obj, String p_methName) {
	// if (MirrorStack.ucaUsed == false) {
	// MirrorStack.ucaUsed = true;
	// int objId = p_obj.hashCode();
	// MirrorStack.ucaUsed = false;
	// MirrorStack.methodExited(objId + "/" + p_methName);
	// }
	// }

	public static void methodExited(Object o, String p_methName) {
		// Timer T4
		Boolean timer4 = new Boolean(
				ConfigProperties
						.getProperty(ConfigProperties.PROPERTIES.TIMER_T4));
		if (timer4) {
			StatisticsWriter.logExecutionTimerT4(p_methName,
					MirrorStack.TIMER_T4);
		}
	}

	public static void methodExitedOld(Object o, String p_methName) {
		Boolean timer3 = new Boolean(
				ConfigProperties
						.getProperty(ConfigProperties.PROPERTIES.TIMER_T3));
		FileDescriptorExtractor fdExt = new FileDescriptorExtractor();
		if (timer3) {
			long start = System.nanoTime();
			Map<String,String> fileDescriptor = (Map<String, String>) fdExt.extract(o);
			p_methName += Utility.STRDELIM + fileDescriptor.get("fd");
			// System.out.println("MIRROR STACK " + p_methName);
			methodExited(p_methName);
			StatisticsWriter.logExecutionTimerT3(p_methName, System.nanoTime()
					- start);
		} else {
			Map<String,String> fileDescriptor = (Map<String, String>) fdExt.extract(o);
			p_methName += Utility.STRDELIM + fileDescriptor.get("fd");
			// System.out.println("MIRROR STACK " + p_methName);
			methodExited(p_methName);
		}

		// Timer T4
		// b = new Boolean(
		// ConfigProperties
		// .getProperty(ConfigProperties.PROPERTIES.TIMER_T4));
		// if (b) {
		// StatisticsWriter.logExecutionTimerT4(p_methName,
		// MirrorStack.TIMER_T4);
		// }
	}

	private static void methodExited(String p_methName) {
		if (MirrorStack.lock()) {
			// _logger.info("MethodExited {}", p_methName);
			final String delim = Utility.STRDELIM;

			MethEvent event = MirrorStack.createMethEventFromString(p_methName);
			event.setType(MethEvent.Type.END);
			event.setActual(true);

			ucCom.sendEvent2Pdp(event, p_methName);

			MirrorStack.unlock();
		}
	}

	// public static void _methodExited(String p_methName) {
	// if (MirrorStack.lock() == true) {
	// try {
	// System.out.println("METH EXITED: " + p_methName);
	// //
	// p_opcode+":"+this.getFullName()+":"+p_owner+"/"+p_name+":"+argT.length+":"+retT
	// String[] s = p_methName.split(":");
	// if (s.length >= 4) {
	// int opcode = Integer.parseInt(s[3]);
	// String invoker = s[0], invokee = s[1];
	// String methDesc = s[2], pipEv2;
	//
	// Type[] argType = Type.getArgumentTypes(methDesc);
	// Type retType = Type.getReturnType(methDesc);
	// Container cntInvokee = null;
	//
	// if (opcode != Opcodes.INVOKESTATIC) {
	// // cntInvokee = Stack.pop();
	// // invokee = cntInvokee.getName();
	// }
	//
	// if (retType.getSort() != Type.VOID) {
	// ObjectReference lv = new ObjectReference(UUID
	// .randomUUID().toString());
	// lv.setType(retType.getClassName().toString()
	// .replace(".", "/"));
	// Stack.push(lv);
	//
	// Container invokeeCnt = MirrorStack.methodInvoker
	// .get(invoker + ":" + invokee);
	// if (invokeeCnt != null) {
	// invokee = invokeeCnt.getName();
	// }
	//
	// String pipEv = "assignPrim";
	// if ((retType.getSort() == Type.OBJECT)
	// || (retType.getSort() == Type.ARRAY)) {
	// if (!retType.getClassName().equals(
	// "java.lang.String")) {
	// pipEv = "assignRef";
	// }
	// }
	// pipEv += ",name=lftVar:value=" + lv.getName()
	// + ",name=rgtVar:value=" + invokee;
	// // ucCom.sendEvent2PIP(pipEv);
	// }
	//
	// // if(opcode == Opcodes.INVOKEVIRTUAL){
	// // Stack.pop();
	// // }
	// // if(retType.getSort() != Type.VOID){
	// // LocalVariable lv = new
	// // LocalVariable(retType.toString()+"[RET_"+invokee+"]");
	// // Stack.push(lv);
	// // }
	// }
	// } catch (EmptyStackException e) {
	// }
	// MirrorStack.unlock();
	// }
	// }

	// *** Aritchmetic instruction ***
	public static void arithInstr(String p_arithInstr) {
		if (MirrorStack.lock()) {
			try {
				// _logger.info("ArtihInstr {}", p_arithInstr);
				String[] arithInstrComp = p_arithInstr.split(":");
				if (arithInstrComp.length >= 2) {
					Container c1 = Stack.pop(), c2 = Stack.pop();
					UUID uuidName = UUID.randomUUID();
					ArithResult arithCont = new ArithResult(
							uuidName.toString(),
							Integer.parseInt(arithInstrComp[1]));
					arithCont.addContainer(c1);
					arithCont.addContainer(c2);
					Stack.push(arithCont);
					String event = "arithComp,name=lftVar:value="
							+ c1.getName() + ",name=rgtVar:value="
							+ c2.getName() + ",name=omega:value="
							+ uuidName.toString();
					// if(sendEvent2PDP(event)){
					// ucCom.sendEvent2PIP(event);
					// }
				}
			} catch (EmptyStackException e) {
				e.printStackTrace();
			}
			MirrorStack.unlock();
		}
	}

	public static void arithInstr(Object p_obj, String p_arithInstr) {
		if (MirrorStack.ucaUsed == false) {
			MirrorStack.ucaUsed = true;
			int objId = p_obj.hashCode();
			MirrorStack.ucaUsed = false;
			MirrorStack.arithInstr(objId + "/" + p_arithInstr);
		}
	}

	// *** Pop instruction ***
	public static void popInstr() {
		if (MirrorStack.lock()) {
			try {
				// _logger.info("POP-Instr");
				Stack.pop();
			} catch (EmptyStackException e) {
			} finally {
				// System.out.println("UCAHA: POP instruction");
			}
			MirrorStack.unlock();
		}
	}

	// *** Branch instruction ***
	public static void ifInstr(String p_ifInstr) {
		if (MirrorStack.lock()) {
			// _logger.info("If-Instr {}", p_ifInstr);
			try {
				String[] ifInstrComp = p_ifInstr.split(":");
				if (ifInstrComp.length >= 2) {
					int opcode = Integer.parseInt(ifInstrComp[1]);

					Stack.pop();
					if ((opcode >= Opcodes.IF_ICMPEQ)
							&& (opcode <= Opcodes.IF_ACMPNE)) {// in case of
																// if_Xcmp pop
																// two stack
																// entries
						Stack.pop();
					}
				}
			} catch (EmptyStackException e) {
			}
			MirrorStack.unlock();
		}
	}

	public static void ifInstr(Object p_obj, String p_ifInstr) {
		if (MirrorStack.ucaUsed == false) {
			MirrorStack.ucaUsed = true;
			int objId = p_obj.hashCode();
			MirrorStack.ucaUsed = false;
			MirrorStack.ifInstr(objId + "/" + p_ifInstr);
		}
	}

	// *** Array intruction ***
	public static void storeArrayVar(String p_var) {
		if (MirrorStack.lock()) {
			// _logger.info("StoreArr {}", p_var);
			try {
				String[] varComp = p_var.split(":");
				if (varComp.length >= 2) {
					int opcode = Integer.parseInt(varComp[1]);
					Container arrVal = Stack.pop(), arrIdx = Stack.pop(), arrRef = Stack
							.pop();
					p_var = varComp[0] + "/" + arrRef.getName() + "["
							+ arrIdx.getName() + "]";
					ArrayField af = new ArrayField(p_var, arrIdx);
					af.setOpcode(opcode);
					af.addContainer(arrVal);
					String event = "assignPrim";
					if (opcode == Opcodes.AASTORE) {
						event = "assignRef";
					}
					event += ",name=lftVar:value=" + af.getName()
							+ ",name=rgtVar:value=" + arrVal.getName();
					// if(sendEvent2PDP(event)){
					// ucCom.sendEvent2PIP(event);
					// }
				}
			} catch (Exception e) {
			} finally {
				// System.out.println("UCAHA: Array store "+p_var);
			}
			MirrorStack.unlock();
		}
	}

	public static void storeArrayVar(Object p_obj, String p_varName) {
		if (MirrorStack.ucaUsed == false) {
			MirrorStack.ucaUsed = true;
			int objId = p_obj.hashCode();
			MirrorStack.ucaUsed = false;
			MirrorStack.storeArrayVar(objId + "/" + p_varName);
		}
	}

	public static void loadArrayVar(String p_var) {
		if (MirrorStack.lock()) {
			// _logger.info("LoadArr {}", p_var);
			String[] varComp = p_var.split(":");
			if (varComp.length >= 2) {
				try {
					Container arrIdx = Stack.pop(), arrName = Stack.pop();
					ArrayField af = new ArrayField(arrName.getName(), arrIdx);
					af.setOpcode(Integer.parseInt(varComp[1]));
					Stack.push(af);
				} catch (EmptyStackException e) {
				} finally {
					// System.out.println("UCAHA: Array load "+p_var);
				}
			}
			MirrorStack.unlock();
		}
	}

	public static void loadArrayVar(Object p_obj, String p_varName) {
		if (MirrorStack.ucaUsed == false) {
			MirrorStack.ucaUsed = true;
			int objId = p_obj.hashCode();
			MirrorStack.ucaUsed = false;
			MirrorStack.loadArrayVar(objId + "/" + p_varName);
		}
	}

	public static void newArray(String p_arr) {
		if (MirrorStack.lock()) {
			// _logger.info("NewArr {}", p_arr);
			try {
				String[] arrComp = p_arr.split(":");
				if (arrComp.length >= 3) {
					Container c = Stack.pop();
					ArrayRef a = new ArrayRef(UUID.randomUUID().toString());
					a.setOpcode(Integer.parseInt(arrComp[2]));
					a.setArrayType(arrComp[1]);
					Stack.push(a);
				}
			} catch (EmptyStackException e) {
			} finally {
				// System.out.println("UCAHA: New Array");
			}
			MirrorStack.unlock();
		}
	}

	public static void newArray(Object p_obj, String p_varName) {
		if (MirrorStack.ucaUsed == false) {
			MirrorStack.ucaUsed = true;
			int objId = p_obj.hashCode();
			MirrorStack.ucaUsed = false;
			MirrorStack.newArray(objId + "/" + p_varName);
		}
	}

	public static void newInstr(String p_new) {
		if (MirrorStack.lock()) {
			// _logger.info("New {}", p_new);
			String[] newComp = p_new.split(":");
			if (newComp.length >= 3) {
				// System.out.println("NEWINSTR "+p_new);
				ObjectReference objRef = new ObjectReference(UUID.randomUUID()
						.toString());
				objRef.setCreator(newComp[0]);
				objRef.setType(newComp[1]);
				objRef.setOpcode(Integer.parseInt(newComp[2]));
				Stack.push(objRef);
			}
			MirrorStack.unlock();
		}
	}

	public static void newInstr(Object p_obj, String p_varName) {
		if (MirrorStack.ucaUsed == false) {
			MirrorStack.ucaUsed = true;
			int objId = p_obj.hashCode();
			MirrorStack.ucaUsed = false;
			MirrorStack.newInstr(objId + "/" + p_varName);
		}
	}

	public static void lengthArray() {
		if (MirrorStack.lock()) {
			// _logger.info("Length-Instr");
			try {
				Container c = Stack.pop();
				ConstInstr cInstr = new ConstInstr(c.getName() + "_length");
				cInstr.setOpcode(Opcodes.ARRAYLENGTH);
				Stack.push(cInstr);
			} catch (EmptyStackException ex) {
			}
			MirrorStack.unlock();
		}
	}

	public static void swapInstr() {
		if (MirrorStack.lock()) {
			// _logger.info("SWAP-Instr");
			try {
				Container c1 = Stack.pop(), c2 = Stack.pop();
				Stack.push(c1);
				Stack.push(c2);
			} catch (EmptyStackException ex) {

			}
			MirrorStack.unlock();
		}
	}

	public static void dupInstr() {
		if (MirrorStack.lock()) {
			// _logger.info("Dup-Instr");
			try {
				Container c1 = Stack.pop();
				Stack.push(c1);
				Stack.push(c1);
			} catch (EmptyStackException ex) {
			}
			MirrorStack.unlock();
		}
	}

	public static void dupX1Instr() {
		if (MirrorStack.lock()) {
			// _logger.info("DupX1-Instr");
			try {
				Container c1 = Stack.pop(), c2 = Stack.pop();
				Stack.push(c1);
				Stack.push(c2);
				Stack.push(c1);
			} catch (EmptyStackException ex) {
			}
			MirrorStack.unlock();
		}
	}

	public static void dupX2Instr() {
		if (MirrorStack.lock()) {
			// _logger.info("DubX2-Instr");
			try {
				Container c1 = Stack.pop(), c2 = Stack.pop();
				if ((MirrorStack.getCompType(c1.getOpcode()) == COMP_TYPE_CAT.COMP_TYPE_CAT_1)
						&& (MirrorStack.getCompType(c2.getOpcode()) == COMP_TYPE_CAT.COMP_TYPE_CAT_2)) {
					Stack.push(c1);
					Stack.push(c2);
					Stack.push(c1);
				} else {
					Container c3 = Stack.pop();
					Stack.push(c1);
					Stack.push(c3);
					Stack.push(c2);
					Stack.push(c1);
				}
			} catch (EmptyStackException ex) {
			}
			MirrorStack.unlock();
		}
	}

	public static void dup2Instr() {
		if (MirrorStack.lock() == true) {
			// _logger.info("Dup2-Instr");
			try {
				Container c1 = Stack.pop();
				if (MirrorStack.getCompType(c1.getOpcode()) == COMP_TYPE_CAT.COMP_TYPE_CAT_2) {
					Stack.push(c1);
					Stack.push(c1);
				} else {
					Container c2 = Stack.pop();
					Stack.push(c2);
					Stack.push(c1);
					Stack.push(c2);
					Stack.push(c1);
				}
			} catch (EmptyStackException ex) {
			}
			MirrorStack.unlock();
		}
	}

	public static void dup2X1Instr() {
		if (MirrorStack.lock()) {
			// _logger.info("Dup2X1-Instr");
			try {
				Container c1 = Stack.pop(), c2 = Stack.pop();
				if ((MirrorStack.getCompType(c1.getOpcode()) == COMP_TYPE_CAT.COMP_TYPE_CAT_2)
						&& (MirrorStack.getCompType(c2.getOpcode()) == COMP_TYPE_CAT.COMP_TYPE_CAT_1)) {
					Stack.push(c1);
					Stack.push(c2);
					Stack.push(c1);
				} else {
					Container c3 = Stack.pop();
					Stack.push(c2);
					Stack.push(c1);
					Stack.push(c3);
					Stack.push(c2);
					Stack.push(c1);
				}
			} catch (EmptyStackException ex) {
			}
			MirrorStack.unlock();
		}
	}

	public static void dup2X2Instr() {
		if (MirrorStack.lock()) {
			// _logger.info("Dup2X2-Instr");
			try {
				Container c1 = Stack.pop(), c2 = Stack.pop();
				if ((MirrorStack.getCompType(c1.getOpcode()) == COMP_TYPE_CAT.COMP_TYPE_CAT_2)
						&& (MirrorStack.getCompType(c2.getOpcode()) == COMP_TYPE_CAT.COMP_TYPE_CAT_2)) {
					Stack.push(c1);
					Stack.push(c2);
					Stack.push(c1);
				} else {
					Container c3 = Stack.pop();
					if ((MirrorStack.getCompType(c1.getOpcode()) == COMP_TYPE_CAT.COMP_TYPE_CAT_1)
							&& (MirrorStack.getCompType(c2.getOpcode()) == COMP_TYPE_CAT.COMP_TYPE_CAT_1)
							&& (MirrorStack.getCompType(c3.getOpcode()) == COMP_TYPE_CAT.COMP_TYPE_CAT_2)) {
						Stack.push(c2);
						Stack.push(c1);
						Stack.push(c3);
						Stack.push(c2);
						Stack.push(c1);
					} else if ((MirrorStack.getCompType(c1.getOpcode()) == COMP_TYPE_CAT.COMP_TYPE_CAT_2)
							&& (MirrorStack.getCompType(c2.getOpcode()) == COMP_TYPE_CAT.COMP_TYPE_CAT_1)
							&& (MirrorStack.getCompType(c3.getOpcode()) == COMP_TYPE_CAT.COMP_TYPE_CAT_1)) {
						Stack.push(c1);
						Stack.push(c3);
						Stack.push(c2);
						Stack.push(c1);
					} else {
						Container c4 = Stack.pop();
						Stack.push(c2);
						Stack.push(c1);
						Stack.push(c4);
						Stack.push(c3);
						Stack.push(c2);
						Stack.push(c1);
					}
				}
			} catch (EmptyStackException ex) {
			}
			MirrorStack.unlock();
		}
	}

	private static boolean lock() {
		if ((MirrorStack.ucaUsed == false) && (MirrorStack.runnable == true)) {
			MirrorStack.ucaUsed = true;
		}
		return MirrorStack.ucaUsed;
	}

	private static boolean unlock() {
		if ((MirrorStack.ucaUsed == true) && (MirrorStack.runnable == true)) {
			MirrorStack.ucaUsed = false;
		}
		return MirrorStack.ucaUsed;
	}

	private static COMP_TYPE_CAT getCompType(int p_opcode) {
		String mnemonic = Mnemonic.OPCODE[p_opcode];
		if (mnemonic.toLowerCase().startsWith("d")
				|| mnemonic.toLowerCase().startsWith("l")) {
			return COMP_TYPE_CAT.COMP_TYPE_CAT_2;
		}
		return COMP_TYPE_CAT.COMP_TYPE_CAT_1;
	}
}

// private static void printStack(){
// System.out.println("--->STACK");
// Iterator it = MirrorStack.Stack.iterator();
// while(it.hasNext()){
// System.out.println(((Container)it.next()).getName());
// }
// System.out.println("<---STACK");
// }

/*
 * //Deprecated Method public static boolean mmethodEntered(String p_methName){
 * boolean _return = true; if(MirrorStack.lock() == true){ try{
 * //p_opcode+":"+this
 * .getFullName()+":"+p_owner+"/"+p_name+":"+argT.length+":"+retT String[] s =
 * p_methName.split(":"); if(s.length >= 4){ int methodAccess =
 * Integer.parseInt(s[0]); String invokee = s[1], methDesc = s[2], methParams =
 * s[3], paramName; String[] methParam = methParams.split("|"), methParamVal;
 * 
 * Type[] argType = Type.getArgumentTypes(methDesc); // Type retType =
 * Type.getReturnType(methDesc);
 * 
 * if(methParam.length > 0){ String event =
 * "java/io/OutputStreamWriter/write,name=methodName:value="+invokee+",";
 * String[] pipParamEv = new String[methParam.length]; // String[] pdpRetEv =
 * new String[argType.length];
 * 
 * for(int i = methParam.length; i > 0; i--){ methParamVal =
 * methParam[i-1].split("#"); if(methParamVal.length >= 2){
 * if(!methParamVal[0].equals("")){ paramName = methParamVal[0]; }else{
 * paramName = methParamVal[1]; } Container c = Stack.pop(); event +=
 * "name=paramName:value="
 * +invokee+"/"+paramName+",name=paramValue:value="+c.getName()+",";
 * 
 * pipParamEv[i-1] = "assignPrim"; if((argType[i-1].getSort() == Type.OBJECT) ||
 * (argType[i-1].getSort() == Type.ARRAY)){ pipParamEv[i-1] = "assignRef"; }
 * pipParamEv[i-1] +=
 * ",name=lftVar:value="+invokee+"/"+paramName+",name=rgtVar:value="
 * +c.getName();
 * 
 * // Process return value // if(retType.getSort() != Type.VOID){ // String
 * event2 = "assignPrim"; // if((retType.getSort() == Type.OBJECT) ||
 * (retType.getSort() == Type.ARRAY)){ // event2 = "assignRef"; // } // event2
 * += ",name=lftVar:value="+retType.toString()+"[RET_"+invokee+"]"+
 * ",name=rgtVar:value="+c.getName(); // pdpRetEv[i-1] = event2; // } } }
 * if(_return = ucCom.sendEvent2PDP(event)){ for(String pipEvent: pipParamEv){
 * if((pipEvent != null) && !pipEvent.equals("")){
 * ucCom.sendEvent2PIP(pipEvent); } } // for(String pdpEvent: pdpRetEv){ //
 * if((pdpEvent != null) && !pdpEvent.equals("") && !pdpEvent.equals(null)){ //
 * sendEvent2PIP(pdpEvent); // } // } } }
 * 
 * if((methodAccess & Opcodes.ACC_STATIC) != Opcodes.ACC_STATIC){ Stack.pop(); }
 * 
 * // if(retType.getSort() != Type.VOID){ // LocalVariable lv = new
 * LocalVariable(retType.toString()+"[RET_"+invokee+"]"); // Stack.push(lv); //
 * } } }catch(EmptyStackException e){} MirrorStack.unlock(); } return _return; }
 * 
 * 
 * public static void methodExitedOld(String p_methName){ if(MirrorStack.lock()
 * == true){ try{
 * //p_opcode+":"+this.getFullName()+":"+p_owner+"/"+p_name+":"+argT
 * .length+":"+retT String[] s = p_methName.split(":"); if(s.length >= 4){ int
 * opcode = Integer.parseInt(s[3]); String invoker = s[1], invokee = s[0];
 * String methDesc = s[2];
 * 
 * Type[] argType = Type.getArgumentTypes(methDesc); Type retType =
 * Type.getReturnType(methDesc);
 * 
 * if(retType.getSort() != Type.VOID){ Container c = Stack.pop(); String event2
 * = "methodExitPrim"; if((retType.getSort() == Type.OBJECT) ||
 * (retType.getSort() == Type.ARRAY)){ event2 = "methodExitRef"; } event2 +=
 * ",name=var:value="
 * +c.getName()+"["+invokee+"]"+",name=retVar:value="+retType.toString();
 * ucCom.sendEvent2PIP(event2); }
 * 
 * // if(opcode == Opcodes.INVOKEVIRTUAL){ // Stack.pop(); // } //
 * if(retType.getSort() != Type.VOID){ // LocalVariable lv = new
 * LocalVariable(retType.toString()+"[RET_"+invokee+"]"); // Stack.push(lv); //
 * } } }catch(EmptyStackException e){} MirrorStack.unlock(); } }
 */

// private static boolean sendEvent2PDP_TCP(String p_event){
// if(pdpSocket == null){
// try {
// pdpSocket = new Socket(PIP_HOST, Integer.parseInt(PIP_PORT));
// } catch (NumberFormatException e) {
// // TODO Auto-generated catch block
// e.printStackTrace();
// } catch (UnknownHostException e) {
// // TODO Auto-generated catch block
// e.printStackTrace();
// } catch (IOException e) {
// // TODO Auto-generated catch block
// e.printStackTrace();
// }
// }
// p_event = "PDP_EVENT:"+p_event;
// if(pdpSocket != null){
// // String _return = sendMessage(p_event, pdpSocket);
// // System.out.println("RETURN "+_return);
// // if(_return.equals("REJECT")){
// // return false;
// // }void
// }
// return true;
// }

// public static boolean beforeMethodInvoke(String p_methName, int uid){
// ucaHA.ucaUsed = true;
// String []str = p_methName.split("/");
// Container pushCont = new Method(p_methName);
// if(str.length>0){
// String[] numParam = str[str.length -1].split(":");
// if(numParam.length > 0){
// String param = numParam[numParam.length-1];
//
// for(int i = 0; i < Integer.parseInt(param); i++){
// Container c = null;
// try{
// c= Stack.pop();
// }catch(EmptyStackException e){}
// if(c != null){
// pushCont.addContainer(c);
// }
// //p_methName += c.param2String();
// }
// }
// }
// Stack.push(pushCont);
//
// String event =
// "MethodInvoked,name=MethodName:value="+uid+"/"+pushCont.param2String();
// // System.out.println(event);
// if(event.toLowerCase().contains("datagramsocket") &&
// event.toLowerCase().contains("send")){
// Iterator<Container> it = Stack.iterator();
// while(it.hasNext()){
// Container c = (Container)it.next();
// //
// System.out.println("--Stack: "+c.getContainerTyp().toString()+", "+c.param2String());
// }
// }
// ucaHA.ucaUsed = false;
// return sendEvent2PDP(event);
// }
//
// public static void afterMethodInvoke(String p_methName){
// // System.out.println("After method invoke "+p_methName);
// Container c = Stack.pop();
// if(c instanceof Method){
// }else{
// Stack.push(c);
// }
// }

/*
 * public static synchronized void methodEntered(String p_methName){//Method
 * entered if((ucaHA.getUcaUsed() == false) && (ucaHA.runnable==true)){
 * ucaHA.setUcaUsed(true); try{ String []str = p_methName.split("/"); String
 * []param = str[str.length-1].split("\\|");
 * 
 * StringBuilder sb = new StringBuilder(); for(int k = 0; k < str.length-1;
 * k++){ sb.append(str[k]).append("/"); }
 * 
 * String event; Container push; LinkedList<Container> ll = new
 * LinkedList<Container>(); for(int k = param.length; k>0; k--){ Container c =
 * Stack.pop(); if(c instanceof MethodInvoked){ c = Stack.pop(); } push = new
 * LocalVariable(sb.toString()+param[k-1]); push.addContainer(c); ll.push(push);
 * event =
 * "MethodEntered,name=methodName:value="+p_methName+",name=paramName:value="
 * +push.getName()+",name=paramValue:value="+c.getName();
 * if(sendEvent2PDP(event)){ sendEvent2PIP(event); } } if(ll.size() > 0){
 * Iterator<Container> it = ll.iterator(); while(it.hasNext()){
 * Stack.push(it.next()); } } }catch(EmptyStackException e){ //
 * e.printStackTrace(); }finally{ //
 * System.out.println("UCAHA: Method entered "+p_methName); }
 * ucaHA.setUcaUsed(false); } }
 * 
 * public static synchronized void methodExit(String p_returnInstr){//Method
 * exit if((ucaHA.getUcaUsed() == false) && (ucaHA.runnable==true)){
 * ucaHA.setUcaUsed(true); try{ String returnInstrComp[] =
 * p_returnInstr.split(":");
 * 
 * if(returnInstrComp[returnInstrComp.length-1].toLowerCase().equals("V".toLowerCase
 * ())){ } else{ Stack.push(new MethodExit(p_returnInstr)); }
 * }catch(EmptyStackException e){} finally{ //
 * System.out.println("UCAHA: Method exit "+p_returnInstr); }
 * ucaHA.setUcaUsed(false); } }
 */

// public static boolean _methodInvoked(String p_methName) {
// boolean _return = true;
// if (MirrorStack.lock() == true) {
// System.out.println("UCAHA methinvok: " + p_methName);
// try {
// // System.out.println("METH INV: "+p_methName);
// String[] s = p_methName.split(":");
// if (s.length >= 4) {
// int opcode = Integer.parseInt(s[3]);
// String invoker = s[0], invokee = s[1];
// String methDesc = s[2];
// String[] invokeeCmp = invokee.split("/");
//
// Type[] argType = Type.getArgumentTypes(methDesc);
// Type retType = Type.getReturnType(methDesc);
//
// String event, pipEv, pipEv2;
// ArrayList<String> pipEvs = new ArrayList<String>();
// // String[] pdpRetEv = new String[argType.length];
// Container cntInvokee = null, cntParam;
// ArrayList<Container> arList = new ArrayList<Container>();
//
// // First pop all method parameters from stack and store them
// // temporarily in an array list
// if (argType.length > 0) {
// arList = new ArrayList<Container>();
// ArrayList<Container> arList2 = new ArrayList<Container>();
// for (int i = 0; i < argType.length; i++) {
// arList.add(Stack.pop());
// }
// for (int i = arList.size() - 1; i >= 0; i--) {
// //
// System.out.println("UCAHA METHINV: "+p_methName+"; "+arList.get(i).getName());
// arList2.add(arList.get(i));
// }
// arList = arList2;
// }
// // System.out.println("UCAHA METHINV ADD: "+invokee+"_"+arList.size());
// MirrorStack.methodParam.put(invokee + "_" + arList.size(),
// arList);
//
// // If method invocation is not static then pop also the
// // reference variable from stack
// if (opcode != Opcodes.INVOKESTATIC) {
// cntInvokee = Stack.pop();
// MirrorStack.methodInvoker.put(invoker + ":" + invokee,
// cntInvokee);
// }
//
// // System.out.println("METHODINVOKED: "+p_methName+", "+arList.size());
// // process method parameters
// if (arList.size() > 0) {
// String pdpInvokee = invokee;
// try {
// Integer.parseInt(invokeeCmp[0].trim());
// StringBuilder sb = new StringBuilder();
// for (int i = 1; i < invokeeCmp.length; i++) {
// sb.append(invokeeCmp[i]);
// sb.append("/");
// }
// pdpInvokee = sb.toString().substring(0,
// sb.toString().length() - 1);
// } catch (NumberFormatException e) {
// }
//
// event = pdpInvokee + ",";// +"java/io/OutputStreamWriter/write,";
//
// for (int i = argType.length; i > 0; i--) {
// // event +=
// //
// "name=paramName:value="+pdpInvokee+"/"+i+",name=paramValue:value="+arList.get(i-1).getName()+",";
// // event +=
// //
// "name=name:value="+pdpInvokee+"/"+i+",name=value:value="+arList.get(i-1).getName()+",";
// event += "name=" + pdpInvokee + "/" + i + ":value="
// + arList.get(i - 1).getName() + ",";
//
// pipEv = "assignPrim";
// pipEv2 = "flowInRef";
// if ((argType[i - 1].getSort() == Type.OBJECT)
// || (argType[i - 1].getSort() == Type.ARRAY)) {
// if (!argType[i - 1].getClassName().equals(
// "java.lang.String")) {
// pipEv = "assignRef";
// }
// }
// if (cntInvokee != null) {
// pipEv += ",name=lftVar:value="
// + cntInvokee.getName() + "/"
// + invokeeCmp[invokeeCmp.length - 1]
// + "/" + i + ",name=rgtVar:value="
// + arList.get(i - 1).getName();
// pipEv2 += ",name=refVar:value="
// + cntInvokee.getName()
// + ",name=var:value="
// + arList.get(i - 1).getName();
// } else {
// pipEv += ",name=lftVar:value=" + invokee + "/"
// + i + ",name=rgtVar:value="
// + arList.get(i - 1).getName();
// pipEv2 += ",name=refVar:value=" + invokee
// + ",name=var:value="
// + arList.get(i - 1).getName();
// }
// pipEvs.add(pipEv);
// pipEvs.add(pipEv2);
// }
//
// // if(_return = ucCom.sendEvent2PDP(event)){
// // Iterator<String> it = pipEvs.iterator();
// // while(it.hasNext()){
// // pipEv = it.next();
// // if(!pipEv.equals("") && !pipEv.equals(null)){
// // ucCom.sendEvent2PIP(pipEv);
// // }
// // }
// // /*for(String pdpEvent: pdpRetEv){
// // if((pdpEvent != null) && !pdpEvent.equals("") &&
// // !pdpEvent.equals(null)){
// // sendEvent2PIP(pdpEvent);
// // }
// // }*/
// // }
// }
//
// if (cntInvokee != null) {
// // Stack.push(cntInvokee);
// }
//
// // Process return value
// // if(retType.getSort() != Type.VOID){
// // String event2 = "assignPrim";
// // if((retType.getSort() == Type.OBJECT) ||
// // (retType.getSort() == Type.ARRAY)){
// // event2 = "assignRef";
// // }
// // event2 +=
// //
// ",name=lftVar:value="+retType.toString()+"[RET_"+invokee+"]"+",name=rgtVar:value="+c.getName();
// // pdpRetEv[i-1] = event2;
// // }
// // if(retType.getSort() != Type.VOID){
// // LocalVariable lv = new
// // LocalVariable(retType.toString()+"[RET_"+invokee+"]");
// // Stack.push(lv);
// // }
// }
// } catch (EmptyStackException e) {
// e.printStackTrace();
// }
// MirrorStack.unlock();
// }
// return _return;
// }