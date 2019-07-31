package com.appedo.agent.manager;

import java.io.IOException;

import javax.management.MBeanServerConnection;

import net.sf.json.JSONObject;

import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpException;
import org.apache.commons.httpclient.HttpStatus;
import org.apache.commons.httpclient.methods.PostMethod;
import org.jboss.as.controller.client.ModelControllerClient;

import com.appedo.agent.utils.Constants;
import com.appedo.agent.utils.Constants.AGENT_TYPE;


public class BeatMonitorManager extends AgentManager {
	
	public static BeatMonitorManager beatMonitorManager = null;
	static String host = "localhost";
	static int port = 0;
	static String password = "";
	static String userid = "";
	static String hostControllerName="master";
	static String serverName="JayServer";
	static boolean standaloneMode=true;//  If you are running your Servers in Domain Mode then set this to false.
	static ModelControllerClient client = null;
	
	MBeanServerConnection connection = null;
	
	
	/**
	 * Avoid the object creation for this Class from outside.
	 */
	private BeatMonitorManager() {
	}
	
	/**
	 * Returns the only object(singleton) of this Class.
	 * 
	 * @return
	 */
	public static BeatMonitorManager getBeatMonitorManager(){
		if( beatMonitorManager == null ){
			beatMonitorManager = new BeatMonitorManager();
		}
		return beatMonitorManager;
	}
	
	/**
	 * Send it to the Collector WebService
	 */
	public void sendBeatCounters(String strGUID){
		sendCounters(strGUID);
	}
	
	/**
	 * Send the collected counter-sets to Collector WebService, by calling parent's sendCounter method
	 */
	public void sendCounters(String strGUID) {
		// send the collected counters to Collector WebService through parent sender function
		//sendCounterToCollector(strGUID, AGENT_TYPE.BEAT);

		String responseJSONStream = null;
		
		PostMethod method = new PostMethod(Constants.WEBSERVICE_URL+"/collectCounters");
		
		try{
			System.out.println(AGENT_TYPE.BEAT+" updating status..");
			
			method.setParameter("agent_type", AGENT_TYPE.BEAT.toString());
			method.setParameter("counter_params_json", "");
			method.setParameter("guid", strGUID);
			method.setParameter("command", "beat");
			HttpClient client = new HttpClient();
			int statusCode = client.executeMethod(method);
			method.setRequestHeader("Connection", "close");
			System.err.println("statusCode: "+statusCode);
			
			if (statusCode != HttpStatus.SC_OK) {
				System.err.println("Method failed: " + method.getStatusLine());
			}
			try {
				responseJSONStream = method.getResponseBodyAsString();
				//System.out.println("Response" + responseJSONStream);
				
				if( responseJSONStream.trim().startsWith("{") && responseJSONStream.trim().endsWith("}")) {
					JSONObject joGUIDConfig = JSONObject.fromObject(responseJSONStream);
					
					if( joGUIDConfig.containsKey("message") && joGUIDConfig.get("message").equals("kill") ) {
						System.out.println("The given application deleted ");
						Thread.sleep(20000);
						System.exit(0);	
					}
				}						
				
			} catch (HttpException he) {
				System.err.println("HTTP Exception in sendCounterToCollector: " + he.getMessage());
			}
		} catch (IOException ie) {
			System.err.println("IO Exception in sendCounterToCollector: " + ie.getMessage());
		} catch (Exception e) {
			System.err.println("Unknown Exception in sendCounterToCollector: " + e);
		} finally {
			method.releaseConnection();
			method = null;
			responseJSONStream = null;
		}
		
	}
	
	@Override
	protected void finalize() throws Throwable {
		super.finalize();
	}
}
