package com.appedo.agent.manager;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Map;

import javax.management.MBeanServerConnection;
import javax.management.ObjectName;
import javax.management.openmbean.CompositeData;
import javax.management.remote.JMXConnector;
import javax.management.remote.JMXConnectorFactory;
import javax.management.remote.JMXServiceURL;

import net.sf.json.JSONArray;
import net.sf.json.JSONObject;

import com.appedo.agent.bean.AgentCounterBean;
import com.appedo.agent.bean.SlaCounterBean;
import com.appedo.agent.utils.Constants;
import com.appedo.agent.utils.Constants.AGENT_TYPE;


/**
 * GlassFish server monitoring class. This has the functionalities to get the counter values of GlassFish server.
 * 
 */
public class GlassFishMonitorManager extends AgentManager {

	private static GlassFishMonitorManager glassFishMonitorManager = null;
	private static HashMap<String, GlassFishMonitorManager> hmApplicationInstance = new HashMap<String, GlassFishMonitorManager>();

	private JMXConnector jmxc = null;
	private String hostName = null, JmxPort = null, userName = null, password = null;

	private JMXServiceURL url = null;
	private MBeanServerConnection connMBeanServer = null;

	Long lHitsCount = null, lActiveSessions = null, lRejectedSessions = null, lExpiredSessions = null;
	Long lRequestCount = null, lErrorCount = null, lBytesSent = null;
	Long lCurrentThreadsBusy = null, lCurrentThreadCount = null, lComittedHeapMemory = null;
	Long lMaxHeapMemory = null, lUsedHeapMemory = null, lFreePhysicalMemorySize = null, lTotalPhysicalMemorySize = null;

	private ObjectName glassFishDataObject = null;
	Double dCounterValue = null;

	/**
	 * Avoid the object creation for this Class from outside.
	 */
	private GlassFishMonitorManager() {
		// default constructor
	}

	/**
	 * Avoid the object creation for this Class from outside.
	 * Param constructor which creates JMX-connection using the credentials details of server from config file
	 * 
	 * @param strSvrAlias
	 * @throws Exception
	 */
	private GlassFishMonitorManager(String strSvrAlias) throws Exception {
		JSONObject joConfig = JSONObject.fromObject(Constants.ASD_DETAILS);
		JSONObject joSvrDetails = joConfig.getJSONObject(strSvrAlias);

		this.hostName = joSvrDetails.getString("host");
		this.JmxPort = joSvrDetails.getString("jmx_port");
		this.userName = joSvrDetails.getString("admin_user_name");
		this.password = joSvrDetails.getString("admin_user_password");

		boolean isRegistered = true;
		this.url = new JMXServiceURL("service:jmx:rmi:///jndi/rmi://" + hostName + ":" + JmxPort + "/jmxrmi");

		Map<String, String[]> env = new Hashtable<String, String[]>();
		String[] credentials = new String[] { userName, password };
		env.put(JMXConnector.CREDENTIALS, credentials);
		this.jmxc = JMXConnectorFactory.connect(url, env);
		this.connMBeanServer = jmxc.getMBeanServerConnection();

		// check whether AMX MBean is registered already if not invoke the same 
		isRegistered = this.connMBeanServer.isRegistered(new ObjectName("amx:pp=/mon/server-mon[server],type=memory-mon,name=jvm/memory"));
		if(!isRegistered){
			this.connMBeanServer.invoke(new ObjectName("amx-support:type=boot-amx"), "bootAMX",  new Object[0],  new String[0]);
		}
	}

	/**
	 * Returns the only object(singleton) of this Class, with respective of AppName
	 * 
	 * @param strAppName
	 * @param strSvrAlias
	 * @return
	 * @throws Exception
	 */
	public static GlassFishMonitorManager getGlassFishMonitorManager(String strAppName, String strSvrAlias) throws Exception {
		if (hmApplicationInstance.containsKey(strSvrAlias + " - " + strAppName)) {
			glassFishMonitorManager = hmApplicationInstance.get(strSvrAlias+" - "+strAppName);
		} else {
			glassFishMonitorManager = new GlassFishMonitorManager(strSvrAlias);
			hmApplicationInstance.put(strSvrAlias+" - "+strAppName, glassFishMonitorManager);
		}

		return glassFishMonitorManager;
	}

	/**
	 * re-connects to the GlassFish's JMX Port 
	 * and initialize the objects.
	 * 
	 * @throws Exception
	 */
	private void reconnectGlassFishJMXObjects() throws Exception {
		try {
			Map<String, String[]> env = new Hashtable<String, String[]>();
			String[] credentials = new String[] { userName, password };
			env.put(JMXConnector.CREDENTIALS, credentials);
			this.jmxc = JMXConnectorFactory.connect(url, env);
			this.connMBeanServer = jmxc.getMBeanServerConnection();
		} catch (Throwable th) {
			System.out.println("Exception in reconnectObjects : " + th.getMessage());
			throw new Exception("Unable to reconnect GlassFish through JMX Port : " + JmxPort);
		}
	}

	/**
	 * Monitor the server and collect the counters
	 * 
	 * @param strGUID
	 * @param strAppName
	 */
	public void monitorGlassFishServer(String strGUID, String strAppName) {
		getCounters(strGUID, strAppName);
	}

	/**
	 * Send it to the Collector WebService
	 *
	 * @param strGUID
	 */
	public void sendGlassFishCounters(String strGUID) {
		sendCounters(strGUID);
	}

	/**
	 * Collect the counter with agent's types own logic or native methods
	 * 
	 * @param strGUID
	 * @param strAppName
	 */
	public void getCounters(String strGUID, String strAppName) {

		String strCounterId = null, strExecutionType = null;
		Integer nCounterId = null;
		String [] saQuery = null;
		boolean bIsDelta = false;

		ArrayList<String> alCommandOutput = null;

		try {
			// reset the counter collector variable in AgentManager.
			resetCounterMap(strGUID);

			// try to connect the MBean Objects, on failure reconnect to MBeanServer.
			try {
				connMBeanServer.getMBeanCount();
			} catch (IOException re) {
				reconnectGlassFishJMXObjects();
			}

			JSONArray joSelectedCounters = AgentCounterBean.getCountersBean(strGUID);

			for (int i = 0; i < joSelectedCounters.size(); i++) {
				try {
					JSONObject joSelectedCounter = joSelectedCounters.getJSONObject(i);				
					strCounterId = joSelectedCounter.getString("counter_id");
					nCounterId = Integer.parseInt(strCounterId);
					String query = joSelectedCounter.getString("query");
					bIsDelta = joSelectedCounter.getBoolean("isdelta");
					strExecutionType = joSelectedCounter.getString("executiontype");

					if ( strExecutionType.equals("cmd") ) {
						// counter value from command
						alCommandOutput = CommandLineExecutor.execute(query);
						if ( alCommandOutput.size() == 0 ) {
							throw new Exception("Metric doesn't return value");
						}
						dCounterValue = convertToDouble(alCommandOutput.get(0));
					} else {
						if(query.contains("#APP_NAME#")) {
							query = query.replace("#APP_NAME#", strAppName);
						}
						saQuery = query.split("#@#");

						glassFishDataObject = new ObjectName(saQuery[0].toString());	            	
						Object objVMInfo = connMBeanServer.getAttribute(glassFishDataObject, saQuery[1]);
						CompositeData compData = (CompositeData) objVMInfo;
						dCounterValue = convertToDouble(compData.get(saQuery[2]));
					}
					if (bIsDelta) {
						dCounterValue = addDeltaCounterValue(Integer.parseInt(strCounterId), dCounterValue);					
					} else {
						addCounterValue( Integer.parseInt(strCounterId), dCounterValue );
					}
					// TODO: Static Counter correction required

	            	// Verify SLA Breach
					// JSONObject joSLACounter = null;
					ArrayList<JSONObject> joSLACounter = null; // Need to change variable name as alSLACounters
					joSLACounter = verifySLABreach(strGUID, SlaCounterBean.getSLACountersBean(strGUID), Integer.parseInt(strCounterId), dCounterValue);

					// if breached then add it to Collector's collection
					if (joSLACounter != null) {
						addSlaCounterValue(joSLACounter);
					}
				} catch (Throwable th) {
					if( th.getMessage() != null && th.getMessage().startsWith("Connection refused to host :") ) {
						System.out.println("Exception in monitorGlassFishServer.counter-loop : "+th.getMessage());
						throw th;
					} else {
						System.out.println("Exception in monitorGlassFishServer.counter-loop : "+th.getMessage());
						th.printStackTrace();
						reportCounterError(nCounterId, th.getMessage());
					}
				}
			}
		} catch (Throwable th) {
			System.out.println("Exception in monitorGlassFishServer : "+th.getMessage());
			th.printStackTrace();
			reportGlobalError(th.getMessage());
		} finally {
			// queue the counter
			try {
				queueCounterValues();
			} catch (Exception e) {
				System.out.println("Exception in queueCounterValues() : "+e.getMessage());
				e.printStackTrace();
			}
		}
	}

	/**
	 * Send the collected counter-sets to Collector WebService, by calling parent's sendCounter method
	 * @param strGUID
	 */
	public void sendCounters(String strGUID) {
		// send the collected counters to Collector WebService through parent sender function
		sendCounterToCollector(strGUID, AGENT_TYPE.GLASSFISH);
		sendSlaCounterToCollector(strGUID, AGENT_TYPE.GLASSFISH);
	}

	public Long convertToLong(Object obj) {
		Long lReturn = 0l;
		if (obj instanceof Long) {
			lReturn = ((Long) obj).longValue();
		} else if (obj instanceof Integer) {
			lReturn = (new Integer((Integer) obj)).longValue();
		}

		return lReturn;
	}

	public Double convertToDouble(Object obj) {
		Double dReturn = 0D;

		if (obj instanceof Long) {
			dReturn = ((Long) obj).doubleValue();
		} else if (obj instanceof Integer) {
			dReturn = ((Integer) obj).doubleValue();
		} else if (obj instanceof Double) {
			dReturn = ((Double) obj).doubleValue();
		} else if (obj instanceof String) {
			dReturn = Double.parseDouble(obj.toString());
		}

		return dReturn;
	}

	@Override
	protected void finalize() throws Throwable {
		clearCounterMap();

		if (jmxc != null) {
			jmxc.close();
			jmxc = null;
		}

		super.finalize();
	}
}
