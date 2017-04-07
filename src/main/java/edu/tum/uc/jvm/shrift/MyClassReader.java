package edu.tum.uc.jvm.shrift;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.Label;

public class MyClassReader extends ClassReader {
	
	Label[] myLabel;

	public MyClassReader(byte[] arg0) {
		super(arg0);
	}
	
	@Override
    protected Label readLabel(int offset, Label[] labels) {
		myLabel = labels;		
		if(myLabel[offset] == null){
			myLabel[offset] = new MyLabel(offset);
		}
		
		return super.readLabel(offset, labels);
    }
}
