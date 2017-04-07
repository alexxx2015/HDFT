package test;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.net.URL;

public class Declassification implements TestIntf {

	public static void main(String[] args) throws IOException {
		doSth();
	}

	public static void doSth() throws IOException {
		String secret;
		URL src = Declassification.class.getResource("/testSource.txt");
		URL sink = Declassification.class.getResource("/testSink.txt");
		BufferedReader fr = new BufferedReader(new FileReader(src.getFile()));

		FileWriter fw = new FileWriter(sink.getFile());
		BufferedWriter bw = new BufferedWriter(fw);
		// FileOutputStream fos = new FileOutputStream("testSink.txt");

		secret = fr.readLine();
		secret = appendSuffix(secret);
		for (int i = 0; i < 5; i++) {
			// fos.write(secret.getBytes());
			bw.write(secret);
			bw.write(System.lineSeparator());
		}
		bw.flush();
		bw.close();
		fr.close();
	}

	private static String appendSuffix(String s) {
		return s + "_suffix";
	}

	@Override
	public void runtest() {
		// TODO Auto-generated method stub
		try {
			main(new String[] {});
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

}
