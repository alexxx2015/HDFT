package edu.tum.uc.jvm.deprecated.container;

import java.util.Iterator;

import edu.tum.uc.jvm.utility.Mnemonic;

public class ArithResult extends Container {
	private int operation;
	private String mnemonic_code;
	
	public ArithResult(String p_name, int p_operation){
		super(ContainerType.ARITHMETIC_OPERATION);
		
		this.setName(p_name);
		this.setOperation(p_operation);
	}
	
	public void setOperation(int p_operation){
		this.operation = p_operation;
		this.mnemonic_code = Mnemonic.OPCODE[p_operation];
	}
	
	public int getOperation(){
		return this.operation;
	}
	
	@Override
	public String getName(){
		String _return = super.getName()+"["+this.mnemonic_code;
		Iterator<Container> it = this.getContainers().iterator();
		while(it.hasNext()){
			_return += "|"+it.next().getName();
		}
		_return += "]";
		return _return;
	}
	/*
	public String getName(){
		String _return = super.getName()+"/"+this.mnemonic_code+"{";
		Iterator<Container> it = this.getContainers().iterator();
		while(it.hasNext()){
			_return += it.next().getName()+";";
		}
		if(this.getContainers().size() > 0){
			_return =  _return.substring(0, _return.length()-1);	
		}
		return _return+"}";
	}
	*/
}
