package edu.tum.uc.jvm.deprecated.misc;

public class FlowID implements Comparable<FlowID> {

    private static int index = 1;

    private int number;
    private String sourceID;
    private String sinkID;

    public FlowID(String sourceID) {
	this.number = index++;
	this.sourceID = sourceID;
    }

    public int getNumber() {
	return number;
    }

    public String getSourceID() {
	return sourceID;
    }

    public String getSinkID() {
	return sinkID;
    }

    public void close(String sinkID) {
	this.sinkID = sinkID;
    }

    public boolean isOpen() {
	return sinkID == null;
    }

    public String toString() {
	return "Flow" + number + ":" + sourceID + (sinkID != null ? (":" + sinkID) : "");
    }

    public int hashCode() {
	return number;
    }

    public boolean equals(Object other) {
	if (other == null)
	    return false;
	else if (!(other instanceof FlowID))
	    return false;
	else {
	    FlowID otherFlID = (FlowID) other;
	    return otherFlID.number == this.number;
	}
    }

    public int compareTo(FlowID other) {
	final int BEFORE = -1;
	final int EQUAL = 0;
	final int AFTER = 1;
	if (this.number == other.number)
	    return EQUAL;
	else if (this.number < other.number)
	    return BEFORE;
	else
	    return AFTER;
    }

}
