package edu.tum.uc.jvm.deprecated.misc;

import java.lang.ref.WeakReference;

public class EventTimer extends Timer {

    private String eventName;
    private long createdTime;
    private int bci;
    private String cnLocation;
    private String cnLabel;
    private WeakReference<FlowID> flowID = new WeakReference<FlowID>(null); // avoid reference cycles

    public EventTimer(String threadId, String eventName, int bci, String cnLocation, String cnLabel) {
	this.threadId = threadId;
	this.eventName = eventName;
	this.bci = bci;
	this.cnLabel = cnLabel;
	this.cnLocation = cnLocation;
    }
    
    public void setFlowID(FlowID flow) {
	flowID = new WeakReference<FlowID>(flow);
    }
    
    public FlowID getFlow() {
	return flowID.get();
    }
    
    public String getEventName() {
	return eventName;
    }
    
    public void setCreated() {
	createdTime = System.nanoTime();
    }
    
    public boolean isSetCreated() {
	return createdTime > 0;
    }
    
    public long getCreationTimeInterval() {
	return createdTime - startTime;
    }
    
    public long getCommTimeInterval() {
	return stopTime - createdTime;
    }
    
    public int getBci() {
        return bci;
    }

    public String getCnLocation() {
        return cnLocation;
    }

    public String getCnLabel() {
        return cnLabel;
    }
    
    public String getUniqueKey() {
	return threadId+eventName+startTime;
    }
    
    public String toString() {
	return threadId + " " + eventName + " creation=" + getCreationTimeInterval() + " comm=" + getCommTimeInterval();
    }
    
    public boolean equals(Object other) {
	if(other == null) return false;
	else if (!(other instanceof EventTimer)) return false;
	else {
	    EventTimer otherMt = (EventTimer)other;
	    return getUniqueKey().equals(otherMt.getUniqueKey());
	}
    }
    
}
