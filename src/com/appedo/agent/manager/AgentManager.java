package com.appedo.agent.manager;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;

import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpException;
import org.apache.commons.httpclient.HttpStatus;
import org.apache.commons.httpclient.methods.PostMethod;

import com.appedo.agent.bean.AgentCounterBean;
import com.appedo.agent.bean.LinuxUnificationBean;
import com.appedo.agent.bean.LinuxUnificationSLACounterBean;
import com.appedo.agent.bean.SlaCounterBean;
import com.appedo.agent.utils.Constants;
import com.appedo.agent.utils.Constants.AGENT_TYPE;
import com.appedo.agent.utils.Constants.SLA_BREACH_SEVERITY;
import com.appedo.agent.utils.UtilsFactory;

import net.sf.json.JSONArray;
import net.sf.json.JSONObject;

/**
 * Manager to do common activities for any agent.
 * Activities such as send first request, send counter values are defined here.
 * 
 * @author Veeru
 *
 */
public class AgentManager {
	
	// Map variable to keep the collected Counters
	//public PriorityBlockingQueue< HashMap<Integer,Object> > queCounters = new PriorityBlockingQueue< HashMap<Integer,Object> >();
	
	// Map variable to keep the collected Counters
	public HashMap<String,Object> hmCounters = null;
	public HashMap<String,Object> hmSlaCounters = null;
	private int nCounterALIndex = 0;
	
	public ArrayList< HashMap<String,Object> > alCounters = new ArrayList< HashMap<String,Object> >();
	public ArrayList<JSONObject> alSlaCounters = new ArrayList<JSONObject >();
	// create map to store
		
	private HttpClient client = new HttpClient();
	private HashMap<String, Long> hmDeltaCounter = new HashMap<String, Long>();
	
	private HashMap<String, Double> hmLinuxDeltaCounter = new HashMap<String, Double>();
	public static HashMap<String, Double> hmQuickViewCounterValues = new HashMap<String, Double>();

	public boolean is_first = true;
	
	/**
	 * Constructor which initializes the HashMap which holds the counter-set and the ArrayList which holds the counter-sets
	 */
	public AgentManager() {
		// @TODO sending is merged with collector
		//alCounters = new ArrayList< HashMap<String,Object> >(Constants.MONITOR_FREQUENCY);
		//for(int i=0; i<Constants.MONITOR_FREQUENCY; i++ ){
		alCounters = new ArrayList< HashMap<String,Object> >(1);
		for(int i=0; i<1; i++ ){
			HashMap<String,Object> hm = new HashMap<String,Object>();
			alCounters.add(hm);
		}
		
		nCounterALIndex = 0;
		hmCounters = alCounters.get(nCounterALIndex);
		
		alSlaCounters = new ArrayList<JSONObject >();
	}
	
	/**
	 * Reset the counter pointer for the ArrayList. And clear the objects in the ArrayList
	 */
	private void resetCounterMapArray() {
		nCounterALIndex = 0;
		
		// don't clear the ArrayList itself.
		// otherwise the ArrayList has to be recreated again; so new objects will be created in heap
		for(int i=0; i<alCounters.size(); i++ ){
			UtilsFactory.clearCollectionHieracy(alCounters.get(i));
		}
	}
	
	private void resetSlaCounterMapArray() {
		alSlaCounters = new ArrayList<JSONObject >();
	}
	
	private void resetSlowQryMapArray() {
		AgentCounterBean.setLastCollectedSlowQueries(null);
	}
	
	/**
	 * Reset the active counter object
	 * 
	 * @return
	 */
	public boolean resetCounterMap(String strGUID){
		
		UtilsFactory.clearCollectionHieracy(hmCounters);
		
		// hmCounters = new HashMap<Integer,Object>();
		
		Package aPackage = Constants.class.getPackage();
		hmCounters.put("1001", "\""+strGUID+"\"");
		hmCounters.put("1002", "\""+UtilsFactory.nowFormattedDate()+"\"");	// Current time stamp of the client machine(where agent is running); graph will use this timestamp only
		hmCounters.put("1004", "\""+aPackage.getSpecificationVersion()+ "-" +aPackage.getImplementationVersion()+"\"");
		hmCounters.put("1005", new HashMap<String,Object>());
		
		return true;
	}
	
	
	/**
	 * Add a counter code-value pair to the counter-set holder.
	 * To add top utilization of processor
	 * @param nCounterCode
	 * @param objCounterValue
	 * @return
	 */
	public boolean addTopProcessCounter(String strCounterCode, Object objCounterValue){
		hmCounters.put(strCounterCode, objCounterValue);
		
		return true;
	}
	
	/**
	 * Add a counter code-value pair to the counter-set holder.
	 * 
	 * @param nCounterCode
	 * @param objCounterValue
	 * @return
	 */
	public boolean addCounterValue(Integer nCounterCode, Object objCounterValue){
		hmCounters.put(Integer.toString(nCounterCode), objCounterValue);
		
		return true;
	}
	
	
	/**
	 * add sla breach counters
	 * 
	 * @param nCounterCode
	 * @param objCounterValue
	 * @return
	 */
	public boolean addSlaCounterValue(ArrayList<JSONObject> joSLABreachCounterSet){
		if( joSLABreachCounterSet != null && joSLABreachCounterSet.size() > 0 ) {
			alSlaCounters.addAll(joSLABreachCounterSet);
			return true;
		} else {
			return false;
		}
	}
	
	/**
	 * add Slow Qry's arry list
	 * 
	 * @param nCounterCode
	 * @param objCounterValue
	 * @return
	 */
	public boolean addSlowQryCounterValue(String strSlowQryCounterSet){
		AgentCounterBean.setLastCollectedSlowQueries(strSlowQryCounterSet);
		return true;
	}
			
	/**
	 * Once a counter-set is populated. Calling this function will move the pointer to next counter-set(HashMap) in the ArrayList.
	 * 
	 * @throws Exception
	 */
	public void queueCounterValues() throws Exception {
		if ( hmCounters == null ) {
			throw new Exception("Object hmCounters should be initailized and value should be set.");
		} else if ( hmCounters.containsKey("1001") == false ) {
			throw new Exception("Object hmCounters should set 1001 with UID.");
		} else if ( hmCounters.containsKey("1002") == false ) {
			throw new Exception("Object hmCounters should set then timestamp.");
		}
		
		// move index 
		nCounterALIndex = (++nCounterALIndex)%alCounters.size();
		
		hmCounters = alCounters.get(nCounterALIndex);
	}
	
	public String output(InputStream inputStream) throws IOException {		
        StringBuilder sb = new StringBuilder();
        BufferedReader br = null;
        try {
            br = new BufferedReader(new InputStreamReader(inputStream));
            String line = null;
            while ((line = br.readLine()) != null) {
            	sb.append(line + System.getProperty("line.separator"));
            }
        } finally {
            br.close();
        }
        return sb.toString();
	}


	public boolean sendBytesToCollector(byte[] dataBytes, String GUID, String command, String fileName) {
		boolean bReturn = false;
		String responseJSONStream = null;
		
		PostMethod method = new PostMethod(Constants.WEBSERVICE_URL+"/collectDataBytes");
		
		//PostMethod method = new PostMethod("http://localhost:8080/Appedo-Collector/collectDataBytes");
		
		
		try {
			
			String databytes = new String(dataBytes);
			method.setParameter("dataBytes", databytes);
			method.setParameter("GUID", GUID);
			method.setParameter("installed_app_on", System.currentTimeMillis()+"");
			method.setParameter("fileName", fileName);
			method.setParameter("command", command);
			
			System.out.println("send databytes to collector..");
			
			int statusCode = client.executeMethod(method);
			System.err.println("statusCode of fileComparison: "+statusCode);
			
			if (statusCode != HttpStatus.SC_OK) {
				System.err.println("Method failed: " + method.getStatusLine());
			}
			
			try {
				responseJSONStream = method.getResponseBodyAsString();
				if( responseJSONStream.trim().startsWith("{") && responseJSONStream.trim().endsWith("}")) {
					JSONObject joResponse = JSONObject.fromObject(responseJSONStream);
					if( joResponse.containsKey("message")) {
						System.out.println("collector success message return..");
						bReturn = true;
					}else if(joResponse.containsKey("errorMessage")) {
						System.out.println("File Compare ErrorMessage: "+joResponse.getString("errorMessage"));
						bReturn = false;
						System.err.println("Exception in fileComparison from collector: "+joResponse.getString("errorMessage"));
					}
				}
				
			}catch (HttpException he) {
				System.err.println("HTTP Exception in sendDataBytesToCollector: "+he.getMessage());
				//LogManager.errorLog(he);
				bReturn = false;
			}
			
		} catch (IOException ie) {
			System.err.println("IO Exception in sendDataBytesToCollector: "+ie.getMessage());
			//LogManager.errorLog(ie);
		} catch (Exception e) {
			System.err.println("Unknown Exception in sendDataBytesToCollector: "+e.getMessage());
			//LogManager.errorLog(e);
		} finally {
			method.releaseConnection();
			method = null;
			UtilsFactory.clearCollectionHieracy(hmCounters);
			responseJSONStream = null;
		}
		return bReturn;
	}
	
	public String sendSystemGeneratorUUIDToCollector(String sysGeneratorUUID) {
		
		String responseJSONStream = null, response = "", SystemId = "";
		
		PostMethod method = new PostMethod(Constants.WEBSERVICE_URL+"/javaUnification");
		
		try{
			method.setParameter("systemGeneratorUUID", sysGeneratorUUID);
			method.setParameter("command", "systemGeneratorInfo");
			method.setRequestHeader("Connection", "close");
			
			int statusCode = client.executeMethod(method);
			System.err.println("send SysInfo of statusCode: "+statusCode);
			
			if (statusCode != HttpStatus.SC_OK) {
				System.err.println("Method failed: " + method.getStatusLine());
			}
			try {
				responseJSONStream = method.getResponseBodyAsString();
				//System.out.println("Response" + responseJSONStream);
				
				if( responseJSONStream.trim().startsWith("{") && responseJSONStream.trim().endsWith("}")) {
					JSONObject joGUIDConfig = JSONObject.fromObject(responseJSONStream);
						
					if(joGUIDConfig.getBoolean("success") && joGUIDConfig.containsKey("message")) {
						response = joGUIDConfig.get("message").toString();
						System.out.println(response);
						SystemId = joGUIDConfig.getString("systemId");
					}else {
						System.out.println(joGUIDConfig);
					}
									
				}						
				
			} catch (HttpException he) {
				System.err.println("HTTP Exception in sendSysInfoToCollector: " + he.getMessage());
			}
		} catch (IOException ie) {
			System.err.println("IO Exception in sendSysInfoToCollector: " + ie.getMessage());
		} catch (Exception e) {
			System.err.println("Unknown Exception in sendSysInfoToCollector: " + e);
		} finally {
			method.releaseConnection();
			method = null;
			responseJSONStream = null;
		}
		
		//return response;
		return SystemId;
	}
	
	
	/**
	 * send the System Information.
	 * 
	 * @param JSONObject
	 * @return
	 * 
	 */
	public String sendSysInfoToCollector(JSONObject sysInfo) {
	
		String responseJSONStream = null, response = "", SystemId = "";
		
		PostMethod method = new PostMethod(Constants.WEBSERVICE_URL+"/javaUnification");
		
		try{
			method.setParameter("systemInformation", sysInfo.toString());
			method.setParameter("command", "systemInformation");
			
			if(Constants.IS_VMWARE_KEY) {
				method.setParameter("VMWARE_Key", Constants.VMWARE_KEY);
			}else {
				method.setParameter("VMWARE_Key", "");
			}
			method.setParameter("Encrypted_id", Constants.ENCRYPTED_ID);
			method.setRequestHeader("Connection", "close");
			
			int statusCode = client.executeMethod(method);
			System.err.println("send SysInfo of statusCode: "+statusCode);
			
			if (statusCode != HttpStatus.SC_OK) {
				System.err.println("Method failed: " + method.getStatusLine());
			}
			try {
				responseJSONStream = method.getResponseBodyAsString();
				//System.out.println("Response" + responseJSONStream);
				
				if( responseJSONStream.trim().startsWith("{") && responseJSONStream.trim().endsWith("}")) {
					JSONObject joGUIDConfig = JSONObject.fromObject(responseJSONStream);
						
					if(joGUIDConfig.getBoolean("success") && joGUIDConfig.containsKey("message")) {
						response = joGUIDConfig.get("message").toString();
						System.out.println(response);
						SystemId = joGUIDConfig.getString("systemId");
					}else {
						System.out.println(joGUIDConfig);
					}
									
				}						
				
			} catch (HttpException he) {
				System.err.println("HTTP Exception in sendSysInfoToCollector: " + he.getMessage());
			}
		} catch (IOException ie) {
			System.err.println("IO Exception in sendSysInfoToCollector: " + ie.getMessage());
		} catch (Exception e) {
			System.err.println("Unknown Exception in sendSysInfoToCollector: " + e);
		} finally {
			method.releaseConnection();
			method = null;
			responseJSONStream = null;
		}
		
		//return response;
		return SystemId;
	}
	
	public void sendJBossAppInfoToCollector(JSONObject moduleInfo, String GUID, String command) {
		
		String responseJSONStream = null;
		
		PostMethod method = new PostMethod(Constants.WEBSERVICE_URL+"/javaUnification");
		
		try{
						
			method.setParameter("moduleInformation", moduleInfo.toString());
			method.setParameter("GUID", GUID);
			method.setParameter("command", command);
			method.setRequestHeader("Connection", "close");
			
			int statusCode = client.executeMethod(method);
			LogManagerExtended.commonInfoLog("send sendJBossAppInfoToCollector of statusCode: "+statusCode, "appInformation");
			
			if (statusCode != HttpStatus.SC_OK) {
				LogManagerExtended.commonInfoLog("Method failed: " + method.getStatusLine(), "appInformation");
				//System.err.println("Method failed: " + method.getStatusLine());
			}
			try {
				
				responseJSONStream = method.getResponseBodyAsString();
				LogManagerExtended.commonInfoLog("Response" + responseJSONStream, "appInformation");
				
				
				
				if( responseJSONStream.trim().startsWith("{") && responseJSONStream.trim().endsWith("}")) {
					JSONObject joGUIDConfig = JSONObject.fromObject(responseJSONStream);
						
					if(joGUIDConfig.getBoolean("success") && joGUIDConfig.containsKey("message")) {
						
						LogManagerExtended.commonInfoLog(joGUIDConfig.getString("message"), "appInformation");
						
					}else {
						LogManagerExtended.commonInfoLog("Exception in sendJBossAppInfoToCollector Response errorMessage : "+ joGUIDConfig.get("errorMessage"), "appInformation");
					}
									
				}						
				
			} catch (HttpException he) {
				LogManagerExtended.commonInfoLog("HTTP Exception in sendJBossAppInfoToCollector: " + he.getMessage(), "appInformation");
			}
		} catch (IOException ie) {
			LogManagerExtended.commonInfoLog("IO Exception in sendJBossAppInfoToCollector: " + ie.getMessage(), "appInformation");
		} catch (Exception e) {
			LogManagerExtended.commonInfoLog("Unknown Exception in sendJBossAppInfoToCollector: " + e, "appInformation");
		} finally {
			method.releaseConnection();
			method = null;
			responseJSONStream = null;
		}
	}
	
	/**
	 * send the System Information.
	 * 
	 * @param JSONObject
	 * @return GUID
	 * 
	 */
	public String sendModuleInfoToCollector(JSONObject moduleInfo, String systemId, String UUID, JSONObject joCounterSet, String command) {
	
		String responseJSONStream = null, ModuleGUID = "";
		
		PostMethod method = new PostMethod(Constants.WEBSERVICE_URL+"/javaUnification");
		
		String strUUID = (Constants.IS_VMWARE_KEY ? UUID+"-VM"+Constants.VMWARE_KEY : UUID);
		
		JSONObject joResponseMessage = null;
		try{
						
			method.setParameter("moduleInformation", moduleInfo.toString());
			method.setParameter("systemId", systemId);
			method.setParameter("UUID", strUUID);
			method.setParameter("Enterprise_Id", Constants.ENTERPRISE_ID);
			method.setParameter("jocounterSet", joCounterSet.toString());
			method.setParameter("command", command);
			method.setRequestHeader("Connection", "close");
			
			int statusCode = client.executeMethod(method);
			LogManagerExtended.commonInfoLog("send ModuleInfo of statusCode: "+statusCode, command);
			
			//System.err.println("send ModuleInfo of statusCode: "+statusCode);
			
			if (statusCode != HttpStatus.SC_OK) {
				LogManagerExtended.commonInfoLog("Method failed: " + method.getStatusLine(), command);
				//System.err.println("Method failed: " + method.getStatusLine());
			}
			try {
				
				responseJSONStream = method.getResponseBodyAsString();
				LogManagerExtended.commonInfoLog("Response" + responseJSONStream, command);
				
				if( responseJSONStream.trim().startsWith("{") && responseJSONStream.trim().endsWith("}")) {
					JSONObject joGUIDConfig = JSONObject.fromObject(responseJSONStream);
						
					if(joGUIDConfig.getBoolean("success") && joGUIDConfig.containsKey("message")) {
						
						joResponseMessage = joGUIDConfig.getJSONObject("message");
						ModuleGUID = joResponseMessage.getString("moduleGUID");
						
						if(command.equalsIgnoreCase("serverInformation")) {
							Constants.LINUX_SERVER_MODULE_TYPE = joResponseMessage.getString("moduleTypeName");
						}/*else if(command.equalsIgnoreCase("appInformation")){
							Constants.LINUX_APP_MODULE_TYPE = joResponseMessage.getString("moduleTypeName");
						}else if(command.equalsIgnoreCase("DBInformation")) {
							Constants.LINUX_DB_MODULE_TYPE = joResponseMessage.getString("moduleTypeName");
						}*/
					}else {
						LogManagerExtended.commonInfoLog("Exception in sendModuleInfoToCollector Response errorMessage : "+ joGUIDConfig.get("errorMessage"), command);
					}
									
				}						
				
			} catch (HttpException he) {
				LogManagerExtended.commonInfoLog("HTTP Exception in sendModuleInfoToCollector: " + he.getMessage(), command);
			}
		} catch (IOException ie) {
			LogManagerExtended.commonInfoLog("IO Exception in sendModuleInfoToCollector: " + ie.getMessage(), command);
		} catch (Exception e) {
			LogManagerExtended.commonInfoLog("Unknown Exception in sendModuleInfoToCollector: " + e, command);
		} finally {
			method.releaseConnection();
			method = null;
			responseJSONStream = null;
		}
		return ModuleGUID;
	}
	
	public boolean sendModuleCountersToCollector(JSONObject counterSet, String strGUID) {
		
		String responseJSONStream = null, response = "";
		
		boolean isCounterSetUpdated = false;
		
		PostMethod method = new PostMethod(Constants.WEBSERVICE_URL+"/javaUnification");
		 
		try{
			method.setParameter("counterSet", counterSet.toString());
			method.setParameter("GUID", strGUID);
			method.setParameter("command", "updateCounters");
			method.setRequestHeader("Connection", "close");
			
			int statusCode = client.executeMethod(method);
			System.err.println("send ModuleCounterSet of statusCode: "+statusCode);
			
			if (statusCode != HttpStatus.SC_OK) {
				System.err.println("Method failed: " + method.getStatusLine());
			}
			try {
				
				responseJSONStream = method.getResponseBodyAsString();
				//System.out.println("Response" + responseJSONStream);
				
				if( responseJSONStream.trim().startsWith("{") && responseJSONStream.trim().endsWith("}")) {
					JSONObject joGUIDConfig = JSONObject.fromObject(responseJSONStream);
						
					if(joGUIDConfig.getBoolean("success") && joGUIDConfig.containsKey("message")) {
						response = joGUIDConfig.get("message").toString();
						System.out.println(response);
						isCounterSetUpdated = true;
						//ModuleGUID = joGUIDConfig.getString("moduleGUID");
					}else {	
						System.out.println("Exception in sendOSInfoToCollector Response errorMessage : "+ joGUIDConfig.get("errorMessage"));
					}
				}						
				
			} catch (HttpException he) {
				System.err.println("HTTP Exception in sendOSInfoToCollector: " + he.getMessage());
			}
		} catch (IOException ie) {
			System.err.println("IO Exception in sendOSInfoToCollector: " + ie.getMessage());
		} catch (Exception e) {
			System.err.println("Unknown Exception in sendOSInfoToCollector: " + e);
		} finally {
			method.releaseConnection();
			method = null;
			responseJSONStream = null;
		}
		
		return isCounterSetUpdated;
		//return ModuleGUID;
	}
	
	public String getModuleRunningStatus(String GUID, String moduleInfo) {
		
		String responseJSONStream = null, response = "";
		
		PostMethod method = new PostMethod(Constants.WEBSERVICE_URL+"/javaUnification");
		
		try{
			
			method.setParameter("GUID", GUID);
			method.setParameter("command", "moduleRunningStatus");
			method.setRequestHeader("Connection", "close");
			
			int statusCode = client.executeMethod(method);
			LogManagerExtended.commonInfoLog("getModuleRunningStatus of statusCode: "+statusCode, moduleInfo);
			
			if (statusCode != HttpStatus.SC_OK) {
				LogManagerExtended.commonInfoLog("Method failed: " + method.getStatusLine(), moduleInfo);
			}
			try {
				
				responseJSONStream = method.getResponseBodyAsString();
				//System.out.println("Response" + responseJSONStream);
				
				if( responseJSONStream.trim().startsWith("{") && responseJSONStream.trim().endsWith("}")) {
					JSONObject joGUIDConfig = JSONObject.fromObject(responseJSONStream);
					
					if(joGUIDConfig.getBoolean("success") && joGUIDConfig.containsKey("message")) {
						response = joGUIDConfig.get("message").toString();
						LogManagerExtended.commonInfoLog(response, moduleInfo);
					}else {
						LogManagerExtended.commonInfoLog("Exception in sendOSInfoToCollector Response errorMessage : "+ joGUIDConfig.get("errorMessage"), moduleInfo);
					}
									
				}						
				
			} catch (HttpException he) {
				LogManagerExtended.commonInfoLog("HTTP Exception in sendOSInfoToCollector: " + he.getMessage(), moduleInfo);
			}
		} catch (IOException ie) {
			LogManagerExtended.commonInfoLog("IO Exception in sendOSInfoToCollector: " + ie.getMessage(), moduleInfo);
		} catch (Exception e) {
			LogManagerExtended.commonInfoLog("Unknown Exception in sendOSInfoToCollector: " + e, moduleInfo);
		} finally {
			method.releaseConnection();
			method = null;
			responseJSONStream = null;
		}
		
		return response;
	}
	
	/**
	 * send the counter-sets available in the ArrayList to the configured WebService.
	 * 
	 * @param agent_type
	 * @return
	 */
	public boolean sendCounterToCollector(String strGUID, AGENT_TYPE agent_type) {
	
		boolean bMonitorReturn = false;
		boolean bSlaReturn = false;
		boolean bReturn =false;
		String responseJSONStream = null;
		
		PostMethod method = new PostMethod(Constants.WEBSERVICE_URL+"/collectCounters");
		
		try{
			System.out.println(agent_type.toString()+" sending hmCounters: "+alCounters.toString());
			
			method.setParameter("agent_type", agent_type.toString());
			method.setParameter("counter_params_json", alCounters.toString());
			method.setParameter("guid", strGUID);
			method.setParameter("command", "");
			method.setRequestHeader("Connection", "close");
			
			int statusCode = client.executeMethod(method);
			System.err.println("statusCode: "+statusCode);
			
			if (statusCode != HttpStatus.SC_OK) {
				System.err.println("Method failed: " + method.getStatusLine());
			}
			try {
				responseJSONStream = method.getResponseBodyAsString();
				//System.out.println("Response" + responseJSONStream);
				
				if( responseJSONStream.trim().startsWith("{") && responseJSONStream.trim().endsWith("}")) {
					JSONObject joGUIDConfig = JSONObject.fromObject(responseJSONStream);
					
					if (is_first && joGUIDConfig.getBoolean("success")) {
							is_first = false;
					}

					if( joGUIDConfig.containsKey("message") && joGUIDConfig.get("message").equals("kill") ) {
						System.out.println("The given application deleted ");
						Thread.sleep(20000);
						System.exit(0);	
					}
					

					// Update monitor counters in bean
					if((joGUIDConfig.containsKey("MonitorCounterSet") && joGUIDConfig.getString("MonitorCounterSet")!=null) && (!joGUIDConfig.getString("MonitorCounterSet").equalsIgnoreCase("null"))) {
						bMonitorReturn = updateCounters(strGUID, joGUIDConfig.getJSONArray("MonitorCounterSet"));
					}
					
					// update sla counters in bean
					if( (joGUIDConfig.containsKey("SlaCounterSet") && joGUIDConfig.getString("SlaCounterSet")!=null) && (!joGUIDConfig.getString("SlaCounterSet").equalsIgnoreCase("null")) ) {
							bSlaReturn = updateSlaCounters(strGUID, joGUIDConfig.getJSONArray("SlaCounterSet"));
					}
					
//					if( (js.containsKey("moduleupgrade"))) {
//						System.out.println("INSIDE***************");
//						Process p = null;
//				    	BufferedReader in = null;
//				    	String s = "";
//				    	int status;
//			    		Runtime rt = Runtime.getRuntime();
//			    		p = rt.exec("java -jar autoinstaller.jar "+strGUID+" " +"test "+ ManagementFactory.getRuntimeMXBean().getName() );
//			    		in = new BufferedReader(new InputStreamReader(p.getInputStream()));
//			    		
//			    		while((s = in.readLine()) != null){
//			    			System.out.println(s);
//			    	    }
//			    		
//			    		status = p.waitFor();
//			    		System.out.println("Exited status: " + status);
//					}
				}						
				
				// reset counters lis
				resetCounterMapArray();
				
				if(bMonitorReturn || bSlaReturn) {
					bReturn = true;
				}
				
				bReturn = true;
			} catch (HttpException he) {
				System.err.println("HTTP Exception in sendCounterToCollector: " + he.getMessage());
				bReturn = false;
			}
		} catch (IOException ie) {
			System.err.println("IO Exception in sendCounterToCollector: " + ie.getMessage());
		} catch (Exception e) {
			System.err.println("Unknown Exception in sendCounterToCollector: " + e);
		} finally {
			method.releaseConnection();
			method = null;
			UtilsFactory.clearCollectionHieracy(hmCounters);
			responseJSONStream = null;
		}
		
		return bReturn;
	}
	
	/**
	 * send the counter-sets available in the ArrayList to the configured WebService.
	 * 
	 * @param agent_type
	 * @return
	 */
	public boolean sendSlowQryToCollector(String strGUID,AGENT_TYPE agent_type) {
	
		boolean bMonitorReturn = false;
		boolean bSlaReturn = false;
		boolean bReturn = false;
		
		PostMethod method = new PostMethod(Constants.WEBSERVICE_URL+"/collectCounters");
		String responseJSONStream = null;
		
		try{
			if( AgentCounterBean.getLastCollectedSlowQueries() != null ) {
				//System.out.println(agent_type.toString()+"Slow Query sending hmCounters: "+AgentCounterBean.getStrSlowQry());
				
				method.setParameter("agent_type", agent_type.toString());
				method.setParameter("command", "slowqry");
				method.setParameter("guid", strGUID);
				method.setParameter("profiler_array_json", AgentCounterBean.getLastCollectedSlowQueries());
				method.setParameter("timer_thread_id", "");
				method.setRequestHeader("Connection", "close");
				
				int statusCode = client.executeMethod(method);
				
				System.err.println("statusCode: "+statusCode);
				
				if (statusCode != HttpStatus.SC_OK) {
					System.err.println("Method failed: " + method.getStatusLine());
				}
				
				try {
					responseJSONStream = method.getResponseBodyAsString();
//					System.out.println("sla Response" + responseJSONStream);
					
					if( responseJSONStream.trim().startsWith("{") && responseJSONStream.trim().endsWith("}")) {
						
						JSONObject js = JSONObject.fromObject(responseJSONStream);
						if(js.containsKey("message")) {
							
							if(js.get("message").equals("kill")) {
								System.out.println("The given application deleted ");
//								Thread.sleep(20000);
								System.exit(0);
							}						
						}
						
//
//						// Update monitor counters in bean
//						if((js.containsKey("MonitorCounterSet") && js.getString("MonitorCounterSet")!=null) && (!js.getString("MonitorCounterSet").equalsIgnoreCase("null"))) {
//								bMonitorReturn = updateCounters(strGUID,responseJSONStream);
//						}
//						
//						// update sla counters in bean
//						if( (js.containsKey("SlaCounterSet") && js.getString("SlaCounterSet")!=null) && (!js.getString("SlaCounterSet").equalsIgnoreCase("null")) ) {
//								bSlaReturn = updateSlaCounters(strGUID,responseJSONStream);
//						}
					}						
					
					// reset counters list
					resetSlowQryMapArray();
					
					if(bMonitorReturn || bSlaReturn) {
						bReturn = true;
					}
					
				} catch (HttpException he) {
					
					System.err.println("HTTP Exception in sendSlaCounterToCollector: " + he.getMessage());
					bReturn = false;
				}
			} else {
				//System.out.println("Not found any breach ");
			}

		} catch (IOException ie) {
			System.err.println("IO Exception in sendSlaCounterToCollector: " + ie.getMessage());
		} catch (Exception e) {
			e.printStackTrace();
			System.err.println("Unknown Exception in sendSlaCounterToCollector: " + e);
		} finally {
			method.releaseConnection();
			method = null;
			UtilsFactory.clearCollectionHieracy(hmSlaCounters);
			responseJSONStream = null;
		}
		
		return bReturn;
	}
	
	/**
	 * send the counter-sets available in the ArrayList to the configured WebService.
	 * 
	 * @param agent_type
	 * @return
	 */
	public boolean sendSlaCounterToCollector(String strGUID,AGENT_TYPE agent_type) {
	
		boolean bMonitorReturn = false;
		boolean bSlaReturn = false;
		boolean bReturn =false;
		String responseJSONStream = null;
		
		PostMethod method = new PostMethod(Constants.WEBSERVICE_URL+"/collectCounters");
		
		try{
			
			if(!alSlaCounters.isEmpty()) {
				System.out.println(agent_type.toString()+" Sla sending hmCounters: "+alSlaCounters.toString());
				
				method.setParameter("agent_type", agent_type.toString());
				method.setParameter("counter_params_json", alSlaCounters.toString());
				method.setParameter("breach_counter_set", alSlaCounters.toString());
//				System.out.println("GUID :" +strGUID);
				method.setParameter("guid", strGUID);
				method.setParameter("command", "sla");
				method.setRequestHeader("Connection", "close");
				
				int statusCode = client.executeMethod(method);
				System.err.println("statusCode: "+statusCode);
				
				if (statusCode != HttpStatus.SC_OK) {
					System.err.println("Method failed: " + method.getStatusLine());
				}
				
				try {
					responseJSONStream = method.getResponseBodyAsString();
//					System.out.println("sla Response" + responseJSONStream);
					
					if( responseJSONStream.trim().startsWith("{") && responseJSONStream.trim().endsWith("}")) {
						JSONObject joGUIDConfig = JSONObject.fromObject(responseJSONStream);
						
						if( joGUIDConfig.containsKey("message") && joGUIDConfig.get("message").equals("kill") ) {
							System.out.println("The given application deleted ");
							Thread.sleep(20000);
							System.exit(0);	
						}
						
//						if(js.containsKey("MonitorCounterSet")) {
//							bMonitorReturn = updateCounters(strGUID,responseJSONStream);
//						}
//						
//						if(js.containsKey("SlaCounterSet")) {
//							bSlaReturn = updateSlaCounters(strGUID,responseJSONStream);
//						}
						
						// Update monitor counters in bean
						if((joGUIDConfig.containsKey("MonitorCounterSet") && joGUIDConfig.getString("MonitorCounterSet")!=null) && (!joGUIDConfig.getString("MonitorCounterSet").equalsIgnoreCase("null"))) {
							bMonitorReturn = updateCounters(strGUID, joGUIDConfig.getJSONArray("MonitorCounterSet"));
						}
						
						// update sla counters in bean
						if( (joGUIDConfig.containsKey("SlaCounterSet") && joGUIDConfig.getString("SlaCounterSet")!=null) && (!joGUIDConfig.getString("SlaCounterSet").equalsIgnoreCase("null")) ) {
							bSlaReturn = updateSlaCounters(strGUID, joGUIDConfig.getJSONArray("SlaCounterSet"));
						}
						
						
					}						
					// reset counters list
					resetSlaCounterMapArray();
					
					if(bMonitorReturn || bSlaReturn) {
						bReturn = true;
					}
					
				} catch (HttpException he) {
					
					System.err.println("HTTP Exception in sendSlaCounterToCollector: " + he.getMessage());
					bReturn = false;
				}
			}else {
				System.out.println("Not found any breach ");
			}

		} catch (IOException ie) {
			System.err.println("IO Exception in sendSlaCounterToCollector: " + ie.getMessage());
		} catch (Exception e) {
			e.printStackTrace();
			System.err.println("Unknown Exception in sendSlaCounterToCollector: " + e);
		} finally {
			method.releaseConnection();
			method = null;
			UtilsFactory.clearCollectionHieracy(hmSlaCounters);
			responseJSONStream = null;
		}
		
		return bReturn;
	}
	
	/**
	 * Run System.gc() for each configured period.
	 */
	public static void runGarbageCollectionRountine() {
		try{
			// Call garbage collection in a separate routine
			new Thread(
				new Runnable() {
					
					
					/**
					 * Call for System.gc()
					 */
					public void run() {
						while(true){
							try {
								Thread.sleep(Constants.FIVE_MINUTE_MILLISECONDS);
							} catch (InterruptedException e) {
								// TODO Auto-generated catch block
								e.printStackTrace();
							}
							
							System.gc();
							System.out.println(Constants.this_agent+"-Monitor called System.GC");
						}
					}
				}
			).start();
		} catch(Throwable e) {
			System.out.println("Exception in runGarbageCollectionRountine: "+e.getMessage());
			e.printStackTrace();
		}
	}
	
	/**
	 * Add Exception Counter
	 * 
	 * @param nCounterCode
	 * @param objCounterValue
	 * @return
	 */
	public boolean reportCounterError(Integer nCounterCode, String strError){
		// counter value has to be -1 to hide the graph line below x-axis.
		hmCounters.put(Integer.toString(nCounterCode), -1);
		
		// assign counter error
		if(strError != null){
			strError = strError.replaceAll("\n", " ").replaceAll("\t", " ").replaceAll("\r", " ").replaceAll("\"", "\\\\\"");
		}else{
			strError = "null";
		}
		((HashMap<String,Object>)hmCounters.get("1005")).put(Integer.toString(nCounterCode), "\""+strError+"\"");
		return true;
	}
	public boolean reportGlobalError(String strError){
		HashMap<String,Object> hm = ((HashMap<String,Object>)hmCounters.get("1005"));
		strError = strError.replaceAll("\n", " ").replaceAll("\t", " ").replaceAll("\r", " ").replaceAll("\"", "\\\\\"");
		hm.put("1006", "\""+UtilsFactory.replaceNull(strError,"Null Pointer Exception")+"\"");
		return true;
	}
	
	/**
	 * Update latest value if the counter is used as Denominator in SLA alert calculation
	 * 
	 * @param nCounterCode
	 * @param dCurrentCounterValue
	 */
	public void updateQuickViewCounterValues(Integer nCounterCode, Double dCurrentCounterValue) {

		// Update the denominator counter id's value
		if (hmQuickViewCounterValues.containsKey(Integer.toString(nCounterCode))) {
			hmQuickViewCounterValues.put(Integer.toString(nCounterCode), dCurrentCounterValue);
		}
	}

	/**
	 * 
	 * @param nCounterCode
	 * @param dCurrentCounterValue
	 */
	public void updateStaticCounterIds(Integer nCounterCode) {
		// Adds the counter ids of static counter when is_first is true
		if (!(hmCounters.containsKey("1007"))) {
			hmCounters.put("1007", new ArrayList<String>());
		}
		ArrayList<String> alStaticCounterIds = ((ArrayList<String>) hmCounters.get("1007"));
		alStaticCounterIds.add("\"" + Integer.toString(nCounterCode) + "\"");
	}

	/**
	 * Clear the current counter-set(HashMap)
	 * 
	 * @throws Throwable
	 */
	public void clearCounterMap()  throws Throwable {
		UtilsFactory.clearCollectionHieracy(hmCounters);
	}
	
	@Override
	protected void finalize() throws Throwable {
		UtilsFactory.clearCollectionHieracy(hmCounters);
		
		super.finalize();
	}
	
	/**
	 * 
	 * @param strAgentGUID
	 * @throws Throwable
	 */
	public  boolean getConfigurationsFromCollector(String strGUID, String strAgentType) throws Throwable {
		
		boolean bMonitorReturn = false;
		boolean bSlaReturn = false;
		boolean bReturn =false;
		
		HttpClient client = null;
		PostMethod method = null;
		int statusCode = 0;
		
		String responseJSONStream = null;		
		HashMap<String, String> hmResponse = new HashMap<String, String>();
		
		do {
			try {
				client = new HttpClient();
				// URLEncoder.encode(requestUrl,"UTF-8");
				method = new PostMethod(Constants.WEBSERVICE_URL+"/getConfigurations");
				// System.out.println(Constants.WEBSERVICE_URL+"/getConfigurations");
				method.setParameter("guid", strGUID);
				method.setParameter("agent_type", strAgentType);
				method.setParameter("command", "AgentFirstRequest");
				method.setRequestHeader("Connection", "close");
				
				statusCode = client.executeMethod(method);
				// System.err.println("statusCode: "+statusCode);
				
				if (statusCode != HttpStatus.SC_OK) {
					System.err.println("Method failed: " + method.getStatusLine());
				} else {
					responseJSONStream = method.getResponseBodyAsString();
					if (strAgentType.equalsIgnoreCase("LINUX")) {
						LogManagerExtended.serverInfoLog("Monitor counters: " +responseJSONStream);
					}else if(strAgentType.equalsIgnoreCase("TOMCAT")) {
						LogManagerExtended.applicationInfoLog("Monitor counters: " +responseJSONStream);
					}else if(strAgentType.equalsIgnoreCase("POSTGRES")) {
						LogManagerExtended.databaseInfoLog("Monitor counters: " +responseJSONStream);
					}else {
						System.out.println("Monitor counters: " +responseJSONStream);
					}
					
					
					if( responseJSONStream.trim().startsWith("{") && responseJSONStream.trim().endsWith("}")) {
						JSONObject joGUIDConfig = JSONObject.fromObject(responseJSONStream);
						
						if( joGUIDConfig.containsKey("message") && joGUIDConfig.get("message").equals("kill") ) {
							System.out.println("The given application deleted ");
							Thread.sleep(20000);
							System.exit(0);	
						}
						
						// Update monitor counters in bean
						if((joGUIDConfig.containsKey("MonitorCounterSet") && joGUIDConfig.getString("MonitorCounterSet")!=null) && (!joGUIDConfig.getString("MonitorCounterSet").equalsIgnoreCase("null"))) {
							if( strAgentType.equals("JSTACK") ) {
								bMonitorReturn = true;
							} else {
								bMonitorReturn = updateCounters(strGUID, joGUIDConfig.getJSONArray("MonitorCounterSet"));
							}
						}
						
						// update sla counters in bean
						if( (joGUIDConfig.containsKey("SlaCounterSet") && joGUIDConfig.getString("SlaCounterSet")!=null) && (!joGUIDConfig.getString("SlaCounterSet").equalsIgnoreCase("null")) ) {
							bSlaReturn = updateSlaCounters(strGUID, joGUIDConfig.getJSONArray("SlaCounterSet"));
						}
					}
					
					//if(bMonitorReturn || bSlaReturn) {
					if(bMonitorReturn) {
						bReturn = true;
					}
					
					if(!bReturn) {
						Thread.sleep(Constants.MONITOR_FREQUENCY_MILLESECONDS);
					}
				}
				
			} catch(Throwable e) {
				System.out.println("Exception while connecting: "+Constants.WEBSERVICE_URL);
				System.out.println("Exception in getConfigurationsFromCollector: "+e.getMessage());
			} finally {
				if( method != null ) {
					method.releaseConnection();
					method = null;
				}
				
				responseJSONStream = null;
				UtilsFactory.clearCollectionHieracy( hmResponse );
			}
			
			if (statusCode != HttpStatus.SC_OK) {
				Thread.sleep(Constants.MONITOR_FREQUENCY_MILLESECONDS);
			}
			
		} while(!bReturn);
		
		return bReturn; 
	}
	
	/**
	 * set GUID's SLA(s) to chcek for breaches
	 * 
	 * @param responseJSONStream
	 * @return
	 */
	public boolean updateSlaCounters(String strGUID, JSONArray jaSLAs) {
		boolean bReturn = false;
		
		try {
			//	System.out.println("Received sla counters" + joResponse.getJSONArray("SlaCounterSet"));
			SlaCounterBean.setJoSlaCountersBean(strGUID, jaSLAs);
			// Change the status to true
			bReturn = true;
		} catch(Throwable t) {
			System.out.println("Exception in updateSlaCounters: " + t.getMessage());
			t.printStackTrace();
		}
		
		return bReturn;
	}
	
	/**
	 * 
	 * @param responseJSONStream
	 * @return
	 */
	public boolean updateCounters(String strGUID, JSONArray jaCounters) {
		boolean bReturn = false;
		JSONObject joCounter = null;
		
		try {
			if( jaCounters.size () > 0 ) {
				AgentCounterBean.setCountersBean(strGUID, jaCounters);
				
				// Set is_first = TRUE, when new monitor_counterset received. 
				is_first = true;
				
				// Reset denomitor counters list
				UtilsFactory.clearCollectionHieracy(hmQuickViewCounterValues);
				
				for (int i = 0; i < jaCounters.size(); i++) {
					joCounter = jaCounters.getJSONObject(i);
					if (joCounter.containsKey("maxValueCounterId")) {
						hmQuickViewCounterValues.put(joCounter.getString("maxValueCounterId"), null);
					}
					if (joCounter.getBoolean("isStaticCounter")) {
						hmQuickViewCounterValues.put(joCounter.getString("counter_id"), null);
					}
				}
				// Change the status to true
				bReturn = true;
			}
		} catch(Throwable t) {
			System.out.println("Exception in updateCounters :" + t.getMessage());
			t.printStackTrace();
		}
		
		return bReturn;
	}
	
	/**
	 * Apply delta operation: Subtract the given current value with the previous value.
	 * Once the delta is done, the current value is stored as previous value for next delta operation.
	 * 
	 * @param nCounterCode
	 * @param lCurrentCounterValue
	 * @return
	 */
	private Long doDelta(Integer nCounterCode, Long lCurrentCounterValue) {
		Long lPrevlCounterValue = 0l, lDeltaCounterValue = 0l;

		// check key is already available
		if (hmDeltaCounter.containsKey(Integer.toString(nCounterCode))) {
			// key exist get the value of counter type
			lPrevlCounterValue = hmDeltaCounter.get(Integer.toString(nCounterCode));

			if (lPrevlCounterValue > lCurrentCounterValue) {
				lDeltaCounterValue = 0l;
			} else {
				lDeltaCounterValue = lCurrentCounterValue - lPrevlCounterValue;
			}

			hmDeltaCounter.put(Integer.toString(nCounterCode), lCurrentCounterValue);
		} else {
			hmDeltaCounter.put(Integer.toString(nCounterCode), lCurrentCounterValue);
			lDeltaCounterValue = 0l;
		}

		return lDeltaCounterValue;
	}
	
	/**
	 * Apply delta operation: Subtract the given current value with the previous value.
	 * Once the delta is done, the current value is stored as previous value for next delta operation.
	 * 
	 * @param nCounterCode
	 * @param lCurrentCounterValue
	 * @return
	 */
	private Double doDelta(Integer nCounterCode, Double dCurrentCounterValue) {
		Double dPrevlCounterValue = 0.0, dDeltaCounterValue = 0.0;

		// check key is already available
		if (hmLinuxDeltaCounter.containsKey(Integer.toString(nCounterCode))) {
			// key exist get the value of counter type
			dPrevlCounterValue = hmLinuxDeltaCounter.get(Integer.toString(nCounterCode));

			if (dPrevlCounterValue > dCurrentCounterValue) {
				dDeltaCounterValue = 0.0;
			} else {
				dDeltaCounterValue = dCurrentCounterValue - dPrevlCounterValue;
			}

			hmLinuxDeltaCounter.put(Integer.toString(nCounterCode), dCurrentCounterValue);
		} else {
			hmLinuxDeltaCounter.put(Integer.toString(nCounterCode), dCurrentCounterValue);
			dDeltaCounterValue = 0.0;
		}

		return dDeltaCounterValue;
	}

	/**
	 * Apply delta operation: Subtract the given current value with the previous value by doDelta().
	 * Then add the code-delta value into the counter-set with addCounterValue()
	 * 
	 * @param nCounterCode
	 * @param lCurrentCounterValue
	 * @return
	 */
	public Long addDeltaCounterValue(Integer nCounterCode, Long lCurrentCounterValue) {
		 
		lCurrentCounterValue = doDelta(nCounterCode, lCurrentCounterValue);
		
		addCounterValue(nCounterCode, lCurrentCounterValue);
		
		return lCurrentCounterValue;
	}
	
	/**
	 * Apply delta operation: Subtract the given current value with the previous value by doDelta().
	 * Then add the code-delta value into the counter-set with addCounterValue()
	 * 
	 * @param nCounterCode
	 * @param lCurrentCounterValue
	 * @return
	 */
	public Long addDeltaCounterValue_v1(Integer nCounterCode, Long lCurrentCounterValue) {
		 
		lCurrentCounterValue = doDelta(nCounterCode, lCurrentCounterValue);
		
		return lCurrentCounterValue;
	}
	
	/**
	 * Apply delta operation: Subtract the given current value with the previous value by doDelta().
	 * Then add the code-delta value into the counter-set with addCounterValue()
	 * 
	 * @param nCounterCode
	 * @param lCurrentCounterValue
	 * @return
	 */
	public Double addDeltaCounterValue(Integer nCounterCode, Double dCurrentCounterValue) {
		
		dCurrentCounterValue = doDelta(nCounterCode, dCurrentCounterValue);
		
		addCounterValue(nCounterCode, dCurrentCounterValue);
		
		return dCurrentCounterValue;
	}
	
	/**
	 * Apply delta operation: Subtract the given current value with the previous value by doDelta().
	 * Then add the code-delta value into the counter-set with addCounterValue()
	 * 
	 * @param nCounterCode
	 * @param lCurrentCounterValue
	 * @return
	 */
	public Double addDeltaCounterValue_v1(Integer nCounterCode, Double dCurrentCounterValue) {
		
		dCurrentCounterValue = doDelta(nCounterCode, dCurrentCounterValue);
		
		//addCounterValue(nCounterCode, dCurrentCounterValue);
		
		return dCurrentCounterValue;
	}
	
	public ArrayList<JSONObject> verifySLABreach(String strGUID, JSONArray jaSlaMapCounters, int nCounterId, double dCounterValue) throws Throwable {
		double dWarningThresholdValue = 0.0;
		double dCriticalThresholdValue = 0.0;
		SLA_BREACH_SEVERITY sla_breach_severity = null;
		double dTempCounterValue;

		boolean bHasBreached = false;
		ArrayList<JSONObject> alRtnSlaCounter = new ArrayList<JSONObject>();
		JSONObject joSlaCounter = null, joRtnSlaCounter = null;

		try {
			if (jaSlaMapCounters != null) {
				for (int j = 0; j < jaSlaMapCounters.size(); j++) {
					String strDenominatorCounterId = null;
					boolean isPercentageCalculation;
					bHasBreached = false;
					joSlaCounter = jaSlaMapCounters.getJSONObject(j);
					dTempCounterValue = dCounterValue;

					if (Integer.parseInt(joSlaCounter.getString("counterid")) == nCounterId) {

						isPercentageCalculation = joSlaCounter.getBoolean("percentage_calculation");

						if (isPercentageCalculation) {
							strDenominatorCounterId = joSlaCounter.getString("denominator_counter_id");

							if (joSlaCounter.containsKey("denominator_counter_id") && hmQuickViewCounterValues.containsKey(strDenominatorCounterId)) {
								double denominator_counter_value = hmQuickViewCounterValues.get(strDenominatorCounterId);

								dTempCounterValue = (dCounterValue / denominator_counter_value) * 100;
							} else {
								System.out.println("Expected denominator counter is not being monitored: " + strDenominatorCounterId);
								continue; // Avoid SLA validation, as this will lead to Divide-By-Null
							}
						}

						dWarningThresholdValue = joSlaCounter.getLong("warning_threshold_value");
						dCriticalThresholdValue = joSlaCounter.getLong("critical_threshold_value");
						sla_breach_severity = null;

						// System.out.println("Counter Id :"+nCounterId +"Map value :" +lSetValue+" Monitor val :"+lCounterValue );
						if (joSlaCounter.getBoolean("isabovethreshold")) {
							if (dTempCounterValue > dCriticalThresholdValue) { // Critical
								bHasBreached = true;
								sla_breach_severity = SLA_BREACH_SEVERITY.CRITICAL;
							} else if (dTempCounterValue > dWarningThresholdValue) { // Warning
								bHasBreached = true;
								sla_breach_severity = SLA_BREACH_SEVERITY.WARNING;
							}
						} else if (!joSlaCounter.getBoolean("isabovethreshold")) {
							if (dTempCounterValue < dCriticalThresholdValue) { // Critical
								bHasBreached = true;
								sla_breach_severity = SLA_BREACH_SEVERITY.CRITICAL;
							} else if (dTempCounterValue < dWarningThresholdValue) { // Warning
								bHasBreached = true;
								sla_breach_severity = SLA_BREACH_SEVERITY.WARNING;
							}
						}

						// Add the details into Collection
						if (bHasBreached) {
							joRtnSlaCounter = joSlaCounter;
							joRtnSlaCounter.put("guid", strGUID);
							joRtnSlaCounter.put("received_value", dTempCounterValue);
							joRtnSlaCounter.put("breached_severity", sla_breach_severity.toString());
							alRtnSlaCounter.add(joRtnSlaCounter);
						} else {
							joSlaCounter = null;
						}

					}
				}
			}
		} catch (Throwable t) {
			System.out.println("Exception in verifySLABreach :" + t.getMessage());
			t.printStackTrace();
		}

		return alRtnSlaCounter;
	}
	
	public void verifySLABreach_v1(JSONArray jaSlaMapCounters, int nCounterId, double dCounterValue, LinuxUnificationBean beanSLA) throws Exception {
		double dWarningThresholdValue = 0.0;
		double dCriticalThresholdValue = 0.0;
		SLA_BREACH_SEVERITY sla_breach_severity = null;
		double dTempCounterValue;

		boolean bHasBreached = false;
		ArrayList<JSONObject> alRtnSlaCounter = new ArrayList<JSONObject>();
		JSONObject joSlaCounter = null, joRtnSlaCounter = null;

		LinuxUnificationSLACounterBean beanSLACounter = null;
		
		try {
			if (jaSlaMapCounters != null) {
				for (int j = 0; j < jaSlaMapCounters.size(); j++) {
					String strDenominatorCounterId = null;
					boolean isPercentageCalculation;
					bHasBreached = false;
					joSlaCounter = jaSlaMapCounters.getJSONObject(j);
					dTempCounterValue = dCounterValue;

					if (Integer.parseInt(joSlaCounter.getString("counterid")) == nCounterId) {

						isPercentageCalculation = joSlaCounter.getBoolean("percentage_calculation");

						if (isPercentageCalculation) {
							strDenominatorCounterId = joSlaCounter.getString("denominator_counter_id");

							if (joSlaCounter.containsKey("denominator_counter_id") && hmQuickViewCounterValues.containsKey(strDenominatorCounterId)) {
								double denominator_counter_value = hmQuickViewCounterValues.get(strDenominatorCounterId);

								dTempCounterValue = (dCounterValue / denominator_counter_value) * 100;
							} else {
								System.out.println("Expected denominator counter is not being monitored: " + strDenominatorCounterId);
								continue; // Avoid SLA validation, as this will lead to Divide-By-Null
							}
						}

						dWarningThresholdValue = joSlaCounter.getLong("warning_threshold_value");
						dCriticalThresholdValue = joSlaCounter.getLong("critical_threshold_value");
						sla_breach_severity = null;

						// System.out.println("Counter Id :"+nCounterId +"Map value :" +lSetValue+" Monitor val :"+lCounterValue );
						if (joSlaCounter.getBoolean("isabovethreshold")) {
							if (dTempCounterValue >= dCriticalThresholdValue) { // Critical
								bHasBreached = true;
								sla_breach_severity = SLA_BREACH_SEVERITY.CRITICAL;
							} else if (dTempCounterValue >= dWarningThresholdValue) { // Warning
								bHasBreached = true;
								sla_breach_severity = SLA_BREACH_SEVERITY.WARNING;
							}
						} else if (!joSlaCounter.getBoolean("isabovethreshold")) {
							if (dTempCounterValue <= dCriticalThresholdValue) { // Critical
								bHasBreached = true;
								sla_breach_severity = SLA_BREACH_SEVERITY.CRITICAL;
							} else if (dTempCounterValue <= dWarningThresholdValue) { // Warning
								bHasBreached = true;
								sla_breach_severity = SLA_BREACH_SEVERITY.WARNING;
							}
						}

						// Add the details into Collection
						/*if (bHasBreached) {
							joRtnSlaCounter = joSlaCounter;
							joRtnSlaCounter.put("guid", strGUID);
							joRtnSlaCounter.put("received_value", dTempCounterValue);
							joRtnSlaCounter.put("breached_severity", sla_breach_severity.toString());
							alRtnSlaCounter.add(joRtnSlaCounter);
						} else {
							joSlaCounter = null;
						}*/
						
						if(bHasBreached) {
							beanSLA.addNewSLACounter(joSlaCounter.getString("slaid"));
							
							beanSLACounter = new LinuxUnificationSLACounterBean();
							beanSLACounter.setBreached_severity(sla_breach_severity.toString());
							beanSLACounter.setCounter_id(nCounterId);
							beanSLACounter.setCritical_threshold_value(joSlaCounter.getLong("critical_threshold_value"));
							beanSLACounter.setIs_above(joSlaCounter.getBoolean("isabovethreshold"));
							beanSLACounter.setPercentage_calculation(joSlaCounter.getBoolean("percentage_calculation"));
							beanSLACounter.setReceived_value(dTempCounterValue);
							beanSLACounter.setSla_id(joSlaCounter.getInt("slaid"));
							beanSLACounter.setWarning_threshold_value(joSlaCounter.getLong("warning_threshold_value"));
							
							beanSLA.addSLACounterEntry(joSlaCounter.getString("slaid"), beanSLACounter);
						}
						
						
					}
				}
			}
		} catch (Exception t) {
			System.out.println("Exception in verifySLABreach :" + t.getMessage());
			t.printStackTrace();
		}

	}
	
}
