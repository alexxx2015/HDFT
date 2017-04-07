package test;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;

public class TestProgram12 implements TestIntf{

	private static void moveData(byte[] inbuf, byte[] outbuf, int x) throws IOException {
		x = inc(x);
		x *= x;
		moveDataHelp(inbuf, outbuf, x);
	}
	private static int inc(int x){
		return x+3;
	}

	private static void moveDataHelp(byte[] inbuf, byte[] outbuf, int x) throws IOException {		
		System.arraycopy(inbuf, 0, outbuf, 0, Math.min(inbuf.length, outbuf.length));
		outbuf[0] = (byte)x;
	}

	/**
	 * @param args
	 * @throws IOException
	 */
	public static void main(String[] args) throws IOException {
		//Scanner s = new Scanner(new BufferedReader(new InputStreamReader(System.in)));
		//int x = s.nextInt();
		
		FileInputStream fis1 = new FileInputStream("in1.txt");
		FileInputStream fis2 = new FileInputStream("in2.txt");
		FileOutputStream fos1 = new FileOutputStream("out1.txt");
		FileOutputStream fos2 = new FileOutputStream("out2.txt");
		byte[] inbuffer1 = new byte[1000];
		byte[] inbuffer2 = new byte[1000];
		byte[] outbuffer1 = new byte[1000];
		byte[] outbuffer2 = new byte[1000];
		fis1.read(inbuffer1);
		fis2.read(inbuffer2);
		
		int x = (byte)inbuffer1[1];
		
		moveData(inbuffer1, outbuffer1, x);
		moveData(inbuffer2, outbuffer2, x);
		fos1.write(outbuffer1);
		fos2.write(outbuffer2);
	}
	@Override
	public void runtest() {
		// TODO Auto-generated method stub
		try {
			main(new String[]{});
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

}
