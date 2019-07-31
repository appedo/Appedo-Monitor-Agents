package com.appedo.agent.timer;

import java.util.HashSet;

import com.appedo.agent.bean.JStackBean;
import com.appedo.agent.manager.JStackMonitorManager;
import com.appedo.agent.utils.Constants;

/**
 * This class handles the thread to monitor the JVM and get all Threads StackTrace with JStack command.
 * 
 * @author Ramkumar
 *
 */
public class JStackMonitorTimer extends Thread {
	
	private String strSvrAlias = null;
	private String strOperation = null;
	
	private Object[] prevJStackProcessObj = new Object[] {null, null};
	
	int nTemp = 0;
	
	/**
	 * Lone constructor which is created with the purpose of the object.
	 * 
	 * @param strOperation It can be MONITOR or SEND
	 */
	public JStackMonitorTimer(String strSvrAlias, String strOperation){
		this.strSvrAlias = strSvrAlias;
		this.strOperation = strOperation;
		
		prevJStackProcessObj = new Object[] {null, null, null};
	}
	
	/**
	 * This thread's run(), which invokes the respective functionality like Monitoring or sending counter-sets to WebService
	 */
	public void run() {
		
		while( true ) {
//		while( nTemp++ < 40 ) {
			try{
				if( strOperation.equals("MONITOR") ) {
					
					prevJStackProcessObj = JStackMonitorManager.getJStackMonitorManager(strSvrAlias).monitorJStack( (JStackBean)prevJStackProcessObj[0], (Long)prevJStackProcessObj[1], (HashSet<String>) prevJStackProcessObj[2]);
					
					try {
						Thread.sleep( Constants.SLEEP_BETWEEN_LOOP_MILLISECONDS );
					} catch (InterruptedException e1) {
						e1.printStackTrace();
					}
				}
			} catch(Throwable e) {
				System.out.println("Exception in JStackMonitorTimer.run: "+e.getMessage());
				e.printStackTrace();
				
				try {
					Thread.sleep( 2000l );
				} catch (InterruptedException e1) {
					e1.printStackTrace();
				}
			}
		}
    }
	
	@Override
	protected void finalize() throws Throwable {
		System.out.println("JStack monitor timer thread stopped");
		
		super.finalize();
	}
}
