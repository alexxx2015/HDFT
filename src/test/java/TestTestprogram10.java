import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.instrument.IllegalClassFormatException;

import org.apache.commons.io.IOUtils;
import org.junit.Before;
import org.junit.Test;

import edu.tum.uc.jvm.MyUcTransformer;
import edu.tum.uc.jvm.utility.eval.StatisticsUtil;

public class TestTestprogram10 extends AbstractTest {

	@Before
	public void init() throws Exception {
		init("/uc-testprogram10.config");
	}

	@Test
	public void testInstrumentation() {
		try {
			Class<?>[] classes = { test.TestProgram10.class, test.DataMover.class };
			Class<?> reloadMainClass = null;
			
			ClassLoader parent = this.getClass().getClassLoader();
			MyClassLoader mcl = new MyClassLoader(parent);
			
			//Load and instrument used needed classes
			for (int i = 0; i < classes.length; i++) {
				Class<?> clazz = classes[i];
				String className = clazz.getName().replace(".",
						System.getProperty("file.separator"))
						+ ".class";
				InputStream is = clazz.getClassLoader().getResourceAsStream(
						className);
				byte[] raw_bytecode = IOUtils.toByteArray(is);

				MyUcTransformer u = new MyUcTransformer();
				byte[] instrumented_bytecode = u.transform(null, className, null,
						null, raw_bytecode);
				
				FileOutputStream fos = new FileOutputStream("target/test-classes/test/" + clazz.getSimpleName() + "Instrd.class");
				fos.write(instrumented_bytecode);
				
				// Reload instrumented class
				Class<?> reloadClass = mcl.define(clazz.getName(),
						instrumented_bytecode);
				if (i == 0) { // first one is always main class
					reloadMainClass = reloadClass;
				}
			}
			
			// Instantiate main class and run test method
			Object obj = reloadMainClass.newInstance();
			test.TestIntf myTest2 = (test.TestIntf) obj;
			myTest2.runtest();
			StatisticsUtil.writeToFile();

		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (InstantiationException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IllegalAccessException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (SecurityException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IllegalClassFormatException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
}