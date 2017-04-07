package edu.tum.uc.jvm.deprecated.container;

public class LocalVariable extends Container {	
	public LocalVariable(String p_name){
		super(ContainerType.VARIABLE);		
		this.setName(p_name);
	}
}
