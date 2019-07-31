package com.appedo.agent.timer;

import java.util.Date;
import java.util.Timer;
import java.util.TimerTask;

import com.appedo.agent.manager.AgentManager;
import com.appedo.agent.manager.LogManagerExtended;
import com.appedo.agent.manager.OracleMonitorManager;
import com.appedo.agent.utils.Constants.AGENT_TYPE;

public class LinuxUnificationOracleMonitorTimer extends TimerTask{

	private Timer timerOracleDB = null;
	private Date collectionData = null;
	static AgentManager am = null;
	String moduleStatus = "", moduleGUID = "";
	
	public LinuxUnificationOracleMonitorTimer(Timer timerObject, String agentGUID) {
		this.timerOracleDB = timerObject;
		this.moduleGUID = agentGUID;
		am = new AgentManager();
	}

	public void run() {
		
		try {
			this.collectionData = new Date();
			this.moduleStatus = am.getModuleRunningStatus(moduleGUID, "DBInformation");
			
			LogManagerExtended.databaseInfoLog("Status of "+moduleGUID+" : "+this.moduleStatus);
			
			if(moduleStatus.equalsIgnoreCase("running")) {
				
				OracleMonitorManager.getOracleMonitorManager().monitorOracleServer(moduleGUID, collectionData);
				
			}else if(moduleStatus.equalsIgnoreCase("restart")) {
				
				if(am.getConfigurationsFromCollector(moduleGUID, AGENT_TYPE.ORACLE.toString())) {
					OracleMonitorManager.getOracleMonitorManager().monitorOracleServer(moduleGUID, collectionData);
				}
				
			}else if(moduleStatus.equalsIgnoreCase("Kill")) {
				
				LogManagerExtended.databaseInfoLog(moduleGUID+" Module card was deleted, so this thread will be stop in 2sec.. ");
				this.timerOracleDB.cancel();
				this.timerOracleDB.purge();
				
			}
			
		} catch (Throwable e) {
			LogManagerExtended.databaseInfoLog("Exception in LinuxUnificationOracleMonitorTimer : "+e);
			e.printStackTrace();
		}
		
	}
	
	public static void main(String[] args) {
		// TODO Auto-generated method stub

	}

}
