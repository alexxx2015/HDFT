import edu.tum.uc.jvm.declassification.qif.QIFTracker;
import edu.tum.uc.jvm.declassification.qif.QIFTracker.SIZE;

public class Sizeof {

	public static void main(String[] args) throws Exception {
		long mstart = 0, mend = 0;
		final int count = 10000;

		runGC();
		mstart = usedMemory();
		char[] val = new char[count];
		for (int i = 0; i < count; i++) {
			val[i] = (char) i;// createString(i);
		}

		runGC();
		mend = usedMemory();
		System.out.println("MEM: " + mstart + ", " + mend);
		System.out.println((mend - mstart) / count);

		int[][][] arr = new int[10][10][88];
		int a = 4;
		
		int s = 2455, z = 6765;
		z += s;
		
		
//		System.out.println(QIFTracker.getNumElements(arr));
//		System.out.println(++a);
		
	}

	public static void main1(String[] args) throws Exception {
		// Warm up all classes/methods we will use
		runGC();
		usedMemory();
		// Array to keep strong references to allocated objects
		final int count = 1000;
		Object[] objects = new Object[count];

		long heap1 = 0;
		// Allocate count+1 objects, discard the first one
		for (int i = -1; i < count; ++i) {
			Object object = null;

			// Instantiate your data here and assign it to object

			// object = new Object ();
			// object = new Integer(i);
			// object = new Long (i);
			// object = new String ();
			// object = new byte [128][1]

			if (i >= 0) {
				object = createString(10);
				objects[i] = object;
			} else {
				object = null; // Discard the warm up object
				runGC();
				heap1 = usedMemory(); // Take a before heap snapshot
			}
		}
		runGC();
		long heap2 = usedMemory(); // Take an after heap snapshot:

		final int size = Math.round(((float) (heap2 - heap1)) / count);
		System.out.println("'before' heap: " + heap1 + ", 'after' heap: " + heap2);
		System.out.println(
				"heap delta: " + (heap2 - heap1) + ", {" + objects[0].getClass() + "} size = " + size + " bytes");
		for (int i = 0; i < count; ++i)
			objects[i] = null;
		objects = null;
	}

	public static String createString(final int length) {
		char[] result = new char[length];
		for (int i = 0; i < length; ++i)
			result[i] = (char) i;

		return new String(result);
	}

	private static void runGC() throws Exception {
		// It helps to call Runtime.gc()
		// using several method calls:
		for (int r = 0; r < 4; ++r)
			_runGC();
	}

	private static void _runGC() throws Exception {
		long usedMem1 = usedMemory(), usedMem2 = Long.MAX_VALUE;
		for (int i = 0; (usedMem1 < usedMem2) && (i < 500); ++i) {
			s_runtime.runFinalization();
			s_runtime.gc();
			Thread.currentThread().yield();

			usedMem2 = usedMem1;
			usedMem1 = usedMemory();
		}
	}

	private static long usedMemory() {
		return s_runtime.totalMemory() - s_runtime.freeMemory();
	}

	private static final Runtime s_runtime = Runtime.getRuntime();
} // End of class