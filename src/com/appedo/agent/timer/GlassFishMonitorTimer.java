package com.appedo.agent.timer;

import java.util.TimerTask;

import com.appedo.agent.manager.GlassFishMonitorManager;

/**
 * This class handles the timer thread for GlassFish Server's counter monitor, which is invoked for each time interval given.
 * This timer calls the monitor functions to collect he counters
 */

public class GlassFishMonitorTimer extends TimerTask {

	private String strOperation = null,
					strGUID = null,
					strAppName = null,
					strSvrAlias = null;
	
	/**
	 * Lone constructor which is created with the purpose of the object.
	 * 
	 * @param strOperation It can be MONITOR or SEND 
	 */
	public GlassFishMonitorTimer(String strGuid, String strAppName, String strSvrAlias, String strOperation){
		this.strGUID = strGuid;
		this.strAppName = strAppName;
		this.strOperation = strOperation;
		this.strSvrAlias = strSvrAlias;
	}
	
	/**
	 * This thread's run(), which invokes the respective functionality like Monitoring or sending counter-sets to WebService
	 */
	public void run() {
		try {
			if( strOperation.equals("MONITOR") ) {
				GlassFishMonitorManager.getGlassFishMonitorManager(strAppName, strSvrAlias).monitorGlassFishServer(strGUID, strAppName);
				GlassFishMonitorManager.getGlassFishMonitorManager(strAppName, strSvrAlias).sendGlassFishCounters(strGUID);
			}				
		} catch(Throwable e) {
			System.out.println("Exception in GlassFishMonitorTimer.run: "+e.getMessage());
			e.printStackTrace();
		}
    }
	
	@Override
	protected void finalize() throws Throwable {
		System.out.println("GlassFish monitor timer thread stopped");
		
		super.finalize();
	}
}
