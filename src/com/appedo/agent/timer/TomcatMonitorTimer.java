package com.appedo.agent.timer;

import java.util.TimerTask;

import com.appedo.agent.manager.TomcatMonitorManager;

/**
 * This class handles the timer thread for Tomcat Server's counter monitor, which is invoked for each time interval given.
 * This timer calls the monitor functions to collect he counters
 */

public class TomcatMonitorTimer extends TimerTask {

	private String strOperation = null;
	private String strGUID = null;
	private String strApp = null;
	private String strSvrAlias = null;
	private String strConnectorPortAddress = null;

	/**
	 * Lone constructor which is created with the purpose of the object.
	 * 
	 * @param strOperation It can be MONITOR or SEND
	 */
	public TomcatMonitorTimer(String strGuid, String strApp, String strSvrAlias, String strConnectorPortAddress, String strOperation){
		this.strGUID = strGuid;
		this.strApp = strApp;
		this.strSvrAlias = strSvrAlias;
		this.strConnectorPortAddress = strConnectorPortAddress;
		this.strOperation = strOperation;
	}

	/**
	 * This thread's run(), which invokes the respective functionality like Monitoring or sending counter-sets to WebService
	 */
	public void run() {
		try {
			if( strOperation.equals("MONITOR") ) {
				//System.out.println("Starting Tomcat monitor timer thread: "+(new Date()));
				TomcatMonitorManager.getTomcatMonitorManager(strApp, strSvrAlias).monitorTomcatServer(strGUID, strApp, strConnectorPortAddress);
				TomcatMonitorManager.getTomcatMonitorManager(strApp, strSvrAlias).sendTomcatCounters(strGUID);
			}
		} catch(Throwable e) {
			System.out.println("Exception in TomcatMonitorTimer.run: "+e.getMessage());
			e.printStackTrace();
		}
    }

	@Override
	protected void finalize() throws Throwable {
		System.out.println("Tomcat monitor timer thread stopped");

		super.finalize();
	}
}
