package com.appedo.agent.init;

import java.util.Timer;
import java.util.TimerTask;

import com.appedo.agent.manager.AgentManager;
import com.appedo.agent.timer.JbossMonitorTimer;
import com.appedo.agent.utils.Constants.AGENT_TYPE;

public class AgentIgnitorJbossThread extends Thread {
	Timer timerTomcatManager = null;
	AgentManager am = null;
	String strGuid = null, strInstance = null, strPort = null;
	
	public AgentIgnitorJbossThread(String strGuid, String strInstance) throws Throwable {
		am = new AgentManager();
		this.strGuid = strGuid;
		this.strInstance = strInstance;
	}
	
	public void run() {
		
		try {
			// When counter is not set in the agent
			// Agent will ping every 2 min
			if( am.getConfigurationsFromCollector(strGuid, AGENT_TYPE.JBOSS.toString()) ) {
				// Start perf.counter populate after 100 ms and do it for each given frequency
				TimerTask ttJBOSSMonitor = new JbossMonitorTimer(strGuid,strInstance,"MONITOR");
				timerTomcatManager = new Timer();
				timerTomcatManager.schedule(ttJBOSSMonitor, 100l,20000);
			}
			
		} catch(Throwable t) {
			System.out.println("Exception in AgentIgnitorJBOSSThread.run() " + t.getMessage());
		} finally {
			am = null;
		}
	}
	
	@Override
	protected void finalize() throws Throwable {
//		System.out.println("JBoss AgentIgnitorTomcatThread stopped");
		super.finalize();
	}
}
