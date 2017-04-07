package edu.tum.uc.jvm.instrum.opt;

import java.lang.reflect.Field;
import java.util.Map;

import de.tum.in.i22.uc.cm.datatypes.basic.EventBasic;

public class MyEventBasic extends EventBasic {

	private Field strName;
	private Field strPep;
	private Field boolAllowImpliecActual;
	private Field boolIsActual;
	private Field mapParameters;
	private Field longTimestamp;

	public MyEventBasic(String name, Map<String, String> params) {
		this(name, params, false);
	}

	public MyEventBasic(String name, Map<String, String> params, boolean isActual) {
		super(name, params, isActual);
		// TODO Auto-generated constructor stub
		try {
			for (Field f : this.getClass().getSuperclass().getDeclaredFields()) {
				switch (f.getName()) {
				case "_name":
					f.setAccessible(true);
					this.strName = f;
					break;
				case "_pep":
					f.setAccessible(true);
					this.strPep = f;
					break;
				case "_allowImpliesActual":
					f.setAccessible(true);
					this.boolAllowImpliecActual = f;
					break;
				case "_isActual":
					f.setAccessible(true);
					this.boolIsActual = f;
					break;
				case "_parameters":
					f.setAccessible(true);
					this.mapParameters = f;
					break;
				case "_timestamp":
					f.setAccessible(true);
					this.longTimestamp = f;
					break;
				}
			}
		} catch (SecurityException | IllegalArgumentException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	public String getStrName() {
		try {
			return (String) strName.get(this);
		} catch (IllegalArgumentException | IllegalAccessException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return "";
	}

	public void setStrName(String strName) {
		try {
			this.strName.set(this, strName);
		} catch (IllegalArgumentException | IllegalAccessException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	public String getStrPep() {
		try {
			return (String) strPep.get(this);
		} catch (IllegalArgumentException | IllegalAccessException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return "";
	}

	public void setStrPep(String strPep) {
		try {
			this.strPep.set(this, strPep);
		} catch (IllegalArgumentException | IllegalAccessException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	public boolean getBoolAllowImpliecActual() {
		try {
			return boolAllowImpliecActual.getBoolean(this);
		} catch (IllegalArgumentException | IllegalAccessException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return false;
	}

	public void setBoolAllowImpliecActual(boolean boolAllowImpliecActual) {
		try {
			this.boolAllowImpliecActual.setBoolean(this, boolAllowImpliecActual);
		} catch (IllegalArgumentException | IllegalAccessException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	public boolean getBoolIsActual() {
		try {
			return boolIsActual.getBoolean(this);
		} catch (IllegalArgumentException | IllegalAccessException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return false;
	}

	public void setBoolIsActual(boolean boolIsActual) {
		try {
			this.boolIsActual.setBoolean(this, boolIsActual);
		} catch (IllegalArgumentException | IllegalAccessException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	public Map<String, String> getMapParameters() {
		try {
			return (Map<String, String>) mapParameters.get(this);
		} catch (IllegalArgumentException | IllegalAccessException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return null;
	}

	public void setMapParameters(Map<String, String> mapParameters) {
		try {
			this.mapParameters.set(this, mapParameters);
		} catch (IllegalArgumentException | IllegalAccessException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	public long getLongTimestamp() {
		try {
			return longTimestamp.getLong(this);
		} catch (IllegalArgumentException | IllegalAccessException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return 0;
	}

	public void setLongTimestamp(long longTimestamp) {
		try {
			this.longTimestamp.setLong(this, longTimestamp);
		} catch (IllegalArgumentException | IllegalAccessException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	public static void main(String[] args) {
		MyEventBasic m = new MyEventBasic("test", null);
		System.out.print(m.getName());
		m.setStrName("Hello");
		System.out.println(m.getName() + ", " + m.getStrName());

	}

}
