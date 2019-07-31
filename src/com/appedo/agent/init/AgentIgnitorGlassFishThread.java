package com.appedo.agent.init;

import java.util.Timer;
import java.util.TimerTask;

import com.appedo.agent.manager.AgentManager;
import com.appedo.agent.timer.GlassFishMonitorTimer;
import com.appedo.agent.utils.Constants.AGENT_TYPE;

public class AgentIgnitorGlassFishThread extends Thread {
	Timer timerGlassFishManager = null;
	AgentManager am = null;
	String strGuid = null, strAppName = null, strPort = null, strSvrAlias = null;
	
	public AgentIgnitorGlassFishThread(String strGuid, String strAppName, String strSvrAlias) throws Throwable {
		am = new AgentManager(); 
		this.strGuid = strGuid;
		this.strAppName = strAppName;
		this.strSvrAlias = strSvrAlias;
	}
	
	public void run() {
		
		try {
			// When counter is not set in the agent
			// Agent will ping every 20 seconds
			if( am.getConfigurationsFromCollector(strGuid, AGENT_TYPE.GLASSFISH.toString()) ) {
				// Start perf.counter populate after 100 ms and do it for each given frequency
				TimerTask ttGlassFishMonitor = new GlassFishMonitorTimer(strGuid, strAppName, strSvrAlias, "MONITOR");
				timerGlassFishManager = new Timer();
				timerGlassFishManager.schedule(ttGlassFishMonitor, 100l, 20000);
			}
			
		}catch(Throwable t) {
			System.out.println("Exception in AgentIgnitorGlassFishThread.run() " + t.getMessage());
			
		}finally {
			am = null;
		}
		
	}
	
	@Override
	protected void finalize() throws Throwable {
//		System.out.println("GlassFish AgentIgnitorGlassFishThread stopped");		
		super.finalize();
	}

}
