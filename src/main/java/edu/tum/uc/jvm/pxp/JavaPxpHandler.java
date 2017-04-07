package edu.tum.uc.jvm.pxp;

import java.io.File;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.thrift.TException;

import de.tum.in.i22.uc.thrift.types.TAny2Pxp;
import de.tum.in.i22.uc.thrift.types.TEvent;
import de.tum.in.i22.uc.thrift.types.TStatus;

public class JavaPxpHandler implements TAny2Pxp.Iface {
	
	public void t(long l){
		l++;
	}

	public void delmr(String s, short time) throws TException {
		t(4);
		// TODO Auto-generated method stub
		String unit = s;
		Short expire = time;
		String path = "/home/alex/apache-tomcat-8.0.0-RC5/webapps/pec-uc-energy-0.0.1-SNAPSHOT/WEB-INF/Data";
		this.delmr(s, time, new File(path));
	}
	
	private void delmr(String s, short time, File root){
		File[] files = root.listFiles();
		for(File f: files){
			if(f.isDirectory()){
				this.delmr(s, time, f);
			} else {
				if(f.getAbsolutePath().endsWith("txt")){
					String fileName = f.getName();
					String[] fileNameCmp = fileName.split("\\.");
					if(fileNameCmp.length >= 2){
						try{
							long fileTimestam = Long.parseLong(fileNameCmp[0]);
							Date now = new Date();
							if((now.getTime() - (time*1000)) > fileTimestam){
								f.delete();
								System.out.println("JPXP DEL FILE "+f.getAbsolutePath());
							}
						}catch(NumberFormatException e){
							
						}
					}
				}
			}
		}
	}
	@Override
	public void executeAsync(List<TEvent> eventList) throws TException {
		// TODO Auto-generated method stub
		Iterator<TEvent> teventIt = eventList.iterator();
		System.out.println("JAVAPXP ");
		while(teventIt.hasNext()){
			TEvent tevent = teventIt.next();
			if(tevent.getName().toLowerCase().equals("deletemr")){
				Map<String,String> param = tevent.getParameters();
				this.delmr(param.get("UNIT"), Short.parseShort(param.get("OLDERTHAN")));
			}
		}
	}

	@Override
	public TStatus executeSync(List<TEvent> eventList) throws TException {
		// TODO Auto-generated method stub
		return null;
	}
}
