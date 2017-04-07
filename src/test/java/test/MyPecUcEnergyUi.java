package test;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileDescriptor;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.reflect.Field;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.JSONValue;


public class MyPecUcEnergyUi implements TestIntf {
	static String ip = "localhost:80";// "93.186.13.243:29003";
	static String gatewayIP = "192.168.0.225";
	// http://localhost:80/gateways/192.168.0.225/devices/00:13:A2:00:40:9C:5A:04/sensors/59555bf7-64d9-3462-aec4-4fb2a2d7ed49/values?limit=20

	static String showLastNValueDeviceSensor = "http://%s/gateways/%s/devices/%s/sensors/%s/values?limit=%d";

	static HashMap<String, String> mac2device = new HashMap<String, String>();

	public static void test() {
		long x = 3L;
		float f = 3.0F;
		double d = 4.3D;
		int i = 3;	
		LinkedList<String>s = new LinkedList<String>();
	}

	public void getData() {
		
 		String sensorUuid = "59555bf7-64d9-3462-aec4-4fb2a2d7ed49";
		String mac = "00:13:A2:00:40:9C:5A:04";
		JSONObject measure = this.showLastNValueDeviceSensor(MyPecUcEnergyUi.ip,
				MyPecUcEnergyUi.gatewayIP, mac, sensorUuid, 20);
		JSONArray sensor_values = (JSONArray) measure.get("sensor_values");
		for (int j = 0; j < sensor_values.size(); j++)
			this.writeConsumption((JSONObject) sensor_values.get(j),
					MyPecUcEnergyUi.gatewayIP, mac, sensorUuid);
	}

	public void writeConsumption(JSONObject p_value, String p_gateway,
			String p_device, String p_sensor) {
		String dirStr = p_gateway.replace(".", "_") + "/mrData/"
				+ p_device.replace(":", "_");
		File dir = new File(dirStr);
		if (!dir.isDirectory()) {
			dir.mkdirs();
		}

		String fileName = "";
		try {
			SimpleDateFormat sdf = new SimpleDateFormat(
					"EEE MMM d HH:mm:ss z yyyy", Locale.US);
			System.out.println("MyTest2: "+p_value.get("timestamp"));
			Date d = sdf.parse((String) p_value.get("timestamp"));
			fileName = dirStr + "/" + d.getTime() + ".txt";
		} catch (ParseException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		File f = new File(fileName);
			try {
				// f.createNewFile();
				FileWriter fos = new FileWriter(f);
				fos.write((String) p_value.get("value"));
				fos.close();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
	}

	public JSONObject showLastNValueDeviceSensor(String p_ip,
			String p_gatewayIP, String p_deviceMac, String p_sensorId, int p_n) {
		String url = String.format(this.showLastNValueDeviceSensor, p_ip,
				p_gatewayIP, p_deviceMac, p_sensorId, p_n);
		System.out.println("MyTest2: "+url);
		return this.getResponse(url);
	}

	private JSONObject getResponse(String p_reqUrl) {
		JSONObject _return = new JSONObject();
		try {
			URL url = new URL(p_reqUrl);
			URLConnection urlCon = url.openConnection();
			BufferedReader in = new BufferedReader(new InputStreamReader(
					urlCon.getInputStream()));
			String response = "";
			String inputLine;
			while ((inputLine = in.readLine()) != null) {
				// response.append(inputLine);
				response += inputLine;
			}
			_return = (JSONObject) JSONValue.parse(response.toString());
		} catch (MalformedURLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return _return;
	}

	@Override
	public void runtest() {
		// TODO Auto-generated method stub
		this.getData();
	}

	public static void main(String[] args) {
		try {
			File f = new File("test2.txt");
			FileWriter fw = new FileWriter(f);
			fw.write("hello world");
//			fw.close();

			
			List<Field> attrs = getAllFields(fw);
			Iterator<Field> it = attrs.iterator();
			while(it.hasNext()){
				Field field = it.next();
				//Find attribute "lock" -> FileOutputStream
				if("lock".equals(field.getName().toLowerCase())){
					field.setAccessible(true);
					Object lock = field.get(fw);
					if(lock instanceof FileOutputStream){
						List<Field> attrs2 = getAllFields(lock);
						Iterator<Field> it2 = attrs2.iterator();
						while(it2.hasNext()){
							field = it2.next();
							//Find attribute "fd" -> FileDescriptor of lock
							if("fd".equals(field.getName().toLowerCase())){
								field.setAccessible(true);
								Object fd = field.get(lock);
								if(fd instanceof FileDescriptor){
									List<Field> attrs3 = getAllFields(fd);
									Iterator<Field> it3 = attrs3.iterator();
									//Find attribue "int fd" -> contains the file descriptor id from os
									while(it3.hasNext()){
										field = it3.next();
										if("fd".equals(field.getName().toLowerCase())){
											field.setAccessible(true);
											Object fdOs = field.get(fd);
											if(fdOs instanceof Integer){
												System.out.println("FileDesc"+ fdOs);
											}
										}
									}
								}
							}
						}
					}
				}
			}
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IllegalArgumentException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IllegalAccessException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}
	
	public static List<Field> getAllFields(Object obj){
		ArrayList<Field> _return = new ArrayList<Field>();
		Class clazz = obj.getClass();
		
		while(clazz != null && clazz != Object.class){
			for(Field f : clazz.getDeclaredFields())
				_return.add(f);
			clazz = clazz.getSuperclass();
		}
		return _return;
	}
}
