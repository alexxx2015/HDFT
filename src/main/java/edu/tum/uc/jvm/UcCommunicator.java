package edu.tum.uc.jvm;

import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.lang.reflect.InvocationTargetException;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.apache.thrift.transport.TTransportException;

import de.tum.in.i22.uc.Controller;
import de.tum.in.i22.uc.cm.datatypes.basic.EventBasic;
import de.tum.in.i22.uc.cm.datatypes.basic.PxpSpec;
import de.tum.in.i22.uc.cm.datatypes.basic.ResponseBasic;
import de.tum.in.i22.uc.cm.datatypes.basic.StatusBasic;
import de.tum.in.i22.uc.cm.datatypes.basic.StatusBasic.EStatus;
import de.tum.in.i22.uc.cm.datatypes.interfaces.IEvent;
import de.tum.in.i22.uc.cm.datatypes.interfaces.IResponse;
import de.tum.in.i22.uc.cm.distribution.IPLocation;
import de.tum.in.i22.uc.cm.distribution.client.Any2PdpClient;
import de.tum.in.i22.uc.cm.distribution.client.Any2PipClient;
import de.tum.in.i22.uc.cm.factories.IMessageFactory;
import de.tum.in.i22.uc.cm.factories.MessageFactoryCreator;
import de.tum.in.i22.uc.thrift.client.ThriftClientFactory;
import de.tum.in.i22.uc.thrift.server.IThriftServer;
import de.tum.in.i22.uc.thrift.server.ThriftServerFactory;
import edu.tum.uc.jvm.pxp.JavaPxpHandler;
import edu.tum.uc.jvm.pxp.MyJavaPxpHandler;
import edu.tum.uc.jvm.utility.ConfigProperties;
import edu.tum.uc.jvm.utility.StatisticsWriter;
import edu.tum.uc.jvm.utility.ConfigProperties.PROPERTIES;
import edu.tum.uc.jvm.utility.MethEvent;
import edu.tum.uc.jvm.utility.analysis.CreationSite;

/**
 * Communication instance to the PDP (UC-Infrastructure)
 */

public class UcCommunicator {

	private static UcCommunicator UC_COM;
	private static UcCommunicator UC_COM2;

	private final String PDP_HOST;
	private final String PDP_PORT;

	private final String PIP_HOST;
	private final String PIP_PORT;

	private final String MYPEP_HOST;
	private final String MYPEP_PORT;

	// PDP client communicating via thrift
	private Any2PdpClient pdpClient;

	private Any2PipClient pipClient;

	// PDP client for local function calls
	private Controller pdpController;

	private boolean async;
	private boolean netcom;

	private UcCommunicator() {
		this.PDP_HOST = ConfigProperties.getProperty(ConfigProperties.PROPERTIES.PDP_HOST);
		this.PDP_PORT = ConfigProperties.getProperty(ConfigProperties.PROPERTIES.PDP_PORT);
		this.PIP_HOST = ConfigProperties.getProperty(ConfigProperties.PROPERTIES.PIP_HOST);
		this.PIP_PORT = ConfigProperties.getProperty(ConfigProperties.PROPERTIES.PIP_PORT);
		this.MYPEP_HOST = ConfigProperties.getProperty(ConfigProperties.PROPERTIES.MYPEP_HOST);
		this.MYPEP_PORT = ConfigProperties.getProperty(ConfigProperties.PROPERTIES.MYPEP_PORT);

		this.async = false;
		boolean pdpAsync = new Boolean(ConfigProperties.getProperty(ConfigProperties.PROPERTIES.PDP_ASYNCOM));
		if (pdpAsync == true)
			this.async = true;

		this.netcom = false;
		boolean configNetcom = new Boolean(ConfigProperties.getProperty(ConfigProperties.PROPERTIES.NETCOM));
		if (configNetcom == true) {
			this.netcom = true;
		}
	}

	public static UcCommunicator getInstance() {
		if (UcCommunicator.UC_COM == null) {
			UcCommunicator.UC_COM = new UcCommunicator();
			// UcCommunicator.UC_COM.initPDP();
		}
		return UcCommunicator.UC_COM;
	}

	public static UcCommunicator get2ndInstance() {
		if (UcCommunicator.UC_COM2 == null) {
			UcCommunicator.UC_COM2 = new UcCommunicator();
			// UcCommunicator.UC_COM.initPDP();
		}
		return UcCommunicator.UC_COM2;
	}

	public void initPDP() {
		if (this.pdpClient != null || this.pdpController != null)
			return;
		boolean netcom = new Boolean(ConfigProperties.getProperty(ConfigProperties.PROPERTIES.NETCOM));
		if (netcom) {
			try {
				if (this.pdpClient == null) {
					ThriftClientFactory thriftClientFactory = new ThriftClientFactory();
					this.pdpClient = thriftClientFactory
							.createAny2PdpClient(new IPLocation(this.PDP_HOST, Integer.parseInt(this.PDP_PORT)));
					this.pdpClient.connect();
					this.pipClient = thriftClientFactory
							.createAny2PipClient(new IPLocation(this.PIP_HOST, Integer.parseInt(this.PIP_PORT)));
					this.pipClient.connect();
					regPxp();
				}
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		} else {
			this.netcom = false;
			if (this.pdpController == null) {
				String ucProperties = ConfigProperties.getProperty(PROPERTIES.UC_PROPERTIES);
				String[] ucProp = new String[0];
				if (!"".equals(ucProperties.trim())) {
					ucProp = new String[] { "-pp", ucProperties };
				}
				this.pdpController = new Controller(ucProp);
				this.pdpController.start();
				if (this.pdpController.isStarted())
					System.out.println("PDP running");
				else
					System.out.println("PDP not running");
				while (!this.pdpController.isStarted()) {
					try {
						System.out.println("Waiting for PDP");
						Thread.sleep(50);
					} catch (InterruptedException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				}

				String uc4win_autostart = ConfigProperties.getProperty(ConfigProperties.PROPERTIES.UC4WIN_AUTOSTART);
				if (!"".equals(uc4win_autostart) && !"".equals(uc4win_autostart.trim())) {
					uc4win_autostart = uc4win_autostart.trim();
					String[] start = new String[] { "cmd.exe", "/c", "sc", "start", uc4win_autostart };
					try {
						Runtime.getRuntime().exec(start);
						Thread.sleep(2000);
					} catch (InterruptedException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					} catch (IOException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				}
			}
		}
	}

	public boolean sendInitPdpEvent(IEvent event) {
		// this.initPDP();
		IResponse response = sendEvent(event);
		if (response != null)
			return (response.getAuthorizationAction().isStatus(EStatus.ALLOW));
		return false;
	}

	private IResponse sendEvent(IEvent event) {
		return sendEvent(event, false);
	}

	public IResponse sendEvent(IEvent event, boolean forceAsync) {
		UcCommunicator.getInstance().initPDP();
		Object o = this.pdpClient != null ? this.pdpClient : this.pdpController;
		synchronized (o) {
			try {
				// Synchronous mode
				if (!this.async && !forceAsync) {
					if (this.netcom)
						return this.pdpClient.notifyEventSync(event);
					else
						return this.pdpController.notifyEventSync(event);
				}
				// Asynchronous mode
				if (this.netcom)
					this.pdpClient.notifyEventAsync(event);
				else
					this.pdpController.notifyEventAsync(event);
			} catch (RuntimeException ttex) {
				this.pdpClient = null;
				this.pdpController = null;
				this.initPDP();
				// Synchronous mode
				if (!this.async && !forceAsync) {
					if (this.netcom)
						return this.pdpClient.notifyEventSync(event);
					else
						return this.pdpController.notifyEventSync(event);
				}
				// Asynchronous mode
				if (this.netcom)
					this.pdpClient.notifyEventAsync(event);
				else
					this.pdpController.notifyEventAsync(event);
			}
			return new ResponseBasic(new StatusBasic(EStatus.ALLOW), null, null);
		}
	}

	public boolean sendEvent2Pdp(IEvent event, String p_methName) {
		event.getParameters().put("ThreadId", String.valueOf(Thread.currentThread().getId()));

		IResponse response;
		Boolean timer5 = new Boolean(ConfigProperties.getProperty(ConfigProperties.PROPERTIES.TIMER_T5));
		if (timer5) {
			long start = System.nanoTime();
			response = this.sendEvent(event);
			StatisticsWriter.logExecutionTimerT5(p_methName, System.nanoTime() - start);

			if (response != null) {
				return (response.getAuthorizationAction().isStatus(EStatus.ALLOW));
			}
		} else {
			response = this.sendEvent(event);
			if (response != null)
				return (response.getAuthorizationAction().isStatus(EStatus.ALLOW));
		}
		return false;
	}

	public boolean sendEvent2Pdp(IEvent event) {
		return sendEvent2Pdp(event, false);
	}

	public boolean sendEvent2Pdp(IEvent event, boolean async) {
		IResponse response;
		response = this.sendEvent(event, async);
		if (response != null)
			return (response.getAuthorizationAction().isStatus(EStatus.ALLOW));
		return false;
	}

	public Map<String, Set<Map<String, String>>> filterPipDataModel(Map<String, String> params) {
		return this.pipClient.filterModel(params);
	}

	public boolean sendEvent2Pdp(MethEvent event, String p_methName) {
		// this.initPDP();
		Map<String, String> map = new HashMap<String, String>();
		map.put("location", event.getMethodInvoker() + event.getMethodInvokerSig() + ":" + event.getOffset());
		map.put("signature", event.getMethodInvokee() + event.getMethodInvokeeSig());
		map.put("delimiter", event.getType().toString());
		map.put("PEP", "Java");
		map.put("ThreadId", String.valueOf(Thread.currentThread().getId()));
		map.put("fileDescriptor", event.getFileDescriptor());
		map.put("context", event.getContextIds());
		String runningVm = ManagementFactory.getRuntimeMXBean().getName();
		String[] runningVmComp = runningVm.split("@");
		if (runningVmComp.length > 0) {
			map.put("PID", runningVmComp[0]);// Add process id
		}

		String sinkSource = "Sink";
		if (event.getSinkSource().toLowerCase().startsWith("source")) {
			sinkSource = "Source";
		}
		map.put("id", event.getSinkSource().toLowerCase());

		// IEvent ievent = new
		// EventBasic(event.getMethodInvokee(),map,event.isActual());
		IEvent ievent = new EventBasic(sinkSource, map, event.isActual());
		IResponse response;
		Boolean netcom = new Boolean(ConfigProperties.getProperty(PROPERTIES.NETCOM));
		Boolean timer5 = new Boolean(ConfigProperties.getProperty(ConfigProperties.PROPERTIES.TIMER_T5));
		if (timer5) {
			long start = System.nanoTime();
			if (netcom) {
				response = this.pdpClient.notifyEventSync(ievent);
				if (response != null) {
					StatisticsWriter.logExecutionTimerT5(p_methName, System.nanoTime() - start);
					return (response.getAuthorizationAction().isStatus(EStatus.ALLOW));
				}
			} else {
				this.pdpController.notifyEventAsync(ievent);
				StatisticsWriter.logExecutionTimerT5(p_methName, System.nanoTime() - start);
				return true;
			}
		} else {
			if (netcom) {
				response = this.pdpClient.notifyEventSync(ievent);

				if (response != null)
					return (response.getAuthorizationAction().isStatus(EStatus.ALLOW));
			} else {
				this.pdpController.notifyEventAsync(ievent);
				return true;
			}
		}
		return false;
	}

	public boolean sendContextEvent2PDP(Object obj, CreationSite cs) {
		Map<String, String> param = new HashMap<String, String>();
		param.put("objectId", String.valueOf(obj.hashCode()));
		param.put("context", cs.getId());
		param.put("contextLocation", cs.getLocation());
		param.put("contextOffset", String.valueOf(cs.getOffset()));
		param.put("PEP", "Java");
		param.put("ThreadId", String.valueOf(Thread.currentThread().getId()));
		String runningVm = ManagementFactory.getRuntimeMXBean().getName();
		String[] runningVmComp = runningVm.split("@");
		if (runningVmComp.length > 0) {
			param.put("PID", runningVmComp[0]);// Add process id
		}

		IMessageFactory _messageFactory = MessageFactoryCreator.createMessageFactory();

		IEvent event = _messageFactory.createActualEvent("Context", param);
		boolean netcom = new Boolean(ConfigProperties.getProperty(PROPERTIES.NETCOM));
		IResponse response;
		if (netcom == true) {
			response = this.pdpClient.notifyEventSync(event);
			if (response != null)
				return response.getAuthorizationAction().isStatus(EStatus.ALLOW);
		} else {
			this.pdpController.notifyEventAsync(event);
			return true;
		}
		return false;
	}

	public boolean sendKillProcessEvent2Pdp() {
		Map<String, String> param = new HashMap<String, String>();
		param.put("PEP", "Windows");
		String runningVm = ManagementFactory.getRuntimeMXBean().getName();
		String[] runningVmComp = runningVm.split("@");
		if (runningVmComp.length > 0) {
			param.put("PID_Child", runningVmComp[0]);// Add process id
		}
		IMessageFactory _messageFactory = MessageFactoryCreator.createMessageFactory();
		IEvent event = _messageFactory.createActualEvent("KillProcess", param);
		IResponse response = sendEvent(event);
		if (!netcom && this.pdpController != null)
			this.pdpController.stop();

		String uc4win_autostart = ConfigProperties.getProperty(ConfigProperties.PROPERTIES.UC4WIN_AUTOSTART);
		if (!"".equals(uc4win_autostart) && !"".equals(uc4win_autostart.trim())) {
			uc4win_autostart = uc4win_autostart.trim();
			String[] start = new String[] { "cmd.exe", "/c", "sc", "stop", uc4win_autostart };
			try {
				Runtime.getRuntime().exec(start);
				Thread.sleep(1500);
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}

		System.exit(0);
		return false;
	}

	public void regPxp() {
		if (!"".equals(MYPEP_HOST) && !"".equals(MYPEP_PORT)) {
			// initPDP();
			PxpSpec pxpSpec = new PxpSpec(MYPEP_HOST, Integer.parseInt(MYPEP_PORT), "JAVAPXP",
					"This is a simple Java PXP");
			boolean b;
			if (netcom) {
				b = this.pdpClient.registerPxp(pxpSpec);
			} else {
				b = this.pdpController.registerPxp(pxpSpec);
			}
			if (b == true) {
				this.startPxpServer(pxpSpec);
			}
		}
	}

	private void startPxpServer(final PxpSpec pxpSpec) {
		IThriftServer pxpHandler = ThriftServerFactory.createPxpThriftServer(pxpSpec.getPort(), new MyJavaPxpHandler());

		// final JavaPxp.Processor pxpProcessor = new
		// JavaPxp.Processor(pxpHandler);
		// Runnable pxpRunner = new Runnable(){
		// public void run(){
		// try {
		// TServerTransport serverTransport = new
		// TServerSocket(pxpSpec.getPort());
		// TServer server = new TSimpleServer(new
		// Args(serverTransport).processor(pxpProcessor));
		// System.out.println("Running UcCommunicator PXP server started");
		// server.serve();
		// } catch (TTransportException e) {
		// // TODO Auto-generated catch block
		// e.printStackTrace();
		// }
		// }
		// };
		new Thread(pxpHandler).start();
	}
}