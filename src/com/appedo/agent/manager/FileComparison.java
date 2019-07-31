package com.appedo.agent.manager;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.TimerTask;

import com.appedo.agent.utils.Constants;
import com.appedo.agent.utils.UtilsFactory;

public class FileComparison extends TimerTask{

	private ProcessBuilder pbProcStat = null;
	private ProcessBuilder pbProcStatWin = null;
	
	/**
	 * Avoid the object creation for this Class from outside.
	 */
	public FileComparison() {
		pbProcStat = new ProcessBuilder("bash", "-c", "tail /proc/stat | grep '^cpu '");
	}
	
	@Override
	public void run() {
		// TODO Auto-generated method stub
		
		Process pProcstat = null;
		InputStream input = null;
		AgentManager agentManager = null;
		LinuxMonitorManager linuxMonitorManager = null;
		String fileName = "", line;
		String[] file_Path;
		InputStreamReader isrProcstat = null;
		BufferedReader rProcstat = null;
		boolean isCorrectPath = false;
		
		try {
			agentManager = new AgentManager();
			linuxMonitorManager = LinuxMonitorManager.getLinuxMonitorManager();
		
			file_Path = Constants.FILE_PATH;
			if(file_Path != null) {
				if(file_Path.length > 1) {
					for(int i = 0; i < file_Path.length ; i++) {
						if(linuxMonitorManager.isExistsDirectory(file_Path[i])) {
							String first_split = file_Path[i].substring(file_Path[i].lastIndexOf('/')+1);
							
							fileName = first_split.substring(0, first_split.lastIndexOf('.'));
							
							pbProcStat = new ProcessBuilder("bash", "-c", "cat "+file_Path[i]);
							
							pProcstat = null;
							
							pProcstat = pbProcStat.start();

							input = pProcstat.getInputStream();
							
							byte[] dataBytes = UtilsFactory.getBytes(input);
							
							agentManager.sendBytesToCollector(dataBytes, Constants.LINUX_AGENT_GUID, "fileCompare", fileName);
						}else {
							System.out.println("cannot access "+file_Path[i]+": No such file or directory");
						}
					}
					
				}else {
					//isCorrectPath = linuxMonitorManager.isExistsDirectory(File_Path[0]);
					if(linuxMonitorManager.isExistsDirectory(file_Path[0])) {
					
						String first_split = file_Path[0].substring(file_Path[0].lastIndexOf('/')+1);
						//String first_split = file_Path[0].substring(file_Path[0].lastIndexOf('\\')+1);
						
						fileName = first_split.substring(0, first_split.lastIndexOf('.'));
						
						//read the file to given file path
						pbProcStat = new ProcessBuilder("bash", "-c", "cat "+file_Path[0]);
						
						//Windows commend
						//String[] command = {"CMD", "/C", "more c:\\Appedo\\resource\\appedo_config_fc.properties"};
						//String[] command = {"CMD", "/C", "more "+file_Path[0]};
						//pbProcStat = new ProcessBuilder(command);
						
						pProcstat = null;
						pProcstat = pbProcStat.start();

						input = pProcstat.getInputStream();
						
						byte[] dataBytes = UtilsFactory.getBytes(input);
						
						agentManager.sendBytesToCollector(dataBytes, Constants.LINUX_AGENT_GUID, "fileCompare", fileName);
					}else {
						System.out.println("cannot access "+file_Path[0]+": No such file or directory");
					}
				}
			}else {
				System.out.println("File Path should not be empty, so add file path in properties");
			}
			
		}catch(Exception e) {
			System.out.println("Exception : "+e);
		}
		
	}
	
	public static void main(String args[]) {
		String Name = "mnt/appedo/resource/config.properties";
		
		String value = Name.substring(Name.lastIndexOf('/')+1);
		
		String res = value.substring(0,value.lastIndexOf('.'));
		
		System.out.println("Result :"+ res);
		
	}

}