package edu.tum.uc.jvm.deprecated.misc;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.Vector;

public class MethodTimer extends Timer {

    private String methodFQName;
    private List<EventTimer> allEvents = new Vector<>();
    private List<EventTimer> currentEvents = new Vector<>();
    private SortedMap<FlowID, List<EventTimer>> flows = new TreeMap<>();
    private boolean flowRunning;

    public MethodTimer(String threadId, String methodFQName) {
	this.threadId = threadId;
	this.methodFQName = methodFQName;
    }
    
    public String getMethodFQName() {
	return methodFQName;
    }
    
    public String getUniqueKey() {
	return threadId+methodFQName+startTime;
    }
    
    public void addEvent(EventTimer event) {
	if (flowRunning) {
	    event.setFlowID(flows.lastKey());
	}
	currentEvents.add(event);
    }
    
    public void startFlow(String sourceId) {
	// if flow is started without ending previous one
	// or events happened before first flow even began
	allEvents.addAll(currentEvents);
	
	currentEvents = new Vector<>();
	
	flows.put(new FlowID(sourceId), currentEvents);
	flowRunning = true;
    }
    
    public void endFlow(String sinkId) {
	if (flowRunning) {
	    flows.lastKey().close(sinkId);
	}
	
	allEvents.addAll(currentEvents);
	currentEvents = new Vector<>();
	
	flowRunning = false;
    }
    
    public void stop() {
	super.stop();
	allEvents.addAll(currentEvents);
	currentEvents = new Vector<>();
	flowRunning = false;
    }
    
    public boolean isFlowRunning() {
	return flowRunning;
    }
    
    public List<EventTimer> getAllEvents() {
	return allEvents;
    }
    
    public SortedMap<FlowID, List<EventTimer>> getFlows() {
	return flows;
    }
    
    public String toString() {
	return threadId + " " + methodFQName + " " + getTimeInterval();
    }
    
    public boolean equals(Object other) {
	if(other == null) return false;
	else if (!(other instanceof MethodTimer)) return false;
	else {
	    MethodTimer otherMt = (MethodTimer)other;
	    return getUniqueKey().equals(otherMt.getUniqueKey());
	}
    }
}
