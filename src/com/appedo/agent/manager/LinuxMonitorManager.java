package com.appedo.agent.manager;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Scanner;
import java.util.Timer;
import java.util.TimerTask;

import com.appedo.agent.bean.AgentCounterBean;
import com.appedo.agent.bean.LinuxUnificationBean;
import com.appedo.agent.bean.LinuxUnificationCounterBean;
import com.appedo.agent.bean.SlaCounterBean;
import com.appedo.agent.init.AgentIgnitorLinuxUnificationApplicationThread;
import com.appedo.agent.init.AgentIgnitorLinuxUnificationDataBaseThread;
import com.appedo.agent.init.AgentIgnitorLinuxUnificationServerThread;
import com.appedo.agent.utils.Constants;
import com.appedo.agent.utils.Constants.AGENT_TYPE;
import com.appedo.agent.utils.UtilsFactory;
import com.appedo.manager.LogManager;

import net.sf.json.JSONArray;
import net.sf.json.JSONObject;

/**
 * Linux OS monitoring class. This has the functionalities to get the counter values of Linux OS.
 * 
 * @author veeru
 *
 */
public class LinuxMonitorManager extends AgentManager {
	
	public static LinuxMonitorManager linuxMonitorManager = null;
	
	private ProcessBuilder pbProcStat = null;
	private ProcessBuilder pbTopProcStat = null;
	
	/**
	 * Avoid the object creation for this Class from outside.
	 */
	private LinuxMonitorManager() {
		
		pbProcStat = new ProcessBuilder("bash", "-c", "tail /proc/stat | grep '^cpu '");
		
	}
	
	/**
	 * Returns the only object(singleton) of this Class.
	 * 
	 * @return
	 */
	public static LinuxMonitorManager getLinuxMonitorManager(){
		if( linuxMonitorManager == null ){
			linuxMonitorManager = new LinuxMonitorManager();
		}
		
		return linuxMonitorManager;
	}
	
	/**
	 * Monitor the server and collect the counters
	 */
	public void monitorLinuxServer(){
		getCounters();
	}
	
	/**
	 * Send it to the Collector WebService
	 */
	public void sendLinuxCounters(){
		sendCounters();
	}
	
	/**
	 * Collect the counter with agent's types own logic or native methods
	 * 
	 */
	public void getCounters(){
		
		int nCounterId ;
		String query = "", topProcessQuery = "", strTopProc_Mem_IO = "", line = null;
		StringBuilder sbErrors = null;
		boolean bIsDelta = false, bDoTopProcess = false, bIsTopProcess = false, is_static_counter = false;
		Double dCounterValue = 0.0;
		
		Process pProcstat = null;
		InputStreamReader isrProcstat = null;
		BufferedReader rProcstat = null;
		
		JSONObject joSelectedCounter = null;
		ArrayList<JSONObject> alSLACounters = null;
		
		try {
		
			// reset the counter collector variable in AgentManager.
			resetCounterMap(Constants.LINUX_AGENT_GUID);
			JSONArray joSelectedCounters = AgentCounterBean.getCountersBean(Constants.LINUX_AGENT_GUID);
			for(int i=0; i<joSelectedCounters.size(); i++){
				
				dCounterValue = 0.0;
				nCounterId = 0;
				query = "";
				bDoTopProcess = false;
				
				joSelectedCounter = joSelectedCounters.getJSONObject(i);			
				nCounterId = Integer.parseInt(joSelectedCounter.getString("counter_id")) ;
				query = joSelectedCounter.getString("query");
				bIsDelta = joSelectedCounter.getBoolean("isdelta");
				// Get the check top cpu/mem/io 
				bIsTopProcess = joSelectedCounter.getBoolean("isTopProcess");
				//strExecutionType = joSelectedCounter.getString("executiontype");
				bDoTopProcess = bIsTopProcess && joSelectedCounter.containsKey("top_process_query");
				is_static_counter = joSelectedCounter.getBoolean("isStaticCounter");
				
				if(bDoTopProcess){
					topProcessQuery = joSelectedCounter.getString("top_process_query");
					query = query+";"+topProcessQuery;
				}
	
				if (!is_static_counter || is_first) {
				//Make a process to execute counter query
				pbProcStat = new ProcessBuilder("bash", "-c", query);
				
				//strTopProc_Mem_IO = getTopProcess();
				
				/**
				 * calculate System CPU 
				 */
				try{
					pProcstat = pbProcStat.start();
					isrProcstat = new InputStreamReader(pProcstat.getInputStream());
					rProcstat = new BufferedReader(isrProcstat);
					
					// only one line should get returned. So used IF instead of WHILE
					if ((line = rProcstat.readLine()) != null) {
						
						dCounterValue = Double.parseDouble(line);
						if(bDoTopProcess) {
	
							strTopProc_Mem_IO = formatTopProcess(rProcstat);
	
							addTopProcessCounter(nCounterId+"_TOP", strTopProc_Mem_IO);
							
							addCounterValue(nCounterId, dCounterValue);
						} else {
							if(bIsDelta) {
								dCounterValue = addDeltaCounterValue(nCounterId, dCounterValue);					
							}else {
								addCounterValue(nCounterId, dCounterValue);
							}
						}
						
						// Update latest value if the counter is used as Denominator in SLA alert calculation
						updateQuickViewCounterValues(nCounterId, dCounterValue);
						
						// If is_first then send the counter_id's of static counters to collector
						if(is_first && is_static_counter){
							updateStaticCounterIds(nCounterId);
						}
					}
					
					
					sbErrors = getErrorString( pProcstat.getErrorStream() );
					if( sbErrors.length() > 0 ) {
						System.out.println("Command execution failed: "+query);
						System.out.println(sbErrors);
						System.out.println();
					}
				} catch(Throwable th) {
					System.out.println("Exception in getLinuxCounters-CPU: "+th.getMessage());
					th.printStackTrace();
					reportCounterError(nCounterId, th.getMessage());
				} finally {
					try{
						rProcstat.close();
					} catch(Exception e) {
						System.out.println("Exception in rProcstat.close(): "+e.getMessage());
						e.printStackTrace();
					}
					rProcstat = null;
					try{
						isrProcstat.close();
					} catch(Exception e) {
						System.out.println("Exception in isrProcstat.close(): "+e.getMessage());
						e.printStackTrace();
					}
					isrProcstat = null;
					try{
						pProcstat.destroy();
					} catch(Exception e) {
						System.out.println("Exception in pIostat.destroy(): "+e.getMessage());
						e.printStackTrace();
					}
					pProcstat = null;
				}
			  }
			}
	
			// Check sla breach after monitoring all the configured counters
			for (int i = 0; i < joSelectedCounters.size(); i++) {
				joSelectedCounter = null;
				alSLACounters = null;
				nCounterId = 0;
				dCounterValue = 0.0;
	
				joSelectedCounter = joSelectedCounters.getJSONObject(i);
				nCounterId = Integer.parseInt(joSelectedCounter.getString("counter_id"));
				is_static_counter = joSelectedCounter.getBoolean("isStaticCounter");
				
				if (is_static_counter) {
					// If it static counters get value from Hash reference, 
					// because it is not being monitored for each twenty seconds.
					dCounterValue = hmQuickViewCounterValues.get(joSelectedCounter.getString("counter_id"));
				} else {
					dCounterValue = (Double) hmCounters.get(joSelectedCounter.getString("counter_id"));
				}
				
				alSLACounters = verifySLABreach(Constants.LINUX_AGENT_GUID, SlaCounterBean.getSLACountersBean(Constants.LINUX_AGENT_GUID), nCounterId, dCounterValue);
				
				// If breached then add it to Collector's collection
				addSlaCounterValue(alSLACounters);
			}
		} catch (Throwable e) {
			System.out.println("Exception in getLinuxCounters: "+e.getMessage());
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
	 * This is to get the top 3 cpu process 
	 * @return
	 */
	public String getTopProcess() {
		
		Process pProcstat = null;
		InputStreamReader isrProcstat = null;
		BufferedReader rProcstat = null;
		
		JSONObject joTopProcess = null; 
		JSONArray jaTopProcess = new JSONArray();
		JSONObject joResult = new JSONObject(); 
		String line = null;
		
		try {
			
			pbTopProcStat = new ProcessBuilder("bash", "-c", Constants.TOP_PROCESS_QUERY);
			
			pProcstat = pbTopProcStat.start();
			isrProcstat = new InputStreamReader(pProcstat.getInputStream());
			rProcstat = new BufferedReader(isrProcstat);
			
			while ( (line = rProcstat.readLine()) != null ) {
				joTopProcess = new JSONObject();
				
				joTopProcess.put("category", line.trim().split("#@#")[0].trim());
				joTopProcess.put("process_value", line.trim().split("#@#")[1].trim());
				joTopProcess.put("process_name", line.trim().split("#@#")[2].trim());
				
				jaTopProcess.add(joTopProcess);
			}
			joResult.put("TOP", jaTopProcess.toString());
			
		}catch(Exception e) {
			System.out.println("Exception in getTopProcess() : "+ e.getMessage());
			e.printStackTrace();
			
		}finally {
			
			try{
				rProcstat.close();
			} catch(Exception e) {
				System.out.println("Exception in rProcstat.close(): "+e.getMessage());
				e.printStackTrace();
			}
			rProcstat = null;
			try{
				isrProcstat.close();
			} catch(Exception e) {
				System.out.println("Exception in isrProcstat.close(): "+e.getMessage());
				e.printStackTrace();
			}
			isrProcstat = null;
			try{
				pProcstat.destroy();
			} catch(Exception e) {
				System.out.println("Exception in pIostat.destroy(): "+e.getMessage());
				e.printStackTrace();
			}
			pProcstat = null;		
			
		}
		return joResult.toString();
		
	}
	
	/**
	 * This is to get the top 3 cpu process 
	 * @return
	 */
	public String formatTopProcess(BufferedReader rProcstat) {
		
		JSONObject joTopProcess = null; 
		JSONArray jaTopProcess = new JSONArray();
		JSONObject joResult = new JSONObject(); 
		String line = null;
		
		try {
			while ( (line = rProcstat.readLine()) != null ) {
					joTopProcess = new JSONObject();
					String strTopProcess[] = new String[3];
					strTopProcess = line.replaceAll(" ", "").split("#@#");
					joTopProcess.put("category",strTopProcess[0]);
					joTopProcess.put("process_value", strTopProcess[1]);
					joTopProcess.put("process_name", strTopProcess[2]);
					
					jaTopProcess.add(joTopProcess);
				}
			joResult.put("TOP", jaTopProcess.toString());
			
		}catch(Exception e) {
			System.out.println("Exception in formatTopProcess() : "+ e.getMessage());
			e.printStackTrace();
			
		}finally {
			try{
				rProcstat.close();
			} catch(Exception e) {
				System.out.println("Exception in rProcstat.close(): "+e.getMessage());
				e.printStackTrace();
			}
			rProcstat = null;
		}
		return joResult.toString();
		
	}
	
	/**
	 * Send the collected counter-sets to Collector WebService, by calling parent's sendCounter method
	 */
	public void sendCounters() {
		
		// send the collected counters to Collector WebService through parent sender function
		sendCounterToCollector(Constants.LINUX_AGENT_GUID, AGENT_TYPE.LINUX);
		sendSlaCounterToCollector(Constants.LINUX_AGENT_GUID,AGENT_TYPE.LINUX);
	}
	
	
	private StringBuilder getErrorString(InputStream errorStream) {
		InputStreamReader isrError = null;
		BufferedReader rError = null;
		String line = null;
		StringBuilder sbError = new StringBuilder();
		
		try{
			isrError = new InputStreamReader(errorStream);
			rError = new BufferedReader(isrError);
			sbError.setLength(0);
			while ((line = rError.readLine()) != null) {
				sbError.append(line).append("\n");
			}
			if( sbError.length() > 0 ){
				sbError.deleteCharAt(sbError.length()-1);
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
		
		return sbError;
	}
	
	public boolean isExistsDirectory(String filePath) throws Exception{
		Process pProcstat = null;
		InputStreamReader isrProcstat = null;
		BufferedReader rProcstat = null;
		String line = null;
		boolean isCorrectPath = false;
		try {
			
			pbTopProcStat = new ProcessBuilder("bash", "-c", "test -f "+filePath+ " ; echo $?");
			
			pProcstat = pbTopProcStat.start();
			isrProcstat = new InputStreamReader(pProcstat.getInputStream());
			rProcstat = new BufferedReader(isrProcstat);
			
			while ( (line = rProcstat.readLine()) != null ) {
				if(line.trim().contains("0")) {
					isCorrectPath = true;
				}
			}
			
			
		}catch(Exception e) {
			System.out.println("Exception in isExistsDirectory() : "+ e.getMessage());
			e.printStackTrace();
			
		}finally {
			
			try{
				rProcstat.close();
			} catch(Exception e) {
				System.out.println("Exception in isExistsDirectory() of rProcstat.close(): "+e.getMessage());
				e.printStackTrace();
			}
			rProcstat = null;
			try{
				isrProcstat.close();
			} catch(Exception e) {
				System.out.println("Exception in isExistsDirectory() of isrProcstat.close(): "+e.getMessage());
				e.printStackTrace();
			}
			isrProcstat = null;
			try{
				pProcstat.destroy();
			} catch(Exception e) {
				System.out.println("Exception in isExistsDirectory() of pIostat.destroy(): "+e.getMessage());
				e.printStackTrace();
			}
			pProcstat = null;		
			
		}
		return isCorrectPath;
		
	}
	
	public JSONObject getOperatingSystemInformation() throws Exception{
		Process pProcstat = null;
		InputStreamReader isrProcstat = null;
		BufferedReader rProcstat = null;
		String line = null;
		JSONObject joServerData = null;
		
		try {
			pbTopProcStat = new ProcessBuilder("bash", "-c", "cat /etc/*-release");
			
			pProcstat = pbTopProcStat.start();
			isrProcstat = new InputStreamReader(pProcstat.getInputStream());
			rProcstat = new BufferedReader(isrProcstat);
			joServerData = new JSONObject();
			while((line = rProcstat.readLine()) != null) {
				
				String split_name[] = line.split("=");
				
				if(split_name.length > 1) {
					joServerData.put(split_name[0], split_name[1].replace("\"", ""));
				}
			}
		}catch(Exception ex) {
			LogManagerExtended.serverInfoLog("Exception in getOperatingSystemInformation : "+ex);
			//System.out.println("Exception in getOperatingSystemInformation : "+ex);
		}
		
		return joServerData;
	}
	
	public JSONObject getOperatingSystemInformationOnpremEnv() throws Exception{
		Process pProcstat = null;
		InputStreamReader isrProcstat = null;
		BufferedReader rProcstat = null;
		String line = null, moduleTypeName = "";
		JSONObject joServerData = null;
		
		try {
			pbTopProcStat = new ProcessBuilder("bash", "-c", "cat /etc/*-release");
			
			pProcstat = pbTopProcStat.start();
			isrProcstat = new InputStreamReader(pProcstat.getInputStream());
			rProcstat = new BufferedReader(isrProcstat);
			joServerData = new JSONObject();
			while((line = rProcstat.readLine()) != null) {
				if(line.trim().toLowerCase().contains("amazon")) {
					moduleTypeName = "amazon";
					break;
				}else if(line.trim().toLowerCase().contains("centos")) {
					moduleTypeName = "centos";
					break;
				}else if(line.trim().toLowerCase().contains("red hat")) {
					moduleTypeName = "red hat";
					break;
				}else if(line.trim().toLowerCase().contains("fedora")) {
					moduleTypeName = "fedora";
					break;
				}else if(line.trim().toLowerCase().contains("ubuntu")){
					moduleTypeName = "ubuntu";
					break;
				}else if(line.trim().toLowerCase().contains("solaris")){
					moduleTypeName = "solaris";
					break;
				}else {
					moduleTypeName = "Linux";
				}
			}
			
			joServerData.put("PRETTY_NAME", moduleTypeName);
			joServerData.put("VERSION_ID", "");
			
		}catch(Exception ex) {
			LogManagerExtended.serverInfoLog("Exception in getOperatingSystemInformation : "+ex);
			//System.out.println("Exception in getOperatingSystemInformation : "+ex);
		}
		
		return joServerData;
	}
	
	public void getSystemInformation() throws Exception{
	
		boolean isExistsDmiDecode;
		
		JSONObject systemInformation = null, serverInformation = null, joCounterSet = null;
		
		String systemId, moduleGUID = "";
		try {
			
			System.out.println("Java Unification services Started........");
						
			systemInformation = new JSONObject();
			
			if(Constants.SYS_UUID == null && Constants.SYS_MANUFACTURER == null) {
				isExistsDmiDecode = isExistsDmiDecode();
				
				if(!isExistsDmiDecode) {
					System.out.println("DmiDecode package is not available in current system.");
					installingDmiDecode();
				}else {
					System.out.println("DmiDecode package is available...");
				}
				
				systemInformation = getServerInformation();
			}else {
				System.out.println("System information collecting from config file....");
				System.out.println(Constants.SYS_UUID);
				System.out.println(Constants.SYS_MANUFACTURER);
				System.out.println(Constants.SYS_PRODUCTNAME);
				System.out.println(Constants.SYS_SERIALNUMBAR);
				
				JSONObject systemData = new JSONObject();
				/*systemData.put("UUID", "4220497E-D391-8726-EA4A-30A1CFF0CA7E");
				systemData.put("Manufacturer","VMware");
				systemData.put("Product Name","VMware Virtual Platform");
				systemData.put("Serial Number","909087781817");*/
				
				systemData.put("UUID", Constants.SYS_UUID);
				systemData.put("Manufacturer", Constants.SYS_MANUFACTURER);
				systemData.put("Product Name", Constants.SYS_PRODUCTNAME);
				systemData.put("Serial Number", Constants.SYS_SERIALNUMBAR);
				
				systemInformation.put("systemInformation", systemData);
			}
			
			
			//System Info data send to collector
			System.out.println("===============================================================");
			System.out.println("Current System information deatils send to collector.....");
			systemId = sendSysInfoToCollector(systemInformation);
			
			if(!systemId.isEmpty()) {
				System.out.println("getting SystemId form collector.");
				System.out.println("systemId : "+ systemId);
				
				if(Constants.SYS_UUID == null && Constants.SYS_MANUFACTURER == null) {
					serverInformation = getOperatingSystemInformation();
				}else {
					serverInformation = getOperatingSystemInformationOnpremEnv();
				}
				
				joCounterSet = getLinuxDynamicCounters();
				moduleGUID = sendModuleInfoToCollector(serverInformation, systemId, systemInformation.getJSONObject("systemInformation").getString("UUID"), joCounterSet, "serverInformation");
				
				System.out.println("===============================================================");
				System.out.println("getting module GUID from collector");
				System.out.println("moduleGUID : "+ moduleGUID);
				
				if(!moduleGUID.isEmpty()) {
					
					//stop and kill old linux agent..
					terminateOldAgent();
					
					Constants.LINUX_AGENT_GUID = moduleGUID;
					
					try {
						if(getConfigurationsFromCollector(Constants.LINUX_AGENT_GUID, AGENT_TYPE.LINUX.toString())) {
							
							final Timer tLinuxUnified = new Timer();
							tLinuxUnified.schedule(new TimerTask() {
								public void run() {
									Date collectionData = new Date();
									String strModuleStatus = getModuleRunningStatus(Constants.LINUX_AGENT_GUID, "serverInformation");
									if(strModuleStatus.equalsIgnoreCase("Running")) {
										System.out.println("module is running mode data is collected.......");
										//old flow of get and send counter data
										//monitorLinuxServer();
										//sendLinuxCounters();
										
										getCountersValue(collectionData);
									}else if(strModuleStatus.equalsIgnoreCase("stop")){
										System.out.println("Module was stoped....");
									}else if(strModuleStatus.equalsIgnoreCase("restart")) {
										System.out.println("Module is restart mode reset counters ....");
										try {
											if(getConfigurationsFromCollector(Constants.LINUX_AGENT_GUID, AGENT_TYPE.LINUX.toString())) {
												getCountersValue(collectionData);
											}
										} catch (Throwable e) {
											e.printStackTrace();
										}
									}else if(strModuleStatus.equalsIgnoreCase("Kill")) {
										System.out.println("GUID is not fount....");
										tLinuxUnified.cancel();
										tLinuxUnified.purge();
									}
								};
							}, 100l, Constants.MONITOR_FREQUENCY_MILLESECONDS);
						}						
					} catch (Throwable e) {
						System.out.println("Exception in start AgentIgnitorLinuxThreas()");
						e.printStackTrace();
					}
				}else {
					System.out.println("Exception in send ServerInformation to collector....");
					System.out.println("could not get module GUID.");
				}
			}else {
				System.out.println("Exception in send System Information to collector...");
				System.out.println("could not get systemId.");
			}
			 
		} catch (Exception e) {
			System.out.println("Exception in getSystemInformation() : "+ e.getMessage());
		}
	}
	
	public void getSystemInformation1() throws Exception{
		
		String systemId, sysGenratorUUID = "";
		try {
			
			System.out.println("Linux Unification services Started.....");
			
			sysGenratorUUID = getSystemGeneratorInfo();
			
			UtilsFactory.printDebugLog(Constants.IS_DEBUG, "System generated UUID from server: "+sysGenratorUUID);
			
			if(!sysGenratorUUID.isEmpty()) {
				UtilsFactory.printDebugLog(Constants.IS_DEBUG, "getSystemId request send to collector.");
				systemId = sendSystemGeneratorUUIDToCollector(sysGenratorUUID);
				UtilsFactory.printDebugLog(Constants.IS_DEBUG, "getSystemId responded from server.");
				if(!systemId.isEmpty()) {
					System.out.println("getting SystemId form collector.");
					System.out.println("systemId : "+ systemId);
					Constants.SYSTEM_ID = systemId;
					Constants.SYS_UUID = sysGenratorUUID;
					System.out.println("Server, Application & DataBase thread is started, and related information write in the log folder in the relevant name..");
					
					//new AgentIgnitorLinuxThread();
					new AgentIgnitorLinuxUnificationServerThread();
					new AgentIgnitorLinuxUnificationApplicationThread();
					new AgentIgnitorLinuxUnificationDataBaseThread();
					
				}else {
					System.out.println("Exception in send System Information to collector...");
					System.out.println("could not get systemId.");
				}
			}else {
				System.out.println("System Generator file is empty..");
			}
			
		} catch (Throwable e) {
			System.out.println("Exception in getSystemInformation() : "+ e.getMessage());
		}
	}
	
	public JSONObject getLinuxDynamicCounters() throws Exception{
		Process pProcstat = null;
	    InputStreamReader isrProcstat = null;
	    BufferedReader rProcstat = null;
	    
	    String line = null;
	    
	    JSONObject joCounterSetData = UtilsFactory.joCounters;
	    JSONObject JoCounterSerSendToCollector = new JSONObject();
	    
	    ArrayList keys = new ArrayList(joCounterSetData.keySet());
	    
	    JSONArray jaCounters = null;
	    boolean bIsNewKey = true;
	    JSONObject joNewCounterSet = null, joQueryString = null;
	    JSONArray jaNewCounterSet = new JSONArray();
	    boolean isUpdateCounterSet = false;
	    try
	    {
	      this.pbTopProcStat = new ProcessBuilder(new String[] { "bash", "-c", "export CPULOOP=1 && top -b -d1 -n1 | grep -A3 -e \"Tasks\"" });
	      
	      pProcstat = this.pbTopProcStat.start();
	      isrProcstat = new InputStreamReader(pProcstat.getInputStream());
	      rProcstat = new BufferedReader(isrProcstat);
	      
	      while((line = rProcstat.readLine()) != null) {
	    	  
	    	  String[] counterCategory = line.split(":");
	    	  
	    	  for (int i = 0; i < keys.size(); i++) {
	    		  String keyValue = keys.get(i).toString();
	    		  
	    		  String unit = keyValue.equals("Cpu") ? "%" : ( (keyValue.equals("Mem") || keyValue.equals("Swap")) ? "KB" : "number" );
	    		  
	    		  if (counterCategory[0].contains(keyValue)) {
	    			  
	    			 /* System.out.println("Keys : " + keys.get(i));
	    			  System.out.println(line);*/
	    			  //String[] counters = counterCategory[1].split("[,.][\\s]");
	    			  String[] counters;
	    			  if(keyValue.contains("Cpu")) {
	    				counters = counterCategory[1].split(",");
	    			  }else {
						counters = counterCategory[1].split("[,.][\\s]"); 
	    			  }
	    			  
	    			  jaCounters = joCounterSetData.getJSONArray(keyValue);
	    			  for (int j = 0; j < counters.length; j++)
	    	          {
	    				  for (int k = 0; k < jaCounters.size(); k++) {
	    					if (counters[j].toLowerCase().contains(jaCounters.getJSONObject(k).getString("Key")))
	    		            {
	    						joQueryString = new JSONObject();
	    						joQueryString.put("cmd_group", "TOP_"+keyValue.toUpperCase());
	    						joQueryString.put("cmd", "export CPULOOP=1 && top -b -d1 -n1 | grep -A7 -e \"Tasks\" -e \"TIME+\"");
	    						joQueryString.put("filter", keyValue.toUpperCase()+"-"+jaCounters.getJSONObject(k).getString("Key").toUpperCase());
	    								
	    						joNewCounterSet = new JSONObject();
	    		                joNewCounterSet.put("category", keyValue);
	    		                joNewCounterSet.put("counter_name", jaCounters.getJSONObject(k).getString("CounterName"));
	    		                joNewCounterSet.put("has_instance", "f");
	    		                joNewCounterSet.put("instance_name", "");
	    		                joNewCounterSet.put("unit", unit);
	    		                if(keyValue.equalsIgnoreCase("cpu")) {
	    		                	joNewCounterSet.put("is_selected", "t");
	    		                }else {
	    		                	joNewCounterSet.put("is_selected", "f");
	    		                }
	    		                joNewCounterSet.put("is_static_counter", "f");
	    		                joNewCounterSet.put("query_string", joQueryString.toString());
	    		                joNewCounterSet.put("counter_description", jaCounters.getJSONObject(k).getString("Desc"));
	    		                joNewCounterSet.put("is_delta", "f");
	    		                
	    		                jaNewCounterSet.add(joNewCounterSet);
	    		                bIsNewKey = false;

	    		                break;  
	    		            }
	    				  }
	    				  if (bIsNewKey) {
	    		              System.out.println("New Key is Added : " + counters[j]);
	    		          }else{
	    		            bIsNewKey = true;
	    		          }
	    	          }
	    		  }
	    	  }  
	      }
	      
	      //add Total CPU% counters
	      joQueryString = new JSONObject();
			joQueryString.put("cmd_group", "TOP_CPU");
			joQueryString.put("cmd", "export CPULOOP=1 && top -b -c -d1 -n1 | grep -A10 -e \"Tasks\" -e \"TIME+\"");
			joQueryString.put("filter", "CPU-TOTAL");
					
		  joNewCounterSet = new JSONObject();
          joNewCounterSet.put("category", "Cpu");
          joNewCounterSet.put("counter_name", "Total CPU%");
          joNewCounterSet.put("has_instance", "f");
          joNewCounterSet.put("instance_name", "");
          joNewCounterSet.put("unit", "%");
          joNewCounterSet.put("is_selected", "t");
          joNewCounterSet.put("is_static_counter", "f");
          joNewCounterSet.put("query_string", joQueryString.toString());
          joNewCounterSet.put("counter_description", "Total CPU%");
          jaNewCounterSet.add(joNewCounterSet);
          
	      JoCounterSerSendToCollector.put("counterData", jaNewCounterSet);
	      
	      //isUpdateCounterSet = sendModuleCountersToCollector(JoCounterSerSendToCollector, moduleGUID);
	    }catch (Exception e) {
	    	LogManagerExtended.serverInfoLog("Exception in getDynamicCounters() : "+ e.getMessage());
	    	//System.out.println("Exception in getDynamicCounters() : "+ e.getMessage());
			e.printStackTrace();
	    } 
	    //return isUpdateCounterSet;
	    return JoCounterSerSendToCollector;
	}
	
	public JSONObject getServerInfo() throws Exception{
		
		JSONObject joServerInfo = null;
		try {
			File f = new File("/tmp/.sysAppedo");
			String sysConfig = "";
			if(f.exists()) {
				Scanner sc = new Scanner(f);
			    if (sc.hasNextLine()) {
			    	sysConfig= sc.nextLine();
			    }
			    
			    String[] configData = sysConfig.split("#@#");
			    //String[] configData = sysConfig.split("@@@");
			    
			    joServerInfo = new JSONObject();
			    joServerInfo.put("UUID", configData[0]);
			    joServerInfo.put("Manufacturer", configData[1]);
			    joServerInfo.put("Product Name", configData[2]);
			    joServerInfo.put("Serial Number", configData[3]);
				
			    System.out.println("System Information : "+ joServerInfo.toString());
			    
			    Constants.SYS_UUID = configData[0];
			}else {
				//System.out.println("System configuration file is missing, contact support@appedo.com for assistance ");
				System.out.println("System configuration file is missing, kindly Rerun @filebeat_installer_linux.sh@ ");
			}
		}catch (Exception e) {
			System.out.println("getServerInfo : " + e);
			LogManager.errorLog(e);
			// TODO: handle exception
		}
		return joServerInfo;
	}
	
	public String getSystemGeneratorInfo() throws Exception{
		
		String currentUsersHomeDir = "", sysGeneratorUUID = "", line = null;
		Scanner sc = null;
		boolean isRootUser = false;
		InputStreamReader isrProcstat = null;
		BufferedReader rProcstat = null;
		try {
			currentUsersHomeDir = Constants.USER_HOME_DIR != null ? Constants.USER_HOME_DIR : System.getProperty("user.home");
			
			/*Process process = Runtime.getRuntime().exec("id -u");
			isrProcstat = new InputStreamReader(process.getInputStream());
			rProcstat = new BufferedReader(isrProcstat);
			if((line = rProcstat.readLine()) != null) {
				if(line.equalsIgnoreCase("0")) {
					isRootUser = true;
				}
			}
			
			if(!isRootUser) {
				System.out.println("Kindly excute administrator mode...");
			}else {*/
				File f = new File(currentUsersHomeDir+"/.sysAppedo");
				if(f.exists()) {
					sc = new Scanner(f);
				    if (sc.hasNextLine()) {
				    	sysGeneratorUUID= sc.nextLine();
				    }
				   
				    System.out.println("System Generator UUID : "+ sysGeneratorUUID);
				    
				}else {
					System.out.println("System configuration file is missing, kindly Rerun @filebeat_installer_linux.sh@ ");
				}
			//}
		}catch (Exception e) {
			System.out.println("getSystemGenratorInfo : " + e);
			LogManager.errorLog(e);
			// TODO: handle exception
		}
		return sysGeneratorUUID;
	}


	public JSONObject getServerInformation() throws Exception{
		Process pProcstat = null;
		InputStreamReader isrProcstat = null;
		BufferedReader rProcstat = null;

		String line = null;
		
		JSONObject systemProperties = new JSONObject();
		JSONObject systemInformation = new JSONObject();
		try {
			
			pbTopProcStat = new ProcessBuilder("bash", "-c", "sudo dmidecode -q -t system");
			
			pProcstat = pbTopProcStat.start();
			isrProcstat = new InputStreamReader(pProcstat.getInputStream());
			rProcstat = new BufferedReader(isrProcstat);

			while((line = rProcstat.readLine()) != null) {
				if(line.startsWith("\t") && line.contains(":")) {
					String Keys[] = line.split(":");
					systemProperties.put(Keys[0].trim(), Keys[1].trim());
				}
			}
			
			systemInformation.put("systemInformation", systemProperties);
						
		}catch(Exception e) {
			System.out.println("Exception in isExistsDmiDecode() : "+ e.getMessage());
			e.printStackTrace();
			
		}finally {
			
			try{
				rProcstat.close();
			} catch(Exception e) {
				System.out.println("Exception in rProcstat.close(): "+e.getMessage());
				e.printStackTrace();
			}
			rProcstat = null;
			try{
				isrProcstat.close();
			} catch(Exception e) {
				System.out.println("Exception in isrProcstat.close(): "+e.getMessage());
				e.printStackTrace();
			}
			isrProcstat = null;
			try{
				pProcstat.destroy();
			} catch(Exception e) {
				System.out.println("Exception in pIostat.destroy(): "+e.getMessage());
				e.printStackTrace();
			}
			pProcstat = null;		
			
		}
		//return systemInfo;
		return systemInformation;
	}
	
	public void installingDmiDecode() throws Exception{
		
		Process pProcstat = null;
		InputStreamReader isrProcstat = null;
		BufferedReader rProcstat = null;
		
		String line = null, query = "";
		
		try {
			
			pbTopProcStat = new ProcessBuilder("bash", "-c", "sudo cat /etc/*-release | grep 'PRETTY_NAME' | cut -d '\"' -f 2 | cut -d ' ' -f 1,2");
			
			pProcstat = pbTopProcStat.start();
			isrProcstat = new InputStreamReader(pProcstat.getInputStream());
			rProcstat = new BufferedReader(isrProcstat);
			
			if((line = rProcstat.readLine()) != null) {
				if(line.toLowerCase().contains("ubuntu")) {
					query = "sudo apt-get -y install dmidecode";
				}else {
					query = "sudo yum -y install dmidecode";
				}
			}
			
			pProcstat = null;
			pbTopProcStat = new ProcessBuilder("bash", "-c", query);
			pProcstat = pbTopProcStat.start();
			isrProcstat = new InputStreamReader(pProcstat.getInputStream());
			rProcstat = new BufferedReader(isrProcstat);
			
			System.out.println("DmiDeCode Package Installing...");
			System.out.println("============================================================");
			while((line = rProcstat.readLine()) != null) {
				System.out.println(line);
			}
			
		}catch(Exception e) {
			System.out.println("Exception in isInstallingDmiDecode() : "+ e.getMessage());
			e.printStackTrace();
			
		}finally {
			
			try{
				rProcstat.close();
			} catch(Exception e) {
				System.out.println("Exception in rProcstat.close(): "+e.getMessage());
				e.printStackTrace();
			}
			rProcstat = null;
			try{
				isrProcstat.close();
			} catch(Exception e) {
				System.out.println("Exception in isrProcstat.close(): "+e.getMessage());
				e.printStackTrace();
			}
			isrProcstat = null;
			try{
				pProcstat.destroy();
			} catch(Exception e) {
				System.out.println("Exception in pIostat.destroy(): "+e.getMessage());
				e.printStackTrace();
			}
			pProcstat = null;		
			
		}
		
	}
	
	public boolean isExistsDmiDecode() throws Exception{
		boolean isExistsDmiDecode = false;
		
		Process pProcstat = null;
		InputStreamReader isrProcstat = null;
		BufferedReader rProcstat = null;
		
		String line = null;
		
		try {
			
			pbTopProcStat = new ProcessBuilder("bash", "-c", "sudo dmidecode -s system-UUID; echo $?");
			
			pProcstat = pbTopProcStat.start();
			isrProcstat = new InputStreamReader(pProcstat.getInputStream());
			rProcstat = new BufferedReader(isrProcstat);
			
			while((line = rProcstat.readLine()) != null) {
				if(line.trim().equals("1")) {
					isExistsDmiDecode = false;
				}else {
					isExistsDmiDecode = true;
				}
			}			
			
		}catch(Exception e) {
			System.out.println("Exception in isExistsDmiDecode() : "+ e.getMessage());
			e.printStackTrace();
			
		}finally {
			
			try{
				rProcstat.close();
			} catch(Exception e) {
				System.out.println("Exception in rProcstat.close(): "+e.getMessage());
				e.printStackTrace();
			}
			rProcstat = null;
			try{
				isrProcstat.close();
			} catch(Exception e) {
				System.out.println("Exception in isrProcstat.close(): "+e.getMessage());
				e.printStackTrace();
			}
			isrProcstat = null;
			try{
				pProcstat.destroy();
			} catch(Exception e) {
				System.out.println("Exception in pIostat.destroy(): "+e.getMessage());
				e.printStackTrace();
			}
			pProcstat = null;		
			
		}
		
		return isExistsDmiDecode;
	}
	
	public void getCountersValue(Date collectionDate) {
		int nCounterId ;
		String query = "", line = null;
		StringBuilder sbErrors = null;
		boolean bIsDelta = false;
		Double dCounterValue = 0.0;
		Double dTotalCpuValue = 0.0;
		
		Process pProcstat = null;
		InputStreamReader isrProcstat = null;
		BufferedReader rProcstat = null;
		
		JSONObject joSelectedCounter = null;
		ArrayList<JSONObject> alSLACounters = null;
		JSONObject joQuery = null;
		
		LinuxUnificationCounterBean beanLinuxCounters = null;
		LinuxUnificationCounterBean beanLinuxCountersTopCmd = null;
		LinuxUnificationBean beanLinuxUnification =null;
		LinuxUnificationBean beanSLA = null;
		
		boolean isTopCmdExcuted = false, isLinuxQuery = false;
		
		HashMap<String, String> hmCounterValues = new HashMap<>();
		try {
			joQuery = new JSONObject();
			JSONArray jaSelectedCounters = AgentCounterBean.getCountersBean(Constants.LINUX_AGENT_GUID);
			
			beanLinuxUnification = new LinuxUnificationBean();
			
			//beanLinuxUnification.setMod_type("UBUNTU");
			beanLinuxUnification.setMod_type(Constants.LINUX_SERVER_MODULE_TYPE);
			beanLinuxUnification.setType("MetricSet");
			beanLinuxUnification.setGuid(Constants.LINUX_AGENT_GUID);
			//beanLinuxUnification.setdDateTime(new Date());
			beanLinuxUnification.setdDateTime(collectionDate);
			
			JSONArray jaSlaCounters = SlaCounterBean.getSLACountersBean(Constants.LINUX_AGENT_GUID);
			
			if(jaSlaCounters != null && jaSlaCounters.size() > 0) {
				beanSLA = new LinuxUnificationBean();
			}
			
			for(int i=0; i<jaSelectedCounters.size(); i++){
				isLinuxQuery = false;
				dCounterValue = 0.0;
				dTotalCpuValue = 0.0;
				nCounterId = 0;
				query = "";
				
				joSelectedCounter = jaSelectedCounters.getJSONObject(i);			
				nCounterId = Integer.parseInt(joSelectedCounter.getString("counter_id")) ;
				//query = joSelectedCounter.getString("query");
				bIsDelta = joSelectedCounter.getBoolean("isdelta");
				
				beanLinuxUnification.addNewCounter(joSelectedCounter.getString("counter_id"));
				
				if( joSelectedCounter.getString("query").startsWith("{") && joSelectedCounter.getString("query").trim().endsWith("}")) {
					joQuery = joSelectedCounter.getJSONObject("query");
					
					if(joQuery.getString("cmd_group").contains("TOP_") && !isTopCmdExcuted) {
						hmCounterValues = excuteTopLinuxCmd(joQuery.getString("cmd"));
						isTopCmdExcuted = true;
					}
					dCounterValue = Double.parseDouble(hmCounterValues.get(joQuery.getString("filter")));
				}else {
					isLinuxQuery = true;
					dCounterValue = ExcuteLinuxQuery(joSelectedCounter.getString("query"));
				}
				
				//dCounterValue = Double.parseDouble(line);
				
				
				if(bIsDelta) {
					dCounterValue = addDeltaCounterValue_v1(nCounterId, dCounterValue);					
				}/*else {
					addCounterValue(nCounterId, dCounterValue);
				}*/
				
				// Create Bean for the LinuxUnification OS Module Counter entry(line)
				beanLinuxCounters = new LinuxUnificationCounterBean();
				
				beanLinuxCounters.setCounter_type(nCounterId);
				
				beanLinuxCounters.setException("");
				
				if(!isLinuxQuery && joQuery.containsKey("filter") && joQuery.getString("filter").equals("CPU-TOTAL")) {
					beanLinuxCounters.setProcess_name("_Total");
					for(int top = 1 ; top < 6; top++) {
						beanLinuxCountersTopCmd= new LinuxUnificationCounterBean();
						beanLinuxCountersTopCmd.setCounter_type(nCounterId);
						beanLinuxCountersTopCmd.setCounter_value(Double.parseDouble(hmCounterValues.get("Top"+top+"_Value")));
						beanLinuxCountersTopCmd.setException("");
						beanLinuxCountersTopCmd.setProcess_name(hmCounterValues.get("Top"+top+"_ProcessName"));
						
						beanLinuxUnification.addCounterEntry(String.valueOf(nCounterId), beanLinuxCountersTopCmd);
						
						dTotalCpuValue = dTotalCpuValue + Double.parseDouble(hmCounterValues.get("Top"+top+"_Value"));
					}
					
					beanLinuxCounters.setCounter_value(dTotalCpuValue);
				}else {
					beanLinuxCounters.setProcess_name("");
					beanLinuxCounters.setCounter_value(dCounterValue);
				}
				
				beanLinuxUnification.addCounterEntry(String.valueOf(nCounterId), beanLinuxCounters);
				
				if(SlaCounterBean.getSLACountersBean(Constants.LINUX_AGENT_GUID) != null && SlaCounterBean.getSLACountersBean(Constants.LINUX_AGENT_GUID).size() > 0) {
					//Verifying SLA Breach
					verifySLABreach_v1(jaSlaCounters, nCounterId, dCounterValue, beanSLA);
				}
			}
			
			if(beanLinuxUnification.isCountersValueAvailable()) {
				LogManagerExtended.logJStackOutput("metrics###"+beanLinuxUnification.toString("MetricSet"));
				LogManagerExtended.serverInfoLog("metrics###"+beanLinuxUnification.toString("MetricSet"));
			}
			
			if(beanSLA != null) {
				if(beanSLA.isSLACountersValueAvailable()) {
					//beanSLA.setMod_type("UBUNTU");
					beanSLA.setMod_type(Constants.LINUX_SERVER_MODULE_TYPE);
					beanSLA.setType("SLASet");
					beanSLA.setGuid(Constants.LINUX_AGENT_GUID);
					//beanSLA.setdDateTime(new Date());
					beanSLA.setdDateTime(collectionDate);
					
					LogManagerExtended.logJStackOutput("metrics###"+beanSLA.toString("SLASet"));
					LogManagerExtended.serverInfoLog("metrics###"+beanSLA.toString("SLASet"));
				}
			}
			
		}catch (Exception ex) {
			// TODO: handle exception
			LogManagerExtended.serverInfoLog("Exception in getCountersforLinuxUnification() :"+ ex);
		}
	}
	
	public HashMap<String, String> excuteTopLinuxCmd(String query) {
		String line = null;
		Process pProcstat = null;
		InputStreamReader isrProcstat = null;
		BufferedReader rProcstat = null;
		
		ArrayList keys = new ArrayList(UtilsFactory.joCounters.keySet());
		
		HashMap<String, String> hmCounterValues = new HashMap<>();
		
		boolean isStartTop3Cmd = false;
		int topCount = 1;
		 
		try {
			pbProcStat = new ProcessBuilder("bash", "-c", query);
			
			pProcstat = pbProcStat.start();
			isrProcstat = new InputStreamReader(pProcstat.getInputStream());
			rProcstat = new BufferedReader(isrProcstat);
			
			while ((line = rProcstat.readLine()) != null) {
				/*if(line.isEmpty()) {
					break;
				}*/
				
				LogManagerExtended.serverInfoLog(line);
				String[] counterCategory;
				
				if(line.trim().startsWith("PID")) {
					isStartTop3Cmd = true;
					continue;
				}
				
				if(!line.isEmpty() && isStartTop3Cmd) {
					
					counterCategory = line.trim().split("\\s+");
					
					hmCounterValues.put("Top"+topCount+"_Value", counterCategory[counterCategory.length - 4]);
					hmCounterValues.put("Top"+topCount+"_ProcessName", counterCategory[counterCategory.length - 1]+"-"+counterCategory[0]);
					
					topCount++;
					
				}else if(!line.isEmpty() && !isStartTop3Cmd) {
					counterCategory = line.split(":");
					
					for (int i = 0; i < keys.size(); i++) {
						 String keyValue = keys.get(i).toString();
						 
						 if (counterCategory[0].contains(keyValue)) {
							 
							 String[] SplitValueInComma;
							 if(keyValue.contains("Cpu")) {
								 SplitValueInComma = counterCategory[1].split(",");
							 }else {
								 SplitValueInComma = counterCategory[1].split("[,.][\\s]"); 
							 }
							 
							 for(int j = 0 ; j < SplitValueInComma.length; j++) {
								
								 //String[] Counters = SplitValueInComma[j].trim().split("[%|\\s|k\\s]");
								 String[] Counters = SplitValueInComma[j].trim().split("%|k\\s|\\s+");
								 
								 if(Counters.length > 1) {
									 hmCounterValues.put(keyValue.toUpperCase()+"-"+Counters[1].toUpperCase(), Counters[0]);
								 }
							 }
						 }				 
					 }
				}
			}
						
			double cpu_total = 100 - Double.parseDouble(hmCounterValues.get("CPU-ID"));
			hmCounterValues.put("CPU-TOTAL", cpu_total+""); 

		}catch (Exception e) {
			LogManagerExtended.serverInfoLog("Exception in ExcuteTopCommand : "+ e);
			// TODO: handle exception
		}
		return hmCounterValues;
	}
	
	public Double ExcuteLinuxQuery(String query) {
		Double dCounterValue = 0.0;
		Process pProcstat = null;
		InputStreamReader isrProcstat = null;
		BufferedReader rProcstat = null;
		String line = null;
		try {
			pbProcStat = new ProcessBuilder("bash", "-c", query);
			
			pProcstat = pbProcStat.start();
			isrProcstat = new InputStreamReader(pProcstat.getInputStream());
			rProcstat = new BufferedReader(isrProcstat);
			
			if ((line = rProcstat.readLine()) != null) {
				dCounterValue = Double.parseDouble(line);
			}	
		}catch (Exception e) {
			System.out.println("Exception in ExcuteLinuxQuery : "+ e);
			// TODO: handle exception
		}
		return dCounterValue;
	}
	
	
	public void terminateOldAgent() throws Exception{
		Process pProcstat = null;
		InputStreamReader isrProcstat = null;
		BufferedReader rProcstat = null;
		
		String line = null, line1 = null;
		boolean isAgentRunning = false;
		try {
			
			pbProcStat = new ProcessBuilder("bash", "-c", "ps -eaf | grep appedo_linux_agent.jar");
			
			pProcstat = pbProcStat.start();
			isrProcstat = new InputStreamReader(pProcstat.getInputStream());
			rProcstat = new BufferedReader(isrProcstat);
			
			System.out.println("============================================");
			System.out.println("stop and kill old agent process starting......");
			
			while((line = rProcstat.readLine()) != null) {
				
				if(line.contains("java -jar")) {
					isAgentRunning = true;
					String grepPattern[] = line.trim().split("\\s+");
					
					pbProcStat = new ProcessBuilder("bash", "-c", "sudo pwdx "+grepPattern[1]);
					pProcstat = pbProcStat.start();
					isrProcstat = new InputStreamReader(pProcstat.getInputStream());
					rProcstat = new BufferedReader(isrProcstat);
					
					if((line1 = rProcstat.readLine()) != null) {
						String[] path = line1.split(":");
						pbProcStat = new ProcessBuilder("bash", "-c", "sudo kill -9 "+grepPattern[1]);
						pProcstat = pbProcStat.start();

						pbProcStat = new ProcessBuilder("bash", "-c", "sudo rm -rf "+path[1]);
						pProcstat = pbProcStat.start();
					}
				}
			}
			
			if(!isAgentRunning) {
				System.out.println("Old Linux agent is Not Running....");
			}
			
			System.out.println("Process completed......");
			System.out.println("==============================================");
			
		}catch (Exception e) {
			// TODO: handle exception
			System.out.println("Exception in terminateOldAgent : "+ e);
		}
	}
	
	public boolean JbossServerStatus() throws Exception{
		Process pProcstat = null;
		InputStreamReader isrProcstat = null;
		BufferedReader rProcstat = null;
		String line = "", jboss_pid = "";
		boolean isAppServerRunning = false;
		try {	
			//pbProcStat = new ProcessBuilder("bash", "-c", "ps -eaf | grep org.jboss.as");
			pbProcStat = new ProcessBuilder("bash", "-c", Constants.JBOSS_GREP_KEY);
			pProcstat = pbProcStat.start();
			isrProcstat = new InputStreamReader(pProcstat.getInputStream());
			rProcstat = new BufferedReader(isrProcstat);
			
			while((line = rProcstat.readLine()) != null) {
				
				if(line.contains("org.jboss.as.standalone")) {
					LogManagerExtended.applicationInfoLog("Jboss server is Running now....");
					String[] spiltted_Res =line.split("\\s+");
					LogManagerExtended.applicationInfoLog("Jboss Pid: "+ spiltted_Res[1]);
					jboss_pid = spiltted_Res[1];
					isAppServerRunning = true;
				}
			}
			
			if(isAppServerRunning && !jboss_pid.isEmpty()) {
				getAvailableJbossPorts(jboss_pid);
			}
			
			if(!isAppServerRunning) {
				LogManagerExtended.applicationInfoLog("Jboss Server is not Running...");
			}
				
		}catch (Exception e) {
			LogManagerExtended.applicationInfoLog(e.getMessage());
			// TODO: handle exception
		}
		return isAppServerRunning;
	}
	
	public void getAvailableJbossPorts(String pid) throws Exception{
		Process pProcstat = null;
		InputStreamReader isrProcstat = null;
		BufferedReader rProcstat = null;
		String line = "";
		//ArrayList<Integer> alJbossPorts = new ArrayList<Integer>();
		
		try {	
			pbProcStat = new ProcessBuilder("bash", "-c", "sudo netstat -lpn | grep "+pid);
			pProcstat = pbProcStat.start();
			isrProcstat = new InputStreamReader(pProcstat.getInputStream());
			rProcstat = new BufferedReader(isrProcstat);
			
			while((line = rProcstat.readLine()) != null) {
				String[] spiltted_Res =line.split("\\s+");
				String[] port = spiltted_Res[3].split(":+");
				Constants.JBOSS_PORTS.add(port[port.length-1]);
			}
			LogManagerExtended.applicationInfoLog("list of jboss ports : "+Constants.JBOSS_PORTS.toString());
		}catch (Exception e) {
			LogManagerExtended.applicationInfoLog(e.getMessage());
		}
	}
	
	public boolean TomcatServerStatus() throws Exception{
		Process pProcstat = null;
		InputStreamReader isrProcstat = null;
		BufferedReader rProcstat = null;
		String line = "";
		boolean isAppServerRunning = false;
		try {	
			UtilsFactory.printDebugLog(Constants.IS_DEBUG, "GREP command for tomcat running status: "+Constants.TOMCAT_GREP_KEY);
			pbProcStat = new ProcessBuilder("bash", "-c", Constants.TOMCAT_GREP_KEY);
			pProcstat = pbProcStat.start();
			isrProcstat = new InputStreamReader(pProcstat.getInputStream());
			rProcstat = new BufferedReader(isrProcstat);
			
			while((line = rProcstat.readLine()) != null) {
				
				if(line.contains("org.apache.catalina.startup.Bootstrap start")) {
					LogManagerExtended.applicationInfoLog("Tomcat is Running now....");
					UtilsFactory.printDebugLog(Constants.IS_DEBUG, "Tomcat is Running now.");
					if(line.contains("jmxremote.port=")) {
						UtilsFactory.printDebugLog(Constants.IS_DEBUG, "JMX port is in enabled status.");
						UtilsFactory.printDebugLog(Constants.IS_DEBUG, "Response Line: "+line);
						LogManagerExtended.applicationInfoLog("JMX Port is enabled....");
						
						String[] JMXSplited = line.split("jmxremote.port=");
					
						String[] JMXPort = JMXSplited[1].split(" ");
						
						LogManagerExtended.applicationInfoLog("JMX Port :" + JMXPort[0]);
						
						//Constants.TOMCAT_JMXPORT = JMXPort[0];
						Constants.TOMCAT_PORTS.add(JMXPort[0]);
						Constants.TOMCAT_APP_LIST.put(JMXPort[0], new HashSet<String>());
						UtilsFactory.printDebugLog(Constants.IS_DEBUG, "Enabled JMX port : "+JMXPort[0]);
						
						isAppServerRunning = true;
						
						//break;
					}else {
						UtilsFactory.printDebugLog(Constants.IS_DEBUG, "JMX port is disabled.");
						LogManagerExtended.applicationInfoLog(" JMX port is disable....");
					}
				}
			}
			
			if(!isAppServerRunning) {
				UtilsFactory.printDebugLog(Constants.IS_DEBUG, "Tomcat server is not running.");
				LogManagerExtended.applicationInfoLog("Tomcat Server is not Running...");
			}
				
		}catch (Exception e) {
			LogManagerExtended.applicationInfoLog(e.getMessage());
			UtilsFactory.printDebugLog(Constants.IS_DEBUG, "ERROR: "+e.getMessage());
			// TODO: handle exception
		}
		return isAppServerRunning;
	}
	
	public boolean PostgresDbStatus() throws Exception{
		boolean isPgDbRunning = false;
		Process pProcstat = null;
		InputStreamReader isrProcstat = null;
		BufferedReader rProcstat = null;
		String line = "";
		
		try {
			pbProcStat = new ProcessBuilder("bash", "-c", Constants.POSTGRES_GREP_KEY);
			pProcstat = pbProcStat.start();
			isrProcstat = new InputStreamReader(pProcstat.getInputStream());
			rProcstat = new BufferedReader(isrProcstat);
			
			while((line = rProcstat.readLine()) != null) {
				
				if(Constants.POSTGRES_CONTAIN_KEY != null) {
					if(line.contains(Constants.POSTGRES_CONTAIN_KEY)) {
						isPgDbRunning = true;
					}
				}else if(line.contains("pg_ctl") && line.contains("server is running")) {
					isPgDbRunning = true;
				}
			}
			if(isPgDbRunning) {
				LogManagerExtended.databaseInfoLog("Postgres DB server is running... ");
			}else {
				LogManagerExtended.databaseInfoLog("Postgres DB server is not running.. ");
			}
		}catch (Exception e) {
			LogManagerExtended.databaseInfoLog("Exception in PostgresDbStatus : "+e);
		}
		return isPgDbRunning;
	}
	
	public boolean OracleDbStatus() throws Exception{
		boolean isOracleDbRunning = false;
		Process pProcstat = null;
		InputStreamReader isrProcstat = null;
		BufferedReader rProcstat = null;
		String line = "";
		
		try {
			pbProcStat = new ProcessBuilder("bash", "-c", "ps -eaf | grep pmon");
			pProcstat = pbProcStat.start();
			isrProcstat = new InputStreamReader(pProcstat.getInputStream());
			rProcstat = new BufferedReader(isrProcstat);
			
			while((line = rProcstat.readLine()) != null) {
				if(line.contains("pmon") && !line.contains("grep")) {
					isOracleDbRunning = true;
				}
			}
			
			if(isOracleDbRunning) {
				LogManagerExtended.databaseInfoLog("Oracle DB server is running... ");
			}else {
				LogManagerExtended.databaseInfoLog("Oracle DB server is not running.. ");
			}
		}catch (Exception e) {
			LogManagerExtended.databaseInfoLog("Exception in OracleDbStatus : "+e);
		}
		return isOracleDbRunning;
	}
	
	@Override
	protected void finalize() throws Throwable {
		clearCounterMap();
		
		super.finalize();
	}
	
	public static void main(String[] args) {
		System.out.println( (1.2d < 49l) );
	}
}
