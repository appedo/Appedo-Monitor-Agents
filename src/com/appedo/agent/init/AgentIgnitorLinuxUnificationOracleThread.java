package com.appedo.agent.init;


import java.util.Timer;
import java.util.TimerTask;

import com.appedo.agent.manager.AgentManager;
import com.appedo.agent.manager.LogManagerExtended;
import com.appedo.agent.manager.OracleMonitorManager;
import com.appedo.agent.timer.LinuxUnificationOracleMonitorTimer;
import com.appedo.agent.utils.Constants;
import com.appedo.agent.utils.Constants.AGENT_TYPE;

import net.sf.json.JSONObject;

public class AgentIgnitorLinuxUnificationOracleThread extends Thread{

	AgentManager am = null;
	public AgentIgnitorLinuxUnificationOracleThread() {
		am = new AgentManager();
		start();
	}

	public void run() {
		String moduleGUID = "";
		
		try {
			LogManagerExtended.databaseInfoLog("Linux Unification Oracle monitor services started...");
			
			JSONObject joModuleData = OracleMonitorManager.getOracleMonitorManager().getModuleInformation();
			
			if(joModuleData != null) {
				
				LogManagerExtended.databaseInfoLog("Oracle DB Module Information : "+ joModuleData.toString());
				
				moduleGUID = am.sendModuleInfoToCollector(joModuleData, Constants.SYSTEM_ID, Constants.SYS_UUID, new JSONObject(), "DBInformation");
				
				LogManagerExtended.databaseInfoLog("ModuleGUID : "+ moduleGUID);
				
				if(!moduleGUID.isEmpty()) {
					
					if(am.getConfigurationsFromCollector(moduleGUID, AGENT_TYPE.ORACLE.toString())) {
						Timer timerDataBaseOracleModule = new Timer();
						TimerTask ttLinuxUnifiedDataBase = new LinuxUnificationOracleMonitorTimer(timerDataBaseOracleModule, moduleGUID);
						timerDataBaseOracleModule.schedule(ttLinuxUnifiedDataBase, 100l, Constants.MONITOR_FREQUENCY_MILLESECONDS);
					}
				}
			}else {
				LogManagerExtended.databaseInfoLog("joModuleData JSON value is empty...");
			}
			
		}catch (Throwable e) {
			LogManagerExtended.databaseInfoLog("Exception in AgentIgnitorLinuxUnificationOracleThread : "+e);
		}
		
	}
	
	public static void main(String[] args) {
		// TODO Auto-generated method stub

	}

}
