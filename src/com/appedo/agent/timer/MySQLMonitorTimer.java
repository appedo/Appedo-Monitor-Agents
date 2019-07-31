package com.appedo.agent.timer;

import java.util.TimerTask;

import com.appedo.agent.manager.MySQLMonitorManager;

/**
 * This class handles the timer thread for MySQL Database Server's counter monitor, which is invoked for each time interval given.
 * This timer calls the monitor functions to collect he counters
 * 
 * @author veeru
 *
 */
public class MySQLMonitorTimer extends TimerTask {

	private String strOperation = null;
	private String strGUID = null;
	private String strDbName = null;
	
	/**
	 * Lone constructor which is created with the purpose of the object.
	 * 
	 * @param strOperation It can be MONITOR or SEND
	 */
	public MySQLMonitorTimer(String strGUID, String strDbName, String strOperation){
		this.strGUID = strGUID;
		this.strDbName = strDbName;
		this.strOperation = strOperation;
	}
	
	/**
	 * This thread's run(), which invokes the respective functionality like Monitoring or sending counter-sets to WebService 
	 * for each THREAD_RUN_INTERVAL_MILLISECOND.
	 */
	public void run() {
		try{
			if( strOperation.equals("MONITOR") ) {
				MySQLMonitorManager.getMySQLMonitorManager().monitorMySQLServer(strGUID,strDbName);
				MySQLMonitorManager.getMySQLMonitorManager().sendMySQLCounters(strGUID);
			}
		} catch(Throwable e) {
			System.out.println("Exception in MYSQLMonitorTimer.run: "+e.getMessage());
			e.printStackTrace();
		}
    }
	
	@Override
	protected void finalize() throws Throwable {
		System.out.println("MySQL monitor timer thread stopped");
		
		super.finalize();
	}
}
