package com.appedo.agent.init;

import java.util.Iterator;

import com.appedo.agent.manager.TomcatMonitorManager;
import com.appedo.agent.utils.Constants;
import com.appedo.agent.utils.UtilsFactory;

import net.sf.json.JSONArray;
import net.sf.json.JSONObject;

/**
 * This class is to start the timer to collect the selected counters
 * and to monitor the tomcat server 
 * @author Veeru
 *
 */
public class AgentIgnitorTomcatMonitor {
	private final static String[] CMD_LIST_PROJECTS = {"-la", "--list-applications"};

	public static void main(String[] args) {
		JSONArray jaAgentConfigs = null;
		JSONObject joASDDetails = null;
		Iterator<String> iterSvr = null;
		String strSvrAlias = null, strGUID = null, strApp = null, strConnectorPortAddress = null;;
		JSONObject joRecord = null;
		
		try {
			// load config parameters
			Constants.loadConfigProperties();
			
			if (args.length > 0 ) {
				
				// list all deployed projects, command line args
				if( UtilsFactory.contains(args, CMD_LIST_PROJECTS[0]) || UtilsFactory.contains(args, CMD_LIST_PROJECTS[1]) ) {
					joASDDetails = JSONObject.fromObject(Constants.ASD_DETAILS);
					iterSvr = joASDDetails.keys();
					
					while ( iterSvr.hasNext() ) {
						strSvrAlias = iterSvr.next();
						TomcatMonitorManager.getTomcatMonitorManager("", strSvrAlias).listAllProjects();
					}
				} else {
					System.out.println("Invalid option");
				}
			} else {
				// Monitor Tomcat
				if( Constants.AGENT_CONFIG != null ) {
					
					jaAgentConfigs = JSONArray.fromObject(Constants.AGENT_CONFIG);
					for (int i = 0; i < jaAgentConfigs.size(); i++) {
						joRecord = jaAgentConfigs.getJSONObject(i);
						strGUID = joRecord.getString("guid");
						strApp = joRecord.getString("app_name");
						strSvrAlias = joRecord.getString("server_alias");
						strConnectorPortAddress = null;
						
						if (joRecord.containsKey("connector_port_address")) {
							strConnectorPortAddress = joRecord.getString("connector_port_address");
						}
					
						//TomcatMonitorManager.getTomcatAttribute(strSvrAlias);
						new AgentIgnitorTomcatThread(strGUID, strApp, strSvrAlias, strConnectorPortAddress).start();
					}
				} else {
					System.out.println("Configuration problem");
				}
			}
			
			// AgentManager.runGarbageCollectionRountine();

		} catch (Throwable e) {
			System.out.println("Exception in InitServlet.init: " + e.getMessage());
			e.printStackTrace();
		}
	}
}
