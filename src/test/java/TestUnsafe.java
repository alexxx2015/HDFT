import java.util.HashMap;

import org.junit.Test;

import edu.tum.uc.jvm.utility.UnsafeUtil;



public class TestUnsafe {

	@Test
	public void test() {
		String s = "bla";
		HashMap<String, String> map = new HashMap<String, String>();
		map.put("string1", s);
		
		long sAddr = UnsafeUtil.getObjectAddress(s);
		long mapAddr = UnsafeUtil.getObjectAddress(map);
		
		System.out.println("Address of s in test(): " + sAddr);
		System.out.println("Address of map in test(): " + mapAddr);
		Foo.printObjectAndAddress(s);
		Foo.printObjectAndAddress(map);
		Foo.printObjectFromAddress(sAddr);
		Foo.printObjectFromAddress(mapAddr);
	}
	
	static class Foo {
		
		static void printObjectAndAddress(Object obj) {
			System.out.println("Address of obj in doSth(): " + UnsafeUtil.getObjectAddress(obj));
			System.out.println("Object obj: " + obj);
		}
		
		static void printObjectFromAddress(long address) {
			System.out.println("Object at address " + address + ": " + UnsafeUtil.objectFromAddress(address));
		}
	}
	
}
