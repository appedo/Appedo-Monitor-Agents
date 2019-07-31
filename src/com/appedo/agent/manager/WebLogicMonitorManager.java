package com.appedo.agent.manager;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import javax.management.MBeanServerConnection;
import javax.management.ObjectName;
import javax.management.remote.JMXConnector;
import javax.management.remote.JMXConnectorFactory;
import javax.management.remote.JMXServiceURL;
import javax.naming.Context;

import net.sf.json.JSONArray;
import net.sf.json.JSONObject;

import com.appedo.agent.bean.AgentCounterBean;
import com.appedo.agent.bean.SlaCounterBean;
import com.appedo.agent.utils.Constants;
import com.appedo.agent.utils.Constants.AGENT_TYPE;
import com.appedo.agent.utils.Constants.WEBLOGIC_SERVICE;
import com.appedo.agent.utils.UtilsFactory;

/**
 * WebLogic monitoring class.
 * 
 * @author veeru
 *
 */
public class WebLogicMonitorManager extends AgentManager {
	
	public static WebLogicMonitorManager WebLogicMonitorManager = null;
	static String host = "localhost";
	static int port = 0;
	static String password = "";
	static String username = "";
	static String serverName = "AdminServer";
	static String JDBCServerName = "examples-demo";
	static String strQueryWorkManagerName = null;
	static String strAttributeName = null;
//	static ModelControllerClient client = null;
	static Map<String,ObjectName> catagoryVsConnObject = new HashMap<String,ObjectName>();
	ObjectName[] appRT = null, workManagerRT = null;
	//private static MBeanServerConnection connection;
	private static JMXConnector connector;
	private static ObjectName service;
	private static WEBLOGIC_SERVICE enumWeblogicServiceType; 
	
	MBeanServerConnection connection = null;
	
	/**
	 * Avoid the object creation for this Class from outside.
	 */
	private WebLogicMonitorManager() {
	}
	
	/**
	 * Returns the only object(singleton) of this Class.
	 * 
	 * @return
	 */
	public static WebLogicMonitorManager getWebLogicMonitorManager(){
		if( WebLogicMonitorManager == null ){
			WebLogicMonitorManager = new WebLogicMonitorManager();
		}
		
		return WebLogicMonitorManager;
	}
	
	/**
	 * Monitor the server and collect the counters
	 */
	public void monitorWebLogicServer(String strGUID,String strApp){
		
		try {
			loadConfigProperties();
			
			createAndCheckClientConnection();
			
			getCounters(strGUID,strApp);
			
		} catch (java.io.IOException ex) {
			System.out.println("\n\n Unable to Connect to the Host: "+host+" or Port:"+port+"\t\t" + ex.getMessage());
		} catch (Throwable th) {
			System.out.println("\n\nException in weblogicMoniter(): "+th.getMessage());
			th.printStackTrace();
		}
		try {
			if (connector != null ) connector.close();
		} catch (Throwable th) {
			System.out.println ("Exception in monitorWebLogicServer(): " + th.getMessage());
			th.printStackTrace();
		}
	}
	
	/**
	 * Send it to the Collector WebService
	 */
	public void sendWebLogicCounters(String strGUID){
		sendCounters(strGUID);
	}
	
	/**
	 * Collect the counter with agent's types own logic or native methods
	 */
	public void getCounters(String strGUID,String strApp){
		
		String strCounterId = null;
		String [] strQuery = null;
		Double dCounterValue = 0.0;
		ObjectName obj = null;
		// create variable's to capture execution_type & is_delta
		boolean bIsDelta = false;
		String strExecutionType = "";
		
		ArrayList<String> alCommandOutput = null;
		Set<String> hsJDBCServerNames = new HashSet<String>();
		
		try{
			// reset the counter collector variable in AgentManager.
			resetCounterMap(strGUID);
			
			// get selected config counters 
			JSONArray joSelectedCounters = AgentCounterBean.getCountersBean(strGUID);
			
			for(int i=0; i<joSelectedCounters.size(); i++){
				
				dCounterValue = null;
				JSONObject joSelectedCounter = joSelectedCounters.getJSONObject(i);				
				strCounterId = joSelectedCounter.getString("counter_id");
				String query = joSelectedCounter.getString("query");
				bIsDelta = joSelectedCounter.getBoolean("isdelta");
				strExecutionType = joSelectedCounter.getString("executiontype");
				
				if ( strExecutionType.equals("cmd") ) {
					// counter value from command
					alCommandOutput = CommandLineExecutor.execute(query);
					if ( alCommandOutput.size() == 0 ) {
						throw new Exception("Metric doesn't return value");
					}
					
					dCounterValue = Double.parseDouble(alCommandOutput.get(0));
				} else if( strExecutionType.equals("jmx") ) {
					strQuery = query.split("#@#");
					
					String strQueryJMXObjectName = strQuery[0];
					String strQueryAttribute = strQuery[1];
					
					strQueryJMXObjectName = strQueryJMXObjectName.replaceAll("#@SERVERNAME@#", serverName);
					strQueryJMXObjectName = strQueryJMXObjectName.replaceAll("#@JDBCSERVERNAME@#", serverName);
					
					dCounterValue = Double.parseDouble( String.valueOf( connection.getAttribute( new ObjectName(strQueryJMXObjectName), strQueryAttribute ) ) );
					
				} else {
					strQuery = query.split("#@#");
					
					String category = strQuery[0];
					if( ! category.equalsIgnoreCase("JDBCRUNTIME") && ! category.equalsIgnoreCase("WORKMANAGER_RUNTIME") ) {
						obj = catagoryVsConnObject.get(category);
						dCounterValue = Double.parseDouble( String.valueOf( connection.getAttribute(obj, strQuery[1]) ) );
					} else if( category.equalsIgnoreCase("WORKMANAGER_RUNTIME") ) {
						strQueryWorkManagerName = strQuery[1];
						strAttributeName = strQuery[2];
						boolean bWorkManagerNameExists = false;
						
						for (int x = 0; x < workManagerRT.length; x++) {
							String strWorkManagerName = (String)connection.getAttribute(workManagerRT[x], "Name");
							
							if( strWorkManagerName.equalsIgnoreCase(strQueryWorkManagerName) ) {
								dCounterValue = Double.parseDouble(String.valueOf(connection.getAttribute(workManagerRT[x], strAttributeName)));
								bWorkManagerNameExists = true;
								break;
							}
						}
						
						if( ! bWorkManagerNameExists ) {
							reportCounterError(Integer.parseInt(strCounterId), "WorkManager \""+strQueryWorkManagerName+"\" is not available.");
						}
					} else if( category.equalsIgnoreCase("JDBCRUNTIME") ) {
						boolean bJDBCServerNameExists = false;
						for (int x = 0; x < appRT.length; x++) {
							String JDBCName = (String)connection.getAttribute(appRT[x], "Name");
							
							if( JDBCServerName == null || JDBCServerName.length() == 0 ) {
								dCounterValue += Double.parseDouble(String.valueOf(connection.getAttribute(appRT[x], strQuery[1])));
							} else if( JDBCName.equalsIgnoreCase(JDBCServerName) ) {
								dCounterValue = Double.parseDouble(String.valueOf(connection.getAttribute(appRT[x], strQuery[1])));
								bJDBCServerNameExists = true;
								break;
							}
							// track JDBCServerName for availability
							else {
								hsJDBCServerNames.add(JDBCName);
							}
						}
						
						if( bJDBCServerNameExists == false ) {
							if ( hsJDBCServerNames.size() > 0 ) {
								System.out.println("JDBCServerName ("+strQueryWorkManagerName+") is unavailable.");
								System.out.println("Available sbJDBCServerNames: "+hsJDBCServerNames);
								System.out.println("Otherwise, leave it blank in proterties file. So values of all JDBCServerName will be summed-up.");
								
								throw new Exception("JDBCServerName ("+strQueryWorkManagerName+") is unavailable.");
							}
						}
					}
				}
				
				if( dCounterValue != null ) {
					if(bIsDelta) {
						dCounterValue = addDeltaCounterValue(Integer.parseInt(strCounterId), dCounterValue);					
					} else {
						addCounterValue(Integer.parseInt(strCounterId), dCounterValue);
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
				}
				
				strCounterId = null;
				strQuery = null;
				dCounterValue = 0.0;
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
	public void sendCounters(String strGUID) {
		
		// send the collected counters to Collector WebService through parent sender function
		sendCounterToCollector(strGUID, AGENT_TYPE.JBOSS);
		sendSlaCounterToCollector(strGUID, AGENT_TYPE.JBOSS);
	}
	
	public static void loadConfigProperties() throws Exception {
		Properties prop = new Properties();
		
		try{
			InputStream is = new FileInputStream(Constants.THIS_JAR_PATH+File.separator+"weblogic_config.properties");
			prop.load(is);
			host = prop.getProperty("HOST");
			port = Integer.parseInt(prop.getProperty("PORT"));
			username = prop.getProperty("USERNAME");
			password = prop.getProperty("PASSWORD");
			serverName = prop.getProperty("SERVERNAME");
			JDBCServerName = prop.getProperty("JDBCSERVERNAME");
			
			enumWeblogicServiceType = WEBLOGIC_SERVICE.valueOf( prop.getProperty("WEBLOGIC_SERVICE_TYPE") );
			
		} catch (Exception ex) {
			System.out.println("Exception in loadConfigProperties: "+ex.getMessage());
			throw ex;
		}
	}
	
	public void createAndCheckClientConnection() throws Throwable {
		String protocol = "t3";
		String jndiroot = "/jndi/";
		
		JMXServiceURL serviceURL = null;
		Hashtable<String, String> h = null;
		
		StringBuilder sbServerNames = new StringBuilder();
		
		// reset global variables
		UtilsFactory.clearCollectionHieracy(catagoryVsConnObject);
		appRT = null;
		
		try {
			if( Constants.PRINT_SYSOUT )
				System.out.println("starting C-C...");
			service = new ObjectName( enumWeblogicServiceType.getService() );
			if( Constants.PRINT_SYSOUT )
				System.out.println("service object created");
		} catch (Throwable th) {
			System.out.println("Exception while creating Service-Object: "+th.getMessage());
			throw th;
		}
		
		// NOTE: For hostname as "localhost" there is no need to pass the username & password  */
//		client = ModelControllerClient.Factory.create(host, port, null);
		// If you are running this program remotely then you need to pass the credentials */
		//client = createClient (InetAddress.getByName(host), port, userid, password.toCharArray(), "ManagementRealm" );
		//System.out.println("Got the client: "+client);
		
		serviceURL = new JMXServiceURL(protocol, host, port, jndiroot + enumWeblogicServiceType.getMServer());
		if( Constants.PRINT_SYSOUT )
			System.out.println("JMX-Service-URL is created.");
		h = new Hashtable<String, String>();
		h.put(Context.SECURITY_PRINCIPAL, username);
		h.put(Context.SECURITY_CREDENTIALS, password);
		h.put(JMXConnectorFactory.PROTOCOL_PROVIDER_PACKAGES, "weblogic.management.remote");
		connector = JMXConnectorFactory.connect(serviceURL, h);
		if( Constants.PRINT_SYSOUT )
			System.out.println("JMX Connect is created");
		connection = connector.getMBeanServerConnection();
		if( Constants.PRINT_SYSOUT )
			System.out.println("MBean Connection created");
		
		ObjectName[] serverRT = getServerRuntimes();
		int length = (int) serverRT.length;
		if( Constants.PRINT_SYSOUT )
			System.out.println("Server-RunTime length: "+serverRT.length);
		for (int i = 0; i < length; i++) {
			String name = (String) connection.getAttribute(serverRT[i], "Name");
			if( Constants.PRINT_SYSOUT )
				System.out.println("Available ServerName: "+name);
			
			if( name.equalsIgnoreCase(serverName) ) {
				ObjectName threadRT = (ObjectName) connection.getAttribute(serverRT[i], "ThreadPoolRuntime");
				ObjectName jvmRT = (ObjectName) connection.getAttribute(serverRT[i], "JVMRuntime");
				ObjectName jmsRuntime = (ObjectName) connection.getAttribute(serverRT[i], "JMSRuntime");
				ObjectName jtaRuntime = (ObjectName) connection.getAttribute(serverRT[i], "JTARuntime");
				if( Constants.PRINT_SYSOUT )
					System.out.println("Runtime objects created");
				
				workManagerRT = (ObjectName[]) connection.getAttribute(serverRT[i], "WorkManagerRuntimes");
				if( Constants.PRINT_SYSOUT )
					System.out.println("WorkManager-Runtime objects created");
				
				try{
					appRT = (ObjectName[]) connection.getAttribute(new ObjectName("com.bea:ServerRuntime="+name+",Name="+name+",Type=JDBCServiceRuntime"), "JDBCDataSourceRuntimeMBeans");
					if( Constants.PRINT_SYSOUT )
						System.out.println("JDBCDataSourceRuntimeMBeans created without location");
				} catch (Exception e) {
					appRT = (ObjectName[]) connection.getAttribute(new ObjectName("com.bea:Name="+name+",ServerRuntime="+name+",Location="+name+",Type=JDBCServiceRuntime"), "JDBCDataSourceRuntimeMBeans");
					if( Constants.PRINT_SYSOUT )
						System.out.println("JDBCDataSourceRuntimeMBeans created with location");
				}
				if( Constants.PRINT_SYSOUT )
					System.out.println("JDBCDataSourceRuntimeMBeans length "+appRT.length);
				
				catagoryVsConnObject.put("THREADPOOL", threadRT);
				catagoryVsConnObject.put("JVMRUNTIME", jvmRT);
				catagoryVsConnObject.put("JMSRUNTIME", jmsRuntime);
				catagoryVsConnObject.put("JTARUNTIME", jtaRuntime);
				//catagoryVsConnObject.put("JDBCRUNTIME", appRT);
			}
			// track ServerName for availability 
			else {
				sbServerNames.append(name).append(",");
			}
		}
		
		if( appRT == null ){
			if( sbServerNames.length() > 0 ) {
				sbServerNames.deleteCharAt( sbServerNames.length()-1 );
			}
			if( Constants.PRINT_SYSOUT )
				System.out.println("ServerName ("+serverName+") is unavailable.");
			if( Constants.PRINT_SYSOUT )
				System.out.println("Available ServerNames: "+sbServerNames);
			
			throw new Exception("ServerName ("+serverName+") is unavailable.");
		}
	}
	
	public ObjectName[] getServerRuntimes() throws Exception {
		Object obj = null;
		
		try{
			obj = connection.getAttribute(service, "ServerRuntimes");
			
		} catch (Throwable th) {
			System.out.println("Unable to retrive ServerRuntimes objects. So tring ServerRuntime object.");
			obj = connection.getAttribute(service, "ServerRuntime");
			obj = new ObjectName[]{ (ObjectName) obj};
		}
		if( Constants.PRINT_SYSOUT )
			System.out.println("ServerRuntime object created.");
		
		return (ObjectName[]) obj;
	}
	
	/*
	static ModelControllerClient createClient (final InetAddress host, final int port, final String username, final char[] password, final String securityRealmName) {
		
		final CallbackHandler callbackHandler = new CallbackHandler() {
			public void handle(Callback[] callbacks) throws IOException, UnsupportedCallbackException
			{
				for (Callback current : callbacks) {
					 if (current instanceof NameCallback) {
						NameCallback ncb = (NameCallback) current;
						System.out.println("\n\t\tncb.setName() = "+new String(password));
						ncb.setName(new String(password));
					} else if (current instanceof PasswordCallback) {
						PasswordCallback pcb = (PasswordCallback) current;
						System.out.println("\n\t\tpcb.setPassword() = "+username);
						pcb.setPassword(username.toCharArray());
					} else if (current instanceof RealmCallback) {
						RealmCallback rcb = (RealmCallback) current;
						System.out.println("\n\t\trcb.getDefaulttest() = "+rcb.getDefaultText());
						rcb.setText(securityRealmName);
					} else {
						throw new UnsupportedCallbackException(current);
					}
				}
			}
		};
		
		return ModelControllerClient.Factory.create(host, port, callbackHandler);
	}
	*/
	
	@Override
	protected void finalize() throws Throwable {
		clearCounterMap();
		
		super.finalize();
	}
}

