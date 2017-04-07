package BytecodeTest;

public class BytecodeTest {

	public static void main(String[] args) {
		
		int i = 1;
		int j = 2;
		int res = i + j;
		
		String s = "Hello, World";
		String t = s;
		
		Foo f = new Foo();
		Foo g = new Foo();
		Foo h;
		f = g;
		h = g;
		
		j = f.doubleArg(res);
		f.print(t);
	}

}
