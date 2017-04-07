package edu.tum.uc.jvm.pxp;

import java.lang.management.ManagementFactory;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.thrift.TException;

import com.google.gson.Gson;
import com.google.gson.JsonObject;

import de.tum.in.i22.uc.cm.datatypes.basic.EventBasic;
import de.tum.in.i22.uc.cm.datatypes.interfaces.IEvent;
import de.tum.in.i22.uc.cm.datatypes.interfaces.IResponse;
import de.tum.in.i22.uc.thrift.types.TAny2Pxp;
import de.tum.in.i22.uc.thrift.types.TEvent;
import de.tum.in.i22.uc.thrift.types.TStatus;
import edu.tum.uc.jvm.UcCommunicator;
import edu.tum.uc.jvm.utility.Utility;

public class MyJavaPxpHandler implements TAny2Pxp.Iface {

    private static UcCommunicator ucCom = UcCommunicator.getInstance();

    private void deleteSource(String source, String unit, double time) {
	Map<String, String> params = new HashMap<>();
	params.put("PEP", "Java");
	params.put("processId", Utility.getPID());
	params.put("sourceId", source);
	params.put("minTimeStamp", String.valueOf(getAgeTimeStamp(unit, time)));
	Map<String, Set<Map<String, String>>> modelSubset = ucCom.filterPipDataModel(params);
	Enforcer.deleteData(modelSubset);
	
	// send model update back to pip
	
	Map<String, String> eventParams = new HashMap<>();
	eventParams.put("PEP", "Java");
	eventParams.put("processId", Utility.getPID());
	eventParams.put("modelSubset", new Gson().toJson(modelSubset));
	IEvent event = new EventBasic("AfterEnforcement", eventParams, true);
	ucCom.sendEvent2Pdp(event, true);
    }

    private long getAgeTimeStamp(String unit, double time) {
	if (unit.equals("sec")) {
	    return (long)(System.currentTimeMillis() - 1000 * time);
	} else {
	    throw new IllegalArgumentException("Invalid time unit identifier: " + unit);
	}
    }

    @Override
    public void executeAsync(List<TEvent> eventList) throws TException {
	Iterator<TEvent> teventIt = eventList.iterator();
	while (teventIt.hasNext()) {
	    TEvent tevent = teventIt.next();
	    if (tevent.getName().toLowerCase().equals("deletesource")) {
		Map<String, String> param = tevent.getParameters();
		this.deleteSource(param.get("SOURCE"), param.get("UNIT"), Double.parseDouble(param.get("OLDERTHAN")));
	    }
	}
    }

    @Override
    public TStatus executeSync(List<TEvent> eventList) throws TException {
	// TODO Auto-generated method stub
	return null;
    }

}
