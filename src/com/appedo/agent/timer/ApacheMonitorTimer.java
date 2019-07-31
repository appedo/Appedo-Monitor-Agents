package com.appedo.agent.timer;

import java.util.TimerTask;

import com.appedo.agent.manager.ApacheMonitorManager;

/**
 * This class handles the timer thread for Apache Server's counter monitor, which is invoked for each time interval given.
 * This timer calls the monitor functions to collect he counters
 * 
 * @author veeru
 *
 */
public class ApacheMonitorTimer extends TimerTask {
	
	private String strOperation = null, strHostName = null, strAppPort = null, strMonitorURL = null;
	
	/**
	 * Lone constructor which is created with the purpose of the object.
	 * 
	 * @param strOperation It can be MONITOR or SEND
	 */
	public ApacheMonitorTimer(String strOperation, String strHostName, String strAppPort, String strMonitorURL){
		this.strOperation = strOperation;
		this.strHostName = strHostName;
		this.strAppPort = strAppPort;
		this.strMonitorURL = strMonitorURL;
	}
	
	/**
	 * This thread's run(), which invokes the respective functionality like Monitoring or sending counter-sets to WebService
	 */
	public void run() {
		try{
			
			if( strOperation.equals("MONITOR") ) {
				//System.out.println("Starting Apache monitor timer thread: "+(new Date()));
				ApacheMonitorManager.getApacheMonitorManager(strHostName, strAppPort, strMonitorURL).monitorApacheServer();
				ApacheMonitorManager.getApacheMonitorManager(strHostName, strAppPort, strMonitorURL).sendApacheCounters();
			}
		} catch(Throwable e) {
			System.out.println("Exception in ApacheMonitorTimer.run: "+e.getMessage());
			e.printStackTrace();
		}
    }
	
	@Override
	protected void finalize() throws Throwable {
		System.out.println("Apache monitor timer thread stopped");
		
		super.finalize();
	}
}
