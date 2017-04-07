package edu.tum.uc.jvm.utility.eval;

import java.text.DecimalFormat;

/**
 * An abstract base class representing a timer that can be started and stopped based on system time.
 * @author alex
 *
 */
public abstract class Timer {

    /**
     * The ID of the thread this timer belongs to.
     */
    protected String threadId;
    /**
     * The start timestamp.
     */
    protected long startTime;
    /**
     * The stop timestamp.
     */
    protected long stopTime;
    
    /**
     * Returns the difference between the start and the stop timestamps.
     * @return A time interval in nanoseconds.
     */
    public long getTimeInterval() {
	return stopTime - startTime;
    }
    
    /**
     * Formats a given nanosecond time interval to milliseconds with two signs after the decimal separator.
     * @param timeInterval A time interval in nanoseconds.
     * @return A string containing the the given time interval formatted to two signs after comma.
     */
    public static String formatInMilliSecs(long timeInterval) {
	return new DecimalFormat("#.##").format((double)timeInterval / 1000000);
    }
    
    public String getThreadID() {
	return threadId;
    }
   
    /**
     * 
     */
    public void start() {
	this.startTime = System.nanoTime();
    }
    
    public void stop() {
	this.stopTime = System.nanoTime();
    }
    
    /**
     * Indicates if the timer is running, i.e. the start time has been set but not the stop time.
     * @return A boolean indicating the running status of the timer.
     */
    public boolean isRunning() {
	return startTime > 0 && stopTime == 0;
    }
    /**
     * Indicates if the timer is already finished, i.e. both start and stop timers are set.
     * @return A boolean indicating the finishing status of the timer.
     */
    public boolean hasFinished() {
	return startTime > 0 && stopTime > 0;
    }
}
