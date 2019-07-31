package com.appedo.agent.init;

import java.util.Timer;
import java.util.TimerTask;

import com.appedo.agent.manager.AgentManager;
import com.appedo.agent.timer.OracleMonitorTimer;
import com.appedo.agent.utils.Constants.AGENT_TYPE;

public class AgentIgnitorOracleThread extends Thread {
	Timer timerOracleManager = null;
	AgentManager am = null;
	String strGuid = null;
	String strDBNAME = null;

	public AgentIgnitorOracleThread(String strGuid, String strDbName) throws Throwable {
		am = new AgentManager();
		this.strGuid = strGuid;
		this.strDBNAME = strDbName;
		start();
	}

	public void run() {

		try {

			// When counter is not set in the agent
			// Agent will ping every 2 min
			if (am.getConfigurationsFromCollector(strGuid, AGENT_TYPE.ORACLE.toString())) {
				// Start perf.counter populate after 100 ms and do it for each given frequency
				TimerTask ttORACLEMonitor = new OracleMonitorTimer(strGuid, strDBNAME, "MONITOR");
				timerOracleManager = new Timer();
				timerOracleManager.schedule(ttORACLEMonitor, 100l, 20000);
			}

		} catch (Throwable t) {
			System.out.println("Exception in AgentIgnitorOracleThread.run() " + t.getMessage());

		} finally {
			am = null;
		}

	}

	@Override
	protected void finalize() throws Throwable {
		// System.out.println("Oracle AgentIgnitorTomcatThread stopped");
		super.finalize();
	}

}
