package edu.tum.uc.jvm.deprecated.container;

public class ArrayField extends Container {
	
	private boolean isArrayField;
	
	private Container arrFieldIdx;
	
	public ArrayField(String p_name, Container p_arrFieldIdx){
		super(ContainerType.ARRAY_FIELD);
		
		this.setName(p_name+"["+p_arrFieldIdx.getName()+"]");
		this.setIsArrayField(true);
		this.setArrayFieldIdx(p_arrFieldIdx);
	}
	
	public boolean isArrayField(){
		return this.isArrayField;
	}
	
	private void setIsArrayField(boolean p_isArray){
		this.isArrayField = p_isArray;
	}
	
	private void setArrayFieldIdx(Container p_arrFieldIdx){
		this.arrFieldIdx = p_arrFieldIdx;
	}
	
	public Container getArrayFieldIdx(){
		return this.arrFieldIdx;
	}
}
