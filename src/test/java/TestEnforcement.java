import java.lang.reflect.Array;

import org.junit.Test;

import test.DataMover;
import edu.tum.uc.jvm.pxp.Enforcer;
import edu.tum.uc.jvm.utility.UnsafeUtil;

import java.lang.System;
import java.util.Arrays;

public class TestEnforcement {

    @Test
    public void test() throws ClassNotFoundException {
	int[] array1 = new int[] { 1, 2, 3 , 4};
	int[] array2 = new int[] { 5, 6, 7, 8 };
	
	DataMover dm1 = new DataMover(1);
	DataMover dm2 = new DataMover(2);
	
	String[] strArray1 = new String[] { "a", "aa", "b", "bb", "c" };
	String[] strArray2 = new String[strArray1.length];
	strArray2[2] = "c";
	
	byte[] byteArray1 = new byte[] { 100, 101, 102, 103, 104, 105, 106, 107, 108, 109, 100, 101, 102, 103, 104, 105, 106, 107, 108 };
	Object byteArray2 = Array.newInstance(byteArray1.getClass().getComponentType(), byteArray1.length);
		
	System.out.println(Arrays.toString(strArray1));
	UnsafeUtil.replaceObject(strArray1, strArray2);
	System.out.println(Arrays.toString(strArray1));
    }
    
}
