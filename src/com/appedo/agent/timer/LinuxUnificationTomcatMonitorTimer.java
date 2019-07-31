package com.appedo.agent.timer;

import java.util.Date;
import java.util.Timer;
import java.util.TimerTask;

import com.appedo.agent.manager.AgentManager;
import com.appedo.agent.manager.LogManagerExtended;
import com.appedo.agent.manager.TomcatMonitorManager;
import com.appedo.agent.utils.Constants;
import com.appedo.agent.utils.UtilsFactory;
import com.appedo.agent.utils.Constants.AGENT_TYPE;

public class LinuxUnificationTomcatMonitorTimer extends TimerTask{

	private Timer timerLinuxUnificationApplicationManager = null;
	private Date collectionData = null;
	AgentManager am = null;
	String moduleStatus = "", moduleGUID = "", JMX_PORT;
	
	public LinuxUnificationTomcatMonitorTimer(Timer timerObject, String agentGUID, String JMXPORT) {
		this.timerLinuxUnificationApplicationManager = timerObject;
		this.moduleGUID = agentGUID;
		am = new AgentManager();
		this.JMX_PORT = JMXPORT;
		// TODO Auto-generated constructor stub
	}

	public void run() {
		//TODO Auto-generated method stub
		try {
			this.collectionData = new Date();
			
			this.moduleStatus = am.getModuleRunningStatus(moduleGUID, "appInformation");
			
			UtilsFactory.printDebugLog(Constants.IS_DEBUG, "Status of Agent("+this.moduleGUID+") running at JMX_PORT "+JMX_PORT+" : "+this.moduleStatus);
			
			if(moduleStatus.equalsIgnoreCase("running")) {
				
				LogManagerExtended.applicationInfoLog("Module Status is running status... Data is collecting");
				TomcatMonitorManager.getTomcatMonitorManager().monitorTomcatCounters(moduleGUID, collectionData, JMX_PORT);
				//LinuxMonitorManager.getLinuxMonitorManager().getCountersValue(collectionData);
			}else if(moduleStatus.equalsIgnoreCase("restart")) {
				
				LogManagerExtended.applicationInfoLog("Module Status is restart mode... so counter data is reset");
				
				if(am.getConfigurationsFromCollector(moduleGUID, AGENT_TYPE.TOMCAT.toString())) {
					TomcatMonitorManager.getTomcatMonitorManager().monitorTomcatCounters(moduleGUID, collectionData, JMX_PORT);
				}
				
			}else if(moduleStatus.equalsIgnoreCase("stop")) {
				
				LogManagerExtended.applicationInfoLog("Module collect counter status is stop mode...");
				
			}else if(moduleStatus.equalsIgnoreCase("Kill")) {
				
				LogManagerExtended.applicationInfoLog("Module card is deleted....");
				this.timerLinuxUnificationApplicationManager.cancel();
				this.timerLinuxUnificationApplicationManager.purge();
				
			}
			LogManagerExtended.applicationInfoLog("================================================================");

		}catch (Exception e) {
			// TODO: handle exception
			
			LogManagerExtended.applicationInfoLog(e.getMessage());
		} catch (Throwable e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
	}
	public static void main(String[] args) {
		// TODO Auto-generated method stub

	}

}
