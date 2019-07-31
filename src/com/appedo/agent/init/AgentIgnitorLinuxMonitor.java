package com.appedo.agent.init;

import java.util.Timer;
import java.util.TimerTask;

import com.appedo.agent.manager.AppListComparison;
import com.appedo.agent.manager.FileComparison;
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
public class AgentIgnitorLinuxMonitor {
	
	public static void main(String[] args) {
		JSONArray jaConfig = null;
		JSONObject joRecord = null;
		
		try{
			// load config parameters
			Constants.loadConfigProperties();		
			
			if( Constants.AGENT_CONFIG!=null ) {
				jaConfig = JSONArray.fromObject(Constants.AGENT_CONFIG);
				joRecord = jaConfig.getJSONObject(0);
				
				Constants.LINUX_AGENT_GUID = joRecord.getString("guid");
				//System.out.println(Constants.LINUX_AGENT_GUID);
				
				new AgentIgnitorLinuxThread();
				
				if(Constants.APP_INSTALL_COMPARE_MODE) {
					System.out.println("Compare of installing application list is enabled");
					System.out.println("Starting installing application list service.... ");
					//compare two different file services started				
					TimerTask timerTaskCompareOfInstallAppList = new AppListComparison();
					Timer timerComparisonFile = new Timer();
					
					timerComparisonFile.schedule(timerTaskCompareOfInstallAppList, 0, Constants.COMPARISON_FREQUENCY_MILLESECONDS);
				}else {
					System.out.println("Compare of installing application list is disabled!");
				}
				
				if(Constants.FILE_COMPARE_MODE) {
					System.out.println("File compare mode is enabled!");
					System.out.println("Starting compare of file services....");
					
					TimerTask timerTaskCompareOfFile = new FileComparison();
					Timer timerComparisonFile = new Timer();
					
					timerComparisonFile.schedule(timerTaskCompareOfFile, 0, Constants.COMPARISON_FREQUENCY_MILLESECONDS);
					
				}else {
					System.out.println("File compare mode is disabled!");
				}
				
			} else {
				System.out.println("Configuration problem");
			}
			//AgentManager.runGarbageCollectionRountine();
	
			/*//Start compare File code
			//this code create file and insert AmazonS3 
			System.out.println("calling compare file manager java class ");
			CompareFileManager cmpfile = new CompareFileManager();
			cmpfile.getInstallFile();*/
			
		} catch(Throwable e) {
			System.out.println("Exception in InitServlet.init: "+e.getMessage());
			e.printStackTrace();
		}
	}
}