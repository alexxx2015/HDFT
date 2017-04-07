package edu.tum.uc.jvm.deprecated.container;

public class Field extends Container {
	
	private boolean isStatic;
	
	public Field(String p_name, boolean p_isStatic){
		super(ContainerType.STATIC_ATTR_FIELD);
		
		this.setName(p_name);
		this.setIsStatic(p_isStatic);
	}
	
	public boolean isStatic(){
		return isStatic;
	}
	
	private void setIsStatic(boolean p_isStatic){
		this.isStatic = p_isStatic;
	}
}
