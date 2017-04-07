package edu.tum.uc.jvm.deprecated.misc;
import java.util.HashMap;
import java.util.Map;

public class OffsetTable {
	private Map<Integer, OffsetParameter> set;

	public OffsetTable() {
		set = new HashMap<Integer, OffsetParameter>();
	}

	public void put(int i, OffsetParameter op){
		set.put(i, op);
	}
	
	public OffsetParameter get(int i){
		return set.get(i);
	}

	public Map<Integer, OffsetParameter> getSet() {
		return set;
	}

	public void setSet(Map<Integer, OffsetParameter> set) {
		this.set = set;
	}
	
	
}
