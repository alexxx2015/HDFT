package restfb.test;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Map;

import test.TestIntf;

public class RestFbFakeDB implements TestIntf{
	
	public static void main(String[] args) throws IOException {
		Map<String,Object> fakedb = new HashMap<String,Object>();
		
		FileInputStream fis = new FileInputStream(new File("target/test-classes/source.txt"));
		BufferedReader br = new BufferedReader(new InputStreamReader(fis));
		String key=br.readLine();		
		fakedb.put(key,new Object());
		
		Object o = fakedb.get(key);
		
		System.out.println(o);
	}

	@Override
	public void runtest() {
		// TODO Auto-generated method stub
		try{
		RestFbFakeDB.main(new String[0]);
		} catch(Exception e){
			
		}
	}
	
//	public void doNothing(){
//		
//		DefaultFacebookClient userClient = new DefaultFacebookClient("access_token", "MY_APP_SECRET", Version.VERSION_2_0);		
//		User loggedInUser = userClient.fetchObject("me", User.class, Parameter.with("fields", "id, name, first_name, last_name, gender, age_range, link, birthday"));
//		String userFBId = loggedInUser.getId();
//		
//		fakedb.put(userFBId, loggedInUser);
//		
//		User u = fakedb.get(userFBId);
//		
//		System.out.println(u.getName()+", "+u.getBirthday());
//	}
}
