package com.appedo.agent.init;

import java.util.ArrayList;
import java.util.Timer;
import java.util.TimerTask;

import com.appedo.agent.connect.PostgresSQLConnector;
import com.appedo.agent.manager.AgentManager;
import com.appedo.agent.manager.LogManagerExtended;
import com.appedo.agent.timer.LinuxUnificationPostgreSQLMonitorTimer;
import com.appedo.agent.utils.Constants;
import com.appedo.agent.utils.Constants.AGENT_TYPE;

import net.sf.json.JSONObject;

public class AgentIgnitorLinuxUnificationPostgreSQLThread extends Thread{

	AgentManager am = null;
	public AgentIgnitorLinuxUnificationPostgreSQLThread() {
		// TODO Auto-generated constructor stub
		am = new AgentManager();
		start();
	}
	
	public void run() {
		String moduleGUID = "";
		JSONObject joModuleGUIDs = new JSONObject();
		try {
			
			LogManagerExtended.databaseInfoLog("Linux Unification PostgreSQL services started...");
			//boolean bIsPostgreSQLconnected = PostgresSQLConnector.getMyPGConnector().getPGConfigFile();
			
			//if(bIsPostgreSQLconnected) {
				ArrayList<String> alDBName = PostgresSQLConnector.getMyPGConnector("postgres").listAllDataBaseNames();
				//ArrayList<String> alDBName = pgConnecter.getAllDataBaseName();
				JSONObject joModuleData = PostgresSQLConnector.getMyPGConnector("postgres").getModuleInformation();
				
				for(String DataBaseName : alDBName) {
					joModuleData.put("moduleName", DataBaseName);
					LogManagerExtended.databaseInfoLog("DataBase Module Information : "+ joModuleData.toString());
					moduleGUID = am.sendModuleInfoToCollector(joModuleData, Constants.SYSTEM_ID, Constants.SYS_UUID, new JSONObject(), "DBInformation");
					if(!moduleGUID.isEmpty()) {
						joModuleGUIDs.put(DataBaseName, moduleGUID);
						moduleGUID = "";
					}
				}
				
				LogManagerExtended.databaseInfoLog("DataBase Guid's : "+ joModuleGUIDs.toString());
				
				for(Object Key : joModuleGUIDs.keySet()) {
					String Guid = joModuleGUIDs.getString((String)Key);
					String DBName = Key.toString();
					try {
						if(am.getConfigurationsFromCollector(Guid, AGENT_TYPE.POSTGRES.toString())) {
							Timer timerDataBasePGModule = new Timer();
							TimerTask ttLinuxUnifiedDataBase = new LinuxUnificationPostgreSQLMonitorTimer(timerDataBasePGModule, Guid, DBName);
							timerDataBasePGModule.schedule(ttLinuxUnifiedDataBase, 100l, Constants.MONITOR_FREQUENCY_MILLESECONDS);
						}
					}catch (Throwable e) {
						// TODO Auto-generated catch block
						LogManagerExtended.databaseInfoLog("Exception in createTimerTask :" + e);
						e.printStackTrace();
					}
				}
				
			/*}else {
				LogManagerExtended.databaseInfoLog("unable to connect pg_config.properties.");
			}*/
		}catch (Throwable e) {
			LogManagerExtended.databaseInfoLog("Exception in AgentIgnitorLinuxUnificationDataBaseThread : "+ e);
		}
	}
	
	public static void main(String[] args) {
		
	}

}
