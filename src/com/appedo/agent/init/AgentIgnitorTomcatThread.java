package com.appedo.agent.init;

import java.util.Timer;
import java.util.TimerTask;

import com.appedo.agent.manager.AgentManager;
import com.appedo.agent.timer.TomcatMonitorTimer;
import com.appedo.agent.utils.Constants.AGENT_TYPE;

public class AgentIgnitorTomcatThread extends Thread {
	Timer timerTomcatManager = null;
	AgentManager am = null;
	String strGuid = null, strInstance = null, strSvrAlias = null, strConnectorPortAddress = null;

	public AgentIgnitorTomcatThread(String strGuid, String strInstance, String strSvrAlias, String strConnectorPortAddress) throws Throwable {
		am = new AgentManager();
		this.strGuid = strGuid;
		this.strInstance = strInstance;
		this.strSvrAlias = strSvrAlias;
		this.strConnectorPortAddress = strConnectorPortAddress;
	}

	public void run() {

		try {

			// When counter is not set in the agent
			// Agent will ping every 2 min
			if (am.getConfigurationsFromCollector(strGuid, AGENT_TYPE.TOMCAT.toString())) {
				// Start perf.counter populate after 100 ms and do it for each given frequency
				TimerTask ttTomcatMonitor = new TomcatMonitorTimer(strGuid, strInstance, strSvrAlias, strConnectorPortAddress, "MONITOR");
				timerTomcatManager = new Timer();
				timerTomcatManager.schedule(ttTomcatMonitor, 100l, 20000);
			}

		} catch (Throwable t) {
			System.out.println("Exception in AgentIgnitorTomcatThread.run() " + t.getMessage());
		} finally {
			am = null;
		}

	}

	@Override
	protected void finalize() throws Throwable {
//		System.out.println("Tomcat AgentIgnitorTomcatThread stopped");		
		super.finalize();
	}

}
