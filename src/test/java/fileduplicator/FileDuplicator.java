package fileduplicator;

import java.io.FileInputStream;
import java.io.FileOutputStream;

import test.TestIntf;

public class FileDuplicator implements TestIntf {

	@Override
	public void runtest() {
		main(new String[] { "FileDuplicator", "target/test-classes/foo.txt", "target/test-classes/bar.txt" });
	}	

	public static void main(String[] args) {
		if (args != null && args.length == 3) {
			FileDuplicator fileDuplicator = new FileDuplicator();
			fileDuplicator.dupeFile(args[1], args[2]);
		} else {
			System.out.println("Not enough arguments");
		}
	}

	public void dupeFile(String inFileName, String outFileName) {
		try {
			FileInputStream inStream = new FileInputStream(inFileName);
			FileOutputStream outStream = new FileOutputStream(outFileName);

			byte[] buffer = new byte[1000];
			inStream.read(buffer); // source
			outStream.write(buffer); // sink

			inStream.close();
			outStream.close();
		} catch (Exception e) {
			System.out.println(e);
		}
	}

}
