package com.appedo.agent.timer;

import java.util.Date;
import java.util.Timer;
import java.util.TimerTask;

import com.appedo.agent.manager.AgentManager;
import com.appedo.agent.manager.LinuxMonitorManager;
import com.appedo.agent.manager.LogManagerExtended;
import com.appedo.agent.utils.Constants;
import com.appedo.agent.utils.Constants.AGENT_TYPE;

public class LinuxUnificationServerMonitorTimer extends TimerTask {
	private Timer timerLinuxUnificationServerManager = null;
	private Date collectionData = null;
	AgentManager am = null; 
	String moduleStatus = "";
	
	public LinuxUnificationServerMonitorTimer(Timer timerObject) {
		this.timerLinuxUnificationServerManager = timerObject;
		am = new AgentManager();
	}

	@Override
	public void run() {

		try {
			
			this.collectionData = new Date();
			
			LogManagerExtended.serverInfoLog("#####--LinuxUnificationServerCounterDataCollectingProcess--#####");
			
			moduleStatus = am.getModuleRunningStatus(Constants.LINUX_AGENT_GUID, "serverInformation");
			
			if(moduleStatus.equalsIgnoreCase("running")) {
				
				LogManagerExtended.serverInfoLog("Module Status is running... Data is collecting");
				LinuxMonitorManager.getLinuxMonitorManager().getCountersValue(collectionData);
				
			}else if(moduleStatus.equalsIgnoreCase("restart")) {
				
				LogManagerExtended.serverInfoLog("Module Status is restart mode... so counter data is reset");
				
				if(am.getConfigurationsFromCollector(Constants.LINUX_AGENT_GUID, AGENT_TYPE.LINUX.toString())) {
					LinuxMonitorManager.getLinuxMonitorManager().getCountersValue(collectionData);
				}
				
			}else if(moduleStatus.equalsIgnoreCase("stop")) {
				
				LogManagerExtended.serverInfoLog("Module collect counter status is stop mode...");
				
			}else if(moduleStatus.equalsIgnoreCase("Kill")) {
				
				LogManagerExtended.serverInfoLog("Module card is deleted....");
				this.timerLinuxUnificationServerManager.cancel();
				this.timerLinuxUnificationServerManager.purge();
				
			}
			LogManagerExtended.serverInfoLog("================================================================");
		}catch (Throwable e) {
			// TODO: handle exception
			LogManagerExtended.serverInfoLog(e.getMessage());
		}
	}

	public static void main(String[] args) {
		// TODO Auto-generated method stub

	}
}
