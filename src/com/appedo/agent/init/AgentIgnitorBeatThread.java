package com.appedo.agent.init;

import java.util.Timer;
import java.util.TimerTask;

import com.appedo.agent.manager.AgentManager;
import com.appedo.agent.timer.BeatMonitorTimer;

public class AgentIgnitorBeatThread extends Thread {
	Timer timerTomcatManager = null;
	AgentManager am = null;
	String strGuid = null, strInstance = null, strPort = null;
	
	public AgentIgnitorBeatThread(String strGuid, String strInstance) throws Throwable {
		am = new AgentManager();
		this.strGuid = strGuid;
		this.strInstance = strInstance;
	}
	
	public void run() {
		
		try {
			// When counter is not set in the agent
			// Agent will ping every 2 min
			TimerTask ttBeatMonitor = new BeatMonitorTimer(strGuid);
			timerTomcatManager = new Timer();
			timerTomcatManager.schedule(ttBeatMonitor, 100l,20000); // make it configurable later
		}catch(Throwable t) {
			System.out.println("Exception in AgentIgnitorBeatThread.run() " + t.getMessage());
			
		}finally {
			am = null;
		}
		
	}
	
	@Override
	protected void finalize() throws Throwable {
//		System.out.println("Tomcat AgentIgnitorTomcatThread stopped");		
		super.finalize();
	}

}
