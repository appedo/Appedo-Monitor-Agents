package com.appedo.agent.init;

import java.util.Timer;
import java.util.TimerTask;

import com.appedo.agent.manager.AgentManager;
import com.appedo.agent.timer.JStackMonitorTimer;
import com.appedo.agent.utils.Constants;
import com.appedo.agent.utils.Constants.AGENT_TYPE;

public class AgentIgnitorJStackThread extends Thread {
	AgentManager am = null; 
	Thread tJStackThread = null;
	
	String strSvrAlias = null;
	
	public AgentIgnitorJStackThread(String strSvrAlias) throws Throwable {
		am = new AgentManager();
		
		this.strSvrAlias = strSvrAlias;
		
		start();
	}
	
	
	public void run() {
		try {
			// When counter is not set in the agent
			// Agent will ping every 2 min
			if(am.getConfigurationsFromCollector(Constants.JSTACK_AGENT_GUID, AGENT_TYPE.JSTACK.toString())) {
				// Start perf.counter populate after 100 ms and do it for each given frequency
				tJStackThread = new JStackMonitorTimer(strSvrAlias, "MONITOR");
				tJStackThread.start();
			}
		} catch(Throwable t) {
			System.out.println("Exception in AgentIgnitorLinuxThread.run() :" + t.getMessage());
			t.printStackTrace();
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

