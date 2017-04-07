package edu.tum.uc.jvm.deprecated.container;

public class ObjectReference extends Container {
	
	private String type;
	private String creator;

	public ObjectReference(String p_name) {
		super(ContainerType.OBJECT_REFERENCE);
		
		this.setName(p_name);
	}
	
	public String getName(){
		return this.getType()+"["+super.getName()+"]";
	}
	
	public void setType(String p_type){
		this.type = p_type;
	}
	
	public String getType(){
		return this.type;
	}
	
	public void setCreator(String p_creator){
		this.creator = p_creator;
	}
	public String getCreator(){
		return this.creator;
	}
}
