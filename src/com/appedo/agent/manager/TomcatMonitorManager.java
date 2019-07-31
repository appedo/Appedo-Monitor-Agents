package com.appedo.agent.manager;

import java.io.FileWriter;
import java.lang.management.MemoryMXBean;
import java.lang.management.MemoryUsage;
import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.Set;

import javax.management.InstanceNotFoundException;
import javax.management.MBeanAttributeInfo;
import javax.management.MBeanInfo;
import javax.management.MBeanServerConnection;
import javax.management.ObjectName;
import javax.management.ReflectionException;
import javax.management.openmbean.CompositeData;
import javax.management.remote.JMXConnector;
import javax.management.remote.JMXConnectorFactory;
import javax.management.remote.JMXServiceURL;

import com.appedo.agent.bean.AgentCounterBean;
import com.appedo.agent.bean.LinuxUnificationBean;
import com.appedo.agent.bean.LinuxUnificationCounterBean;
import com.appedo.agent.bean.SlaCounterBean;
import com.appedo.agent.utils.Constants;
import com.appedo.agent.utils.UtilsFactory;
import com.appedo.agent.utils.Constants.AGENT_TYPE;

import net.sf.json.JSONArray;
import net.sf.json.JSONObject;


/**
 * Tomcat server monitoring class. This has the functionalities to get the counter values of Tomcat server.
 * 
 * @author Ramkumar R
 *
 */
public class TomcatMonitorManager extends AgentManager {

	private static TomcatMonitorManager tomcatMonitorManager = null;
	private static HashMap<String, TomcatMonitorManager> hmApplicationInstance = new HashMap<String, TomcatMonitorManager>();

	private String strHTTPName = null;
	private String hostName = null, strPort = null, JmxPort = null;

	private ObjectName onHTTPName = null;
	private JMXServiceURL url = null;
	private JMXConnector jmxc = null;
	private MBeanServerConnection connMBeanServer = null;

	Long lHitsCount = null, lActiveSessions = null, lRejectedSessions = null, lExpiredSessions = null;
	Long lRequestCount = null, lErrorCount = null, lBytesSent = null;
	Long lCurrentThreadsBusy = null, lCurrentThreadCount = null, lComittedHeapMemory = null;
	Long lMaxHeapMemory = null, lUsedHeapMemory = null, lFreePhysicalMemorySize = null, lTotalPhysicalMemorySize = null;

	private ObjectName obj = null;
	Double dCounterValue = null;

	MemoryMXBean memoryMXBean = null;
	MemoryUsage memNonHeap = null;
	MemoryUsage memHeap = null;
	private ObjectName onServer;
	private String strServerInfo;

	/**
	 *  Avoid the object creation for this Class from outside.
	 */
	public TomcatMonitorManager(){
		// Default constructor
	}

	/**
	 * Avoid the object creation for this Class from outside.
	 * 
	 * @param strSvrAlias
	 * @throws Exception
	 */
	private TomcatMonitorManager(String strSvrAlias) throws Exception {
		JSONObject joConfig = JSONObject.fromObject(Constants.ASD_DETAILS);
		JSONObject joSvrDetails = joConfig.getJSONObject(strSvrAlias);

		this.hostName = joSvrDetails.getString("host");
		this.strPort = joSvrDetails.getString("app_port");
		this.JmxPort = joSvrDetails.getString("jmx_port");
		
		//String strServiceURL  = joSvrDetails.getString("service_url");
		
		// Note: for NTFS, give access rights to "C:\Users\ram\AppData\Local\Temp\hsperfdata_<userName>"
		//this.url = new JMXServiceURL(strServiceURL);
		
		this.url = new JMXServiceURL("service:jmx:rmi:///jndi/rmi://" + this.hostName + ":" + this.JmxPort + "/jmxrmi");
		
		this.jmxc = JMXConnectorFactory.connect(url);

		this.connMBeanServer = jmxc.getMBeanServerConnection(); 

		// get Tomcat Version
		/*this.onServer = new ObjectName("Catalina:type=Server");
		this.strServerInfo = connMBeanServer.getAttribute(onServer, "serverInfo").toString();
		System.out.println("isRegistered(onServer): "+connMBeanServer.isRegistered(onServer));*/
		
		/*this.onServer = new ObjectName("Catalina:type=ProtocolHandler,port=" + strPort);
		strHTTPName = connMBeanServer.getAttribute(onHTTPName,"name").toString();
		
		System.out.println(strHTTPName);*/
		
		FileWriter fw = new FileWriter("F:\\mnt\\Tomcat_Attributes\\TomcatAttributes.txt");
		
		Set<ObjectName> mbeans = connMBeanServer.queryNames(null, null);
		for (Object mbean : mbeans)
		{
		    WriteAttributes(connMBeanServer, (ObjectName)mbean, fw);
		}
		fw.close();
		
	}

	private void WriteAttributes(final MBeanServerConnection mBeanServer, final ObjectName http, FileWriter fw)
	        throws InstanceNotFoundException, ReflectionException
	{
		try {
		    MBeanInfo info = mBeanServer.getMBeanInfo(http);
		    MBeanAttributeInfo[] attrInfo = info.getAttributes();

		    System.out.println("Attributes for object: " + http+ ":\n");
		    fw.write("Attributes for object: " + http+ ":\n");
		    for (MBeanAttributeInfo attr : attrInfo)
		    {
		    	if(attr.getType().contains("int") || attr.getType().contains("long")) {
		    		System.out.println(" Type:  " + attr.getType() + "\n");
			        System.out.println(" Name:  " + attr.getName() + "\n");
			        System.out.println(" Desc:  " + attr.getDescription() + "\n");
			        System.out.println(" IsReadable:  " + attr.isReadable() + "\n");
			        fw.write(" Type:  "+attr.getType()+ ":\n");
			        fw.write(" Name:  " + attr.getName()+ ":\n");
			        fw.write(" Desc:  " + attr.getDescription()+ ":\n");
			        fw.write(" IsReadable:  " + attr.isReadable()+ ":\n");
			        fw.write(" Attribute value:  " + convertToDouble(connMBeanServer.getAttribute(http, attr.getName()))+ ":\n");
			        //convertToDouble(connMBeanServer.getAttribute(obj, saQuery[1]));
			        
			        //System.out.println("Attribute value : " +convertToDouble(connMBeanServer.getAttribute(http, attr.getName())));
		    	}
		    }
		    System.out.println("==========================================================");
		    fw.write("=========================================================\n");
		}catch (Exception e) {
			// TODO: handle exception
			System.out.println(e);
		}
	}
	
	public static void getTomcatAttribute(String strSvrAlias) throws Exception {
		new TomcatMonitorManager(strSvrAlias);
	}
	
	/**
	 * Connect the Tomcat's JMX Port 
	 * and initialize the objects.
	 * 
	 * @throws Exception
	 */
	private void reconnectTomcatJMXObjects() throws Exception {
		try {
			this.jmxc = JMXConnectorFactory.connect(url);

			this.connMBeanServer = jmxc.getMBeanServerConnection(); 
		} catch(Throwable th) {
			System.out.println("Exception in reconnectObjects: "+th.getMessage());
			throw new Exception("Unable to reconnect Tomcat through JMX Port "+ JmxPort);
		}
	}

	/**
	 * Returns the only object(singleton) of this Class, with respective of AppName.
	 * 
	 * @param strApplicationName
	 * @param strSvrAlias
	 * @return
	 * @throws Exception
	 */
	public static TomcatMonitorManager getTomcatMonitorManager(String strApplicationName, String strSvrAlias) throws Exception {
		if (hmApplicationInstance.containsKey(strApplicationName + " - " + strSvrAlias)) {
			tomcatMonitorManager = hmApplicationInstance.get(strApplicationName + " - " + strSvrAlias);
		} else {
			tomcatMonitorManager = new TomcatMonitorManager(strSvrAlias);
			hmApplicationInstance.put(strApplicationName + " - " + strSvrAlias, tomcatMonitorManager);
		}

		return tomcatMonitorManager;
	}

	public static TomcatMonitorManager getTomcatMonitorManager() throws Exception{
		return new TomcatMonitorManager();
	}
	
	/**
	 * Monitor the server and collect the counters
	 * 
	 * @param strGUID
	 * @param strApp
	 * @param strConnectorPortAddress
	 */
	public void monitorTomcatServer(String strGUID, String strApp, String strConnectorPortAddress){
		getCounters(strGUID, strApp, strConnectorPortAddress);
	}

	/**
	 * Send it to the Collector WebService
	 * 
	 * @param strGUID
	 */
	public void sendTomcatCounters(String strGUID){
		sendCounters(strGUID);
	}

	/**
	 * Collect the counter with agent's types own logic or native methods
	 * 
	 * @param strGUID
	 * @param strApp
	 * @param strConnectorPortAddress
	 */
	public void getCounters(String strGUID, String strApp, String strConnectorPortAddress){
		String strCounterId = null, strExecutionType = null, query = null, strCommandOutput = null;
		Integer nCounterId = null;
		String [] saQuery = null;
		boolean bIsDelta = false;
		
		JSONArray joSelectedCounters = null;
		JSONObject joSelectedCounter = null;
		
		CommandLineExecutor cmdExecutor = null;
		
		try {
			try {
				// reset the counter collector variable in AgentManager.
				resetCounterMap(strGUID);

				// try to connect the MBean Objects
				// on failure reconnect it.
				try {
					connMBeanServer.getDefaultDomain();
				} catch (RemoteException re) {
					reconnectTomcatJMXObjects();
				}
				
				// get HTTP name
				if( strConnectorPortAddress != null && strConnectorPortAddress.length() > 0 ) {
					onHTTPName = new ObjectName("Catalina:type=ProtocolHandler,port=" + strPort + ",address=\"" + strConnectorPortAddress + "\"");
				} else {
					onHTTPName = new ObjectName("Catalina:type=ProtocolHandler,port=" + strPort);
				}
				strHTTPName = connMBeanServer.getAttribute(onHTTPName,"name").toString();
				
			} catch(Throwable th) {
				System.out.println("Exception in monitorTomcatServer.initialize: "+th.getMessage());
				th.printStackTrace();
				reportGlobalError(th.getMessage());
			}
			
			joSelectedCounters = AgentCounterBean.getCountersBean(strGUID);
			
			for (int i = 0; i < joSelectedCounters.size(); i++) {
				try {
					joSelectedCounter = joSelectedCounters.getJSONObject(i);
					strCounterId = joSelectedCounter.getString("counter_id");
					nCounterId = Integer.parseInt(strCounterId);
					query = joSelectedCounter.getString("query");
					bIsDelta = joSelectedCounter.getBoolean("isdelta");
					strExecutionType = joSelectedCounter.getString("executiontype");
					
					if ( strExecutionType.equals("cmd") ) {
						// Get counter-value after executing the command
						cmdExecutor = new CommandLineExecutor();
						
						cmdExecutor.executeCommand(query);
						
						strCommandOutput = cmdExecutor.getOutput().toString();
						
						if ( strCommandOutput.length() == 0 ) {
							
							throw new Exception("Metric doesn't return value: "+cmdExecutor.getErrors());
						}
						dCounterValue = convertToDouble(strCommandOutput);
					} else {
						
						if(query.contains("localhost")){
							query = query.replace("localhost", hostName);
						}
						
						if(query.contains("#APP_NAME#")) {
							query = query.replace("#APP_NAME#", strApp);
						}else if(query.contains("#PORT_NUMBER#")) {
							query = query.replace("#PORT_NUMBER#", strPort);
						}
						else if(query.contains("#HTTP_NAME#")) {
							query = query.replace("#HTTP_NAME#", strHTTPName);
						}
						
						saQuery = query.split("#@#");
						
						if( strConnectorPortAddress != null && strConnectorPortAddress.length() > 1 && saQuery[0].contains("Catalina:type=Connector,") ) {
							saQuery[0] = saQuery[0]+",address=\""+strConnectorPortAddress+"\"";
						}
						
						/* Hardcode the counters for testing purpose
						saQuery[0] = "jboss.web:type=Manager,path=/,host=localhost";
						saQuery[1] = "sessionIdLength";
						*/
						
						obj = new ObjectName(saQuery[0]);
						if ( saQuery[0].trim().startsWith("java.lang:type=Memory") ) {
							Object objVMInfo = connMBeanServer.getAttribute(obj, saQuery[1]);
							CompositeData cd = (CompositeData) objVMInfo;
							dCounterValue = convertToDouble(cd.get(saQuery[2]));
						}
						// Other than Memory counters
						else {
							dCounterValue = convertToDouble(connMBeanServer.getAttribute(obj, saQuery[1]));
						}
					}
					
					if ( bIsDelta ) {
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
					if( joSLACounter != null ) {
						addSlaCounterValue(joSLACounter);
					}
					
				} catch (Throwable th) {
					System.out.println("Command execution failed: "+query);
					
					if( th.getMessage().startsWith("Connection refused to host:") ) {
						System.out.println("Exception in monitorTomcatServer.counter-loop: "+th.getMessage());
						throw th;
					} else {
						System.out.println("Exception in monitorTomcatServer.counter-loop: "+th.getMessage());
						th.printStackTrace();
						reportCounterError(nCounterId, th.getMessage());
					}
				}
			}
		} catch (Throwable th) {
			System.out.println("Exception in monitorTomcatServer: "+th.getMessage());
			th.printStackTrace();
			reportGlobalError(th.getMessage());
		} finally {
			// queue the counter
			try {
				queueCounterValues();
			} catch (Exception e) {
				System.out.println("Exception in queueCounterValues(): " + e.getMessage());
				e.printStackTrace();
			}
		}
	}
	
	
	public void monitorTomcatCounters(String strGUID, Date collectionDate, String JMX_PORT){
		
		JSONArray joSelectedCounters = null, jaSlaCounters = null;
		JSONObject joSelectedCounter = null;
		
		LinuxUnificationBean beanLinuxUnification =null;
		LinuxUnificationBean beanSLA = null;
		
		LinuxUnificationCounterBean beanLinuxCounters = null;
		
		Integer nCounterId = null;
		String strExecutionType = null, query = null, strCommandOutput = null;
		String [] saQuery = null;
		boolean bIsDelta = false;
		
		CommandLineExecutor cmdExecutor = null;
		
		try {
			
			joSelectedCounters = AgentCounterBean.getCountersBean(strGUID);
			
			beanLinuxUnification = new LinuxUnificationBean();
			beanLinuxUnification.setMod_type("Apache Tomcat");
			beanLinuxUnification.setType("MetricSet");
			beanLinuxUnification.setGuid(strGUID);
			beanLinuxUnification.setdDateTime(collectionDate);
			
			jaSlaCounters = SlaCounterBean.getSLACountersBean(strGUID);
			
			if(jaSlaCounters != null && jaSlaCounters.size() > 0) {
				beanSLA = new LinuxUnificationBean();
			}
			
			connectTomcatJMXObject(JMX_PORT);
			
			for (int i = 0; i < joSelectedCounters.size(); i++) {
				
				try {
					joSelectedCounter = joSelectedCounters.getJSONObject(i);
					nCounterId = Integer.parseInt(joSelectedCounter.getString("counter_id"));
					query = joSelectedCounter.getString("query");
					bIsDelta = joSelectedCounter.getBoolean("isdelta");
					strExecutionType = joSelectedCounter.getString("executiontype");
					
					beanLinuxUnification.addNewCounter(joSelectedCounter.getString("counter_id"));
					
					if ( strExecutionType.equals("cmd") ) {
						// Get counter-value after executing the command
						cmdExecutor = new CommandLineExecutor();
						
						cmdExecutor.executeCommand(query);
						
						strCommandOutput = cmdExecutor.getOutput().toString();
						
						if ( strCommandOutput.length() == 0 ) {
							
							throw new Exception("Metric doesn't return value: "+cmdExecutor.getErrors());
						}
						dCounterValue = convertToDouble(strCommandOutput);
					}else {
						
						saQuery = query.split("#@#");
						obj = new ObjectName(saQuery[0]);
						
						/*if ( saQuery[0].trim().startsWith("java.lang:type=Memory") ) {
							Object objVMInfo = connMBeanServer.getAttribute(obj, saQuery[1]);
							CompositeData cd = (CompositeData) objVMInfo;
							dCounterValue = convertToDouble(cd.get(saQuery[2]));
						}
						// Other than Memory counters
						else {*/
							dCounterValue = convertToDouble(this.connMBeanServer.getAttribute(obj, saQuery[1]));
						//}

						if ( bIsDelta ) {
							//dCounterValue = addDeltaCounterValue(Integer.parseInt(strCounterId), dCounterValue);
							dCounterValue = addDeltaCounterValue_v1(nCounterId, dCounterValue);
						} /*else {
							addCounterValue( nCounterId, dCounterValue );
						}	*/
						
						// Create Bean for the LinuxUnification OS Module Counter entry(line)
						beanLinuxCounters = new LinuxUnificationCounterBean();
						beanLinuxCounters.setCounter_type(nCounterId);
						beanLinuxCounters.setException("");
						beanLinuxCounters.setProcess_name("");
						beanLinuxCounters.setCounter_value(dCounterValue);
						beanLinuxUnification.addCounterEntry(String.valueOf(nCounterId), beanLinuxCounters);
						if(SlaCounterBean.getSLACountersBean(strGUID) != null && SlaCounterBean.getSLACountersBean(strGUID).size() > 0) {
							//Verifying SLA Breach
							verifySLABreach_v1(jaSlaCounters, nCounterId, dCounterValue, beanSLA);
						}
					}
						
				}catch (Exception e) {
					LogManagerExtended.applicationInfoLog(e.getMessage());
				}
			}
			
			if(beanLinuxUnification.isCountersValueAvailable()) {
				LogManagerExtended.logJStackOutput("metrics###"+beanLinuxUnification.toString("MetricSet"));
				LogManagerExtended.applicationInfoLog("metrics###"+beanLinuxUnification.toString("MetricSet"));
			}
			
			if(beanSLA != null) {
				if(beanSLA.isSLACountersValueAvailable()) {
					beanSLA.setMod_type("Apache Tomcat");
					beanSLA.setType("SLASet");
					beanSLA.setGuid(Constants.LINUX_AGENT_GUID);
					beanSLA.setdDateTime(collectionDate);
					
					LogManagerExtended.logJStackOutput("metrics###"+beanSLA.toString("SLASet"));
					LogManagerExtended.applicationInfoLog("metrics###"+beanSLA.toString("SLASet"));
				}
			}
			
		}catch (Exception e) {
			LogManagerExtended.applicationInfoLog("Exception in monitorModuleCounters(): " + e);
			e.printStackTrace();
		}
	}
	
	/**
	 * Send the collected counter-sets to Collector WebService, by calling parent's sendCounter method
	 * 
	 * @param strGUID
	 */
	public void sendCounters(String strGUID) {
		// send the collected counters to Collector WebService through parent sender function
		sendCounterToCollector(strGUID,AGENT_TYPE.TOMCAT);
		sendSlaCounterToCollector(strGUID, AGENT_TYPE.TOMCAT);
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

	/**
	 * list all deployed projects in Tomcat
	 */
	public void listAllProjects() throws Exception {
		ObjectName[] objectNames = null;
		
		String strObjectName = "", strAppFullName = "", strApplicationName = "";
		String[] saObjectName = null;
		
		try {
			System.out.println("Applications available under JMX Port "+this.JmxPort+":");
			
			// get HTTP name
			onHTTPName = new ObjectName("Catalina:type=Host,host=localhost");
			objectNames = (ObjectName[]) connMBeanServer.getAttribute(onHTTPName, "children");
			
			for (int i = 0; i < objectNames.length; i = i + 1) {
				//System.out.println("objectName: "+objectNames[i]);
				strObjectName = objectNames[i].toString();
				saObjectName = strObjectName.split(",");

				strAppFullName = saObjectName[1];
				strApplicationName = strAppFullName.substring(strAppFullName.lastIndexOf("/"), strAppFullName.length()); 
				System.out.println(strApplicationName);
			}
			System.out.println();

		} catch (Exception e) {
			System.out.println("Exception in listAllProjects: "+e.getMessage());
			e.printStackTrace();
		} finally {
			if (jmxc != null) {
				jmxc.close();
				jmxc = null;
			}
		}
	}
	
	public void connectTomcatJMXObject(String TOMCAT_JMXPORT) {
		String conURL = null;
		try {
			conURL = Constants.TOMCAT_JMX_CONNECTOR_URL.replace("#@#JMX_PORT#@#", TOMCAT_JMXPORT);
			UtilsFactory.printDebugLog(Constants.IS_DEBUG, "Connection string for JMX : "+conURL);
			//this.url = new JMXServiceURL("service:jmx:rmi:///jndi/rmi://localhost:"+TOMCAT_JMXPORT+"/jmxrmi");
			this.url = new JMXServiceURL(conURL);
			
			this.jmxc = JMXConnectorFactory.connect(this.url);
			
			this.connMBeanServer = jmxc.getMBeanServerConnection();
			
			LogManagerExtended.applicationInfoLog("JMX Port is successfully connected...");
			UtilsFactory.printDebugLog(Constants.IS_DEBUG, "JMX Port is successfully connected.");
		}catch (Exception e) {
			
			LogManagerExtended.applicationInfoLog("connectTomcatJMXObject : "+e);
			UtilsFactory.printDebugLog(Constants.IS_DEBUG, "JMX connection failed: "+e.getMessage());
			if(Constants.IS_DEBUG) {
				e.printStackTrace();
			}
			// TODO: handle exception
		}
	}
	
	
	public void getAllProjectNames(String JMX_PORT) throws Exception{
		/*JMXServiceURL jmxUrl = null;
		JMXConnector jmxc = null;
		MBeanServerConnection connMBeanServer = null;*/
		
		String strObjectName = "", strAppFullName = "", strApplicationName = "";
		String[] saObjectName = null;
		
		ObjectName onHTTPName = null;
		ObjectName[] objectNames = null;
		
		boolean isDefaultApp;
		//ArrayList<String> appList = null;
		try {
			//appList = Constants.TOMCAT_APP_LIST.get(JMX_PORT);
			String[] NonMonitorAppList = Constants.NON_MONITORING_APP_LIST;
			
			LogManagerExtended.applicationInfoLog("Applications available under JMX Port :" +JMX_PORT);
			UtilsFactory.printDebugLog(Constants.IS_DEBUG, "Getting all project list.");
			// get HTTP name
			UtilsFactory.printDebugLog(Constants.IS_DEBUG, "Object Host Name : "+Constants.HOST_OBJECT_NAME);
			onHTTPName = new ObjectName(Constants.HOST_OBJECT_NAME);
			objectNames = (ObjectName[]) this.connMBeanServer.getAttribute(onHTTPName, "children");
			UtilsFactory.printDebugLog(Constants.IS_DEBUG, "Total objects found: "+objectNames.length);
			for (int i = 0; i < objectNames.length; i = i + 1) {
				isDefaultApp = false;
				
				strObjectName = objectNames[i].toString();
				UtilsFactory.printDebugLog(Constants.IS_DEBUG, "Full string of each object name: "+strObjectName);
				saObjectName = strObjectName.split(",");

				strAppFullName = saObjectName[1];
				strApplicationName = strAppFullName.substring(strAppFullName.lastIndexOf("/")+1, strAppFullName.length()); 
				
				UtilsFactory.printDebugLog(Constants.IS_DEBUG, "Application name: "+strApplicationName);
				
				if(!strApplicationName.isEmpty()) {
					boolean isNonMonitoringApp = Arrays.asList(NonMonitorAppList).contains(strApplicationName);
					
					if(!isNonMonitoringApp) {
						//Constants.APPLICATION_LIST.add(strApplicationName);
						//appList.add(strApplicationName);
						Constants.TOMCAT_APP_LIST.get(JMX_PORT).add(strApplicationName);
					}
				}
			}
			UtilsFactory.printDebugLog(Constants.IS_DEBUG, "All application list for JMX_PORT("+JMX_PORT+") : " + Constants.TOMCAT_APP_LIST.get(JMX_PORT));
			LogManagerExtended.applicationInfoLog("list of Application Names of JMX_PORT("+JMX_PORT+") : "+ Constants.TOMCAT_APP_LIST.get(JMX_PORT));

		} catch (Exception e) {
			LogManagerExtended.applicationInfoLog("Exception in listAllProjects: "+e.getMessage());
			UtilsFactory.printDebugLog(Constants.IS_DEBUG, "Error: "+e.getMessage());
			e.printStackTrace();
		} finally {
			/*if (jmxc != null) {
				jmxc.close();
				jmxc = null;
			}*/
		}
	}
	
	public JSONObject getModuleTypeVersion() throws Exception{
		ObjectName onHTTPName = null;
		JSONObject joModuleTypeInfo = null;
		String[] moduleTypeInfo;
		try {
			onHTTPName = new ObjectName("Catalina:type=Server");
			String moduleTypeDetails = connMBeanServer.getAttribute(onHTTPName, "serverInfo").toString();
			LogManagerExtended.applicationInfoLog("Module Details : "+ moduleTypeDetails);
			moduleTypeInfo = moduleTypeDetails.split("/");
			
			joModuleTypeInfo = new JSONObject();
			if(moduleTypeInfo.length > 1) {	
				joModuleTypeInfo.put("moduleTypeName", moduleTypeInfo[0]);
				joModuleTypeInfo.put("VERSION_ID", moduleTypeInfo[1]);
			}
		}catch (Exception e) {
			LogManagerExtended.applicationInfoLog("Exception in getModuleTypeVersion : "+ e);
			// TODO: handle exception
		}
		return joModuleTypeInfo;
	}
	
	public HashMap<String, JSONArray> getApplicationDynamicCounters(String JMX_PORT) throws Exception {
		
		ObjectName http;
		
		String obj, ApplicationName = "", category = "", type = "", name = "";
		
		boolean isNonMonitorApp = false, isAppMetrics = false, isProceed = true;
		
		String[] NonMonitoringAppList = Constants.NON_MONITORING_APP_LIST;
		
		JSONObject joNewCounterSet = null;
		
		JSONArray jaNewCounterSet = null;
		
		HashMap<String, JSONArray> hmCounterSet = new HashMap<String, JSONArray>();
		
		try {	
			connectTomcatJMXObject(JMX_PORT);
			UtilsFactory.printDebugLog(Constants.IS_DEBUG, "Getting all dynamic counter set.");
			hmCounterSet.put("Generic", new JSONArray());
			
			if(Constants.TOMCAT_APP_LIST.get(JMX_PORT).size() > 0) {
				for(String AppName : Constants.TOMCAT_APP_LIST.get(JMX_PORT) ) {
					hmCounterSet.put(AppName, new JSONArray());	
				}
			}
			
			Set<ObjectName> mbeans = this.connMBeanServer.queryNames(null, null);
			
			for (Object mbean : mbeans)
			{
				isNonMonitorApp = false;
				http = (ObjectName)mbean;
				obj = http.toString();
				UtilsFactory.printDebugLog(Constants.IS_DEBUG, "Each counter Raw value : "+obj);
				MBeanInfo info = this.connMBeanServer.getMBeanInfo(http);
			    MBeanAttributeInfo[] attrInfo = info.getAttributes();
			    
			    for (String NonMonitorAppName : NonMonitoringAppList) {
			    	if(obj.contains("/"+NonMonitorAppName)) {
			    		isNonMonitorApp = true;
			    		break;
			    	}
			    }
			   
			    if(!isNonMonitorApp) {
			    	UtilsFactory.printDebugLog(Constants.IS_DEBUG, "Each counter Raw value : "+obj);
			    	isAppMetrics = false;
			    	for(String appName : Constants.TOMCAT_APP_LIST.get(JMX_PORT)) {
		    			/*if(obj.contains(appName)){
		    				ApplicationName = appName;
			    			isAppMetrics = true;
			    			break;	
		    			}	*/
			    		if(obj.contains("/"+appName+",") || obj.endsWith("/"+appName)) {
			    			ApplicationName = appName;
			    			isAppMetrics = true;
			    			break;
			    		}
		    		}
			    	
			    	if(isAppMetrics) {
			    		
			    		if(obj.contains("name=") && !obj.contains("j2eeType=WebModule")) {
		    				String[] value = obj.split("[:,]");
		    				for (String res : value) {
		    					if(res.contains("type=") || res.contains("j2eeType=")) {
		    						type = res.split("=")[1].replace("\"", "");
		    					}else if(res.contains("name=")) {
		    						name = res.split("=")[1].replace("\"", "");
		    					}
		    				}
		    				category = type+"-"+name;
		    			}else {
		    				String[] value = obj.split("[:=,]");
		    				category = value[2].replace("\"", "");
		    			}
			    		
			    	}else {
			    		ApplicationName = "Generic";
			    		
			    		if((obj.contains("name=") || obj.contains("port=")) && !obj.contains("j2eeType=WebModule")) {
		    				String[] value = obj.split("[:,]");
		    				for (String res : value) {
		    					if(res.contains("type=") || res.contains("j2eeType=")) {
		    						type = res.split("=")[1].replace("\"", "");
		    					}else if(res.contains("name=") || res.contains("port=")) {
		    						name = res.split("=")[1].replace("\"", "");
		    					}
		    				}
		    				category = type+"-"+name;
		    			}else {
		    				String[] value = obj.split("[:=,]");
		    				category = value[2].replace("\"", "");
		    			}
		    			
			    	}
			    	
			    	if(category.toLowerCase().startsWith("requestprocessor-httprequest")) {
			    		if(obj.contains("http-bio-443")) {
			    			isProceed = true;
			    		}else {
			    			isProceed = false;
			    		}
			    	}else {
			    		isProceed = true;
			    	}
			    	
			    	if(isProceed) {
			    		UtilsFactory.printDebugLog(Constants.IS_DEBUG, "Filtered object : "+obj);
			    		jaNewCounterSet = hmCounterSet.get(ApplicationName);
				    	
				    	for (MBeanAttributeInfo attr : attrInfo)
					    {
					    	if(attr.getType() != null && (attr.getType().contains("int") || attr.getType().contains("long"))) {
					    		
					    		joNewCounterSet = new JSONObject();
					    		
					    		joNewCounterSet.put("category", category);
					    		joNewCounterSet.put("counter_name", attr.getName());
					    		joNewCounterSet.put("has_instance", "f");
		    		            joNewCounterSet.put("instance_name", "");
		    		            joNewCounterSet.put("unit", setMetricsUnits(attr.getDescription() == null ? "" : attr.getDescription()));
		    		            joNewCounterSet.put("is_selected", setIsSelected(category, attr.getName()));
		    		            joNewCounterSet.put("is_static_counter", "f");
		    		            joNewCounterSet.put("query_string", obj+"#@#"+attr.getName());
	    		                joNewCounterSet.put("counter_description", attr.getDescription() == null ? "" : attr.getDescription());
	    		                joNewCounterSet.put("is_delta", "f");
		    		            
	    		                jaNewCounterSet.add(joNewCounterSet);	    		
					    		
					    	}
					    }
				    	hmCounterSet.put(ApplicationName, jaNewCounterSet);
			    	}
			    	
			    }   
			}
		}catch (Exception e) {
			// TODO: handle exception
			LogManagerExtended.applicationInfoLog(e.getMessage());
			if (Constants.IS_DEBUG) {
				e.printStackTrace();
			}
		}
		
		return hmCounterSet;
	}
	
	public boolean setIsSelected(String category, String counterName) {
		boolean bIsSeleted = false;
		
		if(category.equalsIgnoreCase("manager") && (counterName.equalsIgnoreCase("rejectedsessions") || counterName.equalsIgnoreCase("activesessions"))) {
			bIsSeleted = true;
		}else if(category.startsWith("GlobalRequestProcessor") && category.endsWith("443")) {
			if(counterName.equalsIgnoreCase("processingtime") || counterName.equalsIgnoreCase("errorcount") || counterName.equalsIgnoreCase("requestcount") || counterName.equalsIgnoreCase("bytessent") || counterName.equalsIgnoreCase("bytesreceived")) {
				bIsSeleted = true;
			}
		}else if(category.startsWith("Connector") && category.endsWith("443"))  {
			if(counterName.equalsIgnoreCase("acceptcount") || counterName.equalsIgnoreCase("maxthreads")) {
				bIsSeleted = true;
			}
		}else if(category.equalsIgnoreCase("cache")) {
			if(counterName.equalsIgnoreCase("hitscount") || counterName.equalsIgnoreCase("accesscount") || counterName.equalsIgnoreCase("cachesize")) {
				bIsSeleted = true;
			}
		}else if(category.equalsIgnoreCase("webmodule")) {
			if(counterName.equalsIgnoreCase("cachemaxsize") || counterName.equalsIgnoreCase("sessiontimeout")) {
				bIsSeleted = true;
			}
		}else if(category.equalsIgnoreCase("Servlet-default")) {
			if(counterName.equalsIgnoreCase("errorcount")) {
				bIsSeleted = true;
			}
		}
		
		return bIsSeleted;
	}
	
	public String setMetricsUnits(String MetricDesc) {
		String Units= "";
		
		if(MetricDesc.toLowerCase().contains("milliseconds")) {
			Units = "ms";
		}else if(MetricDesc.toLowerCase().contains("seconds")) {
			Units = "sec";
		}else if(MetricDesc.toLowerCase().contains("minutes")) {
			Units = "min";
		}if(MetricDesc.toLowerCase().contains("bytes")) {
			Units = "bytes";
		}else if(MetricDesc.toLowerCase().contains("kb")) {
			Units = "kb";
		}else if(MetricDesc.toLowerCase().contains("count")) {
			Units = "count";
		}else if(MetricDesc.toLowerCase().contains("time")) {
			Units = "ms";
		}else {
			Units = "number";
		}
		
		return Units;
	}
	
}
