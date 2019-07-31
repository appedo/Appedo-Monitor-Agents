package com.appedo.agent.init;

import net.sf.json.JSONArray;
import net.sf.json.JSONObject;

import com.appedo.agent.utils.Constants;

public class AgentIgnitorOracleMonitor {
	
public static void main(String[] args) {
		try {
			Constants.loadConfigProperties();

			if (Constants.AGENT_CONFIG != null) {
				JSONArray jaConfig = JSONArray.fromObject(Constants.AGENT_CONFIG);
				for (int i = 0; i < jaConfig.size(); i++) {
					JSONObject joRecord = jaConfig.getJSONObject(i);
					String strGUID = joRecord.getString("guid");
					String strDbName = joRecord.getString("dbname");
					new AgentIgnitorOracleThread(strGUID, strDbName);
				}
			} else {
				System.out.println("Congiguration problem");
			}
			// AgentManager.runGarbageCollectionRountine();
		} catch (Throwable e) {
			System.out.println("Exception in InitServlet.init: "+ e.getMessage());
			e.printStackTrace();
		}

	}
}

