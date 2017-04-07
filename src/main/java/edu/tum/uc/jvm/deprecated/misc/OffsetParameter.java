package edu.tum.uc.jvm.deprecated.misc;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

public class OffsetParameter {
	/*
	 * parameter = -1 --> return value parameter = 0 --> method invocation
	 * 
	 * (not sure we need the distinction. for sources only -1 makes sense and
	 * for sinks only 0 does)
	 * 
	 * parameter = n>0 --> n-th parameter
	 */

	private Map<Integer, OffsetNode> offsets;
	private String[] signature;
	private int[] context;

	public Map<Integer, OffsetNode> getOffsets() {
		return offsets;
	}

	public void setOffsets(Map<Integer, OffsetNode> p_offsets) {
		this.offsets = p_offsets;
	}

	public OffsetParameter(int parameter, OffsetNode on, String[] sign, int[] context) {
		Map<Integer,OffsetNode> m= new HashMap<Integer,OffsetNode>();
		m.put(parameter, on);
		this.offsets = m;
		this.signature = sign;
		this.append(context);
	}
	
	public OffsetParameter(Map<Integer, OffsetNode> map, String sign) {
		this.offsets = map;
		this.signature = new String[]{sign};
	}

	public OffsetParameter(int parameter, OffsetNode on, String sign) {
		Map<Integer,OffsetNode> m= new HashMap<Integer,OffsetNode>();
		m.put(parameter, on);
		this.offsets = m;
		this.signature = new String[]{sign};
	}
	
	
	public OffsetParameter(Map<Integer, OffsetNode> map) {
		this.offsets = (map != null ? map : new HashMap<Integer, OffsetNode>());
		this.signature = new String[]{"<no signature provided>"};
	}

	
	
	public String getIdOfPar(int parameter){
		if (offsets==null) return null;
		OffsetNode on =this.offsets.get(parameter);
		if (on==null) return null;
		return on.getId();
	}
	
	
	/*
	 * does a bit of math with the nodeType. note that no parameter should be
	 * given type error, nor none.
	 */
	public void append(Map<Integer, OffsetNode> map) {
		if ((map==null)||!(map instanceof Map<?,?>)) return;
		
		for (Entry<Integer, OffsetNode> e : map.entrySet()) {
			OffsetNode on = this.offsets.get(e.getKey());
			if (on == null) {
				this.offsets.put(e.getKey(), e.getValue());
			} else {
				StaticAnalysis.nodeType oldT = on.getType();
				String oldI = on.getId();

				OffsetNode onNew = e.getValue();
				if (onNew != null) {
					String newI = onNew.getId();
					StaticAnalysis.nodeType newT = onNew.getType();
				
					if (oldT==newT){
						if (oldI.equals(newI)){
							System.err.println("No need to merge, old and new are the same");
							break;
						} else{
							System.err.println("Not possible to merge, parameter "+e.getKey()+" alrady exists but with different id");
							break;
						}
					}
					
					StaticAnalysis.nodeType resT = StaticAnalysis.append(oldT, newT);
				
					on.setType(resT);

					if (resT==StaticAnalysis.nodeType.ERROR) return;

					if (oldT != resT){
						on.setId(on.getId()+"-"+onNew.getId());
					}
						
				} else {
					System.err.println("Error! wrong or no type provided!");
				}
			}
		}
	}

	
	public void append(int parameter, OffsetNode on) {
		Map<Integer,OffsetNode> m= new HashMap<Integer,OffsetNode>();
		m.put(parameter, on);
		append(m);
	}
	
	public void append(int parameter, StaticAnalysis.nodeType t) {
		OffsetNode on=new OffsetNode("test",t);
		append(parameter,on);
	}
	
	public void append(int parameter, String id, StaticAnalysis.nodeType t) {
		OffsetNode on=new OffsetNode(id,t);
		append(parameter,on);
	}
	public void append(int parameter, String id, StaticAnalysis.nodeType t, int[] context) {
		this.append(context);
		append(parameter, id, t);
	}
	private void append(int[] pcontext){
		if(this.context == null){
			this.context = pcontext;
		}
		else{
			boolean found = false;
			for(int i = 0; i < pcontext.length; i++){				
				for(int j = 0; j < this.context.length; j++){
					if(this.context[j] == pcontext[i]){
						found = true;
						break;
					}
				}
				if(found)
					break;
			}
			if(!found){
				int[] contextHelper = new int[this.context.length + pcontext.length];
				System.arraycopy(this.context, 0, contextHelper, 0, this.context.length);
				System.arraycopy(pcontext, 0, contextHelper, this.context.length,  pcontext.length);
				this.context = contextHelper;
			}
		}
	}
	
	
	public Set<Integer> getParsOfType(StaticAnalysis.nodeType t) {
		if (!(t instanceof StaticAnalysis.nodeType))
			return null;
		Set<Integer> res = new HashSet<Integer>();
		for (Entry<Integer, OffsetNode> e : this.offsets.entrySet()) {
			if (e.getValue().getType().equals(t))
				res.add(e.getKey());
		}
		return res;
	}

	public StaticAnalysis.nodeType getTypeOfPar(int parameter) {
		try {
			OffsetNode on=offsets.get(parameter);
			if (on == null)
				return StaticAnalysis.nodeType.NONE;
			StaticAnalysis.nodeType n = on.getType();
			if (n == null)
				return StaticAnalysis.nodeType.NONE;
			return n;
		} catch (Exception e) {
			System.err
					.println("Something went horribly wrong in function getTypeOfPar!!!");
			return StaticAnalysis.nodeType.ERROR;
		}
	}

	public String[] getSignature() {
		return signature;
	}

	public void setSignature(String[] signature) {
		this.signature = signature;
	}

	public int[] getContext() {
		return context;
	}

	public void setContext(int[] context) {
		this.context = context;
	}	
}
