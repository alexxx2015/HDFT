package edu.tum.uc.jvm;

import java.lang.instrument.*;
import java.lang.management.ManagementFactory;

import edu.tum.uc.jvm.instrum.opt.InstrumDelegateOpt;
import edu.tum.uc.jvm.shrift.MirrorStack;

/**
 * Hooks into the java class loading process as a Javaagent
 * @author alex
 *
 */
public final class UcAgent {	
	public static Instrumentation INSTR;

	public static void premain(String args, Instrumentation instr) throws Exception{
		INSTR = instr;
//		INSTR.addTransformer(new MyUcTransformer());
		INSTR.addTransformer(new MyUcTransformerOpt());
		MirrorStack.runnable = true;	
		
		if (!InstrumDelegateOpt.eventBasicRepoAdded) {
			InstrumDelegateOpt.populateMyEventBasic();
			InstrumDelegateOpt.eventBasicRepoAdded = true;
		}
	}

	public static void main(String args, Instrumentation p_instr) throws Exception{
	}

	public static void agentmain(String args, Instrumentation p_instr) throws Exception{
	}	
}