import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.instrument.IllegalClassFormatException;

import org.apache.commons.io.IOUtils;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import edu.tum.uc.jvm.UcTransformer;
import edu.tum.uc.jvm.utility.ConfigProperties;
import edu.tum.uc.jvm.utility.StatisticsWriter;

@Ignore
public class TestStaticJFTP extends AbstractTest {

	@Before
	public void init() throws Exception {
		startPdpServer = true;
		init("/uc-javaftp.config");
	}

	@Test
	public void testInstrumentation() {
		String root = "/home/alex/ws_ucSimplified/JavaFTP/bin";
		this.instrumentDir(root);
		String statistic = ConfigProperties
				.getProperty(ConfigProperties.PROPERTIES.STATISTICS);
		if (!"".equals(statistic)) {
			StatisticsWriter.dumpFile(statistic);
		}
	}

	private void instrumentDir(String dirName) {
		try {
			File files = new File(dirName);
			for (String f : files.list()) {
				File tmpFile = new File(dirName+"/"+f);
				if (tmpFile.isDirectory()) {
					instrumentDir(tmpFile.getAbsolutePath());
					continue;
				}
				InputStream is = new FileInputStream(tmpFile);
				byte[] raw_bytecode = new byte[(int)tmpFile.length()];
				is.read(raw_bytecode);

				UcTransformer u = new UcTransformer();
				byte[] instrumented_bytecode = u.transform(null,
						tmpFile.getAbsolutePath(), null, null, raw_bytecode);
			}
		} catch (IllegalClassFormatException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
}