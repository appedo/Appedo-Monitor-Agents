package com.appedo.agent.init;

import java.util.Timer;
import java.util.TimerTask;

import com.appedo.agent.manager.AgentManager;
import com.appedo.agent.timer.MySQLMonitorTimer;
import com.appedo.agent.utils.Constants;
import com.appedo.agent.utils.Constants.AGENT_TYPE;

public class AgentIgnitorMySQLThread extends Thread {
	
	Timer timerMySQLManager = null;
	String strGUID = null;
	String strDBNAME = null;
	AgentManager am = new AgentManager();
	
	public AgentIgnitorMySQLThread(String strGuid, String strDbName) throws Throwable {
		am = new AgentManager();
		this.strGUID = strGuid;
		start();
	}
	
	public void run() {
		try {
			// When counter is not set in the agent
			// Agent will ping every 2 min
			if(am.getConfigurationsFromCollector(strGUID, AGENT_TYPE.MYSQL.toString())) {
				
				// Start monitor for MYSQL counters
				TimerTask ttSQLMonitor = new MySQLMonitorTimer(strGUID,strDBNAME,"MONITOR");
				timerMySQLManager = new Timer();
				timerMySQLManager.schedule(ttSQLMonitor, 100l, Constants.MONITOR_FREQUENCY_MILLESECONDS);
			}
		} catch(Throwable e) {
			System.out.println("Exception in AgentIgnitorMySQLThread.run() :"+e.getMessage());
			e.printStackTrace();
		} finally {
			am = null;
		}
	}
	
	@Override
	protected void finalize() throws Throwable {
//		System.out.println("MySQL AgentIgnitorMySQLThread stopped");		
		super.finalize();
	}
}
