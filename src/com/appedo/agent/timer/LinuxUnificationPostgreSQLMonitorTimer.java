package com.appedo.agent.timer;

import java.util.Date;
import java.util.Timer;
import java.util.TimerTask;

import com.appedo.agent.manager.AgentManager;
import com.appedo.agent.manager.LogManagerExtended;
import com.appedo.agent.manager.PostgresSQLMonitorManager;
import com.appedo.agent.utils.Constants.AGENT_TYPE;

public class LinuxUnificationPostgreSQLMonitorTimer extends TimerTask{

	private Timer timerDataBaseObj = null;
	private Date collectionData = null;
	static AgentManager am = null;
	String moduleStatus = "", moduleGUID = "", DBName;
	
	public LinuxUnificationPostgreSQLMonitorTimer(Timer timerObject, String agentGUID, String DBName) {
		// TODO Auto-generated constructor stub
		this.timerDataBaseObj = timerObject;
		this.moduleGUID = agentGUID;
		this.DBName = DBName;
		am = new AgentManager();
	}

	public void run() {
		
		try {
			
			this.collectionData = new Date();
			this.moduleStatus = am.getModuleRunningStatus(moduleGUID, "DBInformation");
			
			LogManagerExtended.databaseInfoLog("Status of "+this.DBName+"-"+moduleGUID+" : "+this.moduleStatus);
			
			if(moduleStatus.equalsIgnoreCase("running")) {
				
				PostgresSQLMonitorManager.getPGMonitorManager().monitorLinuxUnifiedPGServer(this.moduleGUID, this.DBName, this.collectionData);
				
			}else if(moduleStatus.equalsIgnoreCase("restart")) {
				
				if(am.getConfigurationsFromCollector(this.moduleGUID, AGENT_TYPE.POSTGRES.toString())) {
					PostgresSQLMonitorManager.getPGMonitorManager().monitorLinuxUnifiedPGServer(this.moduleGUID, this.DBName, this.collectionData);
				}
				
			}else if(moduleStatus.equalsIgnoreCase("Kill")) {
				
				LogManagerExtended.databaseInfoLog(this.DBName+"-"+moduleGUID+" Module card was deleted, so this thread will be stop in 2sec.. ");
				this.timerDataBaseObj.cancel();
				this.timerDataBaseObj.purge();
				
			}
			
		} catch (Throwable e) {
			//LogManagerExtended.logApplicationInfoOutput(e.getMessage());
			LogManagerExtended.databaseInfoLog("Exception in LinuxUnificationDataBaseMonitorTimer : "+e);
			e.printStackTrace();
		}
		
	}
	
	public static void main(String[] args) {
		
	}

}
