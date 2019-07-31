package com.appedo.agent.init;

import java.util.Timer;
import java.util.TimerTask;

import com.appedo.agent.manager.AgentManager;
import com.appedo.agent.timer.ApacheMonitorTimer;
import com.appedo.agent.utils.Constants;
import com.appedo.agent.utils.Constants.AGENT_TYPE;

public class AgentIgnitorApacheThread extends Thread {
	AgentManager am = null; 
	Timer timerApacheManager = null;
	String strHostName = null, strAppPort = null, strMonitorURL = null;
	
	public AgentIgnitorApacheThread(String strHostName, String strAppPort, String strMonitorURL) throws Throwable {
		this.strHostName = strHostName;
		this.strAppPort = strAppPort;
		this.strMonitorURL = strMonitorURL;
		
		am = new AgentManager();
		start();
	}
	
	
	public void run() {
		try {
			
			// When counter is not set in the agent
			// Agent will ping every 2 min
			if(am.getConfigurationsFromCollector(Constants.APACHE_AGENT_GUID, AGENT_TYPE.APACHE.toString())) {
				// Start perf.counter populate after 100 ms and do it for each given frequency
				TimerTask ttApacheMonitor = new ApacheMonitorTimer("MONITOR", strHostName, strAppPort, strMonitorURL);
				timerApacheManager = new Timer();
				timerApacheManager.schedule(ttApacheMonitor, 100l,20000);
			}
			
		}catch(Throwable t) {
			t.printStackTrace();
			System.out.println("Exception in AgentIgnitorLinuxThread.run() :" + t.getMessage());
		}finally {
			am = null;
		}
	}
	
	@Override
	protected void finalize() throws Throwable {
//		System.out.println("LINUX AgentIgnitorLinuxThread stopped");		
		super.finalize();
	}
}

