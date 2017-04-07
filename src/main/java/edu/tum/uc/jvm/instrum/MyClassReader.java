package edu.tum.uc.jvm.instrum;

import org.objectweb.asm.ClassReader;

public class MyClassReader extends ClassReader {
	

	public MyClassReader(byte[] arg0) {
		super(arg0);
	}
}
