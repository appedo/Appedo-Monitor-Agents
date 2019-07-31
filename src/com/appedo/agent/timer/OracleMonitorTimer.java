package com.appedo.agent.timer;

import java.util.Date;
import java.util.TimerTask;

import com.appedo.agent.manager.OracleMonitorManager;
import com.appedo.agent.utils.Constants;


public class OracleMonitorTimer extends TimerTask {

	private String strOperation = null;
	private String strGUID = "";
	private String strDbName = "";
	
	private Date dateSlowQueryLastReadOn = null;
	
	/**
	 * Lone constructor which is created with the purpose of the object.
	 * 
	 * @param strOperation It can be MONITOR or SEND
	 */
	public OracleMonitorTimer(String strGuid, String strDbName, String strOperation){
		this.strGUID = strGuid;
		this.strDbName = strDbName;
		this.strOperation = strOperation;
		
		dateSlowQueryLastReadOn = new Date( new Date().getTime() - Constants.SLOW_QUERY_READ_FREQUENCY_MILLISECONDS );
	}
	
	/**
	 * This thread's run(), which invokes the respective functionality like Monitoring or sending counter-sets to WebService
	 */
	public void run() {
			try{
				if( strOperation.equals("MONITOR") ) {
					dateSlowQueryLastReadOn = OracleMonitorManager.getOracleMonitorManager().monitorOracleServer(strGUID, strDbName, dateSlowQueryLastReadOn);
					OracleMonitorManager.getOracleMonitorManager().sendOracleCounters(strGUID);
				}
			} catch(Throwable e) {
				System.out.println("Exception in OracleMonitorTimer.run: "+e.getMessage());
				e.printStackTrace();
			}
    }
	
	@Override
	protected void finalize() throws Throwable {
		System.out.println("Oracle monitor timer thread stopped");
		
		super.finalize();
	}
}
