package com.appedo.agent.init;

import net.sf.json.JSONArray;
import net.sf.json.JSONObject;

import com.appedo.agent.manager.JbossMonitorManager;
import com.appedo.agent.utils.Constants;
import com.appedo.agent.utils.Constants.COMMAND_LINE_OPTIONS;
import com.appedo.agent.utils.UtilsFactory;

public class AgentIgnitorJbossMonitor {
	public static void main(String[] args) {
		String strGUID = null, strApp = null, strAppPort = null;
		JSONObject joRecord = null;
		int nIndex = -1;
		Integer nPID = null;
		
		try{
			// load config parameters
			Constants.loadConfigProperties();
			
			Constants.IS_DEBUG = UtilsFactory.contains(args, "--debug");
			
			
			// list all deployed projects, command line args
			if( UtilsFactory.contains(args, COMMAND_LINE_OPTIONS.LIST_APPLICATIONS_1.toString()) || UtilsFactory.contains(args, COMMAND_LINE_OPTIONS.LIST_APPLICATIONS_2.toString()) ) {
				JbossMonitorManager.getJbossMonitorManager().listAllApplications();
			}
			// Print application statistics for the given application
			else if( (nIndex = UtilsFactory.contains(args, COMMAND_LINE_OPTIONS.APPLICATION_STATISTICS_1.toString(), Integer.class)) >= 0 || (nIndex = UtilsFactory.contains(args, COMMAND_LINE_OPTIONS.APPLICATION_STATISTICS_2.toString(), Integer.class)) >= 0 ) {
				if ( args.length <= nIndex+1 ) {
					System.out.println("Application name is missing in the command-line.");
				} else {
					JbossMonitorManager.getJbossMonitorManager().monitorApplicationStatistics( args[nIndex+1]);
				}
			}
			// Print the JNDI details
			else if( UtilsFactory.contains(args, COMMAND_LINE_OPTIONS.PRINT_JNDI_DETAILS_1.toString()) || UtilsFactory.contains(args, COMMAND_LINE_OPTIONS.PRINT_JNDI_DETAILS_2.toString()) ) {
				JbossMonitorManager.getJbossMonitorManager().printJNDIDetails();
			}
			// Find BoundPort of the JBoss
			else if( UtilsFactory.contains(args, COMMAND_LINE_OPTIONS.GET_BOUND_PORT.toString()) ) {
				if( args[1].toLowerCase().startsWith("pid=") ) {
					nPID = Integer.parseInt( args[1].split("=")[1] );
				} else if( args[1].toLowerCase().startsWith("app_port=") ) {
					strAppPort = args[1].split("=")[1];
				}
				JbossMonitorManager.getJbossMonitorManager().findBoundPort(nPID, strAppPort);
			}
			// Print all possible Counters
			else if( UtilsFactory.contains(args, COMMAND_LINE_OPTIONS.PRINT_ALL_COUNTERS.toString()) ) {
				JbossMonitorManager.getJbossMonitorManager().printAllJbossCounters();
			}
			else {
				if ( args.length > 0 && Constants.IS_DEBUG == false )
					System.out.println("Invalid option given. Starting agent in normal mode.");
				
				if ( Constants.IS_DEBUG ) {
					System.out.println("Agent started in Debug mode.");
				}
				
				// Monitor JBoss Server
				if( Constants.AGENT_CONFIG!=null ) {
					
					JSONArray jaConfig = JSONArray.fromObject(Constants.AGENT_CONFIG);
					for(int i=0; i<jaConfig.size(); i++) {
						
						joRecord = jaConfig.getJSONObject(i);
						strGUID = joRecord.getString("guid");
						//String strPort = joRecord.getString("port");
						strApp = joRecord.getString("app");
						
						new AgentIgnitorJbossThread(strGUID, strApp).start();
					}
				} else {
					System.out.println("Configuration problem");
				}
				//AgentManager.runGarbageCollectionRountine();
			}
		} catch(Throwable e) {
			System.out.println("Exception in InitServlet.init: "+e.getMessage());
			e.printStackTrace();
		}
	}
}
