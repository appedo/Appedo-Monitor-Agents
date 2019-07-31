package com.appedo.agent.init;

import java.util.Timer;
import java.util.TimerTask;

import com.appedo.agent.manager.AgentManager;
import com.appedo.agent.manager.LinuxMonitorManager;
import com.appedo.agent.manager.LogManagerExtended;
import com.appedo.agent.timer.LinuxUnificationServerMonitorTimer;
import com.appedo.agent.utils.Constants;
import com.appedo.agent.utils.UtilsFactory;
import com.appedo.agent.utils.Constants.AGENT_TYPE;

import net.sf.json.JSONObject;

public class AgentIgnitorLinuxUnificationServerThread extends Thread{
	AgentManager am = null;
	Timer timerLinuxUnificationServerManager = null;
		
	public AgentIgnitorLinuxUnificationServerThread() throws Throwable{
		am = new AgentManager();
		start();
	}
	
	public void run() {
		
		JSONObject serverInformation = null;
		JSONObject joLinuxServerCounterSer = null;
		
		String moduleGUID = "";
		try {
			
			LogManagerExtended.serverInfoLog("Linux Unification server thread started...");
			UtilsFactory.printDebugLog(Constants.IS_DEBUG, "Linux Unification server thread started.");
			joLinuxServerCounterSer = LinuxMonitorManager.getLinuxMonitorManager().getLinuxDynamicCounters();
			
			/*if(Constants.SYS_SERIALNUMBAR == null && Constants.SYS_MANUFACTURER == null && Constants.SYS_PRODUCTNAME == null) {
				serverInformation = LinuxMonitorManager.getLinuxMonitorManager().getOperatingSystemInformation();
			}else {
				serverInformation = LinuxMonitorManager.getLinuxMonitorManager().getOperatingSystemInformationOnpremEnv();
			}*/
			
			serverInformation = LinuxMonitorManager.getLinuxMonitorManager().getOperatingSystemInformation();
			
			if(serverInformation.isEmpty()) {
				serverInformation = LinuxMonitorManager.getLinuxMonitorManager().getOperatingSystemInformationOnpremEnv();
			}
			
			LogManagerExtended.serverInfoLog("OS ServerInformation : "+ serverInformation.toString());
			
			moduleGUID = am.sendModuleInfoToCollector(serverInformation, Constants.SYSTEM_ID, Constants.SYS_UUID, joLinuxServerCounterSer, "serverInformation");
			
			if(!moduleGUID.isEmpty()) {
				
				Constants.LINUX_AGENT_GUID = moduleGUID;
				
				try {
					if(am.getConfigurationsFromCollector(Constants.LINUX_AGENT_GUID, AGENT_TYPE.LINUX.toString())) {
						Timer timerLinuxUnificationServerManager = new Timer();
						TimerTask ttLinuxUnificationServer = new LinuxUnificationServerMonitorTimer(timerLinuxUnificationServerManager);
						timerLinuxUnificationServerManager.schedule(ttLinuxUnificationServer, 100l, Constants.MONITOR_FREQUENCY_MILLESECONDS);
					}
				} catch (Throwable e) {
					// TODO Auto-generated catch block
					LogManagerExtended.serverInfoLog(e.getMessage());
					e.printStackTrace();
				}
				
			}else {
				LogManagerExtended.serverInfoLog("module GUID is return empty value...");
			}
			
		}catch (Exception e) {
			// TODO: handle exception
			System.out.println(e);
		}finally {
			
		}
	}
	
	
	public static void main(String[] args) {
		// TODO Auto-generated method stub
		
		String name = "";
		
		System.out.println("isEmpty : "+ name.isEmpty());
		
		//String obj = "Catalina:type=ThreadPool,name=\"ajp-bio-8009\":";
		
		//String obj = "Catalina:j2eeType=Servlet,name=jsp,WebModule=//localhost/appedo_rum_collector,J2EEApplication=none,J2EEServer=none:";
		
		/*String obj = "Catalina:type=Valve,context=/appedo_sla_collector,host=localhost,name=StandardContextValve:";
		String[] value = obj.split("[:,]");
		
		System.out.println(obj.length());
		System.out.println(obj.lastIndexOf("name="));
		System.out.println(obj.indexOf("name="));
		
		String type = "", name = "", category = "";
		
		for(String res : value) {
			System.out.println(res);
			if(res.contains("type=")) {
				type = res.split("=")[1];
			}else if(res.contains("name=")) {
				name = res.split("=")[1];
			}
			category = type+"-"+name;
		}
		
		System.out.println("Category : "+category);*/
		
	}

}
