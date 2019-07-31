package com.appedo.agent.init;

import java.io.File;

import com.appedo.agent.manager.LinuxMonitorManager;
import com.appedo.agent.manager.LogManagerExtended;
import com.appedo.agent.utils.Constants;
import com.appedo.agent.utils.UtilsFactory;
import com.appedo.manager.LogManager;

/**
 * Starting class for JavaUnification agent.
 * This class starts the timer, which monitors the Linux OS, DB, Services and retrieves the counters & values.
 * 
 * @author Siddiq
 *
 */
public class AgentIgnitorLinuxUnification {

	public static void main(String[] args) {
		// TODO Auto-generated method stub

		try{
			// load config parameters
			Constants.loadConfigProperties();
			
			String strLog4jExtPropertiesFile = Constants.THIS_JAR_PATH + File.separator + "log4j_linuxUnified.properties";
			//String strLog4jPropertiesFile = Constants.THIS_JAR_PATH + File.separator + "log4j.properties";
			
			System.out.println("strLog4jPropertiesFile: "+strLog4jExtPropertiesFile);
			
			LogManager.initializePropertyConfigurator(strLog4jExtPropertiesFile);
			
			LogManagerExtended.initializePropertyConfigurator(strLog4jExtPropertiesFile);
			
			Constants.IS_DEBUG = UtilsFactory.contains(args, "--debug");
			
			if (Constants.IS_DEBUG) {
				System.out.println("Agent started in Debug mode.");
			}
			
			if( Constants.ENCRYPTED_ID!=null ) {				
				
				LinuxMonitorManager.getLinuxMonitorManager().getSystemInformation1();
				
			} else {
				System.out.println("Configuration problem");
			}			
		} catch(Throwable e) {
			System.out.println("Exception in InitServlet.init: "+e.getMessage());
			e.printStackTrace();
		}
	}

}
