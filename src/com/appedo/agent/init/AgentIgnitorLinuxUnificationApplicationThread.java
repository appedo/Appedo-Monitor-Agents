package com.appedo.agent.init;

import com.appedo.agent.manager.LinuxMonitorManager;
import com.appedo.agent.manager.LogManagerExtended;
import com.appedo.agent.utils.Constants;
import com.appedo.agent.utils.UtilsFactory;

public class AgentIgnitorLinuxUnificationApplicationThread extends Thread{

	public AgentIgnitorLinuxUnificationApplicationThread() {
		// TODO Auto-generated constructor stub
		start();
	}

	public void run() {
		UtilsFactory.printDebugLog(Constants.IS_DEBUG, "Linux Unification Application thread started.");
		try {
			if(LinuxMonitorManager.getLinuxMonitorManager().TomcatServerStatus()) {
				//Tomcat Server monitor flow.
				UtilsFactory.printDebugLog(Constants.IS_DEBUG, "Tomcat serve is running in ports: "+Constants.TOMCAT_PORTS.size());
				for(int i=0; i<Constants.TOMCAT_PORTS.size(); i++) {
					new AgentIgnitorLinuxUnificationTomcatThread(Constants.TOMCAT_PORTS.get(i));
				}
			}
			if(LinuxMonitorManager.getLinuxMonitorManager().JbossServerStatus()) {
				//Jboss server monitor flow.
				UtilsFactory.printDebugLog(Constants.IS_DEBUG, "JBOSS server is running.");
				new AgentIgnitorLinuxUnificationJbossThread();
			}
			
		}catch (Exception e) {
			LogManagerExtended.applicationInfoLog(e.getMessage());
		}
	}
	
	public static void main(String[] args) {
		// TODO Auto-generated method stub

	}

}
