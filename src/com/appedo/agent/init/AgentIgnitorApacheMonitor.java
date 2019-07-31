package com.appedo.agent.init;

import com.appedo.agent.utils.Constants;

import net.sf.json.JSONArray;
import net.sf.json.JSONObject;

/**
 * Starting class for Linux agent.
 * This class starts the timer, which monitors the Linux OS and retrieves the counter values.
 * 
 * @author veeru
 *
 */
public class AgentIgnitorApacheMonitor {
	
	public static void main(String[] args) {
		
		try {
			// load config parameters
			Constants.loadConfigProperties();
			
			if( Constants.AGENT_CONFIG!=null ) {
				JSONArray jaConfig = JSONArray.fromObject(Constants.AGENT_CONFIG);
				
				JSONObject joRecord = jaConfig.getJSONObject(0);
				Constants.APACHE_AGENT_GUID = joRecord.getString("guid");
				String strHostName = joRecord.getString("host");
				String strAppPort = joRecord.getString("app_port");
				String strMonitorURL = joRecord.containsKey("monitor_url")?joRecord.getString("monitor_url"):null;
				System.out.println("APACHE Agent GUID: "+Constants.APACHE_AGENT_GUID);
				//System.out.println(Constants.LINUX_AGENT_GUID);
				new AgentIgnitorApacheThread(strHostName, strAppPort, strMonitorURL);
				
			}else {
				System.out.println("Congiguration problem");
			}
			//AgentManager.runGarbageCollectionRountine();
			
		} catch(Throwable e) {
			System.out.println("Exception in InitServlet.init: "+e.getMessage());
			e.printStackTrace();
		}
	}
}
