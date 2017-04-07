package edu.tum.uc.jvm.shrift;

import org.objectweb.asm.Label;

public class MyLabel extends Label {
	
	private int offset;
	
	public MyLabel(int offset){
		super();
		this.offset = offset;
	}

	public int getOffset() {
		return offset;
	}	
	
	public String toString(){
		return "Label "+this.getOffset();
	}
	
}
