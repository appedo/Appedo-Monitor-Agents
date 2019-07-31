package com.appedo.agent.init;

import java.util.Timer;
import java.util.TimerTask;

import com.appedo.agent.manager.AgentManager;
import com.appedo.agent.manager.JbossMonitorManager;
import com.appedo.agent.manager.LogManagerExtended;
import com.appedo.agent.timer.LinuxUnificationJbossMonitorTimer;
import com.appedo.agent.utils.Constants;
import com.appedo.agent.utils.Constants.AGENT_TYPE;
import com.appedo.agent.utils.UtilsFactory;

import net.sf.json.JSONObject;

public class AgentIgnitorLinuxUnificationJbossThread extends Thread{

	JbossMonitorManager jbossMonitorManager = null;
	AgentManager am = null;
	
	public AgentIgnitorLinuxUnificationJbossThread() {
		am = new AgentManager();
		start();
	}

	public void run() {
		
		boolean isJMXConnected;
		String moduleGUID = "";
		
		try {
			
			jbossMonitorManager = JbossMonitorManager.getJbossMonitorManager();
			
			UtilsFactory.printDebugLog(Constants.IS_DEBUG, "JBOSS Object is creted..."+ jbossMonitorManager);
			
			isJMXConnected = jbossMonitorManager.createJBossJMXConnection();
			
			UtilsFactory.printDebugLog(Constants.IS_DEBUG, "JBOSS connectted. -->"+isJMXConnected);
			
			if(isJMXConnected) {
			
				JSONObject joNewCounterSet = jbossMonitorManager.getDynamicJbossMetrics();
				
				UtilsFactory.printDebugLog(Constants.IS_DEBUG, "JBOSS Dynamic counters is getting.");
				
				JSONObject joModuleInfo = jbossMonitorManager.getJbossServerDetails();
								
				moduleGUID = am.sendModuleInfoToCollector(joModuleInfo, Constants.SYSTEM_ID, Constants.SYS_UUID, joNewCounterSet, "appInformation");
				
				LogManagerExtended.applicationInfoLog("ModuleGUID : "+ moduleGUID);
				
				if(!moduleGUID.isEmpty()) {
					
					Constants.JBOSS_AGENT_GUID = moduleGUID;
					
					if(am.getConfigurationsFromCollector(Constants.JBOSS_AGENT_GUID, AGENT_TYPE.TOMCAT.toString())) {
						Timer timerLinuxUnificationJbossObj = new Timer();
						TimerTask ttLinuxUnificationJboss = new LinuxUnificationJbossMonitorTimer(timerLinuxUnificationJbossObj);
						timerLinuxUnificationJbossObj.schedule(ttLinuxUnificationJboss, 100l, Constants.MONITOR_FREQUENCY_MILLESECONDS);
					}
					
				}
				
			}else {
				LogManagerExtended.applicationInfoLog("failed connect JMX.");
			}
			
		}catch (Throwable e) {
			LogManagerExtended.applicationInfoLog("Exception in AgentIgnitorLinuxUnificationJbossThread : "+e);
		}
	}
	
	public static void main(String[] args) {
	
	}

}
