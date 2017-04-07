package edu.tum.uc.jvm.deprecated.misc;
public class OffsetNode {
	private String id;
	private StaticAnalysis.nodeType type;

	public OffsetNode(String id, StaticAnalysis.nodeType type) {
		super();
		this.id = id;
		this.type = type;
	}

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public StaticAnalysis.nodeType getType() {
		return type;
	}

	public void setType(StaticAnalysis.nodeType type) {
		this.type = type;
	}

}
