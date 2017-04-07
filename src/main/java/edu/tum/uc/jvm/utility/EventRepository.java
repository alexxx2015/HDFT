package edu.tum.uc.jvm.utility;

import java.io.File;
import java.lang.management.ManagementFactory;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import de.tum.in.i22.uc.cm.datatypes.basic.EventBasic;
import de.tum.in.i22.uc.cm.datatypes.interfaces.IEvent;
import edu.tum.uc.jvm.utility.analysis.SinkSource;
import edu.tum.uc.jvm.utility.analysis.StaticAnalysis;

public class EventRepository {
	private static HashMap<String,IEvent> sourceSinkEvents = new HashMap<String,IEvent>();

	public static IEvent getEvent(String id){
		return sourceSinkEvents.get(id);
	}

	public static void createEventObjects(String file) {
		String runningVm = ManagementFactory.getRuntimeMXBean().getName();
		String[] runningVmComp = runningVm.split("@");
		String pid = "";
		if (runningVmComp.length > 0) {
			pid = runningVmComp[0];
		}

		List<SinkSource> sources = StaticAnalysis.getSources();
		List<SinkSource> sinks = StaticAnalysis.getSinks();
		if (sources == null || sinks == null || sources.size() == 0
				|| sinks.size() == 0) {
			StaticAnalysis.importXML(new File(file).getAbsolutePath());
			sources = StaticAnalysis.getSources();
			sinks = StaticAnalysis.getSinks();
		}
		long start = System.nanoTime();
		if (sources.size() > 0) {
			for (SinkSource s : sources) {
				String signature = "";
				int contextId = -1;
				if (s.getPossibleSignatures() != null) {
					for (String s1 : s.getPossibleSignatures()) {
						signature = s1;
						break;
					}
				}
				if (s.getContext() != null) {
					for (int i : s.getContext()) {
						contextId = i;
						break;
					}
				}
				Map<String, String> map = new HashMap<String, String>();
				map.put("location", s.getLocation()); // event.getMethodInvoker()
														// +
														// event.getMethodInvokerSig()
														// + ":"+
														// event.getOffset());
				map.put("signature", signature); // event.getMethodInvokee() +
													// event.getMethodInvokeeSig());
				map.put("delimiter", MethEvent.Type.START.toString());
				map.put("PEP", "Java");
				map.put("context", String.valueOf(contextId));
				map.put("PID", pid);// Add process id
				map.put("id", s.getId());
				
				IEvent ievent = new EventBasic("Source", map, true);
				sourceSinkEvents.put(s.getId(), ievent);
			}
		}

		if (sinks.size() > 0) {
			for (SinkSource s : sinks) {
				String signature = "";
				int contextId = -1;
				if (s.getPossibleSignatures() != null) {
					for (String s1 : s.getPossibleSignatures()) {
						signature = s1;
						break;
					}
					;
				}
				
				if (s.getContext() != null) {
					for (int i : s.getContext()) {
						contextId = i;
					}
				}
				
				Map<String, String> map = new HashMap<String, String>();
				map.put("location", s.getLocation()); // event.getMethodInvoker()
														// +
														// event.getMethodInvokerSig()
														// + ":"+
														// event.getOffset());
				map.put("signature", signature); // event.getMethodInvokee() +
													// event.getMethodInvokeeSig());
				map.put("delimiter", MethEvent.Type.START.toString());
				map.put("PEP", "Java");
				map.put("context", String.valueOf(contextId));
				map.put("PID", pid);// Add process id
				map.put("id", s.getId());

				IEvent ievent = new EventBasic("Sink", map, true);
				sourceSinkEvents.put(s.getId(), ievent);
			}
		}
		StatisticsWriter.numObj = sources.size()+sinks.size();
		StatisticsWriter.time4GenSinkSource = System.nanoTime()-start;
	}
}
