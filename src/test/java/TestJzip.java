import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.instrument.IllegalClassFormatException;
import java.net.URL;

import org.apache.commons.io.IOUtils;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.junit.Before;
import org.junit.Test;

import edu.tum.uc.jvm.MyUcTransformer;
import edu.tum.uc.jvm.instrum.opt.InstrumDelegateOpt;
import edu.tum.uc.jvm.utility.StatisticsWriter;

public class TestJzip extends AbstractTest {
	
	private boolean runCode = true;

	@Before
	public void init() throws Exception {
//		this.startPdpServer=true;
//		init("/uc-config/uc-myjzip.config");
//		init("/uc-config/uc-jzip.config");
		init("/uc-config/uc.config");
//		runCode = false;
	}

	@Test
	public void testInstrumentation() {
		try {
			Logger.getRootLogger().setLevel(Level.OFF);
			//Load and instrument JZip's bytecode
			Class<?> clazz = jzip.JZip.class;
			String className = clazz.getName().replace(".",
					System.getProperty("file.separator"))
					+ ".class";			
			InputStream is = clazz.getClassLoader().getResourceAsStream(
					className);
			byte[] raw_bytecode = IOUtils.toByteArray(is);

			MyUcTransformer u = new MyUcTransformer();
			byte[] instrumented_bytecode = u.transform(null, className, null,
					null, raw_bytecode);
			
//			do not run the instrumented version
			if(!runCode) return;
			
			InstrumDelegateOpt.populateMyEventBasic();
			
			ClassLoader parent = this.getClass().getClassLoader();
			MyClassLoader mcl = new MyClassLoader(parent);
			
			//Reload instrumented JZip class and execute test run: zip a bunch of file in the resource folder 'toBeZippedFiles'
			Class<?> reloadClass = mcl.define("jzip.JZip", instrumented_bytecode);
			Object obj = reloadClass.newInstance();
			test.TestIntf myTest2 = (test.TestIntf) obj;
						
			for (int i = 0; i < 1; i++) {
				String instruction = "";
				URL url = null;//this.getClass().getResource("/toBeZippedFilesSmall");
				if (url != null) {
					File f = new File(url.getFile());
					instruction = "zip " + f.getParent() + "/toBeZipped.zip "+ f.getAbsolutePath() + "/";
					// instruction = "zip /home/alex/toBeZipped.zip "// + f.getAbsolutePath();
					// File f = new File("/home/alex/xlayerpip.zip");
				}
				else{
					File f = new File("/Users/cataldocalo/git/pdp/Gui/UcWebManager/src/main/webapp/apps/d41d8cd98f00b204e9800998ecf8427e/instrumentations/20161114111246/tozip");
					instruction = "zip " + f.getParent() + "/toBeZipped.zip "+ f.getAbsolutePath() + "/";
				}

				//instruction += "\n exit";
				InputStream is2 = System.in;
				System.setIn(new ByteArrayInputStream(instruction.getBytes()));
				myTest2.runtest();
				System.setIn(is2);
				StatisticsWriter.clear();
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