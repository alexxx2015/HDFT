package edu.tum.uc.jvm.instrum;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.sql.Timestamp;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Invocation;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.glassfish.jersey.uri.UriComponent;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import edu.tum.uc.jvm.utility.ConfigProperties;
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

public class ChopNodeDumper implements Runnable {

	public static enum REQPARAM {
		APPID, MSG
	};

	public static enum JSONMsg {
		NODES, LINKS, FQNAME, OFFSET, OPCODE, MISC, ID, SOURCE, TARGET
	}

	// helper class for chop nodes, serves just as a container
	private class ChopNode {
		String fqName;
		String offset;
		String opcode;
		String misc;
		String label;
		ChopNode prev;
		ChopNode next;
		// contains the graph gephi node object
		Node graph_node;

		public String getLabel() {
			return label;
		}
	}

	// start thread that processes read events
	public static CountDownLatch countDown;
	public static ExecutorService exec;

	private static BlockingQueue<String> queue = new LinkedBlockingQueue<String>();
	private static Gexf gexf;
	private static Graph graph;
	private static int nodeId = 0;
	private static int attrId = 0;
	private static Map<Integer, Attribute> attributeMap = new HashMap<Integer, Attribute>();
	private static Attribute fqName;
	private static Attribute offset;
	private static Attribute opcode;
	private static Attribute misc;
	private static ChopNode lastNode;
	private static Map<String, ChopNode> createdChopNodes = new HashMap<String, ChopNode>();

	private static Client client = null;
	private static WebTarget target = null;
	private static String ucWebMgmUrl = null;
	private static boolean webMgmUrl;
	private static String appId = null;
	
	static {
		// new Thread(new ChopNodeDumper()).start();
		int cores = 1;// Runtime.getRuntime().availableProcessors();
		countDown = new CountDownLatch(cores);
		exec = Executors.newFixedThreadPool(cores);
		exec.submit(new ChopNodeDumper());
		add2Queue("START|---|---|---|---");
	}

	public static void add2Queue(String s) {
		queue.add(s);
	}

	public static void addEndNode() {
		// if (webMgmUrl) {
		// addNodeAndSend(n);
		// jzip.JZip|main([Ljava/lang/String;)V|39|invokevirtual|jzip/JZip.start:()V|--
		// visitMethodInsn
		add2Queue("END|---|---|---|---");
		try {
			countDown.await();
			exec.shutdown();
		} catch (Exception e) {

		}
		// } else {
		// addNodeAndDump(n);
		// }
	}

	@Override
	public void run() {
		ucWebMgmUrl = ConfigProperties.getProperty(ConfigProperties.PROPERTIES.UCWEBMGMURL);
		appId = ConfigProperties.getProperty(ConfigProperties.PROPERTIES.APPID);
		webMgmUrl = !"".equals(ucWebMgmUrl);

		// stop execution if not url to the web manager is provided
		client = ClientBuilder.newClient();

		// initialize Gephi graph
		if (!webMgmUrl) {
			initGraph();
		}

		while (!Thread.interrupted()) {
			try {
				String chop = queue.take();
				// Structure of each parameter 'chop'
				// this.fqName+"|"+label.getOffset()+"|"+Mnemonic.OPCODE[p_opcode]+"|"+p_owner
				// + "." + p_name + ":" + p_desc+"| -- visitFieldInsn";
				String[] s = chop.split("\\|");
				String label = s[2] + ":" + s[0] + "." + s[1];

				ChopNode n = null;
				if (createdChopNodes.containsKey(label)) {
					n = createdChopNodes.get(label);
				} else {
					// create and populate chop node
					n = new ChopNode();
					n.fqName = s[0];
					n.offset = s[2];
					n.opcode = s[3];
					n.misc = s[1];
					n.label = label;
					createdChopNodes.put(label, n);
				}
				

				if (webMgmUrl)
					addNodeAndSend(n);
				else
					addNodeAndDump(n);
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}
	
	// sends a node over the network to the graph rendering engine
	private static void addNodeAndSend(ChopNode n) {
		if ("".equals(ucWebMgmUrl) || ucWebMgmUrl.trim().equals(""))
			return;

		// create json-message elements
		JSONArray nodes = new JSONArray();
		JSONArray edges = new JSONArray();
		

		ChopNode nhelp;
		if (lastNode != null) { // && lastNode.next == null) {
			lastNode.next = n;// sends a node to the graph rendering
			n.prev = lastNode;
			nhelp=lastNode;
			lastNode = n;
		} else {
			lastNode = n;
			nhelp=lastNode;
		}
		
		JSONArray prev = convertNodeToJSON(nhelp);
		nodes.add(prev);
		
		JSONArray next = convertNodeToJSON(n);
		nodes.add(next);

		JSONObject link = createLink(nhelp, n);
		edges.add(link);

		JSONObject jsonMsg = new JSONObject();
		jsonMsg.put(JSONMsg.NODES, nodes);
		jsonMsg.put(JSONMsg.LINKS, edges);
		System.out.println("\n"+jsonMsg.toString());
		target = client.target(ucWebMgmUrl);// .path("graph");
		target = target.queryParam(REQPARAM.APPID.toString(), appId);
		target = target.queryParam(REQPARAM.MSG.toString(),
				UriComponent.encode(jsonMsg.toString(), UriComponent.Type.QUERY_PARAM));
		Invocation.Builder invocation = target.request(MediaType.APPLICATION_JSON);
		Response resp = invocation.get();
		if (n.getLabel().toLowerCase().contains("end")) {
			countDown.countDown();
			Thread.currentThread().interrupt();
		}
		// Form form = new Form();
		// form.param("id", appId);
		// form.param("msg", jsonMsg.toJSONString());
		// MyJAXBBean bean =
		// target.request(MediaType.APPLICATION_JSON_TYPE)
		// .post(Entity.entity(form,MediaType.APPLICATION_FORM_URLENCODED_TYPE),
		// MyJAXBBean.class);
	}

	private static void initGraph() {
		if (gexf == null) {
			gexf = new GexfImpl();
			Calendar date = Calendar.getInstance();

			gexf.getMetadata().setLastModified(date.getTime()).setCreator("Java Pep")
					.setDescription("Visualizing program's taken runtime path");
			gexf.setVisualization(true);

			graph = gexf.getGraph();
			graph.setDefaultEdgeType(EdgeType.UNDIRECTED).setMode(Mode.STATIC);

			AttributeList attrList = new AttributeListImpl(AttributeClass.NODE);
			graph.getAttributeLists().add(attrList);

			fqName = attrList.createAttribute(String.valueOf(++attrId), AttributeType.STRING, "FQName");
			attributeMap.put(attrId, fqName);
			offset = attrList.createAttribute(String.valueOf(++attrId), AttributeType.STRING, "Offset");
			attributeMap.put(attrId, offset);
			opcode = attrList.createAttribute(String.valueOf(++attrId), AttributeType.STRING, "Opcode");
			attributeMap.put(attrId, opcode);
			misc = attrList.createAttribute(String.valueOf(++attrId), AttributeType.STRING, "Misc");
			attributeMap.put(attrId, misc);

			// Node n = graph.createNode(String.valueOf(++nodeId));
			// n.setLabel("Start").setColor(new ColorImpl(0, 153, 0));
			// lastNode = n;
		}
	}

	private static JSONObject createLink(ChopNode prev, ChopNode next) {
		JSONObject _return = new JSONObject();
		_return.put(JSONMsg.SOURCE, prev.getLabel());
		_return.put(JSONMsg.TARGET, next.getLabel());
		return _return;
	}

	private static JSONArray convertNodeToJSON(ChopNode n) {
		JSONArray _return = new JSONArray();
		JSONObject attrib = new JSONObject();
		attrib.put(JSONMsg.ID.toString().toLowerCase(), n.label);
		_return.add(attrib);
		attrib = new JSONObject();
		attrib.put(JSONMsg.FQNAME, n.fqName);
		_return.add(attrib);
		attrib = new JSONObject();
		attrib.put(JSONMsg.MISC, n.misc);
		_return.add(attrib);
		attrib = new JSONObject();
		attrib.put(JSONMsg.OFFSET, n.offset);
		_return.add(attrib);
		attrib = new JSONObject();
		attrib.put(JSONMsg.OPCODE, n.opcode);
		_return.add(attrib);

		return _return;
	}

	// adds a node at the end of the list and dumps the result into the output
	// file
	private static void addNodeAndDump(ChopNode n) {
		// initialize the graph
		initGraph();

		if (lastNode != null && lastNode.next == null) {
			lastNode.next = n;
			n.prev = lastNode;
			lastNode = n;
		} else {
			lastNode = n;
		}

		if (n.graph_node == null) {
			Node gn = graph.createNode(String.valueOf(++nodeId));
			gn.setLabel(n.getLabel()).getAttributeValues().addValue(fqName, n.fqName).addValue(offset, n.offset)
					.addValue(opcode, n.opcode).addValue(misc, n.misc);
			n.graph_node = gn;
		}
		try {
			String filename = ConfigProperties.getProperty(ConfigProperties.PROPERTIES.CHOPNODES_FILE);
			if (!"".equals(filename)) {
				StaxGraphWriter graphWriter = new StaxGraphWriter();
				File f = new File(filename);
				Writer out = new FileWriter(f);
				graphWriter.writeToStream(gexf, out, "UTF-8");
				// System.out.println("Graph written to : " +
				// f.getAbsolutePath());
			}
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
}
