package edu.tum.uc.jvm.declassification.qif;

import java.lang.reflect.Array;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import org.objectweb.asm.Type;

import edu.tum.uc.jvm.utility.Mnemonic;
import edu.tum.uc.jvm.utility.UnsafeUtil;
import edu.tum.uc.jvm.utility.Utility;
import groovyjarjarasm.asm.Opcodes;

public class QIFTrackerOri {
	private static class Qif {
		private String id;
		private double oriQty = 0.0;
		private double actQty = 0.0;

		Qif(double qty, String id) {
			this.oriQty = this.actQty = qty;
			this.id = id;
		}

		public double getOriQty() {
			return this.oriQty;
		}

		public double getActQty() {
			return this.actQty;
		}

		public String getId() {
			return this.id;
		}

		public void setOritQty(double qty) {
			this.oriQty = qty;
		}

		public void setActQty(double qty) {
			this.actQty = qty;
		}
	}

	private static Map<String, Qif> qif = new HashMap<String, Qif>();

	public static enum SIZE {
		BYTE(1), BOOLEAN(1), CHARACTER(2), DOUBLE(8), FLOAT(4), INTEGER(4), LONG(8), SHORT(2);

		private int size;

		SIZE(int size) {
			this.size = size;
		}

		public int getSize() {
			return this.size;
		}
	}

	public static void addQty(Object o, String sourceId) {
		long size = getSize(o);
		qif.put(sourceId, new Qif((double) size, sourceId));
	}

	// public static void decAbsQty(double amount, String sourceId) {
	public static void decSubSequQty(String shortenStr, String origStr, String sourceId) {
		Qif actualAmount = qif.get(sourceId);
		double amount = (double) shortenStr.length() / origStr.length();
		double aQif = actualAmount.getActQty() * amount;
		actualAmount.setActQty(aQif);
	}

	public static void decSplitQty(String[] splitStr, String sourceId) {
		Qif actualAmount = qif.get(sourceId);
		int totalLength = Arrays.toString(splitStr).length() - 2;
		double amount = -1;
		for (String s : splitStr) {
			s = s.trim();
			amount = Math.max(amount, (double) s.length() / totalLength);
		}
		double aQif = actualAmount.getActQty() * (amount);
		actualAmount.setActQty(aQif);
	}

	public static void decArithQty(Object o1, Object o2, String sourceId, int opcode) {
		double diff = 0;
		double amnt1 = -1, amnt2 = -1;
		if (o1 instanceof Integer && o2 instanceof Integer){
//			diff = (double)Math.abs((Math.abs(((Integer) o1).intValue()) - Math.abs(((Integer) o2).intValue())));
			amnt1 = (double)Math.abs(((Integer) o2).intValue())/Integer.MAX_VALUE;
			amnt2 = (double)Math.abs(((Integer) o1).intValue())/Integer.MAX_VALUE;
		}
		else if (o1 instanceof Long && o2 instanceof Long){
//			diff = Math.abs(((Long) o1).intValue() - ((Long) o2).intValue()) / (double) Integer.MAX_VALUE;
			amnt1 = (double)Math.abs(((Long) o2).longValue())/Long.MAX_VALUE;
			amnt2 = (double)Math.abs(((Long) o1).longValue())/Long.MAX_VALUE;
		}
		else if (o1 instanceof Float && o2 instanceof Float){
//			diff = Math.abs(((Float) o1).intValue() - ((Float) o2).intValue()) / (double) Integer.MAX_VALUE;
			amnt1 = (double)Math.abs(((Float) o2).floatValue())/Float.MAX_VALUE;
			amnt2 = (double)Math.abs(((Float) o1).floatValue())/Float.MAX_VALUE;
		}
		else if (o1 instanceof Double && o2 instanceof Double){
//			diff = Math.abs(((Double) o1).intValue() - ((Double) o2).intValue()) / (double) Integer.MAX_VALUE;
			amnt1 = (double)Math.abs(((Double) o2).doubleValue())/Double.MAX_VALUE;
			amnt2 = (double)Math.abs(((Double) o1).doubleValue())/Double.MAX_VALUE;
		}
		diff = Math.min(amnt1, amnt2);

		// compute the diff as the ratio between the number of shifted bits and
		// the number of total possible shifts which is 2^5
		// as java only considers the last five bits of a word for the number of
		// shift positions.
		if (opcode == Opcodes.ISHL || opcode == Opcodes.ISHR || opcode == Opcodes.IUSHR) {
			diff = ((Integer) o2).intValue() / Math.pow(2, 5);
		} else if (opcode == Opcodes.LSHL || opcode == Opcodes.LSHR || opcode == Opcodes.LUSHR) {
			diff = ((Long) o2).intValue() / Math.pow(2, 6);
		}

		Qif actualAmount = qif.get(sourceId);
		if (actualAmount == null)
			return;

		double aQif = actualAmount.getActQty() * (1 - diff);
		actualAmount.setActQty(aQif);
	}

	// Tracks the quantitative flow for conversion commands
	// Quantity is reduced to the value space of the target data type, but only
	// if the actual value exceed the max value of the target type
	public static void decConvQty(Object o, String sourceid, int opcode) {
		double qty = 0;
		int i;
		float f;
		long l;
		double d;
		switch (opcode) {
		case Opcodes.I2B:
			i = ((Integer) o).intValue();
			if (i > Byte.MAX_VALUE)
				qty = (i - Byte.MAX_VALUE) / (double) Integer.MAX_VALUE;
			break;
		case Opcodes.I2C:
			i = ((Integer) 0).intValue();
			if (i > Character.MAX_VALUE)
				qty = (i - Character.MAX_VALUE) / (double) Integer.MAX_VALUE;
			break;
		case Opcodes.I2S:
			i = ((Integer) 0).intValue();
			if (i > Short.MAX_VALUE)
				qty = (i - Short.MAX_VALUE) / (double) Integer.MAX_VALUE;
			break;
		case Opcodes.F2I:
			f = ((Float) o).floatValue();
			if (f > Integer.MAX_VALUE)
				qty = (f - Integer.MAX_VALUE) / (double) Float.MAX_VALUE;
			break;
		case Opcodes.F2L:
			f = ((Float) o).floatValue();
			if (f > Long.MAX_VALUE)
				qty = (f - Long.MAX_VALUE) / (double) Float.MAX_VALUE;
			break;
		case Opcodes.L2I:
			l = ((Long) o).longValue();
			if (l > Integer.MAX_VALUE)
				qty = (l - Integer.MAX_VALUE) / (double) Long.MAX_VALUE;
			break;
		case Opcodes.L2F:// TODO: I think float is larger than long.
			l = ((Long) o).longValue();
			if (l > Float.MAX_VALUE)
				qty = (l - Float.MAX_VALUE) / (double) Long.MAX_VALUE;
			break;
		case Opcodes.D2I:
			d = ((Double) o).doubleValue();
			if (d > Integer.MAX_VALUE) {
				qty = (d - (double) Integer.MAX_VALUE) / Double.MAX_VALUE;
			}
			break;
		case Opcodes.D2F:
			d = ((Double) o).doubleValue();
			if (d > Float.MAX_VALUE)
				qty = (d - Float.MAX_VALUE) / Double.MAX_VALUE;
			break;
		case Opcodes.D2L:
			d = ((Double) o).doubleValue();
			if (d > Long.MAX_VALUE)
				qty = (d - Long.MAX_VALUE) / Double.MAX_VALUE;
			break;
		}
		if (qty > 0) {
			Qif actualAmount = qif.get(sourceid);
			if (actualAmount != null) {
				double aQif = actualAmount.getActQty() * (1 - qty);
				actualAmount.setActQty(aQif);
			}
		}
	}

	public static void decStringQty(String o1, String o2, String sourceId) {
		if (!o1.contains(o2) && !o2.contains(o1)) {
			int d = Utility.levenshteinDistance(o1.trim(), o2.trim());
			Qif actualAmount = qif.get(sourceId);
			if (actualAmount != null) {
				int length = Math.max(o1.trim().length(), o2.trim().length());
				double aQif = actualAmount.getActQty() * (1 - ((double) d / length));
				// System.out.println("-- "+sourceId+" reduced from "+
				// actualAmount.getActQty()+" to "+aQif);
				actualAmount.setActQty(aQif);
			}
		}
	}

	public static void check(String sinkId, String... sources) {
		for (String s : sources)
			System.out.println("-- Reached sink " + sinkId + " with qty " + qif.get(s).getActQty() + " from " + s
					+ "; Original-Qty " + qif.get(s).getOriQty());
	}

	public static long getSize(Object o) {
		long size = 0;
		if (o instanceof Byte)
			size = SIZE.BYTE.getSize();
		else if (o instanceof Boolean)
			size = SIZE.BOOLEAN.getSize();
		else if (o instanceof Character)
			size = SIZE.CHARACTER.getSize();
		else if (o instanceof Short)
			size = SIZE.SHORT.getSize();
		else if (o instanceof Double)
			size = SIZE.DOUBLE.getSize();
		else if (o instanceof Float)
			size = SIZE.FLOAT.getSize();
		else if (o instanceof Integer)
			size = SIZE.INTEGER.getSize();
		else if (o instanceof Long)
			size = SIZE.LONG.getSize();
		else if (o instanceof String)
			size = ((String) o).length();
		else if (o.getClass().isArray()) {
			size = (long) getNumElements(o);
		} else
			size = UnsafeUtil.sizeOf(o);

		return size;
		// System.out.println("UsafeUtil-Size "+size+",
		// "+o.getClass().getName());
		// System.out.println("SimpleAgent-Size
		// "+SimpleAgent.getObjectSize(o)+", "+o.getClass().getName());
	}

	// Computes the number of elements in a multi-dimensional array
	public static int getNumElements(Object o) {
		int _return = 1;
		if (o.getClass().isArray()) {
			int length = Array.getLength(o);
			_return *= length;
			if (length > 0) {
				o = Array.get(o, 0);
				length = getNumElements(o);
				_return = (length > 0) ? _return *= length : _return;
			}
		}

		if (_return == 1) {
			// Compute the size of an atom array element, and multiply it with
			// the array dimension
			long arrAtomSize = getSize(o);
			if (arrAtomSize != 0)
				_return *= arrAtomSize;
		}

		return _return;
	}

}
