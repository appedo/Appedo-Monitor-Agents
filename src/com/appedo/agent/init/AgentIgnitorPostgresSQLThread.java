package com.appedo.agent.init;

import java.util.Timer;
import java.util.TimerTask;

import com.appedo.agent.manager.AgentManager;
import com.appedo.agent.timer.PostgresSQLMonitorTimer;
import com.appedo.agent.utils.Constants.AGENT_TYPE;

public class AgentIgnitorPostgresSQLThread extends Thread {
	AgentManager am = null; 
	Timer timerPGManager = null;
	String strGUID = null;
	String strDBNAME = null;
	
	public AgentIgnitorPostgresSQLThread(String strGuid, String strDbName) throws Throwable {
		am = new AgentManager();
		this.strGUID = strGuid;
		this.strDBNAME = strDbName;
		start();
		
	}
	
	
	public void run() {
		try {
			
			// When counter is not set in the agent
			// Agent will ping every 2 min
			if(am.getConfigurationsFromCollector(strGUID, AGENT_TYPE.POSTGRES.toString())) {
				// Start perf.counter populate after 100 ms and do it for each given frequency
				TimerTask ttPGMonitor = new PostgresSQLMonitorTimer(strGUID,strDBNAME,"MONITOR");
				timerPGManager = new Timer();
				timerPGManager.schedule(ttPGMonitor, 100l,20000);
			}
			
		} catch(Throwable t) {
			t.printStackTrace();
			System.out.println("Exception in AgentIgnitorPGThread.run() :" + t.getMessage());
			
		} finally {
			am = null;
		}
	}
	
	@Override
	protected void finalize() throws Throwable {
//		System.out.println("LINUX AgentIgnitorLinuxThread stopped");		
		super.finalize();
	}
}

