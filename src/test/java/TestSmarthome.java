import java.io.IOException;
import java.io.InputStream;
import java.lang.instrument.IllegalClassFormatException;

import org.apache.commons.io.IOUtils;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import edu.tum.uc.jvm.UcTransformer;
import edu.tum.uc.jvm.utility.ConfigProperties;
import edu.tum.uc.jvm.utility.StatisticsWriter;

@Ignore
public class TestSmarthome extends AbstractTest{

	@Before
	public void init() throws Exception{
		init("/uc-smarthome.config");
	}
	
	@Rule
    public TemporaryFolder folder= new TemporaryFolder();
	/**
	 * This test requires an established connection to a remote smart home, see /home/alex/connect.sh
	 * 
	 * @author alex
	 * 
	 */
//	@Test	
	public void testInstrumentation() {
		try {
			// Class clazz = Class.forName("UcJavaPepTest.MyTest");
			// File f = new
			// File("src/test/resources/classes/org/pec/uc/energy/PecUcEnergyUi.class");v
			// URL policy = getClass().getResource("/uc.config");
			// File policyFile = new File(policy.getFile());
			// URL url = getClass().getResource("/test/TestProgram.class");
			// File f = new File(url.getFile());
			// FileInputStream fin = new FileInputStream(f);
			// byte[] b = new byte[(int)f.length()];
			// fin.read(b);
			
			Class<?> clazz = test.MyPecUcEnergyUi.class;
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

			Class<?> myTest2Clazz = mcl
					.define("test.MyPecUcEnergyUi", instrumented_bytecode);
			Object obj = myTest2Clazz.newInstance();
			test.TestIntf myTest2 = (test.TestIntf) obj;
			myTest2.runtest();
			String statistic = ConfigProperties.getProperty(ConfigProperties.PROPERTIES.STATISTICS);
			if(statistic != null){
				StatisticsWriter.dumpFile(statistic);
			}
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