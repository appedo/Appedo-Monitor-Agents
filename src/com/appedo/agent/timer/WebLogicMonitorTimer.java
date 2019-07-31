package com.appedo.agent.timer;

import java.util.TimerTask;

import com.appedo.agent.manager.WebLogicMonitorManager;

public class WebLogicMonitorTimer extends TimerTask {

	private String strOperation = null;
	private String strGUID = "";
	private String strApp = "";
	
	/**
	 * Lone constructor which is created with the purpose of the object.
	 * 
	 * @param strOperation It can be MONITOR or SEND
	 */
	public WebLogicMonitorTimer(String strGuid, String strApp, String strOperation){
		this.strGUID = strGuid;
		this.strApp = strApp;
		this.strOperation = strOperation;
	}
	
	/**
	 * This thread's run(), which invokes the respective functionality like Monitoring or sending counter-sets to WebService
	 */
	public void run() {
			try{
				if( strOperation.equals("MONITOR") ) {
					WebLogicMonitorManager.getWebLogicMonitorManager().monitorWebLogicServer(strGUID,strApp);
					WebLogicMonitorManager.getWebLogicMonitorManager().sendWebLogicCounters(strGUID);
				}				
			} catch(Throwable e) {
				System.out.println("Exception in WebLogicMonitorTimer.run: "+e.getMessage());
				e.printStackTrace();
			}
    }
	
	@Override
	protected void finalize() throws Throwable {
		System.out.println("WebLogic monitor timer thread stopped");
		
		super.finalize();
	}
}
