package com.appedo.agent.manager;

import org.apache.log4j.Logger;

import com.appedo.manager.LogManager;

public class LogManagerExtended extends LogManager {

	/** Logger to handle the Informations/Debugs */
	static Logger jstackLog = null;
	static Logger serverInfoLog = null;
	static Logger applicationInfoLog = null;
	static Logger dataBaseInfoLog = null;
	
	/**
	 * Initialize the log4j properties. 
	 * And create the info & error logs.
	 */
	public static void initializePropertyConfigurator(String strLog4jPropertiesFile) {
		
		jstackLog = Logger.getLogger("jstackLogger");
		serverInfoLog = Logger.getLogger("serverLogger");
		applicationInfoLog = Logger.getLogger("applicationLogger");
		dataBaseInfoLog = Logger.getLogger("databaseLogger");
	}
	
	/**
	 * Write the JStack info into jstack.log
	 * 
	 * @param strInfo
	 */
	public static void logJStackOutput(String strInfo) {
		
		jstackLog.info(strInfo);
	}
	
	/**
	 * Write the LinuxUnification server level info into serverInfoLog.log
	 * 
	 * @param strinfo
	 */
	
	public static void serverInfoLog(String strinfo) {
		serverInfoLog.info(strinfo);
	}
	
	/**
	 * Write the LinuxUnification application level info into applicationInfoLog.log
	 * 
	 * @param strinfo
	 */
	
	public static void applicationInfoLog(String strinfo) {
		applicationInfoLog.info(strinfo);
	}

	/**
	 * Write the LinuxUnification database level info into databaseInfoLog.log
	 * 
	 * @param strinfo
	 */
	
	public static void databaseInfoLog(String strinfo) {
		dataBaseInfoLog.info(strinfo);
	}
	
	/**
	 * Write the LinuxUnification application, Server & DataBase level information into the dependent log
	 * 
	 * @param strinfo
	 */	
	public static void commonInfoLog(String strinfo, String module) {
		if(module.equalsIgnoreCase("serverInformation")) {
			serverInfoLog.info(strinfo);
		}else if(module.equalsIgnoreCase("appInformation")){
			applicationInfoLog.info(strinfo);
		}else if(module.equalsIgnoreCase("DBInformation")){
			dataBaseInfoLog.info(strinfo);
		}
	}
}
