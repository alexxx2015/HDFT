package edu.tum.uc.jvm.utility;

public class MemoryAddress {
	static {
		System.loadLibrary("memadr");
	}
	
	private native void sayHello();
	
	private native long getObjectAddress(Object o);
	
	public static void main(String[] args){
		MemoryAddress m = new MemoryAddress();
		m.sayHello();
		long l = m.getObjectAddress(m);
		long adr = UnsafeUtil.getObjectAddress(m);
		System.out.println(l+", "+adr+", ");
	}
}
