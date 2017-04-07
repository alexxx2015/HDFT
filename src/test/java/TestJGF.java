import java.io.IOException;
import java.io.InputStream;
import java.lang.instrument.IllegalClassFormatException;

import org.apache.commons.io.IOUtils;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import edu.tum.uc.jvm.UcTransformer;
import edu.tum.uc.jvm.utility.StatisticsWriter;

@Ignore
public class TestJGF extends AbstractTest {

	@Before
	public void init() throws Exception {
		startPdpServer= false;
		init("/uc-jgf.config");
	}

	@Test
	public void testInstrumentation() {
		try {
			Class<?> clazz = JGF.search.JGFSearchBench.class;

			String className = clazz.getName().replace(".",
					System.getProperty("file.separator"))
					+ ".class";
			InputStream is = clazz.getClassLoader().getResourceAsStream(
					className);
			byte[] raw_bytecode = IOUtils.toByteArray(is);

			UcTransformer u = new UcTransformer();
			byte[] instrumented_bytecode = u.transform(null, className, null,
					null, raw_bytecode);

			ClassLoader parent = this.getClass().getClassLoader();
			MyClassLoader mcl = new MyClassLoader(parent);
			

			Class<?> reloadClass = mcl.define("search.JGFSearchBench",
					instrumented_bytecode);
			Object obj = reloadClass.newInstance();
			test.TestIntf myTest2 = (test.TestIntf) obj;

			for (int i = 0; i < 1; i++) {
				myTest2.runtest();
				StatisticsWriter.clear();
			}
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (SecurityException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IllegalClassFormatException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (InstantiationException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IllegalAccessException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
}