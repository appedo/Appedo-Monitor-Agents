package com.appedo.agent.init;

import net.sf.json.JSONArray;
import net.sf.json.JSONObject;

import com.appedo.agent.utils.Constants;

/**
 * This class is to start the timer to collect the selected counters
 * and to monitor the tomcat server 
 * @author Veeru
 *
 */
public class AgentIgnitorMySQLMonitor {
	
	public static void main(String[] args) {
		
		try{
			// load config parameters
			Constants.loadConfigProperties();
			
			if( Constants.AGENT_CONFIG!=null ) {
				
				JSONArray jaConfig = JSONArray.fromObject(Constants.AGENT_CONFIG);
				for(int i=0; i<jaConfig.size(); i++) {
					
					JSONObject joRecord = jaConfig.getJSONObject(i);
					String strGUID = joRecord.getString("guid");
					String strDbName = joRecord.getString("dbname");
//					System.out.println(strGUID+"--"+strPort+"---"+strApp);
					new AgentIgnitorMySQLThread(strGUID, strDbName);
				}
			} else {
				System.out.println("Configuration problem");
			}
			//AgentManager.runGarbageCollectionRountine();
			
		} catch(Throwable e) {
			System.out.println("Exception in InitServlet.init: "+e.getMessage());
			e.printStackTrace();
		}
	}
}
