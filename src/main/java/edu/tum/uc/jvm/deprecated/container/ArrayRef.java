package edu.tum.uc.jvm.deprecated.container;

public class ArrayRef extends Container {
	
	private String length;
	private String type;
	
	public ArrayRef(String p_name){
		super(ContainerType.ARRAY);
		
		this.setName(p_name);
	}
	
	public void setArrayLength(String p_length){
		this.length = p_length;
	}
	
	public String getArrayLength(){
		return this.length;
	}
	
	public void setArrayType(String p_type){
		this.type = p_type;
	}
	
	public String getArrayType(){
		return this.type;
	}
}
