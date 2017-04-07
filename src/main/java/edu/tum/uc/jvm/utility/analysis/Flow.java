package edu.tum.uc.jvm.utility.analysis;

import java.util.LinkedList;
import java.util.List;

import edu.tum.uc.jvm.instrum.opt.InstrumDelegateOpt;

public class Flow {

	public static String OP_ASSIGN = "assign";
	public static String OP_COMPOUND = "compound";
	public static String OP_CALL = "call";
	public static String OP_REFERENCE = "reference";
	public static String OP_MODIFY = "modify";

	private String sink;
	private List<String> source;
	private String currentSource;
	private List<Chop> chopNodes;

	public void addSource(String source) {
		if (this.source == null)
			this.source = new LinkedList<String>();
		this.source.add(source);
		this.currentSource = source;
	}

	public void addChopNode(int p_byteCodeIndex, String p_ownerMethod, String p_label, String p_operation,
			String p_local2vn) {
		if (this.chopNodes == null)
			chopNodes = new LinkedList<Chop>();
		this.chopNodes
				.add(new Chop(p_byteCodeIndex, p_ownerMethod, p_label, p_operation, p_local2vn, this.currentSource));
	}

	public List<Chop> getChopNodes() {
		return this.chopNodes;
	}

	public String getSink() {
		return sink;
	}

	public void setSink(String sink) {
		this.sink = sink;
	}

	public List<String> getSource() {
		return source;
	}

	public void setSource(List<String> source) {
		this.source = source;
	}

	public class Chop {
		private int byteCodeIndex;
		private String ownerMethod;
		private String label;
		private String operation;
		private String sourceId;
		private String local2vn;
		public static final String LABEL_SEP = "|";
		public static final String LABEL_SPLIT = "\\|";

		public Chop(int p_byteCodeIndex, String p_ownerMethod) {
			this(p_byteCodeIndex, p_ownerMethod, "", "", "");
		}

		public Chop(int p_byteCodeIndex, String p_ownerMethod, String p_label, String p_operation, String p_local2vn,
				String sourceId) {
			this(p_byteCodeIndex, p_ownerMethod, p_label, p_operation, p_local2vn);
			this.sourceId = sourceId;
		}

		public Chop(int p_byteCodeIndex, String p_ownerMethod, String p_label, String p_operation, String p_local2vn) {
			this.byteCodeIndex = p_byteCodeIndex;
			this.ownerMethod = p_ownerMethod;
			this.label = p_label;
			this.operation = p_operation;
			this.local2vn = p_local2vn;
		}

		public int getByteCodeIndex() {
			return this.byteCodeIndex;
		}

		public String getOwnerMethod() {
			return this.ownerMethod;
		}

		public String getLabel() {
			return this.label;
		}

		public String getLabelWithSource() {
			return this.label + LABEL_SEP + this.sourceId;
		}

		public String getOperation() {
			return this.operation;
		}

		public String getSourceId() {
			return this.sourceId;
		}

		public String getLocal2vn() {
			return this.local2vn;
		}
	}
}
