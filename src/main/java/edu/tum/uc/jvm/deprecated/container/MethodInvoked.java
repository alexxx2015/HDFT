package edu.tum.uc.jvm.deprecated.container;

public class MethodInvoked extends Container {
	
	public MethodInvoked(String p_name){
		super(ContainerType.METHOD);		
		this.setName(p_name);
	}
}
