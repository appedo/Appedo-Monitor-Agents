package com.appedo.agent.timer;

import java.util.TimerTask;

import com.appedo.agent.manager.PostgresSQLMonitorManager;

/**
 * This class handles the timer thread for Linux Server's counter monitor, which is invoked for each time interval given.
 * This timer calls the monitor functions to collect he counters
 * 
 * @author veeru
 *
 */
public class PostgresSQLMonitorTimer extends TimerTask {
	
	private String strOperation = null;
	String strGUID, strDbName;
	
	/**
	 * Lone constructor which is created with the purpose of the object.
	 * 
	 * @param strOperation It can be MONITOR or SEND
	 */
	public PostgresSQLMonitorTimer(String strGuid, String strDbName,String strOperation){
		this.strOperation = strOperation;
		this.strGUID = strGuid;
		this.strDbName = strDbName;
		
	}
	
	/**
	 * This thread's run(), which invokes the respective functionality like Monitoring or sending counter-sets to WebService
	 */
	public void run() {
			try{
				
				if( strOperation.equals("MONITOR") ) {
					//System.out.println("Starting Linux monitor timer thread: "+(new Date()));
					PostgresSQLMonitorManager.getPGMonitorManager().monitorPGServer(strGUID,strDbName);
					PostgresSQLMonitorManager.getPGMonitorManager().sendPGCounters(strGUID);
				}
			} catch(Throwable e) {
				System.out.println("Exception in LinuxMonitorTimer.run: "+e.getMessage());
				e.printStackTrace();
			}
    }
	
	@Override
	protected void finalize() throws Throwable {
		System.out.println("Linux monitor timer thread stopped");
		
		super.finalize();
	}
}
