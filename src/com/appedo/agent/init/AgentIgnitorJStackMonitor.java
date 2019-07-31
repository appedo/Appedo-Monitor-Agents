package com.appedo.agent.init;

import java.io.File;

import com.appedo.agent.manager.LogManagerExtended;
import com.appedo.agent.utils.Constants;
import com.appedo.manager.LogManager;

import net.sf.json.JSONArray;
import net.sf.json.JSONObject;

/**
 * Starting class for JStack agent.
 * This class starts the timer, which monitors the JVM and retrieves all Threads StackTrace.
 * 
 * @author Ramkumar
 *
 */
public class AgentIgnitorJStackMonitor {
	
	public static void main(String[] args) {
		JSONArray jaConfig = null;
		JSONObject joRecord = null;
		
		try{
			// load config parameters
			Constants.loadConfigProperties();
			
			String strLog4jPropertiesFile = Constants.THIS_JAR_PATH + File.separator + "log4j.properties";
			System.out.println("strLog4jPropertiesFile: "+strLog4jPropertiesFile);
			
			LogManager.initializePropertyConfigurator(strLog4jPropertiesFile);
			
			LogManagerExtended.initializePropertyConfigurator(strLog4jPropertiesFile);
			
			if( Constants.AGENT_CONFIG != null ) {
				jaConfig = JSONArray.fromObject(Constants.AGENT_CONFIG);
				joRecord = jaConfig.getJSONObject(0);
				
				Constants.JSTACK_AGENT_GUID = joRecord.getString("guid");
				//System.out.println(Constants.JSTACK_AGENT_GUID);
				
				new AgentIgnitorJStackThread(joRecord.getString("server_alias"));
				
			} else {
				System.out.println("Configuration problem");
			}
			//AgentManager.runGarbageCollectionRountine();
			
			/*//Start compare File code
			//this code create file and insert AmazonS3 
			System.out.println("calling compare file manager java class ");
			CompareFileManager cmpfile = new CompareFileManager();
			cmpfile.getInstallFile();*/
			
		} catch(Throwable e) {
			System.out.println("Exception in InitServlet.init: "+e.getMessage());
			e.printStackTrace();
		}
	}
}