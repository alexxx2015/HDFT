package edu.tum.uc.jvm.utility.eval;

import java.io.File;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Vector;
import java.util.stream.Collectors;

import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import javax.servlet.annotation.WebListener;

import org.hibernate.validator.internal.metadata.raw.ConfigurationSource;

import edu.tum.uc.jvm.utility.ConfigProperties;
import edu.tum.uc.jvm.utility.Utility;
import edu.tum.uc.jvm.utility.analysis.Flow.Chop;
import edu.tum.uc.jvm.utility.analysis.SinkSource;
import edu.tum.uc.jvm.utility.analysis.StaticAnalysis.NODETYPE;

/**
 * This class is responsible for tracking method and event execution times. The timers are exported to disk upon
 * shutdown of a webservice (therefore this is a subclass of <code>ServletContextListener</code>) or by calling
 * <code>writeToFile()</code>.
 * 
 * @author alex
 *
 */
@WebListener
public class StatisticsUtil implements ServletContextListener {

    /**
     * The method timers.
     */
    private static List<MethodTimer> MethodTimers = new Vector<MethodTimer>();
    /**
     * The event timers mapped to the ID of the threads that has thrown the event.
     */
    private static Map<String, List<EventTimer>> EventTimers = new HashMap<>();

    /**
     * Creates a new method timer for a given method name and starts it.
     * 
     * @param methodFQName
     *            The fully qualified method name.
     */
    public static void startMethodTimer(String methodFQName) {
	String threadId = Utility.getThreadId();
	MethodTimer timer = new MethodTimer(threadId, methodFQName);
	MethodTimers.add(timer);
	timer.start();
//	System.out.println("START method timer "+timer.startTime+", "+timer.getMethodFQName());
    }

    /**
     * Stops the method timer for a given method name if it exists and is running.
     * 
     * @param methodFQName
     *            The fully qualified method name.
     */
    public static void stopMethodTimer(String methodFQName) {
	String threadId = Utility.getThreadId();
	MethodTimer timer = getLastMethodTimer(methodFQName, threadId, false);
	if (timer != null) {
	    if (timer.isRunning()) {
		timer.stop();
	    }
//	    System.out.println("STOP method timer "+timer.stopTime +", "+timer.getTimeInterval()+", "+timer.getMethodFQName());
	}
    }

    /**
     * Returns the last method timer for a given method name and thread ID by traversing the list of timers in reverse
     * order and performing comparisons. If <code>hasToBeRunning</code> is set to <code>true</code>, then the method
     * searches for the last running timer for the thread ID (regardless of method name).
     * 
     * @param methodFQName
     *            The fully qualified method name.
     * @param threadId
     *            The ID of the thread.
     * @param hasToBeRunning
     *            A boolean value indicating if the method timer has to be started.
     * @return A method timer fitting the search criteria.
     */
    private static MethodTimer getLastMethodTimer(String methodFQName, String threadId, boolean hasToBeRunning) {
	// search for method timer by traversing list from end to start
	for (int i = MethodTimers.size() - 1; i >= 0; i--) {
	    MethodTimer timer = MethodTimers.get(i);
	    if (hasToBeRunning && timer.getThreadID().equals(threadId)) {
		return timer;
	    } else if (timer.getThreadID().equals(threadId) && timer.getMethodFQName().equals(methodFQName)) {
		return timer;
	    }
	}
	return null;
    }

    /**
     * Creates a new event timer for a given event name and starts it. If there is a running method timer for this
     * thread, then the newly created event timer is added to it.
     * 
     * @param eventName The name of the event.
     * @param bci The bytecode index where the event was thrown.
     * @param cnOwnerMethod The fully qualified method name where the event was thrown.
     * @param cnLabel The chopnode label belonging to the event.
     * @param sinksAndSources A set of source and sink IDs that have been triggered upon event creation.
     */
    public static void startEventTimer(String eventName, int bci, String cnOwnerMethod, String cnLabel,
	    Set<String> sinksAndSources) {
	String threadId = Utility.getThreadId();
	List<EventTimer> eventTimers = getEventTimersForThread(threadId);
	EventTimer eventTimer = new EventTimer(threadId, eventName, bci, cnOwnerMethod, cnLabel, sinksAndSources);
	eventTimers.add(eventTimer);

	MethodTimer currentMethodTimer = getLastMethodTimer(null, threadId, true);
	if (currentMethodTimer != null) {
	    currentMethodTimer.addEvent(eventTimer);
	}

	eventTimer.start();
    }
    
    /**
     * Stops the creation timer of the last event object with the given event name if it exists and has its creation timer not stopped.
     * 
     * @param eventName
     *            The name of the event to stop the timer.
     */
    public static void endEventCreation(String eventName) {
	String threadId = Utility.getThreadId();
	EventTimer eventTimer = getLastEventTimer(threadId);
	if (eventTimer != null && eventTimer.getEventName().equals(eventName)) {
	    if (eventTimer.isSetCreated()) {
	    } else {
		eventTimer.setCreated();
	    }
	}
    }

    /**
     * Stops the event timer of the last event object with the given event name if it exists and has its overall timer not stopped.
     * 
     * @param eventName
     *            The name of the event to stop the timer.
     */
    public static void stopEventTimer(String eventName) {
	String threadId = Utility.getThreadId();
	EventTimer eventTimer = getLastEventTimer(threadId);
	if (eventTimer != null && eventTimer.getEventName().equals(eventName)) {
	    if (eventTimer.isRunning()) {
		eventTimer.stop();
	    }
	}
    }

    /**
     * Returns a list of event timers for the given thread ID. If none is found, then an empty list is created.
     * @param threadId The ID of the thread.
     * @return A list of event timers for the given thread ID.
     */
    private static List<EventTimer> getEventTimersForThread(String threadId) {
	List<EventTimer> eventTimers;
	if (EventTimers.containsKey(threadId)) {
	    eventTimers = EventTimers.get(threadId);
	} else {
	    eventTimers = new Vector<>();
	    EventTimers.put(threadId, eventTimers);
	}
	return eventTimers;
    }

    /**
     * Returns the last event timer for a given thread ID by traversing the list of timers in reverse
     * order and performing comparisons.
     * @param threadId The ID of the thread.
     * @return The last event timer for the thread ID.
     */
    private static EventTimer getLastEventTimer(String threadId) {
	List<EventTimer> eventTimers = getEventTimersForThread(threadId);
	if (eventTimers.size() > 0) {
	    EventTimer lastTimer = eventTimers.get(eventTimers.size() - 1);
	    return lastTimer;
	}
	return null;
    }

    /**
     * Writes a table of all method and event timers in CSV format to disk at the path specified in the configuration.
     */
    public static void writeToFile() {

	try {
	    File statsFile = new File(ConfigProperties.getProperty(ConfigProperties.PROPERTIES.STATISTICS));
	    if (!statsFile.getParentFile().exists()) {
		statsFile.getParentFile().mkdirs();
	    }
	    FileWriter statsFW = new FileWriter(statsFile, true);
	    StringBuilder sb = new StringBuilder();
	    sb.append("-----RUNTIME STATISTICS-----" + (new Date()) + "\n");
	    sb.append("tracking on: " + ConfigProperties.getProperty(ConfigProperties.PROPERTIES.INSTRUMENTATION)
		    + "\n");
	    sb.append("1. Method timers\n");
	    CSVStringBuilder csvSb = new CSVStringBuilder();
	    csvSb.append("Request#");
	    csvSb.append("MethodName");
	    csvSb.append("Stopped?");
	    csvSb.append("CN#proposed");
	    csvSb.append("CN#actual");
	    csvSb.append("Time");
	    csvSb.newLine();

	    for (int i = 0; i < MethodTimers.size(); i++) {
		MethodTimer methodTimer = MethodTimers.get(i);
		csvSb.append("" + (i + 1));
		csvSb.append(methodTimer.getMethodFQName());
		csvSb.append("" + !methodTimer.isRunning());
		csvSb.append("??");
		csvSb.append("" + methodTimer.getEvents().size());
		csvSb.append(Timer.formatInMilliSecs(methodTimer.getTimeInterval()));
		csvSb.newLine();
	    }

	    sb.append(csvSb.toString());
	    sb.append("\n");
	    sb.append("2. Event timers\n");
	    csvSb.reset();
	    csvSb.append("Request#");
	    csvSb.append("Event#");
	    csvSb.append("Sources/Sinks");
	    csvSb.append("CNLabel");
	    csvSb.append("CNbci");
	    csvSb.append("CNLocation");
	    csvSb.append("EventType");
	    csvSb.append("CreationTime");
	    csvSb.append("CommunicationTime");
	    csvSb.append("SummaryTime");
	    csvSb.newLine();

	    for (int i = 0; i < MethodTimers.size(); i++) {
		MethodTimer methodTimer = MethodTimers.get(i);
		for (int j = 0; j < methodTimer.getEvents().size(); j++) {
		    EventTimer eventTimer = methodTimer.getEvents().get(j);
		    csvSb.append("" + (i + 1));
		    csvSb.append("" + (j + 1));
		    csvSb.append(eventTimer.getSourcesAndSinks().toString().substring(1,
			    eventTimer.getSourcesAndSinks().toString().length() - 1)); // no NRE if flow==null
		    csvSb.append(eventTimer.getCnLabel());
		    csvSb.append("" + eventTimer.getBci());
		    csvSb.append(eventTimer.getCnLocation());
		    csvSb.append(eventTimer.getEventName());
		    csvSb.append(Timer.formatInMilliSecs(eventTimer.getCreationTimeInterval()));
		    csvSb.append(Timer.formatInMilliSecs(eventTimer.getCommTimeInterval()));
		    csvSb.append(Timer.formatInMilliSecs(eventTimer.getCreationTimeInterval()
			    + eventTimer.getCommTimeInterval()));
		    csvSb.newLine();
		}
	    }
	    sb.append(csvSb.toString());
	    sb.append("\n\n\n\n");

	    statsFW.append(sb.toString());
	    statsFW.close();

	} catch (Exception e) {
	    e.printStackTrace();
	}

    }

    @Override
    public void contextDestroyed(ServletContextEvent arg0) {
	writeToFile();
	// ConfigProperties.getProperty(ConfigProperties.PROPERTIES.STATISTICS));
    }

    @Override
    public void contextInitialized(ServletContextEvent arg0) {

    }
}
