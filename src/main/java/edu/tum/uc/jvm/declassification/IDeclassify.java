package edu.tum.uc.jvm.declassification;

public abstract class IDeclassify<T> {
	
	private T declassify(T o){
		return exec(o);
	}
	
	protected abstract T exec(T o);
	
	public static IDeclassify getInstance(){
		return null;
	}

}
