package com.appedo.agent.utils;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Properties;

import org.apache.commons.lang.math.NumberUtils;

/**
 * This class holds the application level variables which required through the application.
 *
 * @author Ramkumar R
 *
 */
public class Constants {

	// milliseconds unit
	public final static long FIVE_MINUTE_MILLISECONDS = 1000 * 60 * 5;
	public static long COMPARISON_FREQUENCY_MILLESECONDS = 1000 * 60 * 60 * 1;
	
	public static String MYSQL_AGENT_GUID = null;
	public static String LINUX_AGENT_GUID = null;
	// public static String TOMCAT_AGENT_GUID = null;
	public static String JBOSS_AGENT_GUID = null;
	public static String APACHE_AGENT_GUID = null;
	public static String JSTACK_AGENT_GUID = null;
	
	public static String MYSQL_AGENT_UID = null;
	public static String LINUX_AGENT_UID = null;
	public static String TOMCAT_AGENT_UID = null;
	public static String JBOSS_AGENT_UID = null;
	
	public static String WEBSERVICE_URL = null;
	
	// public static String APPLICATION_NAME = null;
	// public static String TOMCAT_PORT = null;
	public static String strAgentVersion = null;
	
	// run time configurations
	private static String strUID = null;
	
	public static AGENT_TYPE this_agent = null;
	public static String THIS_JAR_PATH = null;
	public static OSType THIS_OS_TYPE = null;
	
	public static long MONITOR_FREQUENCY_MILLESECONDS = 0; // in milliseconds
	public static int MONITOR_FREQUENCY = 0; // in integer
	public static Boolean IS_COUNTERS_SET = false;
	
	public static String URL_MONITOR_FREQUENCY;
	public static String AGENT_CONFIG;
	public static String ASD_DETAILS;
	public static boolean PRINT_SYSOUT = false;
	public static boolean IS_DEBUG = false;
	public static boolean APP_INSTALL_COMPARE_MODE = false;
	public static boolean FILE_COMPARE_MODE = false;
	public static String[] FILE_PATH;
	
	public static String ENCRYPTED_ID;
	public static String ENTERPRISE_ID;
	public static String VMWARE_KEY;
	public static boolean IS_VMWARE_KEY = false;
	public static String SYS_UUID;
	public static String SYS_MANUFACTURER;
	public static String SYS_PRODUCTNAME;
	public static String SYS_SERIALNUMBAR;
	public static String SYSTEM_ID;
	public static String TOMCAT_JMXPORT;
	public static String JBOSS_JMXPORT = "";
	public static String JBOSS_HOST= "localhost";
	public static String[] NON_MONITORING_APP_LIST;
	public static ArrayList<String> APPLICATION_LIST = new ArrayList<String>();
	public static HashMap<String, HashSet<String>> TOMCAT_APP_LIST = new HashMap<String, HashSet<String>>();
	public static String HOST_OBJECT_NAME= "Catalina:type=Host,host=localhost";
	public static String TOMCAT_GREP_KEY = "ps -eaf | grep org.apache.catalina.startup.Bootstrap";
	public static String JBOSS_GREP_KEY = "ps -eaf | grep org.jboss.as";
	public static String POSTGRES_GREP_KEY = "sudo service --status-all | grep 'postgres\\|pg_ctl'";
	public static String TOMCAT_JMX_CONNECTOR_URL = "service:jmx:rmi:///jndi/rmi://localhost:#@#JMX_PORT#@#/jmxrmi";
	public static String USER_HOME_DIR = null;
	public static String JBOSS_USER_ID = null;
	public static String JBOSS_PASSWORD = null;
	public static String POSTGRES_CONTAIN_KEY = null;
	
	
	public static String TOP_PROCESS_QUERY = "";
	
	public static String LINUX_SERVER_MODULE_TYPE = "";
	//public static String LINUX_APP_MODULE_TYPE = "";
	//public static String LINUX_DB_MODULE_TYPE = "";
	public static ArrayList<String> JBOSS_PORTS = new ArrayList<String>();
	public static ArrayList<String> TOMCAT_PORTS = new ArrayList<String>();
	
	// prefix for APPEDO queries used for monitors, since to avoid APPEDO query in slow queries
	public static final String QUERY_COMMENT_PREFIX = "/* APPEDO */ ";
	public static boolean COLLECT_SLOW_QUERY = true;
	public static boolean OMIT_AGENT_QUERIES_IN_SLOW_QUERIES = true;
	
	public static String ORCL_SQ_QUERY = null;
	
	public static int SLOW_QUERY_READ_FREQUENCY_MILLISECONDS = 20*1000;
	
	public static boolean ORCL_SQ_DISABLE_DBAVIEW_VALIDATION = true;
	
	public static String[] JSTACK_EXCLUDE_PACKAGES = new String[]{};
	
	public static int SLEEP_BETWEEN_LOOP_MILLISECONDS = 200;
	
	public enum COMMAND_LINE_OPTIONS {
		LIST_APPLICATIONS_1("-la"),
		LIST_APPLICATIONS_2("--list-applications"),
		APPLICATION_STATISTICS_1 ("-as"),
		APPLICATION_STATISTICS_2 ("--application-statistics"),
		PRINT_JNDI_DETAILS_1 ("-jd"),
		PRINT_JNDI_DETAILS_2 ("--jndi-details"),
		GET_BOUND_PORT ("--get-bound-port"),
		PRINT_ALL_COUNTERS ("--print-all-counters");
		
		private String strOption;
		
		private COMMAND_LINE_OPTIONS(String sOption) {
			strOption = sOption;
		}
		
		public String toString() {
			return strOption;
		}
	}
	
	public enum SLA_BREACH_SEVERITY {
		WARNING("WARNING"), CRITICAL("CRITICAL");
		
		private String strSLABreachSeverity;
		
		private SLA_BREACH_SEVERITY(String sSLABreachSeverity) {
			strSLABreachSeverity = sSLABreachSeverity;
		}
		
		public String toString() {
			return strSLABreachSeverity;
		}
	}
	
	public enum WEBLOGIC_SERVICE {
		DOMAIN_RUNTIME_SERVICE("DOMAIN_RUNTIME_SERVICE", "com.bea:Name=DomainRuntimeService,Type=weblogic.management.mbeanservers.domainruntime.DomainRuntimeServiceMBean", "weblogic.management.mbeanservers.domainruntime"), 
		RUNTIME_SERVICE("RUNTIME_SERVICE", "com.bea:Name=RuntimeService,Type=weblogic.management.mbeanservers.runtime.RuntimeServiceMBean", "weblogic.management.mbeanservers.runtime"),
		JDBC_DATASOURCE_RUNTIME_SERVICE("JDBC_DATASOURCE_RUNTIME_SERVICE", "", "weblogic.management.runtime.JDBCDataSourceRuntimeMBean");
		
		private String strRuntimeType, strService, strMServer;
		
		private WEBLOGIC_SERVICE (String sRuntimeType, String sService, String sMServer) {
			strRuntimeType = sRuntimeType;
			strService = sService;
			strMServer = sMServer;
		}
		
		public String getService() {
			return strService;
		}
		
		public String getMServer() {
			return strMServer;
		}
		
		public String toString() {
			return strRuntimeType;
		}
	}
	
	/**
	 * types of Operating Systems
	 */
	public enum OSType {
		Windows, MacOS, Linux, Other
	};
	
	// public static Map<Integer, JSONObject> map = new HashMap<Integer, JSONObject>();
	/**
	 * Returns the UID assigned to this Agent.
	 * 
	 * @return
	 */
	public static String getUID() {
		return strUID;
	}

	/**
	 * Sets a UID to this Agent.
	 * 
	 * @param strParamUID
	 */
	public static void setUID(String strParamUID) {
		strUID = strParamUID;
	}

	/**
	 * Enum which has all the agent types available.
	 * 
	 * @author Ramkumar R
	 * 
	 */
	public enum AGENT_TYPE {
		LINUX("LINUX"), 
		TOMCAT("TOMCAT"), JBOSS("JBOSS"), APACHE("APACHE"), GLASSFISH("GLASSFISH"), WEBLOGIC("WEBLOGIC"), 
		JAVA_PROFILER("JAVA_PROFILER"), JSTACK("JSTACK"), 
		MYSQL("MYSQL"), POSTGRES("POSTGRES"), ORACLE("ORACLE"), 
		BEAT("BEAT");

		private String strAgentType;

		private AGENT_TYPE(String agentType) {
			strAgentType = agentType;
		}

		public void setMySQLVersion(String strVersionNo) {
			strAgentType = "MYSQL " + strVersionNo;
		}

		public String toString() {
			return strAgentType;
		}
	}

	/**
	 * Load the configuration details required for this agent like,
	 * WebService URL, Tomcat port numbers for Tomcat Agent and others.
	 * @throws Throwable 
	 */
	public static void loadConfigProperties() throws Throwable {
		Properties prop = new Properties();
		InputStream isFile = null;
		
		try {
			THIS_JAR_PATH = UtilsFactory.getThisJarPath();
			//THIS_JAR_PATH ="F:/Siddiq/workspace/Appedo-Monitor-Agents/";
			System.out.println("Agent Path: " + THIS_JAR_PATH);
			
			THIS_OS_TYPE = UtilsFactory.getOperatingSystemType();
			
			isFile = new FileInputStream(Constants.THIS_JAR_PATH + File.separator + "config.properties");
			prop.load(isFile);
			
			WEBSERVICE_URL = prop.getProperty("WEBSERVICE_URL");
			ASD_DETAILS = prop.getProperty("ASD_DETAILS");
			AGENT_CONFIG = prop.getProperty("AGENT_CONFIG");
			
			if( prop.getProperty("COLLECT_SLOW_QUERY") != null ) {
				COLLECT_SLOW_QUERY = Boolean.parseBoolean( prop.getProperty("COLLECT_SLOW_QUERY") );
			}
			
			if( prop.getProperty("OMIT_AGENT_QUERIES_IN_SLOW_QUERIES") != null ) {
				OMIT_AGENT_QUERIES_IN_SLOW_QUERIES = Boolean.parseBoolean( prop.getProperty("OMIT_AGENT_QUERIES_IN_SLOW_QUERIES") );
			}
			
			if (prop.getProperty("HOST_OBJECT_NAME") != null) {
				HOST_OBJECT_NAME = prop.getProperty("HOST_OBJECT_NAME");
			}
			//JMX_CONNECTOR_URL
			if (prop.getProperty("JMX_CONNECTOR_URL") != null) {
				TOMCAT_JMX_CONNECTOR_URL = prop.getProperty("JMX_CONNECTOR_URL");
			}
			
			if (prop.getProperty("USER_HOME_DIR") != null) {
				USER_HOME_DIR = prop.getProperty("JMX_CONNECTOR_URL");
			}
			if( prop.getProperty("ORCL_SQ_QUERY") != null ) {
				ORCL_SQ_QUERY = prop.getProperty("ORCL_SQ_QUERY");
			}
			if( prop.getProperty("TOMCAT_GREP_KEY") != null ) {
				TOMCAT_GREP_KEY = prop.getProperty("TOMCAT_GREP_KEY");
			}
			if( prop.getProperty("JBOSS_GREP_KEY") != null ) {
				JBOSS_GREP_KEY = prop.getProperty("JBOSS_GREP_KEY");
			}
			if( prop.getProperty("POSTGRES_GREP_KEY") != null ) {
				POSTGRES_GREP_KEY = prop.getProperty("POSTGRES_GREP_KEY");
			}
			if( prop.getProperty("POSTGRES_CONTAIN_KEY") != null ) {
				POSTGRES_CONTAIN_KEY = prop.getProperty("POSTGRES_CONTAIN_KEY");
			}
			if( prop.getProperty("JBOSS_HOST") != null ) {
				JBOSS_HOST = prop.getProperty("JBOSS_HOST");
			}
			if( prop.getProperty("JBOSS_JMXPORT") != null ) {
				JBOSS_JMXPORT = prop.getProperty("JBOSS_JMXPORT");
			}
			if (prop.getProperty("JBOSS_USER_ID") != null) {
				JBOSS_USER_ID = prop.getProperty("JBOSS_USER_ID");
			}
			if (prop.getProperty("JBOSS_PASSWORD") != null) {
				JBOSS_PASSWORD = prop.getProperty("JBOSS_PASSWORD");
			}
			if( prop.getProperty("SLOW_QUERY_READ_FREQUENCY_SECONDS") != null ) {
				SLOW_QUERY_READ_FREQUENCY_MILLISECONDS = Integer.parseInt( prop.getProperty("SLOW_QUERY_READ_FREQUENCY_SECONDS") )*1000;
			}
			if( prop.getProperty("ORCL_SQ_DISABLE_DBAVIEW_VALIDATION") != null ) {
				ORCL_SQ_DISABLE_DBAVIEW_VALIDATION = Boolean.parseBoolean( prop.getProperty("ORCL_SQ_DISABLE_DBAVIEW_VALIDATION") );
			}
			
			if( prop.getProperty("JSTACK_EXCLUDE_PACKAGES") != null ) {
				JSTACK_EXCLUDE_PACKAGES = prop.getProperty("JSTACK_EXCLUDE_PACKAGES").split(",");
			}
			
			if( prop.getProperty("SLEEP_BETWEEN_LOOP_MILLISECONDS") != null ) {
				SLEEP_BETWEEN_LOOP_MILLISECONDS = Integer.parseInt( prop.getProperty("SLEEP_BETWEEN_LOOP_MILLISECONDS") );
				if( SLEEP_BETWEEN_LOOP_MILLISECONDS < 200 ) {
					SLEEP_BETWEEN_LOOP_MILLISECONDS = 200;
				}
			}
			
			
			// MONITOR_FREQUENCY_MILLESECONDS = getMonitorFrequency();
			MONITOR_FREQUENCY_MILLESECONDS = Long.parseLong(UtilsFactory.replaceNull(prop.getProperty("MONITOR_FREQUENCY_MILLESECONDS"), "20000"));
			
			if ( prop.containsKey("PRINT_SYSOUT") ) {
				PRINT_SYSOUT = Boolean.parseBoolean( prop.getProperty("PRINT_SYSOUT") );
			}
			
			//compare file process 
			APP_INSTALL_COMPARE_MODE = Boolean.parseBoolean(prop.getProperty("INSTALLED_APP_COMPARE_MODE"));
			FILE_COMPARE_MODE = Boolean.parseBoolean(prop.getProperty("FILE_COMPARE_MODE"));
			if(prop.containsKey("FILE_PATH")) {
				FILE_PATH = prop.getProperty("FILE_PATH").split(",");
			}
			
			//LinuxUnification variables
			if(prop.containsKey("ENCRYPTED_ID")) {
				ENCRYPTED_ID = prop.getProperty("ENCRYPTED_ID");
				ENTERPRISE_ID = prop.getProperty("eid");
				VMWARE_KEY = prop.getProperty("VMWARE_KEY");
				if(VMWARE_KEY != null) {
					IS_VMWARE_KEY = true;
				}
				
				SYS_UUID = prop.getProperty("UUID");
				SYS_MANUFACTURER = prop.getProperty("MANUFACTURER");
				SYS_PRODUCTNAME = prop.getProperty("SYSTEM_NAME");
				SYS_SERIALNUMBAR = prop.getProperty("SYSTEM_NUMBER");
				
				//ApplicationModule
				
				if ( prop.containsKey("NON_MONITORING_APP_LIST") ) {
					NON_MONITORING_APP_LIST = prop.getProperty("NON_MONITORING_APP_LIST").split(",");
				}
				
			}
			
			String GET_INSTAL_LIST_FREQUENCY_HOUR = prop.getProperty("GET_INSTAL_LIST_FREQUENCY_HOUR");
			
			if(NumberUtils.isDigits(GET_INSTAL_LIST_FREQUENCY_HOUR) && Integer.parseInt(GET_INSTAL_LIST_FREQUENCY_HOUR) > 0){
				COMPARISON_FREQUENCY_MILLESECONDS = 1000 * 60 * 60 * Integer.parseInt( prop.getProperty("GET_INSTAL_LIST_FREQUENCY_HOUR"));
			}
			
			
		} catch (Exception ex) {
			System.out.println("Exception in loadConfigProperties: " + ex.getMessage());
			throw ex;
		} finally {
			prop.clear();
			prop = null;
			
			isFile.close();
			isFile = null;
		}
	}
	
	/**
	 * 
	 * @return
	 * @throws Throwable
	 */
	public static long getMonitorFrequency() throws Throwable {
		long lMonitorFrequency = 0l;

		try {
			// creates a HTTP connection
			URL url = new URL(Constants.URL_MONITOR_FREQUENCY);

			URLConnection urlConn = url.openConnection();
			urlConn.setUseCaches(false);
			String data = "command=frequency";
			OutputStreamWriter wr = new OutputStreamWriter(urlConn.getOutputStream());
			wr.write(data);
			wr.flush();
			lMonitorFrequency = Long.parseLong(urlConn.getHeaderField("frequency"));
		} catch (Throwable t) {
			System.out.println("Exception in getMonitorFrequency()" + t.getMessage());
		} finally {

		}

		return lMonitorFrequency;
	}

	/**
	 * Writes the UID of this Agent in the configuration file.
	 * 
	 * @param agent_Type
	 * @param strUID
	 * @throws Exception
	 *
	public static void updateUIDInConfigProperties(AGENT_TYPE agent_Type, String strUID) throws Exception {
		Properties prop = new Properties();
		InputStream isFile = null;
		OutputStream outFile = null;

		try {
			isFile = new FileInputStream(Constants.THIS_JAR_PATH + File.separator + "config.properties");
			prop.load(isFile);

			if (agent_Type == AGENT_TYPE.MYSQL) {
				prop.setProperty("MYSQL_UID", strUID);
			} else if (agent_Type == AGENT_TYPE.LINUX) {
				prop.setProperty("LINUX_UID", strUID);
			} else if (agent_Type == AGENT_TYPE.TOMCAT) {
				prop.setProperty("TOMCAT_UID", strUID);
			} else if (agent_Type == AGENT_TYPE.JBOSS) {
				prop.setProperty("JBOSS_UID", strUID);
			}

			outFile = new FileOutputStream(Constants.THIS_JAR_PATH + File.separator + "config.properties");

			System.out.println("Storing the UID in config file");
			prop.store(outFile, null);
		} catch (Exception ex) {
			System.out.println("Exception in loadConfigProperties: " + ex.getMessage());
			throw ex;
		} finally {
			isFile.close();
			isFile = null;

			prop.clear();
			prop = null;

			outFile.close();
			outFile = null;
		}
	}*/
}
