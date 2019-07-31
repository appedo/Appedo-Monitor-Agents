package com.appedo.agent.manager;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.HashMap;

import org.apache.commons.io.IOUtils;

import net.sf.json.JSONArray;
import net.sf.json.JSONObject;

import com.appedo.agent.bean.AgentCounterBean;
import com.appedo.agent.bean.SlaCounterBean;
import com.appedo.agent.utils.Constants;
import com.appedo.agent.utils.Constants.AGENT_TYPE;
import com.appedo.agent.utils.UtilsFactory;

/**
 * Apache monitoring class. This has the functionalities to get the counter values of Apache.
 * 
 * @author 
 *
 */
public class ApacheMonitorManager extends AgentManager {
	
	public static ApacheMonitorManager apacheMonitorManager = null;
	private static HashMap<String, ApacheMonitorManager> hmApplicationInstance = new HashMap<String, ApacheMonitorManager>();
	
	private static HashMap<String, String> hmApacheCountersData = new HashMap<String, String>();
	
	private String hostName = null, appPort = null, monitorURL = null;

	//private ProcessBuilder pbProcStat = null;
	
	/**
	 * Avoid the object creation for this Class from outside.
	 */
	private ApacheMonitorManager() {
		
		//pbProcStat = new ProcessBuilder("bash", "-c", "tail /proc/stat | grep '^cpu '");
		
	}
	
	/**
	 * Param constructor which initializes the host name and app port
	 * @param strHostName
	 * @param strAppPort
	 */
	private ApacheMonitorManager(String strHostName, String strAppPort, String strMonitorURL) {
		this.hostName = strHostName;
		this.appPort = strAppPort;
		this.monitorURL = strMonitorURL;
	}
	
	/**
	 * Returns the only object(singleton) of this Class.
	 * 
	 * @return
	 */
	public static ApacheMonitorManager getApacheMonitorManager(String strHostName, String strAppPort, String strMonitorURL){
		if(hmApplicationInstance.containsKey(strHostName + " - " + strAppPort)){
			apacheMonitorManager = hmApplicationInstance.get(strHostName + " - " + strAppPort);
		}else{
			apacheMonitorManager = new ApacheMonitorManager(strHostName, strAppPort, strMonitorURL);
			hmApplicationInstance.put(strHostName + " - " + strAppPort, apacheMonitorManager);
		}
		
		return apacheMonitorManager;
	}
	
	/**
	 * Monitor the server and collect the counters
	 */
	public void monitorApacheServer(){
		getCounters();
	}
	
	/**
	 * Send it to the Collector WebService
	 */
	public void sendApacheCounters(){
		sendCounters();
	}
	
	/**
	 * Collect the counter with agent's types own logic or native methods
	 * 
	 */
	public void getCounters() {
		int nCounterId ;
		String query = "";
		
		// create variable's to capture execution_type & is_delta
		boolean bIsDelta = false;
		//String strExecutionType = "";
		
		try {
			Double dCounterValue = 0.0;
			
			// reset the counter collector variable in AgentManager.
			resetCounterMap(Constants.APACHE_AGENT_GUID);
			JSONArray joSelectedCounters = AgentCounterBean.getCountersBean(Constants.APACHE_AGENT_GUID);
			
			// loads all Apache counters data from URL and adds into HashMap
			loadAllApacheCountersData();
			
			for(int i = 0; i < joSelectedCounters.size(); i++){
				dCounterValue = 0.0;
				nCounterId = 0;
				query = "";
				
				JSONObject joSelectedCounter = joSelectedCounters.getJSONObject(i);			
				nCounterId = Integer.parseInt(joSelectedCounter.getString("counter_id")) ;
				query = joSelectedCounter.getString("query");
				bIsDelta = joSelectedCounter.getBoolean("isdelta");
				//strExecutionType = joSelectedCounter.getString("executiontype");
				
				try {
					if ( ! containsApacheCounter(query) ) {
						// counter not found in the apache server
						System.out.println("Exception: Counter '"+query+"' not found.");
					} else {
						// gets Apache counter value from query
						dCounterValue = Double.parseDouble( getApacheCounterValue(query) );
						
						if(bIsDelta) {
							dCounterValue = addDeltaCounterValue(nCounterId, dCounterValue);					
						}else {
							addCounterValue(nCounterId, dCounterValue);
						}
						// TODO: Static Counter correction required

		            	// Verify SLA Breach
						ArrayList<JSONObject> alSLACounters = null;
						alSLACounters = verifySLABreach(Constants.APACHE_AGENT_GUID, SlaCounterBean.getSLACountersBean(Constants.APACHE_AGENT_GUID), nCounterId, dCounterValue );
						
						// If breached then add it to Collector's collection
						if( alSLACounters != null ) {
							addSlaCounterValue(alSLACounters);
						}
					}
				} catch(Throwable th) {
					System.out.println("Exception in getApacheCounters: "+th.getMessage());
					th.printStackTrace();
					//reportCounterError(nCounterId, e.getMessage());
				} finally {
				}
			}
		
		} catch (Exception e) {
			System.out.println("Exception in getApacheCounters: "+e.getMessage());
			e.printStackTrace();
			reportGlobalError(e.getMessage());
		} finally {
			// queue the counter
			try {
				queueCounterValues();
			} catch (Exception e1) {
				System.out.println("Exception in queueCounterValues(): "+e1.getMessage());
				e1.printStackTrace();
			}
		}
	}
	
	
	/**
	 * Send the collected counter-sets to Collector WebService, by calling parent's sendCounter method
	 */
	public void sendCounters() {
		
		// send the collected counters to Collector WebService through parent sender function
		sendCounterToCollector(Constants.APACHE_AGENT_GUID, AGENT_TYPE.APACHE);
		sendSlaCounterToCollector(Constants.APACHE_AGENT_GUID, AGENT_TYPE.APACHE);
	}
	
	
	private String getErrorString(InputStream errorStream) {
		InputStreamReader isrError = null;
		BufferedReader rError = null;
		String line = null;
		StringBuilder sbError = new StringBuilder();
		
		try{
			isrError = new InputStreamReader(errorStream);
			rError = new BufferedReader(isrError);
			sbError.setLength(0);
			while ((line = rError.readLine()) != null) {
				sbError.append(line).append(" ");
			}
			if( sbError.length() > 0 ){
				sbError.deleteCharAt(sbError.length()-1);
				
				System.out.println("sbError in CPU: "+sbError);
			}
		} catch ( Exception e ) {
			System.out.println("Exception in getErrorString: "+e.getMessage());
			e.printStackTrace();
		} finally {
			try{
				isrError.close();
			} catch(Exception e) {
				System.out.println("Exception in isrError.close(): "+e.getMessage());
				e.printStackTrace();
			}
			isrError = null;
			try{
				rError.close();
			} catch(Exception e) {
				System.out.println("Exception in rError.destroy(): "+e.getMessage());
				e.printStackTrace();
			}
			rError = null;
		}
		
		return sbError.toString();
	}
	
	private void loadAllApacheCountersData() throws Exception {
		String strApacheCountersData = "", strEncoding = "", strLine = "";
		
		URL url = null;
		URLConnection urlConnection = null;
		
		InputStream in = null;
		
		StringReader stringReaderCountersData = null;
		BufferedReader buffReaderApacheCounters = null;
		
		String[] saCounterValue = null;
		
		try {
			if( monitorURL == null ) {
				//url = new URL("http://54.149.100.7/server-status/?auto");
				url = new URL("http://" + hostName + ":" + appPort + "/server-status/?auto");
			} else {
				url = new URL( monitorURL );	
			}
			
			urlConnection = url.openConnection();
			in = urlConnection.getInputStream();
			
			strEncoding = urlConnection.getContentEncoding();
			strEncoding = (strEncoding == null ? "UTF-8" : strEncoding);
			
			// all Apache Counters Data 
			strApacheCountersData = IOUtils.toString(in, strEncoding);
			
			// stores all counters data into HashMap
			stringReaderCountersData = new StringReader(strApacheCountersData);
			buffReaderApacheCounters = new BufferedReader(stringReaderCountersData);
			// only one line should get returned. so use IF instead of WHILE
			while ( (strLine = buffReaderApacheCounters.readLine()) != null ) {
				saCounterValue = strLine.split(":");
				if ( saCounterValue.length == 2 ) {
					addApacheCounter(saCounterValue[0].trim(), saCounterValue[1].trim());
				}
			}
		} catch (Exception e) {
			System.out.println("Unable to loadAllApacheCounters: "+e.getMessage());
			e.printStackTrace();
			throw e;
		} finally {
			UtilsFactory.close(in);
			in = null;
			
			UtilsFactory.close(stringReaderCountersData);
			stringReaderCountersData = null;
			
			UtilsFactory.close(buffReaderApacheCounters);
			buffReaderApacheCounters = null;
		}
	}
	/*
	private Object[] getCounterValue(String[] saCounterValue, String strCounterName) {
		Double dCounterValue = 0D;
		boolean bCounterMatches = false;
		
		if ( saCounterValue[0].trim().equalsIgnoreCase(strCounterName) ) {
			System.out.println("saCounterValue[0]: "+saCounterValue[0]+" <> strCounterName: "+strCounterName+" <> "+saCounterValue[0].trim().equalsIgnoreCase(strCounterName));
			dCounterValue = Double.parseDouble(saCounterValue[1].trim());
			bCounterMatches = true;
			System.out.println("dCounterValue: "+dCounterValue);
		}
		
		return (new Object[]{dCounterValue, bCounterMatches});
	}*/
	
	private boolean isCounterMatches(String[] saCounterValue, String strCounterName) {
		boolean bCounterMatches = false;
		
		if ( saCounterValue[0].trim().equalsIgnoreCase(strCounterName) ) {
			bCounterMatches = true;
		}
		
		return bCounterMatches;
	}
	
	/**
	 * stores apache counters data
	 * 
	 * @param strCounterName
	 * @param objCounterValue
	 */
	private static void addApacheCounter(String strCounterName, String strCounterValue){
		hmApacheCountersData.put(strCounterName, strCounterValue);
	}
	
	/**
	 * get counter value
	 * 
	 * @param strCounterName
	 * @return
	 */
	private static String getApacheCounterValue(String strCounterName) {
		return hmApacheCountersData.get(strCounterName);
	}
	
	/**
	 * check counter contains
	 * 
	 * @param strCounterName
	 * @return
	 */
	private static boolean containsApacheCounter(String strCounterName) {
		return hmApacheCountersData.containsKey(strCounterName);
	}
	
	@Override
	protected void finalize() throws Throwable {
		clearCounterMap();
		
		super.finalize();
	}
}
