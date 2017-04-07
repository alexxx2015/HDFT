package edu.tum.uc.jvm.utility;

import java.util.LinkedList;
import java.util.List;

public class MethEvent {

	public enum Type {
		START, END;
	}

	private String methodInvoker;
	private String methoderInvokee;
	private String methodInvokerSig;
	private String methodInvokeeSig;
	private String direction;
	int opcode;
	int offset;
	private Type type;
	private boolean actual;
	private String sinkSource;
	private String fileDescriptor = "";
	private List<Integer> contextIds;

	public MethEvent(Type t) {
		this.type = t;
	}

	public MethEvent() {
	}

	public String getMethodInvoker() {
		return methodInvoker;
	}

	public void setMethodInvoker(String methodInvoker) {
		this.methodInvoker = methodInvoker;
	}

	public String getMethodInvokee() {
		return methoderInvokee;
	}

	public void setMethodInvokee(String methoderInvokee) {
		this.methoderInvokee = methoderInvokee;
	}

	public String getMethodInvokerSig() {
		return methodInvokerSig;
	}

	public void setMethodInvokerSig(String methodInvokerSig) {
		this.methodInvokerSig = methodInvokerSig;
	}

	public String getMethodInvokeeSig() {
		return methodInvokeeSig;
	}

	public void setMethodInvokeeSig(String methodInvokeeSig) {
		this.methodInvokeeSig = methodInvokeeSig;
	}

	public int getOpcode() {
		return opcode;
	}

	public void setOpcode(int opcode) {
		this.opcode = opcode;
	}

	public int getOffset() {
		return offset;
	}

	public void setOffset(int offset) {
		this.offset = offset;
	}

	public String getDirection() {
		return direction;
	}

	public void setDirection(String direction) {
		this.direction = direction;
	}

	public boolean isActual() {
		return actual;
	}

	public void setActual(boolean actual) {
		this.actual = actual;
	}

	public Type getType() {
		return this.type;
	}

	public void setType(Type type) {
		this.type = type;
	}

	public String toString() {
		return this.getMethodInvoker() + ":" + this.getMethodInvokerSig() + ":"
				+ this.getMethodInvokee() + ":" + this.getMethodInvokeeSig()
				+ ":" + this.getOffset() + ":" + this.getOpcode() + ":"
				+ this.sinkSource+":"+this.getContextIds();
	}

	public String getSinkSource() {
		return sinkSource;
	}

	public void setSinkSource(String sinkSource) {
		this.sinkSource = sinkSource;
	}

	public String getFileDescriptor() {
		return fileDescriptor;
	}

	public void setFileDescriptor(String fileDescriptor) {
		this.fileDescriptor = fileDescriptor;
	}

	public List<Integer> getContextId() {
		return contextIds;
	}

	public void setContextId(List<Integer> contextId) {
		this.contextIds = contextId;
	}

	public void addContextIds(int id) {
		if (this.contextIds == null) {
			this.contextIds = new LinkedList<Integer>();
		}
		this.contextIds.add(id);
	}

	public void addContextIds(String ids) {
		for (String s : ids.split(",")) {
			s = s.replace("[", "").replace("]", "");
			if (!"".equals(s.trim())) {
				this.addContextIds(Integer.parseInt(s));
			}
		}
	}

	public String getContextIds() {
		String _return = "[";
		if (this.contextIds != null) {
			for (int i = 0; i < this.contextIds.size(); i++) {
				_return += this.contextIds.get(i) + ",";
			}
			if (_return.charAt(_return.length() - 1) == ',') {
				_return = _return.substring(0, _return.length() - 1);
			}
		}
		return _return + "]";
	}

//	public String toString() {
//		return this.getMethodInvoker() + ":" + this.getMethodInvokerSig() + ":"
//				+ this.getMethodInvokee() + ":" + this.getMethodInvokeeSig()
//				+ ":" + this.getOffset() + ":" + this.getOpcode() + ":"
//				+ this.getSinkSource() + ":" + this.getContextIds() + ":"
//				+ this.getFileDescriptor();
//	}
}
