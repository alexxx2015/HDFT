package test;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;

public class TestProgram10 implements TestIntf {

    @Override
    public void runtest() {
	try {
	    main(new String[] {});
	} catch (Exception e) {
	    e.printStackTrace();
	}
    }

    private static void moveData(byte[] inbuf, byte[] outbuf, int x, String dummyParam) throws IOException {
	x = inc(x);
	x *= x;

	DataMover dataMover = new DataMover(x);
	dataMover.inbuf = inbuf;
	dataMover.outbuf = outbuf;
	dataMover.staticx = inbuf[2];
	IDataMover dataMoverInterface = dataMover;
	dataMoverInterface.moveData();
    }

    private static int inc(int x) {
	x = x ^ x;
	x = x | x;
	x = x & x;
	x = x << 4;
	x = -x;
	return ++x;
    }

    /**
     * @param args
     * @throws IOException
     */
    public static void main(String[] args) throws IOException {
	// Scanner s = new Scanner(new BufferedReader(new
	// InputStreamReader(System.in)));
	// int x = s.nextInt();

	FileInputStream fis1 = new FileInputStream("target/test-classes/foo1.txt");
	FileInputStream fis2 = new FileInputStream("target/test-classes/foo2.txt");
	FileOutputStream fos1 = new FileOutputStream("out1.txt");
	FileOutputStream fos2 = new FileOutputStream("out2.txt");
	byte[] inbuffer1 = new byte[10];
	byte[] inbuffer2 = new byte[10];
	byte[] outbuffer1 = new byte[10];
	byte[] outbuffer2 = new byte[10];

	int x1 = fis1.read(inbuffer1);
	fis2.read(inbuffer2);

	/*
	 * short[] sa = new short[10]; sa[1] = (short)inbuffer1[3]; inbuffer2[4]
	 * = (byte)sa[1];
	 * 
	 * int i = inbuffer2[10] % 1000;
	 */
	// int x = (byte)inbuffer1[1];

	moveData(inbuffer1, outbuffer1, (byte) inbuffer1[0], ",");
	moveData(inbuffer2, outbuffer2, x1, "");
	outbuffer1[1] = inbuffer2[4];
	fos1.write(outbuffer1);
	fos2.write(outbuffer2);
	
	while(true){}
    }
}
