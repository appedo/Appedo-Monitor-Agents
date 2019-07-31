package com.appedo.agent.init;

import net.sf.json.JSONArray;
import net.sf.json.JSONObject;

import com.appedo.agent.manager.AgentManager;
import com.appedo.agent.utils.Constants;

/**
 * This class is to start the timer to collect the selected counters
 * and to monitor the GlassFish server 
 * 
 */
public class AgentIgnitorGlassFishMonitor {

	public static void main(String[] args) {
		
		try {
			// load config parameters
			Constants.loadConfigProperties();		
			
			if( Constants.AGENT_CONFIG != null ) {
				JSONArray jaConfig = JSONArray.fromObject(Constants.AGENT_CONFIG);

				for(int i=0; i<jaConfig.size(); i++) {
					JSONObject joRecord = jaConfig.getJSONObject(i);
					String strGUID = joRecord.getString("guid");
					String strAppName = joRecord.getString("app_name");
					String strSvrAlias = joRecord.getString("server_alias");

					new AgentIgnitorGlassFishThread(strGUID, strAppName, strSvrAlias).start();
				}
				
			} else {
				System.out.println("Configuration problem");
			}
			
			AgentManager.runGarbageCollectionRountine();
			
		} catch(Throwable e) {
			System.out.println("Exception in InitServlet.init: "+e.getMessage());
			e.printStackTrace();
		}
	}
}
