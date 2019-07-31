package com.appedo.agent.init;

import com.appedo.agent.manager.LinuxMonitorManager;
import com.appedo.agent.manager.LogManagerExtended;

public class AgentIgnitorLinuxUnificationDataBaseThread extends Thread{

	public AgentIgnitorLinuxUnificationDataBaseThread() {
		start();
	}
	
	public void run() {
		try {
			if(LinuxMonitorManager.getLinuxMonitorManager().OracleDbStatus()) {
				//Orcle DB monitor flow.
				new AgentIgnitorLinuxUnificationOracleThread();
			}
			
			if(LinuxMonitorManager.getLinuxMonitorManager().PostgresDbStatus()) {
				//Postgres DB monitor flow.
				new AgentIgnitorLinuxUnificationPostgreSQLThread();
			}
		}catch (Exception e) {
			LogManagerExtended.databaseInfoLog(e.getMessage());
		}
	}

	public static void main(String[] args) {
		// TODO Auto-generated method stub

	}

}
