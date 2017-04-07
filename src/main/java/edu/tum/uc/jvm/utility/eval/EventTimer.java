package edu.tum.uc.jvm.utility.eval;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import edu.tum.uc.jvm.utility.analysis.SinkSource;

/**
 * Represents a timer for an event.
 * @author alex
 *
 */
public class EventTimer extends Timer {

    /**
     * The name of the event.
     */
    private String eventName;
    /**
     * The timestamp of the event creation.
     */
    private long createdTime;
    /**
     * The bytecode index where the event was thrown.
     */
    private int bci;
    /**
     * The fully qualified method name where the event was thrown.
     */
    private String cnLocation;
    /**
     * The chopnode label belonging to the event.
     */
    private String cnLabel;
    /**
     * A set of source and sink IDs that have been triggered upon event creation.
     */
    private Set<String> sourcesAndSinks = new HashSet<>();
    
    public EventTimer(String threadId, String eventName, int bci, String cnLocation, String cnLabel, Set<String> sourcesAndSinks) {
	this.threadId = threadId;
	this.eventName = eventName;
	this.bci = bci;
	this.cnLabel = cnLabel;
	this.cnLocation = cnLocation;
	this.sourcesAndSinks = sourcesAndSinks;
    }
    
    public Set<String> getSourcesAndSinks() {
        return sourcesAndSinks;
    }
    
    public String getEventName() {
	return eventName;
    }
    
    /**
     * Sets the creation timestamp to the current system nanosecond time.
     */
    public void setCreated() {
	createdTime = System.nanoTime();
    }
    
    /**
     * Indicates if the event timer has logged creation, i.e. the creation timestamp has been set.
     * @return A boolean indicating the creation status of the timer.
     */
    public boolean isSetCreated() {
	return createdTime > 0;
    }
    
    /**
     * Returns the difference between the start and the creation timestamps.
     * @return A time interval in nanoseconds.
     */
    public long getCreationTimeInterval() {
	return createdTime - startTime;
    }
    
    public long getCreatedTime(){
    	return this.createdTime;
    }
    public long getStartTime(){
    	return this.startTime;
    }
    
    /**
     * Returns the difference between the creation and the stop timestamps.
     * @return A time interval in nanoseconds.
     */
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
    
    /**
     * Concatenates the thread ID, the event name and the creation/communication time intervals and returns the resulting string.
     * @return A string representing this object.
     */
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
