package com.appedo.agent.manager;

import java.io.InputStream;
import java.util.TimerTask;

import org.apache.commons.lang.StringUtils;

import com.appedo.agent.utils.Constants;
import com.appedo.agent.utils.UtilsFactory;

public class AppListComparison extends TimerTask{

	private ProcessBuilder pbProcStat = null;
	private ProcessBuilder pbProcStatWin = null;
	
	/**
	 * Avoid the object creation for this Class from outside.
	 */
	public AppListComparison() {
		pbProcStat = new ProcessBuilder("bash", "-c", "tail /proc/stat | grep '^cpu '");
	}
	
	@Override
	public void run() {
		// TODO Auto-generated method stub
		
		Process pProcstat = null;
		InputStream input = null;
		AgentManager agentManager = null;
		int errCode;
		String response = null, OS_Name = "";
		try {
			agentManager = new AgentManager();
			
			//Make a process to execute counter query
			//pbProcStat = new ProcessBuilder("bash", "-c", "yum list installed >> fileName.txt");
			
			pbProcStat = new ProcessBuilder("bash", "-c", "sudo cat /etc/*-release | grep 'PRETTY_NAME' | cut -d '\"' -f 2 | cut -d ' ' -f 1,2");
			
			pProcstat = pbProcStat.start();
			
			errCode = pProcstat.waitFor();

			System.out.println("getting OSName command executed, any errors?  " + (errCode == 0 ? "No" : "Yes"));
			
			response = agentManager.output(pProcstat.getInputStream());
			
			if(errCode != 0) {
				System.out.println("App Installing list Exception : "+ response);
			}else {
				OS_Name = response;
				System.out.println("OS Name : "+ OS_Name);
			}
			
			if(OS_Name.toLowerCase().contains("amazon") || OS_Name.toLowerCase().contains("centos") || StringUtils.deleteSpaces(OS_Name.toLowerCase()).contains("redhat") || OS_Name.toLowerCase().contains("fedora")) {
				pProcstat = null;
				
				pbProcStat = new ProcessBuilder("bash", "-c", "yum list installed");
				
				pProcstat = pbProcStat.start();
				
				input = pProcstat.getInputStream();
				
				//byte[] tfile = IOUtils.toByteArray(input);
				
				byte[] dataBytes = UtilsFactory.getBytes(input);
				
				agentManager.sendBytesToCollector(dataBytes, Constants.LINUX_AGENT_GUID, "installingAppList", "");
			}else if(OS_Name.toLowerCase().contains("ubuntu")){
				pProcstat = null;
				
				pbProcStat = new ProcessBuilder("bash", "-c", "apt list --installed");
				
				pProcstat = pbProcStat.start();
				
				input = pProcstat.getInputStream();
				
				//byte[] tfile = IOUtils.toByteArray(input);
				
				byte[] dataBytes = UtilsFactory.getBytes(input);
				
				agentManager.sendBytesToCollector(dataBytes, Constants.LINUX_AGENT_GUID, "installingAppList", "");
			}else {
				System.out.println("App list compare is not support for "+OS_Name);
			}
			
		}catch(Exception e) {
			System.out.println("Exception : "+e);
		}
		
	}

}
