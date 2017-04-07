package edu.tum.uc.jvm.deprecated.container;

import java.util.Iterator;
import java.util.LinkedList;

public class Container {
	private ContainerType type; 
	private String name;
	private int opcode;
	
	private LinkedList<Container> coveredContainer;
	
	public Container(ContainerType p_type){
		this.setContainerType(p_type);
		this.coveredContainer = new LinkedList<Container>();
	}
	
	public void setContainerType(ContainerType p_type){
		this.type = p_type;
	}
	
	public ContainerType getContainerTyp(){
		return this.type;
	}
	
	protected void setName(String p_name){
		this.name = p_name;
	}
	
	public String getName(){
		return this.name;
	}
	
	public void addContainer(Container p_container){
		this.coveredContainer.add(p_container);
	}
	
	public LinkedList<Container> getContainers(){
		return this.coveredContainer;
	}
	
	public void setOpcode(int p_opcode){
		this.opcode = p_opcode;
	}
	
	public int getOpcode(){
		return this.opcode;
	}
	
	public String param2String(){
		StringBuilder sb = new StringBuilder();
		sb.append(this.getName());
		//sb.append(";").append(this.coveredContainer.size());
		Iterator<Container> it = this.coveredContainer.iterator();
		while(it.hasNext()){
			sb.append(it.next().param2String());
		}
		return sb.toString();
	}
}
