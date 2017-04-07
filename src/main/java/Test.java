import edu.tum.uc.jvm.declassification.Declassifier;

/**
 * This is just a simple test file, doing nothing
 * @author alex
 *
 */
public class Test {
	public static void main(String[] args) {
		int o = 13;
		System.out.println("Before O: "+Integer.toBinaryString(o));
		int b = (o >> 1) & 1;
		System.out.println("Afer O: "+Integer.toBinaryString(b));
	}
	public static void m(){
		int i = 54;
		Object o = new Object();
		Declassifier.declassify(o);
//		System.out.println(Object.class.getName());
	}

}
