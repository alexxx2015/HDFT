package edu.tum.uc.jvm.extractor;

import java.lang.reflect.Field;
import java.net.HttpURLConnection;
import java.util.HashMap;
import java.util.Map;

import com.sun.jersey.api.client.ClientResponse;

public class JerseyUrlExtractor implements IExtractor {
	
	private Map<String,String> _return; 
	
	public JerseyUrlExtractor(){
		_return = new HashMap<String,String>();
	}

	@Override
	public Map<?,?> extract(Object o) {
		_return.clear();
		// TODO Auto-generated method stub
		if (o instanceof ClientResponse) {
			try {
				Field f = o.getClass().getDeclaredField("uc");
				f.setAccessible(true);
				HttpURLConnection uri = (HttpURLConnection) f.get(o);
				_return.put("url-protocol", uri.getURL().getProtocol());
				_return.put("url-host", uri.getURL().getHost());
				_return.put("url-port", String.valueOf(uri.getURL().getPort()));
				_return.put("url-path", uri.getURL().getPath());
				_return.put("url-query", uri.getURL().getQuery());
				f.setAccessible(false);
			} catch (NoSuchFieldException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (SecurityException e) {
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
		return _return;
	}

}
