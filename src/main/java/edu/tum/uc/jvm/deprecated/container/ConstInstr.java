package edu.tum.uc.jvm.deprecated.container;

import edu.tum.uc.jvm.utility.Mnemonic;

public class ConstInstr extends Container{
	
	private String value;
	private String mnemonic_code;
		
	public ConstInstr(String p_name){
		super(ContainerType.CONSTANT);
		this.setName(p_name);
	}	
	
	public void setValue(String p_value){
		this.value = p_value;
	}
	
	public String getValue(){
		return this.value;
	}
	
	@Override
	public String getName(){		
		this.mnemonic_code = Mnemonic.OPCODE[this.getOpcode()];
		return this.mnemonic_code+"["+super.getName()+"]";
	} 
}
