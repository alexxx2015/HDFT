package edu.tum.uc.jvm.utility.eval;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.Vector;

/**
 * Represents a timer for a method where <code>EventTimer</code>s can be added.
 * @author alex
 *
 */
public class MethodTimer extends Timer {

    /**
     * The fully qualified name of the method.
     */
    private String methodFQName;
    /**
     * The timers of all events thrown during execution of the method.
     */
    private List<EventTimer> events = new Vector<>();

    public MethodTimer(String threadId, String methodFQName) {
	this.threadId = threadId;
	this.methodFQName = methodFQName;
    }
    
    public String getMethodFQName() {
	return methodFQName;
    }
    
    /**
     * Returns a concatenated string with thread, method and start time uniquely identifying this timer.
     * @return The thread ID + method name + start time in string form.
     */
    public String getUniqueKey() {
	return threadId+methodFQName+startTime;
    }
    
    /**
     * Adds an event timer object to the list.
     * @param event
     */
    public void addEvent(EventTimer event) {
	events.add(event);
    }
    
    public List<EventTimer> getEvents() {
	return events;
    }
    
    /**
     * Concatenates the thread ID, the method name and the time interval and returns the resulting string.
     * @return A string representing this object.
     */
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
