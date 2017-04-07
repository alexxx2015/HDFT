import java.io.IOException;
import java.io.InputStream;
import java.lang.instrument.IllegalClassFormatException;

import org.apache.commons.io.IOUtils;
import org.junit.Before;
import org.junit.Test;

import edu.tum.uc.jvm.MyUcTransformer;

public class TestDeclassification extends AbstractTest {

	@Before
	public void init() throws Exception {
		 this.startPdpServer=true;
		init("/uc-config/uc-declassify.config");
	}

	@Test
	public void testInstrumentation() {
		try {
			// Load and instrument JZip's bytecode
			Class<?> clazz = test.Declassification.class;
			String className = clazz.getName().replace(".",
					System.getProperty("file.separator"))
					+ ".class";
			InputStream is = clazz.getClassLoader().getResourceAsStream(
					className);
			byte[] raw_bytecode = IOUtils.toByteArray(is);

			MyUcTransformer u = new MyUcTransformer();
			byte[] instrumented_bytecode = u.transform(null, className, null,
					null, raw_bytecode);

			ClassLoader parent = this.getClass().getClassLoader();
			MyClassLoader mcl = new MyClassLoader(parent);

			// Reload instrumented JZip class and execute test run: zip a bunch
			// of file in the resource folder 'toBeZippedFiles'
			Class<?> reloadClass = mcl.define(clazz.getName(),
					instrumented_bytecode);
			Object obj = reloadClass.newInstance();
			test.TestIntf myTest2 = (test.TestIntf) obj;

			myTest2.runtest();
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