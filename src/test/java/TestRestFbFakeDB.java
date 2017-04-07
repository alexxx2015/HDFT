import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.instrument.IllegalClassFormatException;

import org.apache.commons.io.IOUtils;
import org.junit.Before;
import org.junit.Test;

import edu.tum.uc.jvm.MyUcTransformer;
import restfb.test.RestFbFakeDB;

public class TestRestFbFakeDB extends AbstractTest {

	@Before
	public void init() throws Exception {
		this.startPdpServer=false;
		init("/uc-config/uc-restfbfakedb.config");
		System.out.println(System.getProperty("user.dir"));
	}

	@Test
	public void testInstrumentation() {
		try {
			//Load and instrument JZip's bytecode
			Class<?> clazz = restfb.test.RestFbFakeDB.class;
			String className = clazz.getName().replace(".",
					System.getProperty("file.separator"))
					+ ".class";			
			InputStream is = clazz.getClassLoader().getResourceAsStream(
					className);
			byte[] raw_bytecode = IOUtils.toByteArray(is);

			MyUcTransformer u = new MyUcTransformer();
			byte[] instrumented_bytecode = u.transform(null, className, null,
					null, raw_bytecode);
			
			FileOutputStream fos = new FileOutputStream("target/test-classes/restfb/test/RestFbFakeDBI.class");
			fos.write(instrumented_bytecode);
			
			ClassLoader parent = this.getClass().getClassLoader();
			MyClassLoader mcl = new MyClassLoader(parent);
			
			//Reload instrumented JZip class and execute test run: zip a bunch of file in the resource folder 'toBeZippedFiles'
			Class<?> reloadClass = mcl.define("restfb.test.RestFbFakeDB",
					instrumented_bytecode);
			Object obj = reloadClass.newInstance();
			test.TestIntf o = (test.TestIntf) obj;
			o.runtest();

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