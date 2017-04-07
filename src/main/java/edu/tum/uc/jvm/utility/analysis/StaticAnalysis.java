package edu.tum.uc.jvm.utility.analysis;

import java.io.File;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import org.objectweb.asm.Label;

import edu.tum.uc.jvm.utility.ConfigProperties;
import edu.tum.uc.jvm.utility.analysis.Flow.Chop;

public class StaticAnalysis {

	private static ReportReader reportreader = new ReportReader();

	public static enum NODETYPE {
		NONE, SOURCE, SINK, BOTH, ERROR, CHOP_NODE;
	}

	static {
		String file = ConfigProperties.getProperty(ConfigProperties.PROPERTIES.ANALYSIS_REPORT);
		StaticAnalysis.importXML(new File(file).getAbsolutePath());
	}

	// static {
	// StaticAnalysis.importXML(
	// new
	// File(ConfigProperties.getProperty(ConfigProperties.PROPERTIES.ANALYSIS_REPORT)).getAbsolutePath());
	// }

	// public StaticAnalysis() {
	// this.reportreader = new ReportReader();
	// }

	public static void importXML(String filename) {
		try {
			reportreader = new ReportReader();
			reportreader.readReport(filename);
		} catch (Exception e) {
		}
	}

	public static SinkSource getSinkById(String p_sinkId) {
		SinkSource _return = null;
		for (SinkSource s : reportreader.getSinks()) {
			if (p_sinkId.equals(s.getId())) {
				_return = s;
				break;
			}
		}
		return _return;
	}

	public static SinkSource getSourceById(String p_sourceId) {
		SinkSource _return = null;
		for (SinkSource s : reportreader.getSources()) {
			if (p_sourceId.equals(s.getId())) {
				_return = s;
				break;
			}
		}
		return _return;
	}

	public static List<SinkSource> getSinks() {
		return reportreader.getSinks();
	}

	public static List<SinkSource> getSources() {
		return reportreader.getSources();
	}

	public static List<Flow> getFlows() {
		return reportreader.getFlows();
	}

	/*
	 * note that if the same invocation is both, output is only the one in
	 * sinks. (shouldn't matter) in case of error, nodeType.ERROR is returned
	 * and output=null
	 */
	public static List<SinkSource> getType(String fullyQualifiedName, int offset) {
		if (fullyQualifiedName.contains("zipIt")) {
			int u = 0;
			u++;
		}

		List<SinkSource> _return = new LinkedList<SinkSource>();

		Iterator<SinkSource> it = reportreader.getSinks().iterator();
		while (it.hasNext()) {
			SinkSource ss = it.next();
			if (fullyQualifiedName.equals(ss.getLocation()) && (offset == ss.getOffset())) {
				_return.add(ss);
			}
		}

		it = reportreader.getSources().iterator();
		while (it.hasNext()) {
			SinkSource ss = it.next();
			if (fullyQualifiedName.equals(ss.getLocation()) && (offset == ss.getOffset())) {
				_return.add(ss);
			}
		}
		return _return;
	}

	// Returns a sink/source specified by id and type
	public static SinkSource getSinkSourceById(String id, NODETYPE type) {
		SinkSource _return = null;
		Iterator<SinkSource> it;
		if (type.equals(NODETYPE.SINK)) {
			it = reportreader.getSinks().iterator();
		} else {
			it = reportreader.getSources().iterator();
		}

		while (it.hasNext()) {
			SinkSource s = it.next();
			if (id.equals(s.getId())) {
				_return = s;
				break;
			}
		}

		if (_return == null) {
			int i = 0;
			i++;
		}

		return _return;
	}

	// Returns a list of chop-nodes that have the ownerMethod as parent method
	public static List<Chop> getChop(String ownerMethod) {
		if (ownerMethod == null)
			return null;
		List<Flow.Chop> _return = new LinkedList<Flow.Chop>();
		List<Flow> flows = reportreader.getFlows();
		Iterator<Flow> flowIt = flows.iterator();
		while (flowIt.hasNext()) {
			Flow f = flowIt.next();
			if (f.getChopNodes() != null) {
				Iterator<Chop> chopIt = f.getChopNodes().iterator();
				while (chopIt.hasNext()) {
					Chop c = chopIt.next();
					if (ownerMethod.equals(c.getOwnerMethod()))
						_return.add(c);
				}
			}
		}

		return _return;
	}

	public static List<CreationSite> getCreationSite() {
		return reportreader.getCreationSites();
	}

	public static List<CreationSite> getCreationSiteByLabel(String location, Label lab) {
		List<CreationSite> _return = new LinkedList<CreationSite>();
		List<CreationSite> allCreationSites = getCreationSite();
		for (int i = 0; i < allCreationSites.size(); i++) {
			CreationSite cs = allCreationSites.get(i);
			if (cs.getOffset() == lab.getOffset() && cs.getLocation().startsWith(location)) {
				_return.add(cs);
			}
		}
		return _return;
	}

	public static List<CreationSite> getCreationSiteByLocation(String location, int offset, String type) {
		List<CreationSite> _return = new LinkedList<CreationSite>();
		List<CreationSite> allCreationSites = getCreationSite();
		for (int i = 0; i < allCreationSites.size(); i++) {
			CreationSite cs = allCreationSites.get(i);
			if (cs.getOffset() <= offset && cs.getLocation().startsWith(location) && type.equals(cs.getType())) {
				_return.add(cs);
			}
		}
		return _return;
	}

	// Checks if bytecodeOffset in method parentMethodFQN is a source
	public static List<SinkSource> isSource(String parentMethodFQN, int bytecodeOffset) {
		List<SinkSource> _return = new LinkedList<SinkSource>();
		for (SinkSource s : contains(parentMethodFQN, bytecodeOffset, getSources())) {
			int param = s.getParam();
			if (s.isReturn())
				_return.add(s);
			else if (param > 0)
				_return.add(s);
		}
		return _return;
		// return contains(parentMethodFQN, bytecodeOffset, getSources());
	}

	public static List<SinkSource> isSourceWithFlow(String parentMethodFQN, int bytecodeOffset) {
		List<SinkSource> _return = new LinkedList<SinkSource>();
		List<SinkSource> sources = contains(parentMethodFQN, bytecodeOffset, getSources());
		List<Flow> flows = getFlows();
//		check if the passed method is a source in one of the flows
		for (Flow f : flows) {
			if (f.getSource() != null && f.getSource().size() != 0) {
				for (String src : f.getSource()) {
					for (SinkSource s : sources) {
						if (src.equals(s.getId())) {
							_return.add(s);
							break;
						}
					}
				}
			}
		}
		return _return;
	}

	// Checks if bytecodeOffset in method parentMethodFQN is a source
	public static List<SinkSource> isSink(String parentMethodFQN, int bytecodeOffset) {
		return contains(parentMethodFQN, bytecodeOffset, getSinks());
	}

	public static List<SinkSource> isSinkWithFlow(String parentMethodFQN, int bytecodeOffset) {
		List<SinkSource> _return = new LinkedList<SinkSource>();
		List<SinkSource> sinks = contains(parentMethodFQN, bytecodeOffset, getSinks());
		List<Flow> flows = getFlows();
		for (Flow f : flows) {
			if (f.getSource() != null && f.getSource().size() != 0)
				for (SinkSource s : sinks) {
					if (f.getSink().equals(s.getId()))
						_return.add(s);
				}
		}
		return _return;
	}

	// check if parentMethodFQN with the corresponding bytecodeOffset is listed
	// in parameter list
	private static List<SinkSource> contains(String parentMethodFQN, int bytecodeOffset, List<SinkSource> list) {
		List<SinkSource> _return = new LinkedList<SinkSource>();
		for (SinkSource sinksource : list) {
			if (sinksource.getLocation().equals(parentMethodFQN.replace("|", "."))
					&& sinksource.getOffset() == bytecodeOffset) {
				_return.add(sinksource);
			}
		}
		return _return;
	}

	// Returns a list of sources that a sink depends on according to the report
	public static List<SinkSource> getDepSources(SinkSource sink) {
		List<SinkSource> _return = new LinkedList<SinkSource>();
		for (Flow f : getFlows()) {
			if (f.getSink().equals(sink.getId())) {
				for (String source : f.getSource())
					_return.add(getSourceById(source));
			}
		}
		return _return;
	}

	// Returns a list of flows that that contain the chopNode on its path
	public static List<Flow> getFlowsByChopNode(Chop chopNode) {
		List<Flow> _return = new LinkedList<Flow>();

		List<Flow> flows = reportreader.getFlows();
		Iterator<Flow> flowIt = flows.iterator();
		while (flowIt.hasNext()) {
			Flow f = flowIt.next();
			if (f.getChopNodes() != null) {
				Iterator<Chop> chopIt = f.getChopNodes().iterator();
				while (chopIt.hasNext()) {
					Chop c = chopIt.next();
					if (chopNode.getOwnerMethod().equals(c.getOwnerMethod())
							&& chopNode.getByteCodeIndex() == c.getByteCodeIndex()) {
						_return.add(f);
						break;
					}
				}
			}
		}

		return _return;
	}
}
