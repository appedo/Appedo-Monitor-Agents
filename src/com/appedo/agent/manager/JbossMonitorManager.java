package com.appedo.agent.manager;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Properties;
import java.util.Set;

import javax.management.MBeanAttributeInfo;
import javax.management.MBeanInfo;
import javax.management.MBeanServerConnection;
import javax.management.ObjectName;
import javax.management.remote.JMXConnector;
import javax.management.remote.JMXConnectorFactory;
import javax.management.remote.JMXServiceURL;
import javax.security.auth.callback.Callback;
import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.callback.NameCallback;
import javax.security.auth.callback.PasswordCallback;
import javax.security.auth.callback.UnsupportedCallbackException;
import javax.security.sasl.RealmCallback;

import org.jboss.as.controller.client.ModelControllerClient;
import org.jboss.dmr.ModelNode;

import com.appedo.agent.bean.AgentCounterBean;
import com.appedo.agent.bean.LinuxUnificationBean;
import com.appedo.agent.bean.LinuxUnificationCounterBean;
import com.appedo.agent.bean.SlaCounterBean;
import com.appedo.agent.utils.Constants;
import com.appedo.agent.utils.Constants.AGENT_TYPE;
import com.appedo.agent.utils.Constants.OSType;
import com.appedo.agent.utils.UtilsFactory;
import com.sun.tools.attach.VirtualMachine;

import net.sf.json.JSONArray;
import net.sf.json.JSONObject;
import sun.management.ConnectorAddressLink;
import sun.tools.attach.HotSpotVirtualMachine;

/**
 * Linux OS monitoring class. This has the functionalities to get the counter values of Linux OS.
 * 
 * @author veeru
 *
 */

public class JbossMonitorManager extends AgentManager {
	
	public static JbossMonitorManager jbossMonitorManager = null;
	
	String host = "localhost";
	int port = 0;
	String userid = null;
	String password = null;
	//String vendorSpecificProtocol = null;
	String vendorSpecificProtocol = "remoting-jmx";
	
	String hostControllerName = "master";
	String serverName = "JayServer";
	
	boolean standaloneMode = true;	// If you are running your Servers in Domain Mode then set this to false.
	
	ModelControllerClient client = null;
	JMXConnector jmxConnector = null;
	MBeanServerConnection connection = null;
	
	private String strGlobalException = null;
	
	/**
	 * Avoid the object creation for this Class from outside.
	 */
	private JbossMonitorManager() throws Throwable {
		if(Constants.JBOSS_PORTS.isEmpty()){
			loadConfigProperties();
		}
	}
	
	/**
	 * Returns the only object(singleton) of this Class.
	 * 
	 * @return
	 */
	public static JbossMonitorManager getJbossMonitorManager() throws Throwable {
		if( jbossMonitorManager == null ){
			jbossMonitorManager = new JbossMonitorManager();
		}
		
		return jbossMonitorManager;
	}
	
	private void recreateClientConnection() throws Throwable {
		
		if( client == null ) {
			createClientConnection(host, port, userid, password);
		}
		
		// Use the established connection
		// if connection is not available, then exception will be thrown. 
		// Establish a new connection and retry 
		// If still connection exception persists, then need to handle it further.
		try{
			connection.getDefaultDomain();
		} catch(Throwable th) {
			System.out.println("Retrying JBoss-JMX connection.");
			createClientConnection(host, port, userid, password);
			
			try{
				connection.getMBeanCount();
			} catch(Throwable th1) {
				throw new Exception("JBoss-JMX connection is failed.");
			}
		}
	}
	
	private void createClientConnection(String host, int port, String userid, String password) throws Throwable {
		HashMap<String, String[]> environment = null;
		String[] credentials = null;
		
		// Close objects if exists
		closeObjects();
		
		try{
			// Simple connection to the client, without credentials.
			if( userid == null || password == null ) {
				// NOTE: For hostname as "localhost", there is no need to pass the username & password
				client = ModelControllerClient.Factory.create(host, port, null);
			}
			// Provide credentials required by server for user authentication. Important while monitor remotely.
			else {
				client = createClient(InetAddress.getByName(host), port, userid, password, "ManagementRealm" );
				
				credentials = new String[] {userid, password};
				environment = new HashMap<String, String[]>();
				environment.put (JMXConnector.CREDENTIALS, credentials);
			}
			
			// Connect JMX
			String urlString ="service:jmx:"+vendorSpecificProtocol+"://"+host+":"+port;
			//String urlString ="service:jmx:remoting-jmx://"+host+":"+port;
			
			UtilsFactory.printDebugLog(Constants.IS_DEBUG, "URLString: "+urlString+" <> ("+userid+", pass.length: "+(password!=null?password.length():0)+")");
			
			JMXServiceURL serviceURL = new JMXServiceURL(urlString);
			jmxConnector = JMXConnectorFactory.connect(serviceURL, environment);
			connection = jmxConnector.getMBeanServerConnection();
			
			UtilsFactory.printDebugLog(Constants.IS_DEBUG, "JMX-Connection-MBeanCount: "+connection.getMBeanCount());
			
		} catch(Throwable th) {
			System.out.println("Unable to establish JBoss's JMX Connection."+th.getMessage());
			System.out.println("Check for \"boundPort\" in JConsole in the below path:\njboss.as -> standard-sockets -> management-native -> Attribute :: boundPort");
			
			throw th;
		}
	}
	
	private ModelControllerClient createClient(final InetAddress host, final int port, final String username, final String password, final String securityRealmName) {
		
		final CallbackHandler callbackHandler = new CallbackHandler() {
			public void handle(Callback[] callbacks) throws IOException, UnsupportedCallbackException {
				for (Callback current : callbacks) {
					 if (current instanceof NameCallback) {
						NameCallback ncb = (NameCallback) current;
						System.out.println("ncb.setName() = "+username);
						ncb.setName(username);
					} else if (current instanceof PasswordCallback) {
						PasswordCallback pcb = (PasswordCallback) current;
						System.out.println("pcb.setPassword() = "+password);
						pcb.setPassword(password.toCharArray());
					} else if (current instanceof RealmCallback) {
						RealmCallback rcb = (RealmCallback) current;
						System.out.println("rcb.getDefaulttest() = "+rcb.getDefaultText());
						rcb.setText(securityRealmName);
					} else {
						throw new UnsupportedCallbackException(current);
					}
				}
			}
		};
		
	 	return ModelControllerClient.Factory.create(host, port, callbackHandler);
  	}

	/**
	 * 
	 * connecting jboss JMX client connection based on the list of jboss ports
	 * 
	 * @return
	 */
	public boolean createJBossJMXConnection() throws Throwable {
		boolean isJMXConnected = false;
		if( client == null ) {
			isJMXConnected = createJbossClientConnection();
		}
		
		try{
			connection.getDefaultDomain();
			isJMXConnected = true;
		} catch(Throwable th) {
			LogManagerExtended.applicationInfoLog("Retrying JBoss-JMX connection.");
			isJMXConnected = createJbossClientConnection();
		}		
		return isJMXConnected;
	}
	
	private boolean createJbossClientConnection() throws Throwable {
		
		boolean isJMXConnected = false;
		
		if(Constants.JBOSS_JMXPORT.isEmpty()) {
			for(int i=0; i<Constants.JBOSS_PORTS.size(); i++) {
				try {
					createClientConnection(Constants.JBOSS_HOST, Integer.parseInt(Constants.JBOSS_PORTS.get(i)), Constants.JBOSS_USER_ID, Constants.JBOSS_PASSWORD);
					
					Constants.JBOSS_JMXPORT = Constants.JBOSS_PORTS.get(i);
					LogManagerExtended.applicationInfoLog("\nJMX Connected to the Host: "+host+" or Port: "+ Constants.JBOSS_PORTS.get(i));
					isJMXConnected = true;
					break;
				}catch (Exception e) {
					LogManagerExtended.applicationInfoLog("\nUnable to Connect to the Host: "+host+" or Port: "+Constants.JBOSS_PORTS.get(i));
				}
			}
		}else {
			try {
				createClientConnection(Constants.JBOSS_HOST, Integer.parseInt(Constants.JBOSS_JMXPORT), Constants.JBOSS_USER_ID, Constants.JBOSS_PASSWORD);
				LogManagerExtended.applicationInfoLog("\nJMX Connected to the Host: "+host+" or Port: "+ Constants.JBOSS_JMXPORT);
				isJMXConnected = true;
			}catch (Exception e) {
				LogManagerExtended.applicationInfoLog("\nUnable to Connect to the Host: "+host+" or Port: "+Constants.JBOSS_JMXPORT);
			}
		}
		
		return isJMXConnected;
	}
	
	public JSONObject getJbossServerDetails() {
		JSONObject joJbossInfo = new JSONObject();
		ModelNode responseJbossInfo = null;
		ArrayList<String> appName = new ArrayList<String>();
		try {
			StandaloneModel objStandaloneModel = new StandaloneModel();
			
			responseJbossInfo = objStandaloneModel.getJbossDetails(client);
			
			ModelNode deployment = responseJbossInfo.get("result").get("deployment");
			
			for(String AppName : deployment.keys()) {
				appName.add(AppName.substring(0, AppName.indexOf(".war")));
			}
			
			String moduleName = "Jboss : "+appName.toString().substring(1, appName.toString().length()-1);
			joJbossInfo.put("moduleName", moduleName);
			joJbossInfo.put("moduleTypeName", "JBoss");
			joJbossInfo.put("VERSION_ID", responseJbossInfo.get("result").get("release-version").toString().replace("\"", ""));
			
		}catch (Exception e) {
			LogManagerExtended.applicationInfoLog("Exception in getJbossServerDetails "+e);
		}
		return joJbossInfo;
	}
	
	public JSONObject getDynamicJbossMetrics() {
		
		JSONObject joNewCounterSet = null;
		JSONArray jaNewCounterSet = new JSONArray();
		try {
			
			joNewCounterSet = new JSONObject();
			jaNewCounterSet.addAll(getJbossJMXCounters());
			jaNewCounterSet.addAll(getStandaloneCounter());
			
			joNewCounterSet.put("counterData", jaNewCounterSet);
			
			if (Constants.IS_DEBUG) {
				System.out.println("Jboss Dynamic Counter data...");
				System.out.println(joNewCounterSet);
			}
		}catch (Exception e) {
			LogManagerExtended.applicationInfoLog("Exception in getDynamicJbossMetrics "+e);
		}
		return joNewCounterSet;
	}
	
	private JSONArray getStandaloneCounter() throws Exception {
		
		ModelNode requestProcessorVal = null, transactionsVal = null;
		JSONArray joNewCounterSet = new JSONArray();
		
		try {
			// FOR STANDALONE MODE use the "StandaloneModel" class as following:
			StandaloneModel objStandaloneModel = new StandaloneModel();
			
			// Getting Web Subsystem runtime Details */
			requestProcessorVal = objStandaloneModel.getWebSubsystemRuntimeAttributes(client);
			
			transactionsVal = objStandaloneModel.getTransactionRunTimeAttributes(client);
			
			joNewCounterSet.addAll(getJbossStandaloneCounters(requestProcessorVal, "RequestProcessor"));
			joNewCounterSet.addAll(getJbossStandaloneCounters(transactionsVal, "Transactions"));
			
			
		}catch (Exception e) {
			LogManagerExtended.applicationInfoLog("Exception in getStandaloneCounter(): "+e);
		}
		return joNewCounterSet;
	}
	
	private JSONArray getJbossStandaloneCounters(ModelNode responseValue, String category) throws Exception{
		
		JSONArray jaNewCounterSet = new JSONArray();
		JSONObject joNewCounterSet = null;
		try {
			if (responseValue.get("result").hasDefined("attributes")) {
				ModelNode attributes = responseValue.get("result").get("attributes");
				
				for(String counterName : attributes.keys()) {
					
					ModelNode counterDetails = attributes.get(counterName);
					
					if( counterDetails.get("access-type").toString().contains("metric") && (counterDetails.get("type").toString().equalsIgnoreCase("INT") || counterDetails.get("type").toString().equalsIgnoreCase("LONG"))) {
						
						String counterDesc = counterDetails.get("description").toString().replaceAll("\"", "");
						joNewCounterSet = new JSONObject();
						
						joNewCounterSet.put("category", category);
						joNewCounterSet.put("counter_name", counterName);
						joNewCounterSet.put("has_instance", "f");
						joNewCounterSet.put("instance_name", "");
						joNewCounterSet.put("unit", setMetricsUnits(counterName));
						joNewCounterSet.put("is_selected", true);
						joNewCounterSet.put("is_static_counter", "f");
						joNewCounterSet.put("query_string", category+"#@#"+counterName);
						joNewCounterSet.put("counter_description", counterDesc);
						joNewCounterSet.put("is_delta", setIsDelta(category, counterName));

						jaNewCounterSet.add(joNewCounterSet);
					}
				}
			}
		}catch (Exception e) {
			LogManagerExtended.applicationInfoLog("Exception in getJbossStandaloneCounters: "+e);
		}
		return jaNewCounterSet;
	}
	
	private JSONArray getJbossJMXCounters() throws Exception{
		
		ObjectName http;
		String obj, category = "";
		boolean isFirstLoop = true;
		
		JSONObject joNewCounterSet = null;
		JSONArray jaNewCounterSet = new JSONArray();
		try {
			
			Set<ObjectName> mbeans = this.connection.queryNames(null, null);
			
			for (Object mbean : mbeans)
			{
				http = (ObjectName)mbean;
				obj = http.toString();
				
				if(!obj.contains("java.lang:type=MemoryPool") && !obj.contains("jboss.ws:service=ServerConfig")) {
				
					MBeanInfo info = this.connection.getMBeanInfo(http);
					MBeanAttributeInfo[] attrInfo = info.getAttributes();
					isFirstLoop = true;
					for (MBeanAttributeInfo attr : attrInfo)
					{
						if(attr.getType() != null && (attr.getType().contains("int") || attr.getType().contains("long"))) 
						{
							if(isFirstLoop) {
								category = getJbossCounterCategory(obj);
								isFirstLoop = false;
							}
							
							joNewCounterSet = new JSONObject();
							
							joNewCounterSet.put("category", category);
							joNewCounterSet.put("counter_name", attr.getName());
							joNewCounterSet.put("has_instance", "f");
							joNewCounterSet.put("instance_name", "");
							joNewCounterSet.put("unit", setMetricsUnits(attr.getDescription()));
							joNewCounterSet.put("is_selected", false);
							joNewCounterSet.put("is_static_counter", "f");
							joNewCounterSet.put("query_string", obj+"#@#"+attr.getName());
							joNewCounterSet.put("counter_description", attr.getDescription());
							joNewCounterSet.put("is_delta", "f");
							
							jaNewCounterSet.add(joNewCounterSet);
						}
					}
					
				}
			}
			
		}catch (Exception e) {
			LogManagerExtended.applicationInfoLog("Exception in getJbossJMXCounters "+e);
		}
		return jaNewCounterSet;
	}
	
	private String getJbossCounterCategory(String objName) {
		String category= "", type = "", name="";
		int startIndex, endIndex;
		
		try {
			if(objName.contains("type=")) {
				
				startIndex = objName.indexOf("type=")+5;
				endIndex = (objName.indexOf(",", startIndex) < 0) ? objName.length() : objName.indexOf(",", startIndex);
				
				type = objName.substring(startIndex, endIndex);
			}
			
			if (objName.contains("name=")){
				startIndex = objName.indexOf("name=")+5;
				endIndex = (objName.indexOf(",", startIndex) < 0) ? objName.length() : objName.indexOf(",", startIndex);
				
				name = objName.substring(startIndex, endIndex);
			}
			
			category = (name.isEmpty() && name.equals("/")) ? type : type+"-"+name;
			
		} catch (Exception e) {
			LogManagerExtended.applicationInfoLog("Exception in getJbossCounterCategory "+e);
		}
		
		return category;
	}
	
	private String setMetricsUnits(String MetricDesc) {
		String Units= "";
		
		if(MetricDesc.toLowerCase().contains("milliseconds")) {
			Units = "ms";
		}else if(MetricDesc.toLowerCase().contains("seconds")) {
			Units = "sec";
		}else if(MetricDesc.toLowerCase().contains("minutes")) {
			Units = "min";
		}if(MetricDesc.toLowerCase().contains("byte")) {
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
	
	private boolean setIsDelta(String category, String counter_name) {
		boolean isDelta = false;
		String[] RequestProcessor_counters = {"bytesReceived", "bytesSent", "requestCount", "errorCount"};
		String[] Transactions_counters = {"number-of-committed-transactions", "number-of-resource-rollbacks"}; 
		
		if(category.equalsIgnoreCase("RequestProcessor")) {
			for(String name : RequestProcessor_counters) {
				if(counter_name.equalsIgnoreCase(name)) {
					isDelta = true;
				}
			}
		}else if(category.equalsIgnoreCase("Transactions")) {
			for(String name : Transactions_counters) {
				if(counter_name.equalsIgnoreCase(name)) {
					isDelta = true;
				}
			}
		}
		
		return isDelta; 
	}
	
	public void updateJbossAppInfo(String GUID){
		JSONObject joJbossAppInfo = new JSONObject();
		ModelNode responseJbossInfo = null;
		ArrayList<String> appName = new ArrayList<String>();
		
		try {
			StandaloneModel objStandaloneModel = new StandaloneModel();
			
			//createJBossConnection();
			createJBossJMXConnection();
			
			responseJbossInfo = objStandaloneModel.getJbossDetails(client);
			
			ModelNode deployment = responseJbossInfo.get("result").get("deployment");
			
			for(String AppName : deployment.keys()) {
				appName.add(AppName.substring(0, AppName.indexOf(".war")));
			}
			
			String moduleName = "Jboss : "+appName.toString().substring(1, appName.toString().length()-1);
			joJbossAppInfo.put("moduleName", moduleName);
			joJbossAppInfo.put("moduleTypeName", "JBoss");
			joJbossAppInfo.put("VERSION_ID", responseJbossInfo.get("result").get("release-version").toString().replace("\"", ""));
			
			sendJBossAppInfoToCollector(joJbossAppInfo, GUID, "UpdateJbossAppInfo");
		}catch (Throwable th) {
			LogManagerExtended.applicationInfoLog("Exception in getJbossServerDetails "+th);
		}
	}
	
	public void monitorJbossServer(String strGUID, Date collectionDate){
		JSONArray joSelectedCounters = null, jaSlaCounters = null;
		JSONObject joSelectedCounter = null;
		
		LinuxUnificationBean beanLinuxUnification =null;
		LinuxUnificationBean beanSLA = null;
		
		LinuxUnificationCounterBean beanLinuxCounters = null;
		
		Integer nCounterId = null;
		String strExecutionType = null, query = null, strCommandOutput = null;
		String [] strQuery = null;
		boolean bIsDelta = false;
		Double dCounterValue = 0D;
		
		CommandLineExecutor cmdExecutor = null;
		ModelNode requestProcessorVal = null, transactionsVal = null;
		
		try {
			joSelectedCounters = AgentCounterBean.getCountersBean(strGUID);
			
			beanLinuxUnification = new LinuxUnificationBean();
			beanLinuxUnification.setMod_type("JBoss");
			beanLinuxUnification.setType("MetricSet");
			beanLinuxUnification.setGuid(strGUID);
			beanLinuxUnification.setdDateTime(collectionDate);
			
			jaSlaCounters = SlaCounterBean.getSLACountersBean(strGUID);
			
			if(jaSlaCounters != null && jaSlaCounters.size() > 0) {
				beanSLA = new LinuxUnificationBean();
			}
			
			//createJBossConnection();
			createJBossJMXConnection();
			
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
						if(standaloneMode==true){
							// FOR STANDALONE MODE use the "StandaloneModel" class as following:
							StandaloneModel objStandaloneModel = new StandaloneModel();

							// Getting Web Subsystem runtime Details */
							requestProcessorVal = objStandaloneModel.getWebSubsystemRuntimeDetails(client);
							
							// Monitoring Transactions
							transactionsVal = objStandaloneModel.monitorTransactionStatistics(client);
						}
						
						strQuery = query.split("#@#");
						
						if(strQuery[0].toString().trim().equalsIgnoreCase("RequestProcessor")) {
							dCounterValue = convertToDouble(requestProcessorVal.get("result").get(strQuery[1].toString()).asString());
						} else if (strQuery[0].toString().trim().equalsIgnoreCase("Transactions")) {
							dCounterValue = convertToDouble(transactionsVal.get("result").get(strQuery[1].toString()).asString());
						} else {
							ObjectName objectName=new ObjectName(strQuery[0].toString());
							dCounterValue = convertToDouble(connection.getAttribute(objectName, strQuery[1].toString()).toString());
						}
						
						if ( bIsDelta ) {
							dCounterValue = addDeltaCounterValue_v1(nCounterId, dCounterValue);
						}
						
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
					beanSLA.setMod_type("JBoss");
					beanSLA.setType("SLASet");
					beanSLA.setGuid(Constants.LINUX_AGENT_GUID);
					beanSLA.setdDateTime(collectionDate);
					
					LogManagerExtended.logJStackOutput("metrics###"+beanSLA.toString("SLASet"));
					LogManagerExtended.applicationInfoLog("metrics###"+beanSLA.toString("SLASet"));
				}
			}
			
		}catch (Throwable th) {

		}
	}
	
	/**
	 * Monitor the server and collect the counters
	 */
	public boolean monitorJbossServer(String strGUID,String strApp){
		try {
			// reset the counter collector variable in AgentManager.
			resetCounterMap(strGUID);
			strGlobalException = null;

			recreateClientConnection();
			
		} catch (Throwable th) {
			System.out.println("\n\nUnable to Connect to the Host: "+host+" or Port: "+port+" :: " + th.getMessage());
			th.printStackTrace();
			
			strGlobalException = th.getMessage();
			reportGlobalError("Unable to Connect to the Host: "+host+" or Port: "+port+" :: " + th.getMessage());
			
			// Stop the process from collecting counters.
			return false;
		}
		
		try {
			getCounters(strGUID,strApp);
		}catch (Exception e){
			System.out.println ("Exception in getCounters(): " + e.getMessage());
			e.printStackTrace();
		}
		
		return true;
	}
	
	/**
	 * Send it to the Collector WebService
	 */
	public void sendJbossCounters(String strGUID){
		sendCounters(strGUID);
	}
	
	/**
	 * Collect the counter with agent's types own logic or native methods
	 */
	private void getCounters(String strGUID,String strApp){
		
		String strCounterId = null, query = null;
		String [] strQuery = null;
		Double dCounterValue = 0D;
		ModelNode requestProcessorVal = null, transactionsVal = null;
		// create variable's to capture execution_type & is_delta
		boolean bIsDelta = false;
		String strExecutionType = "";

		ArrayList<String> alCommandOutput = null;
		
		// FOR STANDALONE MODE use the "StandaloneModel" class as following:
		StandaloneModel objStandaloneModel = new StandaloneModel();
		
		try{
			try {
				if(standaloneMode==true){

//					objStandaloneModel.runCommands(client);
					
					// Getting Web Subsystem runtime Details */
					requestProcessorVal = objStandaloneModel.getWebSubsystemRuntimeDetails(client);
					
					// cond. `requestProcessorVal != null` added, to avoid exception shown, since `getCounters` called even JMX not connected, 
					if ( requestProcessorVal != null ) {
						String runtimeResult = requestProcessorVal.get("result").toString();
						String path = "/subsystem=web/connector=http";
						/*
//						System.out.println("Result in console:");
						// working 
						/*PrintWriter pw = new PrintWriter(new File("E:/Ramkumar/jboss_counters.log"));
						requestProcessorVal.get("result").writeJSONString(pw, false);
						pw.flush();
						pw.close();*
						requestProcessorVal.get("result").writeExternal(new DataOutputStream(System.out));
//						System.out.println("Result printed in console.");
						
						*/
						
						if( runtimeResult.equals("undefined") ) {
							System.out.println("\n\tCheck ["+path+"] ** Server may not be Running");
						}
						
						// Testing Non XA DataSource ExampleDS */
						//objStandaloneModel.testNonXADataSource(client,"ExampleDS");
						
						// Monitoring Transactions
						transactionsVal = objStandaloneModel.monitorTransactionStatistics(client);
						
						// get the jvm status
						//HashMap hmJsonCounters=objStandaloneModel.getPlatformJvm(client);
					}
				}
				else if(standaloneMode==false){
					DomainModeModel objDomainModeModel= new DomainModeModel();
					
					// Getting Web Subsystem runtime Details */
					objDomainModeModel.getWebSubsystemRuntimeDetails(client,hostControllerName,serverName);
					
					// Testing Non XA DataSource ExampleDS */
					objDomainModeModel.testNonXADataSource(client,hostControllerName,serverName,"test");
					
					// Monitoring Application Statistics where application name is "Log4jDemo.war" */
					objDomainModeModel.monitorApplicationStatistics(client,hostControllerName,serverName,"Log4jDemo.war");
				}
			} catch(Exception e) {
				System.out.println("Excpetion in connect: "+e.getMessage());
				e.printStackTrace();
			}
			
			
			// get selected config counters 
			
			JSONArray joSelectedCounters = AgentCounterBean.getCountersBean(strGUID);
			
			for(int i=0; i<joSelectedCounters.size(); i++){
				try{
					dCounterValue = 0D;
					JSONObject joSelectedCounter = joSelectedCounters.getJSONObject(i);				
					strCounterId = joSelectedCounter.getString("counter_id");
					query = joSelectedCounter.getString("query");
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
						if( query.contains("#APP_NAME#") ) {
							query = query.replace("#APP_NAME#", strApp);
						}
						
						if( strExecutionType.equals("jboss-cli") ) {
							dCounterValue = 0.0;
							
							UtilsFactory.printDebugLog(Constants.IS_DEBUG, "query: "+query);
							for( String queryString_sub: query.split("#@@#") ) {
								UtilsFactory.printDebugLog(Constants.IS_DEBUG, "queryString_sub: "+queryString_sub);
								String[] strQuery_sub = queryString_sub.split("#@#");
								dCounterValue += convertToDouble( objStandaloneModel.getRuntimeDetails(client, strQuery_sub[0], strQuery_sub[1]) );
							}
							
						} else {
							strQuery = query.split("#@#");
							
							if(strQuery[0].trim().equalsIgnoreCase("RequestProcessor")) {
								
								// Avoid exception shown, since `getCounters` called even JMX not connected,
								if ( requestProcessorVal == null ) {
									dCounterValue = -1D;
									System.out.println("Unable to get the metric `"+strCounterId+"` due to "+strGlobalException);
									reportCounterError(Integer.parseInt(strCounterId), strGlobalException);
								} else {
									dCounterValue = convertToDouble(requestProcessorVal.get("result").get(strQuery[1]).asString());								
								}
								
							} else if (strQuery[0].trim().equalsIgnoreCase("Transactions")) {
								
								dCounterValue = convertToDouble(transactionsVal.get("result").get(strQuery[1]).asString());
							} else {
								
								ObjectName objectName = new ObjectName(strQuery[0]);
								dCounterValue = convertToDouble(connection.getAttribute(objectName, strQuery[1]));
							}
						}
					}
					
					if(bIsDelta) {
						dCounterValue = addDeltaCounterValue(Integer.parseInt(strCounterId), dCounterValue);					
					} else {
						addCounterValue(Integer.parseInt(strCounterId), dCounterValue);
					}
					UtilsFactory.printDebugLog(Constants.IS_DEBUG, "Counter: "+strCounterId+" = "+dCounterValue);
					
					// TODO: Static Counter correction required

					// Verify SLA Breach
					// JSONObject joSLACounter = null;
					ArrayList<JSONObject> joSLACounter = null; // Need to change variable name as alSLACounters
					joSLACounter = verifySLABreach(strGUID, SlaCounterBean.getSLACountersBean(strGUID), Integer.parseInt(strCounterId), dCounterValue);
					
					// if breached then add it to Collector's collection
					if( joSLACounter != null ) {
						addSlaCounterValue(joSLACounter);
					}

				} catch(Throwable th) {
					System.out.println("Exception in monitorJbossServer.counter:: "+strCounterId+" :: "+th.getMessage());
					System.out.println(strCounterId+" :: "+query);
					th.printStackTrace();
					reportCounterError(Integer.parseInt(strCounterId), th.getMessage());
				}
				
				strCounterId = null;
				strQuery = null;
				dCounterValue = 0D;
			}
		} catch(Throwable th) {
			System.out.println("Exception in monitorJbossServer: "+th.getMessage());
			th.printStackTrace();
			reportGlobalError(th.getMessage());
		} finally {
			try {
				queueCounterValues();
			} catch (Exception e) {
				System.out.println("Exception in queueCounterValues(): "+e.getMessage());
				e.printStackTrace();
			}
		}
	}
	
	/**
	 * Send the collected counter-sets to Collector WebService, by calling parent's sendCounter method
	 */
	private void sendCounters(String strGUID) {
		
		// send the collected counters to Collector WebService through parent sender function
		sendCounterToCollector(strGUID, AGENT_TYPE.JBOSS);
		sendSlaCounterToCollector(strGUID,AGENT_TYPE.JBOSS);
	}
	
	private Double convertToDouble(Object obj){
		Double dReturn = 0D;
		
		if( obj instanceof Long ) {
			dReturn = ((Long) obj).doubleValue();
		} else if( obj instanceof Integer ){
			dReturn = ((Integer) obj).doubleValue();
		} else if( obj instanceof Double ){
			dReturn = ((Double) obj).doubleValue();
		} else if ( obj instanceof String ) {
			dReturn = Double.parseDouble(obj.toString());
		}
		
		return dReturn;
	}
	
	/**
	 * List all applications which can be monitored under the given params (Host, Port).
	 * 
	 */
	public void listAllApplications() {
		try {
			createClientConnection(host, port, userid, password);
			
			if( standaloneMode == true ) {
				StandaloneModel objStandaloneModel = new StandaloneModel();
				
				objStandaloneModel.listAllApplications(client);
			}
		} catch (Throwable th) {
			System.out.println("Exception in listAllApplications: "+th.getMessage());
			th.printStackTrace();
		}
	}
	
	/**
	 * Print application statistics for the given application
	 * 
	 * @param strAppName
	 */
	public void monitorApplicationStatistics(String strAppName) {
		try {
			createClientConnection(host, port, userid, password);
			
			if( standaloneMode == true ) {
				StandaloneModel objStandaloneModel = new StandaloneModel();
				
				System.out.println("Monitoring the application: "+strAppName);
				objStandaloneModel.monitorApplicationStatistics(client, strAppName);
			}
		} catch (Throwable th) {
			System.out.println("Exception in monitorApplicationStatistics: "+th.getMessage());
			th.printStackTrace();
		}
	}
	
	/**
	 * Print the JNDI details
	 */
	public void printAllJbossCounters() {
		try {
			createClientConnection(host, port, userid, password);
			
			System.out.println(getDynamicJbossMetrics());
			
		} catch (Throwable th) {
			System.out.println("Exception in printDynamicJbossMetrics: "+th.getMessage());
			th.printStackTrace();
		}
	}
	
	/**
	 * Print the JNDI details
	 */
	public void printJNDIDetails() {
		try {
			createClientConnection(host, port, userid, password);
			
			if( standaloneMode == true ) {
				StandaloneModel objStandaloneModel = new StandaloneModel();
				
				objStandaloneModel.printJNDIDetails(client);
			}
		} catch (Throwable th) {
			System.out.println("Exception in printJNDIDetails: "+th.getMessage());
			th.printStackTrace();
		}
	}
	
	/**
	 * Load jboss_config.properties file, which is located near this JAR
	 * 
	 * @throws Exception
	 */
	public void loadConfigProperties() throws Exception {
		Properties prop = new Properties();
		
		try{
			InputStream is = new FileInputStream(Constants.THIS_JAR_PATH+File.separator+"jboss_config.properties");
			prop.load(is);
			
			host = prop.getProperty("HOST");
			port =Integer.parseInt(prop.getProperty("PORT"));
			userid = prop.getProperty("USERID");
			password = prop.getProperty("PASSWORD");
			
			if( prop.containsKey("VENDOR-SPECIFIC-PROTOCOL") ) {
				vendorSpecificProtocol = prop.getProperty("VENDOR-SPECIFIC-PROTOCOL");
			} else {
				vendorSpecificProtocol = "remoting-jmx";
			}
		} catch (Exception ex) {
			System.out.println("Exception in loadConfigProperties: "+ex.getMessage());
			throw ex;
		}
	}
	
	/**
	 * Close objects if exists
	 */
	private void closeObjects() {
		try{
			if (jmxConnector != null )	jmxConnector.close();
		} catch(Throwable th) {}
		
		try{
			if (client != null ) client.close();
		} catch(Throwable th) {}
	}
	
	@Override
	protected void finalize() throws Throwable {
		clearCounterMap();
		
		closeObjects();
		
		super.finalize();
	}
	
	/**
	 * JBoss might be configured with bound port, which could be different then Application-Port.
	 * This bound-port may be viewable in any admin page. So use this function to get its value.
	 * 
	 * To find the bound-port, PID is required. If PID is unknown then, find it with App-Port, through "netstat" command.
	 * 
	 * @param nPID
	 * @param strAppPort
	 */
	public void findBoundPort(Integer nPID, String strAppPort) {
		CommandLineExecutor cmdExe = null;
		StringBuilder sbOutput = null;
		
		String address = null, strCommand = null;
		JMXServiceURL jmxUrl = null;
		JMXConnector jmxConnector = null;
		MBeanServerConnection connection = null;
		
		VirtualMachine vm = null;
		HotSpotVirtualMachine hsvm = null;
		
		OSType osType = null;
		
		try {
			cmdExe = new CommandLineExecutor();
			
			// If PID is unknown then, find it with App-Port, through "netstat" command.
			if( nPID == null ) {
				System.out.println("Finding PID with Application's Port: "+strAppPort);
				
				osType = UtilsFactory.getOperatingSystemType();
				if( osType == OSType.Windows ) {
					strCommand = "netstat -a -n -o | findstr \""+strAppPort+"\" > _jboss_pid_appedo.txt && (for /f \"tokens=4,* delims= \" %a in (_jboss_pid_appedo.txt) do @echo %b) && del _jboss_pid_appedo.txt";
				} else if( osType == OSType.Linux ) {
					strCommand = "netstat -lpn | grep \""+strAppPort+"\" | awk '{print $7}' | cut -d \"/\" -f1";
				}
				
				System.out.println("Trying: "+strCommand+" ...");
				cmdExe.executeCommand(strCommand);
				if( (sbOutput = cmdExe.getErrors()) != null && sbOutput.length() > 0 ) {
					System.out.println("Error: "+sbOutput);
				}
				sbOutput = cmdExe.getOutput();
				
				nPID = Integer.parseInt(sbOutput.toString());
			}
			
			System.out.println("Connecting to PID: "+nPID+" ...");
			
			address = ConnectorAddressLink.importFrom(nPID);
			
			if( address == null ) {
				System.out.println("Trying to attach&execute HotSpotVirtualMachine...");
				vm = VirtualMachine.attach(nPID+"");
				hsvm = (HotSpotVirtualMachine) vm;
				hsvm.executeJCmd("ManagementAgent.start_local");
				address = ConnectorAddressLink.importFrom(nPID);
			}
			// System.out.println("address: "+address);
			
			jmxUrl = new JMXServiceURL(address);
			jmxConnector = JMXConnectorFactory.connect(jmxUrl);
			connection = jmxConnector.getMBeanServerConnection();
			
			int nBoundPort = (Integer) connection.getAttribute(new ObjectName("jboss.as:socket-binding-group=standard-sockets,socket-binding=management-native"), "boundPort");
			System.out.println("BoundPort: "+nBoundPort);
		} catch (Throwable th) {
			System.out.println("Exception in findBoundPort: "+th.getMessage());
			th.printStackTrace();
		} finally {
			try{
				if (jmxConnector != null )	jmxConnector.close();
			} catch(Throwable th) {}
		}
	}
}
