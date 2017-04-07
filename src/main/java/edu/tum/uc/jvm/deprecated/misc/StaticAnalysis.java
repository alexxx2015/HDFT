package edu.tum.uc.jvm.deprecated.misc;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.Set;
import java.util.regex.Pattern;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;

public class StaticAnalysis {
	private final static String sourceGroupKey = "sources";
	private final static String sourceKey = "source";
	private final static String sinkGroupKey = "sinks";
	private final static String flowGroupKey = "flows";
	private final static String defaultFilename = "/tmp/report.xml";

	private static boolean isInitialized = false;

	public enum nodeType {
		NONE, SOURCE, SINK, BOTH, ERROR, CHOP_NODE;
	}

	public static nodeType append(nodeType oldT, nodeType newT) {
		if ((oldT == nodeType.ERROR) || (newT == nodeType.ERROR))
			return nodeType.ERROR;

		if (newT != null) {
			switch (oldT) {
			case SOURCE:
				switch (newT) {
				case SINK:
				case BOTH:
					return nodeType.BOTH;
				case NONE:
				case SOURCE:
					break;
				}
				break;
			case SINK:
				switch (newT) {
				case SOURCE:
				case BOTH:
					return nodeType.BOTH;
				case NONE:
				case SINK:
					break;
				}
				break;
			case BOTH:
				break;
			case NONE:
				switch (newT) {
				case SINK:
				case BOTH:
				case SOURCE:
					return newT;
				case NONE:
					break;
				}
				break;
			}
		}
		return oldT;
	}

	/*
	 * Sources and sinks are stored as follows
	 * 
	 * sourcesMap contains a mapping where the key is the fully qualified
	 * signature of the owner and the value is a table that maps each offset in
	 * that method to an OffsetParameter, which is a data type that contains the
	 * mapping between each parameter of the method invoked at <fqn>:<offset>
	 * and the respective offsetNode obejct. The offsetNode object contains the
	 * type of the parameter (see above) and the unique id of the source/sink
	 * 
	 * Same holds for sinksMap
	 */

	// String is the fully qualified name of the owner
	private static Map<String, OffsetTable> sourcesMap;
	// String is the fully qualified name of the owner
	private static Map<String, OffsetTable> sinksMap;
	// String1 and String2 are mappings of ids
	private static Map<String, String[]> flowsMap;
	// String1= Identifier of a sink, String2= Identifier of a source,
	// OffsetTable= all chop nodes in between
	private static Map<String, ArrayList<Properties>> chops;

	static {
		sourcesMap = new HashMap<String, OffsetTable>();
		sinksMap = new HashMap<String, OffsetTable>();
		flowsMap = new HashMap<String, String[]>();
	}

	private static void importSet(Node sets, Map<String, OffsetTable> setsMap,
			String setName) throws SAXException {

		int totalSets = sets.getChildNodes().getLength();

		Node firstSetNode = sets.getFirstChild();
		for (int s = 0; s < totalSets; s++) {

			if (firstSetNode.getNodeType() == Node.ELEMENT_NODE) {
				Element firstSetElement = (Element) firstSetNode;

				// ------- Extract id
				NodeList idList = firstSetElement.getElementsByTagName("id");
				if (idList.getLength() != 1)
					throw new SAXException("Error! Exactly 1 id required in "
							+ setName + " no " + (s + 1));
				Element idElement = (Element) idList.item(0);
				NodeList idTextList = idElement.getChildNodes();
				if (idTextList.getLength() != 1)
					throw new SAXException(
							"Error! Exactly 1 id text field required in "
									+ setName + " no " + (s + 1));
				String id = ((Node) idTextList.item(0)).getNodeValue().trim();

				// ------- Extract location
				NodeList locationList = firstSetElement
						.getElementsByTagName("location");
				if (locationList.getLength() != 1)
					throw new SAXException(
							"Error! Exactly 1 location required in " + setName
									+ " no " + (s + 1));
				Element locationElement = (Element) locationList.item(0);
				NodeList locationTextList = locationElement.getChildNodes();
				if (locationTextList.getLength() != 1)
					throw new SAXException(
							"Error! Exactly 1 location text field required in "
									+ setName + " no " + (s + 1));
				String location = ((Node) locationTextList.item(0))
						.getNodeValue().trim();

				// ------- Extract signature
				NodeList signatureList = firstSetElement
						.getElementsByTagName("signature");
				if (signatureList.getLength() == 0)
					throw new SAXException(
							"Error! No signature-tag specified for " + setName
									+ " no " + (s + 1));

				String[] signature = new String[signatureList.getLength()];
				for (int i = 0; i < signatureList.getLength(); i++) {
					Element signatureElement = (Element) signatureList.item(i);
					NodeList signatureTextList = signatureElement
							.getChildNodes();
					signature[i] = ((Node) signatureTextList.item(0))
							.getNodeValue().trim();
				}

				// location is of the form <FullyQualifiedName>:<Offset> -->
				// <parameter>
				// where parameter is either pn to indicate the nth parameter or
				// exit to indicate return value
				// although <parameter> may be missing. That explains the
				// difference between the first condition (>2)
				// and the second condition (!=2)

				NodeList paramList = firstSetElement
						.getElementsByTagName("param");
				Element paramElem = (Element) paramList.item(0);
				String paramIdx = "";
				if (paramElem != null) {
					paramIdx = paramElem.getAttribute("index");
				} else {
					NodeList returnList = firstSetElement
							.getElementsByTagName("return");
					if (returnList.getLength() > 0) {
						paramIdx = "-1";
					}
				}
				
				//Extract context items
				NodeList contextList = firstSetElement.getElementsByTagName("context");
				if((contextList == null) || (contextList.getLength() == 0)){
					throw new SAXException("Error no context specified for "+setName+" "+id);
				}				
				Element contextElement = (Element) contextList.item(0);
				NodeList contextTextList = contextElement.getChildNodes();
				int[] context = new int[0];
				if(contextTextList.item(0) != null){
					String contextAsString =((Node) contextTextList.item(0)).getNodeValue().trim();
					String[] contextAsStringArr = contextAsString.split(",");
					if(contextAsStringArr.length > 0){
						context = new int[contextAsStringArr.length];
						for(int i = 0; i< contextAsStringArr.length; i++){
							context[i] = Integer.parseInt(contextAsStringArr[i]);
						}
					}
				}

				// String[] st =
				// Pattern.compile(Pattern.quote("-->")).split(location);
				// if (st.length > 2)
				// throw new SAXException("Invalid location specified for "+
				// setName + " " + (s + 1));

				// String[] st2 = st[0].split(":");
				String[] st2 = location.split(":");
				if (st2.length != 2)
					throw new SAXException("Invalid location specified for "
							+ setName + " " + (s + 1));

				String fullyQualifiedName = st2[0];
				int offset = Integer.parseInt(st2[1]);
				int parameter = 0;
				if (!"".equals(paramIdx)) {
					parameter = Integer.parseInt(paramIdx);
				}

				// if (st.length > 1)
				// if (st[1].toLowerCase().equals("exit"))
				// parameter = -1;
				// else
				// parameter = Integer.parseInt(st[1].substring(1)); //string
				// here is supposed to be pN
				// else
				// parameter = 0;

				// check if an offest table for the current fqn exists,
				// otherwise create it

				OffsetTable ot;

				if (setsMap.containsKey(fullyQualifiedName)) {
					ot = setsMap.get(fullyQualifiedName);
				} else {
					ot = new OffsetTable();
					setsMap.put(fullyQualifiedName, ot);
				}

				// check if the current offset table contains already a
				// reference to <fqn>:<offset> otherwise make it new

				StaticAnalysis.nodeType nt;
				if (setName.equals("source"))
					nt = StaticAnalysis.nodeType.SOURCE;
				else if (setName.equals("sink"))
					nt = StaticAnalysis.nodeType.SINK;
				else {
					System.err.println("ERROR. this should never happen.");
					nt = StaticAnalysis.nodeType.ERROR; // this case should
				} // never happen

				OffsetParameter op;
				if (ot.getSet().containsKey(offset)) {
					op = ot.get(offset);
					op.append(parameter, id, nt, context);
				} else {
					op = new OffsetParameter(parameter, new OffsetNode(id, nt),signature, context);
					ot.put(offset, op);
				}

			}// end of if clause

			firstSetNode = firstSetNode.getNextSibling();
		}// end of for loop with s var
	}

	private static void importFlows(Node sets, Map<String, String[]> setsMap)
			throws SAXException {

		Node firstSetNode = sets.getFirstChild();
		if (firstSetNode == null) {
			System.out.println("No flows detected!");
			return;
		}

		int totalSets = 0;
		do {
			// assumes a sink appears only once
			// TODO:check it
			if (firstSetNode.getNodeType() == Node.ELEMENT_NODE) {
				Element firstSetElement = (Element) firstSetNode;
				String currSink = firstSetElement.getAttribute("id");
				System.out.print("parsing mapping between sink " + currSink
						+ " and ");
				Set<String> sources = new HashSet<String>();

				// Parse source-nodes nested in a sink-node
				NodeList nl = firstSetElement.getElementsByTagName(sourceKey);
				for (int i = 0; i < nl.getLength(); i++) {
					Node e = nl.item(i);
					// if (e.getNodeType()==Node.TEXT_NODE){
					sources.add(e.getAttributes().getNamedItem("id")
							.getNodeValue());
					System.out.print(e.getAttributes().getNamedItem("id")
							.getNodeValue()
							+ ", ");
					totalSets++;
					// }
				}
				setsMap.put(currSink, sources.toArray(new String[0]));
				System.out.println("... that's it! " + nl.getLength());
			}// end of if clause

			firstSetNode = firstSetNode.getNextSibling();
		} while (firstSetNode != null);

		// Parse chop-nodes
		if (chops == null) {
			chops = new HashMap<String, ArrayList<Properties>>();
		}

		NodeList nl = ((Element) sets).getElementsByTagName("chopNode");
		for (int i = 0; i < nl.getLength(); i++) {
			Node e = nl.item(i);
			NamedNodeMap nnm = e.getAttributes();
			if (nnm != null) {
				Properties p = new Properties();

				if (nnm.getNamedItem("byteCodeIndex") != null) {
					String byteCodeIndex = nnm.getNamedItem("byteCodeIndex")
							.getTextContent().trim();
					p.put("byteCodeIndex", byteCodeIndex);
				}

				if (nnm.getNamedItem("label") != null) {
					String label = nnm.getNamedItem("label").getTextContent()
							.trim();
					p.put("label", label);
				}

				if (nnm.getNamedItem("operation") != null) {
					String operation = nnm.getNamedItem("operation")
							.getTextContent().trim();
					p.put("operation", operation);
				}

				String ownerMethod = "";
				if (nnm.getNamedItem("ownerMethod") != null) {
					ownerMethod = nnm.getNamedItem("ownerMethod")
							.getTextContent().trim();
				}

				if ("".equals(ownerMethod)) {
					ArrayList<Properties> myOffsets = chops.get(ownerMethod);
					if (myOffsets == null) {
						myOffsets = new ArrayList<Properties>();
						chops.put(ownerMethod, myOffsets);
					}
					myOffsets.add(p);
				}
			}
		}
		System.out.println("Total no of flows : " + totalSets);
	}

	public static void importXML(String filename) {
		isInitialized = true;
		try {

			DocumentBuilderFactory docBuilderFactory = DocumentBuilderFactory
					.newInstance();
			DocumentBuilder docBuilder = docBuilderFactory.newDocumentBuilder();
			Document doc = docBuilder.parse(new File(filename));

			// normalize text representation
			doc.getDocumentElement().normalize();
			System.out.println("Root element of the doc is "
					+ doc.getDocumentElement().getNodeName());

			// Extract all sources
			NodeList sourcesList = doc.getElementsByTagName(sourceGroupKey);
			if (sourcesList.getLength() != 1)
				throw new SAXException(
						"Expected one and only one set of sources");
			Node sources = sourcesList.item(0);

			sourcesMap = new HashMap<String, OffsetTable>();
			importSet(sources, sourcesMap, "source");

			// Extract all sinks
			NodeList sinksList = doc.getElementsByTagName(sinkGroupKey);
			if (sinksList.getLength() != 1)
				throw new SAXException("Expected one and only one set of sinks");
			Node sinks = sinksList.item(0);

			sinksMap = new HashMap<String, OffsetTable>();
			importSet(sinks, sinksMap, "sink");

			// Extract all flows
			NodeList flowsList = doc.getElementsByTagName(flowGroupKey);
			if (flowsList.getLength() != 1)
				throw new SAXException("Expected one and only one set of flows");
			Node flows = flowsList.item(0);

			flowsMap = new HashMap<String, String[]>();
			importFlows(flows, flowsMap);

		} catch (SAXParseException err) {
			System.out.println("** Parsing error" + ", line "
					+ err.getLineNumber() + ", uri " + err.getSystemId());
			System.out.println(" " + err.getMessage());
			isInitialized = false;

		} catch (SAXException e) {
			e.getException();
			System.err.println("Error during XML import: " + e.getMessage()
					+ "\n");
			isInitialized = false;

		} catch (Throwable t) {
			t.printStackTrace();
		}
	}

	public static void importXML() {
		importXML(defaultFilename);
	}

	public static boolean isInitialized() {
		return isInitialized;
	}

	public static Map<String, OffsetTable> getSourcesMap() {
		return sourcesMap;
	}

	public static void setSourcesMap(Map<String, OffsetTable> sourcesMap) {
		StaticAnalysis.sourcesMap = sourcesMap;
	}

	public static Map<String, OffsetTable> getSinksMap() {
		return sinksMap;
	}

	public static void setSinksMap(Map<String, OffsetTable> sinksMap) {
		StaticAnalysis.sinksMap = sinksMap;
	}

	public static Map<String, String[]> getFlowsMap() {
		return flowsMap;
	}

	public static void setFlowsMap(Map<String, String[]> flowsMap) {
		StaticAnalysis.flowsMap = flowsMap;
	}

	/*
	 * Return the type (source, sink, node, none, err) of parameter <parameter>
	 * of the method invocation at offset <offset> of class <fullyQualifiedName>
	 */
	public static nodeType getTypeOfPar(String fullyQualifiedName, int offset,
			int parameter) {
		if (fullyQualifiedName == null || fullyQualifiedName == ""
				|| offset < 0 || parameter < -1) {
			System.out.println("Invalid Parameters: fullyQualifiedName="
					+ fullyQualifiedName + " offset=" + offset
					+ "  and parameter = " + parameter);
			return null;
		}

		nodeType src = nodeType.NONE;
		OffsetTable ot = sourcesMap.get(fullyQualifiedName);
		if (ot != null) {
			OffsetParameter op = ot.get(offset);
			if (op != null) {
				src = op.getTypeOfPar(parameter);
			}
		}

		nodeType snk = nodeType.NONE;
		ot = sinksMap.get(fullyQualifiedName);
		if (ot != null) {
			OffsetParameter op = ot.get(offset);
			if (op != null) {
				snk = op.getTypeOfPar(parameter);
			}
		}

		return append(src, snk);

	}

	public static ArrayList<Properties> getChop(String ownerMethod) {
		return ownerMethod == null ? null : chops.get(ownerMethod);
	}

	public static String getChopLabel(String ownerMethod, int offset) {
		String label = null;
		ArrayList<Properties> chop = getChop(ownerMethod);
		if (chop != null) {
			Iterator<Properties> chopIterator = chop.iterator();
			while (chopIterator.hasNext()) {
				Properties p = chopIterator.next();
				if (Integer.valueOf(p.getProperty("byteCodeIndex")) == offset) {
					label = p.getProperty("label");
					break;
				}
			}
		}
		return label;
	}

	/*
	 * note that if the same invocation is both, output is only the one in
	 * sinks. (shouldn't matter) in case of error, nodeType.ERROR is returned
	 * and output=null
	 */
	public static Map<Integer, nodeType> getType(String fullyQualifiedName,
			int offset) {
		if (fullyQualifiedName == null || fullyQualifiedName == ""
				|| offset < 0) {
			System.out.println("Invalid Parameters: fullyQualifiedName="
					+ fullyQualifiedName + " and offset=" + offset);
			return null;
		}

		Map<Integer, nodeType> res = new HashMap<Integer, nodeType>();

		OffsetTable ot = sourcesMap.get(fullyQualifiedName);
		if (ot != null) {
			OffsetParameter op = ot.get(offset);
			if (op != null) {
				Map<Integer, OffsetNode> m = op.getOffsets();
				if (m != null) {
					for (int i : m.keySet()) {
						res.put(i, m.get(i).getType());
					}
				}
			}
		}

		ot = sinksMap.get(fullyQualifiedName);
		if (ot != null) {
			OffsetParameter op = ot.get(offset);
			if (op != null) {
				Map<Integer, OffsetNode> m = op.getOffsets();
				if (m != null) {
					for (int i : m.keySet()) {
						if (res.containsKey(i)) {
							nodeType nt = res.get(i);
							res.remove(i);
							res.put(i, StaticAnalysis.append(nt, m.get(i)
									.getType()));
						} else {
							res.put(i, m.get(i).getType());
						}
					}
				}
			}
		}

		return res;
	}

	/*
	 * complete list of source nodes
	 */
	public static Map<Integer, OffsetNode> getSourceOffsetNode(
			String fullyQualifiedName, int offset) {
		if (fullyQualifiedName == null || fullyQualifiedName == ""
				|| offset < 0) {
			System.out.println("Invalid Parameters: fullyQualifiedName="
					+ fullyQualifiedName + " and offset=" + offset);
			return null;
		}

		OffsetTable ot = sourcesMap.get(fullyQualifiedName);
		if (ot != null) {
			OffsetParameter op = ot.get(offset);
			if (op != null) {
				return op.getOffsets();
			}
		}
		return null;
	}

	/*
	 * complete list of sink nodes
	 */
	public static Map<Integer, OffsetNode> getSinkOffsetNode(
			String fullyQualifiedName, int offset) {
		if (fullyQualifiedName == null || fullyQualifiedName == ""
				|| offset < 0) {
			System.out.println("Invalid Parameters: fullyQualifiedName="
					+ fullyQualifiedName + " and offset=" + offset);
			return null;
		}

		OffsetTable ot = sinksMap.get(fullyQualifiedName);
		if (ot != null) {
			OffsetParameter op = ot.get(offset);
			if (op != null) {
				return op.getOffsets();
			}
		}
		return null;
	}

	/*
	 */
	public static String getIdOfPar(String fullyQualifiedName, int offset,
			int parameter) {
		OffsetParameter res = new OffsetParameter(
				new HashMap<Integer, OffsetNode>());
		Map<Integer, OffsetNode> sources = getSourceOffsetNode(
				fullyQualifiedName, offset);
		Map<Integer, OffsetNode> sinks = getSinkOffsetNode(fullyQualifiedName,
				offset);

		res.append(sources);
		res.append(sinks);

		// if ((os!=null)&&(on!=null)) return

		return res.getIdOfPar(parameter);
	}

	/*
	 */
	public static Map<Integer, String> getId(String fullyQualifiedName,
			int offset) {
		OffsetParameter res = new OffsetParameter(
				new HashMap<Integer, OffsetNode>());
		Map<Integer, OffsetNode> sources = getSourceOffsetNode(
				fullyQualifiedName, offset);
		Map<Integer, OffsetNode> sinks = getSinkOffsetNode(fullyQualifiedName,
				offset);

		res.append(sources);
		res.append(sinks);

		// if ((os!=null)&&(on!=null)) return

		Map<Integer, String> result = new HashMap<Integer, String>();
		Map<Integer, OffsetNode> m = res.getOffsets();
		if (m != null) {
			for (Entry<Integer, OffsetNode> e : m.entrySet()) {
				OffsetNode on = e.getValue();
				if (on == null) {
					System.err.println("Error. At this point OffsetNode for "
							+ e.getKey() + " cannot be null");
					;
					return null;
				}
				result.put(e.getKey(), on.getId());
			}
			return result;
		} else
			return null;
	}

	/*
	 * fully qualified name to be checked, offset and output node
	 */
	private static void print(String s, int i) {

		Map<Integer, nodeType> map = getType(s, i);

		for (int p : map.keySet()) {

			nodeType n = map.get(p);

			System.out.print("Parameter " + p + " : ");
			if (n == nodeType.ERROR)
				System.out.println("Error");
			if (n == nodeType.NONE)
				System.out.println("None");
			if (n == nodeType.SOURCE) {
				System.out.println("Source");
			}
			if (n == nodeType.SINK) {
				System.out.println("Sink");
			}
			if (n == nodeType.BOTH) {
				System.out.println("Both");
			}
		}
	}

	// public static void main(String[] args) {
	//
	// importXML("C:\\temp\\test.xml");
	//
	// String valid = "test.TestProgram5.main([Ljava/lang/String;)V";
	// String notValid = "test";
	//
	// print(valid, 102);
	// System.out.println("");
	//
	// print(valid, 111);
	// System.out.println("");
	//
	// print(notValid, 103);
	// System.out.println("");
	//
	// for (int x = -1; x < 3; x++) {
	// int offset = 116;
	// nodeType n = getTypeOfPar(valid, offset, x);
	// System.out.println("result [+" + x + "]= " + n + " ("
	// + getIdOfPar(valid, offset, x) + ")");
	// }
	//
	// }

}
