package edu.tum.uc.jvm.deprecated.misc;

import java.text.DecimalFormat;

public abstract class Timer {

    protected String threadId;
    
    protected long startTime;
    protected long stopTime;
    
    public long getTimeInterval() {
	return stopTime - startTime;
    }
    
    public static String formatInMilliSecs(long timeInterval) {
	return new DecimalFormat("#.##").format((double)timeInterval / 1000000);
    }
    
    public String getThreadID() {
	return threadId;
    }
   
    public void start() {
	this.startTime = System.nanoTime();
    }
    
    public void stop() {
	this.stopTime = System.nanoTime();
    }
    
    public boolean isRunning() {
	return startTime > 0 && stopTime == 0;
    }
    
    public boolean hasFinished() {
	return startTime > 0 && stopTime > 0;
    }
}
