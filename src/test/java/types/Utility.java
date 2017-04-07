package types;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.net.URL;

public class Utility {
	public static StringBuilder readFile(String filename) throws IOException{
		StringBuilder _return = new StringBuilder();
		File file = searchFile(filename);
		if(file != null){
			BufferedReader br = new BufferedReader(new FileReader(file));
			String line;
			while((line = br.readLine()) != null){
				_return.append(line);
			}
		}
		return _return;		
	}
	
	public static File searchFile(String filename){
		File _return = new File(filename);
		if(!_return.exists()){
			URL _returnUrl = Utility.class.getClassLoader().getResource(_return.getName());
			if(_returnUrl == null){
				_return = null;
			} else {
				_return = new File(_returnUrl.getFile());
			}
		}
		return _return;
	}
}
