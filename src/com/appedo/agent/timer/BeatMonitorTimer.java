package com.appedo.agent.timer;

import java.util.TimerTask;

import com.appedo.agent.manager.BeatMonitorManager;


public class BeatMonitorTimer extends TimerTask {

	private String strGUID = "";
	
	/**
	 * Lone constructor which is created with the purpose of the object.
	 * 
	 * @param strOperation It can be MONITOR or SEND
	 */
	public BeatMonitorTimer(String strGuid){
		this.strGUID = strGuid;
	}
	
	/**
	 * This thread's run(), which invokes the respective functionality like Monitoring or sending counter-sets to WebService
	 */
	public void run() {
			try{
				BeatMonitorManager.getBeatMonitorManager().sendBeatCounters(strGUID);
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
