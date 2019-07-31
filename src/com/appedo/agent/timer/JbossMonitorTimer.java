package com.appedo.agent.timer;

import java.util.TimerTask;

import com.appedo.agent.manager.JbossMonitorManager;

public class JbossMonitorTimer extends TimerTask {

	private String strOperation = null;
	private String strGUID = "";
	private String strApp = "";
	
	/**
	 * Lone constructor which is created with the purpose of the object.
	 * 
	 * @param strOperation It can be MONITOR or SEND
	 */
	public JbossMonitorTimer(String strGuid, String strApp, String strOperation){
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
				JbossMonitorManager.getJbossMonitorManager().monitorJbossServer(strGUID,strApp);
				
				JbossMonitorManager.getJbossMonitorManager().sendJbossCounters(strGUID);
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
