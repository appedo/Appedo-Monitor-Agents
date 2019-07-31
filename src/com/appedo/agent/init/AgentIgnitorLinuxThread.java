package com.appedo.agent.init;

import java.util.Timer;
import java.util.TimerTask;

import com.appedo.agent.manager.AgentManager;
import com.appedo.agent.timer.LinuxMonitorTimer;
import com.appedo.agent.utils.Constants;
import com.appedo.agent.utils.Constants.AGENT_TYPE;

public class AgentIgnitorLinuxThread extends Thread {
	AgentManager am = null; 
	Timer timerLinuxManager = null;
	
	public AgentIgnitorLinuxThread() throws Throwable {
		am = new AgentManager();
		start();
	}
	
	
	public void run() {
		try {
			// When counter is not set in the agent
			// Agent will ping every 2 min
			if(am.getConfigurationsFromCollector(Constants.LINUX_AGENT_GUID, AGENT_TYPE.LINUX.toString())) {
				// Start perf.counter populate after 100 ms and do it for each given frequency
				TimerTask ttLinuxMonitor = new LinuxMonitorTimer("MONITOR");
				timerLinuxManager = new Timer();
				timerLinuxManager.schedule(ttLinuxMonitor, 100l, Constants.MONITOR_FREQUENCY_MILLESECONDS);
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

