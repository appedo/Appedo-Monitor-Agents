package com.appedo.agent.timer;

import java.util.Date;
import java.util.Timer;
import java.util.TimerTask;

import com.appedo.agent.manager.AgentManager;
import com.appedo.agent.manager.JbossMonitorManager;
import com.appedo.agent.manager.LogManagerExtended;
import com.appedo.agent.utils.Constants;
import com.appedo.agent.utils.Constants.AGENT_TYPE;

public class LinuxUnificationJbossMonitorTimer extends TimerTask{

	private Timer timerLinuxUnificationJbossObj = null;
	private Date collectionData = null;
	String moduleStatus = "";
	AgentManager am = null;
	
	public LinuxUnificationJbossMonitorTimer(Timer timerObject) {
		this.timerLinuxUnificationJbossObj = timerObject;
		am = new AgentManager();
	}

	public void run() {
		
		try {
			this.collectionData = new Date();
			
			this.moduleStatus = am.getModuleRunningStatus(Constants.JBOSS_AGENT_GUID, "appInformation");
			
			LogManagerExtended.applicationInfoLog("Status of "+Constants.JBOSS_AGENT_GUID+" : "+this.moduleStatus);
			
			if(this.moduleStatus.equalsIgnoreCase("running")) {
				
				JbossMonitorManager.getJbossMonitorManager().monitorJbossServer(Constants.JBOSS_AGENT_GUID, this.collectionData);
				
			}else if(this.moduleStatus.equalsIgnoreCase("restart")) {
				
				JbossMonitorManager.getJbossMonitorManager().updateJbossAppInfo(Constants.JBOSS_AGENT_GUID);
				
				if(am.getConfigurationsFromCollector(Constants.JBOSS_AGENT_GUID, AGENT_TYPE.TOMCAT.toString())) {
					JbossMonitorManager.getJbossMonitorManager().monitorJbossServer(Constants.JBOSS_AGENT_GUID, this.collectionData);
				}
				
			}else if(this.moduleStatus.equalsIgnoreCase("Kill")) {
				
				LogManagerExtended.applicationInfoLog("Module card is deleted....");
				this.timerLinuxUnificationJbossObj.cancel();
				this.timerLinuxUnificationJbossObj.purge();
				
			}
			
		}catch (Throwable th) {
			LogManagerExtended.applicationInfoLog(th.getMessage());
		}
	}
	
	public static void main(String[] args) {
		// TODO Auto-generated method stub

	}

}
