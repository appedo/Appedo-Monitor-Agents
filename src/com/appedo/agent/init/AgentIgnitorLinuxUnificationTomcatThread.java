package com.appedo.agent.init;

import java.util.HashMap;
import java.util.Timer;
import java.util.TimerTask;

import com.appedo.agent.manager.AgentManager;
import com.appedo.agent.manager.LogManagerExtended;
import com.appedo.agent.manager.TomcatMonitorManager;
import com.appedo.agent.timer.LinuxUnificationTomcatMonitorTimer;
import com.appedo.agent.utils.Constants;
import com.appedo.agent.utils.UtilsFactory;
import com.appedo.agent.utils.Constants.AGENT_TYPE;

import net.sf.json.JSONArray;
import net.sf.json.JSONObject;

public class AgentIgnitorLinuxUnificationTomcatThread extends Thread{

	AgentManager am = null;
	//Timer timerLinuxUnificationServerManager = null;
	TomcatMonitorManager tmManager = null;
	String JMX_PORT;
	
	boolean isAppServerRunning;
	
	public AgentIgnitorLinuxUnificationTomcatThread(String JmxPort) {
		am = new AgentManager();
		tmManager = new TomcatMonitorManager();
		JMX_PORT = JmxPort;
		start();
		// TODO Auto-generated constructor stub
	}

	public void run() {
		
		HashMap<String, JSONArray> hmCounterSet = null;
		
		String moduleGUID = "";
		
		JSONObject joModuleGUIDs = new JSONObject();
		JSONObject joAppInformation = null;
		JSONObject joAppNewCounterSet = new JSONObject();
		
		try {
			UtilsFactory.printDebugLog(Constants.IS_DEBUG, "Thread started for JMX port : "+JMX_PORT);
			LogManagerExtended.applicationInfoLog("Linux Unification Tomcat thread started. for JMX_PORT: "+JMX_PORT);
			
			//this.isAppServerRunning = LinuxMonitorManager.getLinuxMonitorManager().TomcatServerStatus();
			
			//if(this.isAppServerRunning) {
				tmManager.connectTomcatJMXObject(JMX_PORT);
				tmManager.getAllProjectNames(JMX_PORT);
				hmCounterSet = tmManager.getApplicationDynamicCounters(JMX_PORT);
				joAppInformation = tmManager.getModuleTypeVersion();
				
				for(String AppName : Constants.TOMCAT_APP_LIST.get(JMX_PORT)) {
					joAppNewCounterSet.put("counterData", hmCounterSet.get(AppName));
					UtilsFactory.printDebugLog(Constants.IS_DEBUG, "Counter Set "+AppName+" : "+hmCounterSet.get(AppName));
					joAppInformation.put("moduleName", AppName);
					joAppInformation.put("jmx_port", JMX_PORT);
					
					UtilsFactory.printDebugLog(Constants.IS_DEBUG, "Application Information : "+joAppInformation.toString());
					LogManagerExtended.applicationInfoLog("Application Information : "+joAppInformation.toString());
					UtilsFactory.printDebugLog(Constants.IS_DEBUG, "AppInformation data send to collector for card creation/fetching of "+AppName+"::"+JMX_PORT);
					moduleGUID = am.sendModuleInfoToCollector(joAppInformation, Constants.SYSTEM_ID, Constants.SYS_UUID, joAppNewCounterSet, "appInformation");
					UtilsFactory.printDebugLog(Constants.IS_DEBUG, "Responded from collector for "+AppName+"::"+JMX_PORT);
					if(!moduleGUID.isEmpty()) {
						UtilsFactory.printDebugLog(Constants.IS_DEBUG, "Card created/fetched for "+AppName+"::"+JMX_PORT+", GUID: "+moduleGUID);
						joModuleGUIDs.put(AppName, moduleGUID);
						moduleGUID = "";
					}
				}
				
				joAppNewCounterSet.put("counterData", hmCounterSet.get("Generic"));
				
				joAppInformation.put("moduleName", joAppInformation.getString("moduleTypeName"));
				joAppInformation.put("jmx_port", JMX_PORT);
				UtilsFactory.printDebugLog(Constants.IS_DEBUG, "Application Information : "+joAppInformation.toString());
				LogManagerExtended.applicationInfoLog("Application Information : "+joAppInformation.toString());
				UtilsFactory.printDebugLog(Constants.IS_DEBUG, "AppInformation data send to collector for card creation/fetching: Generic");
				moduleGUID = am.sendModuleInfoToCollector(joAppInformation, Constants.SYSTEM_ID, Constants.SYS_UUID, joAppNewCounterSet, "appInformation");
				UtilsFactory.printDebugLog(Constants.IS_DEBUG, "Responded from collector for Generic");
				if(!moduleGUID.isEmpty()) {
					UtilsFactory.printDebugLog(Constants.IS_DEBUG, "Card created/fetched for Generic, GUID: "+moduleGUID);
					joModuleGUIDs.put("Generic", moduleGUID);
					moduleGUID = "";
				}
				
				LogManagerExtended.applicationInfoLog("All Application module is created...");
				UtilsFactory.printDebugLog(Constants.IS_DEBUG, "All Application module created.");
				UtilsFactory.printDebugLog(Constants.IS_DEBUG, "All GUIDs : "+joModuleGUIDs.toString());
				LogManagerExtended.applicationInfoLog("List of GUID's...");
				LogManagerExtended.applicationInfoLog(joModuleGUIDs.toString());
				LogManagerExtended.applicationInfoLog("==========================creating Process completed========================");
				
				for(Object Key : joModuleGUIDs.keySet()) {
					String Guid = joModuleGUIDs.getString((String)Key);
					
					try {
						if(am.getConfigurationsFromCollector(Guid, AGENT_TYPE.TOMCAT.toString())) {
							UtilsFactory.printDebugLog(Constants.IS_DEBUG, "Data collection started for GUID : "+Guid);
							Timer timerLinuxUnificationApplicationManager = new Timer();
							TimerTask ttLinuxUnificationServer = new LinuxUnificationTomcatMonitorTimer(timerLinuxUnificationApplicationManager, Guid, JMX_PORT);
							timerLinuxUnificationApplicationManager.schedule(ttLinuxUnificationServer, 100l, Constants.MONITOR_FREQUENCY_MILLESECONDS);
						}
					}catch (Throwable e) {
						LogManagerExtended.applicationInfoLog(e.getMessage());
						e.printStackTrace();
					}
				}
				
			//}
			
		}catch (Exception e) {
			System.out.println(e);
			// TODO: handle exception
		}
	}
	
	public static void main(String[] args) {
		// TODO Auto-generated method stub
	}

}
