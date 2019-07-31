package com.appedo.agent.manager;

import javax.management.MBeanServerConnection;
import javax.management.ObjectName;
import javax.management.openmbean.CompositeData;
import javax.management.remote.JMXConnector;
import javax.management.remote.JMXConnectorFactory;
import javax.management.remote.JMXServiceURL;

public class TomcatMonitor {
	
	public static void main(String[] args){
		
		String strApplicationName = "QAMonitor";
		
		try{
			// Note: for NTFS, give access rights to "C:\Users\ram\AppData\Local\Temp\hsperfdata_<userName>"
			
			JMXServiceURL url = new JMXServiceURL("service:jmx:rmi:///jndi/rmi://localhost:9004/jmxrmi");
			JMXConnector jmxc = JMXConnectorFactory.connect(url);
			
			MBeanServerConnection conn = jmxc.getMBeanServerConnection(); 
			
			
			// For Tomcat version
			System.out.println("Tomcat version");
			System.out.println("~~~~~~");
			ObjectName server = new ObjectName("Catalina:type=Server");
			String serverInfo = conn.getAttribute(server, "serverInfo").toString();
			
			
			//For Cache
			System.out.println("Cache");
			System.out.println("~~~~~~");
			ObjectName Cache = null;
			if( serverInfo.startsWith("Apache Tomcat/7.") ) {
				Cache = new ObjectName("Catalina:type=Cache,host=localhost,context=/"+strApplicationName);
			} else if ( serverInfo.startsWith("Apache Tomcat/6.") ) {
				Cache = new ObjectName("Catalina:type=Cache,host=localhost,path=/"+strApplicationName);
			}
			String hitsCount = conn.getAttribute(Cache, "hitsCount").toString();
			System.out.println("Hits Count = "+hitsCount);
			System.out.println();
			
			
			//For Sessions
			System.out.println("Sessions");
			System.out.println("~~~~~~~~");
			ObjectName Sessions = null;
			if( serverInfo.startsWith("Apache Tomcat/7.") ) {
				Sessions = new ObjectName("Catalina:type=Manager,host=localhost,context=/"+strApplicationName);
			} else if ( serverInfo.startsWith("Apache Tomcat/6.") ) {
				Sessions = new ObjectName("Catalina:type=Manager,host=localhost,path=/"+strApplicationName);
			}
			String activeSessions=conn.getAttribute(Sessions,"activeSessions").toString();
			String rejectedSessions=conn.getAttribute(Sessions,"rejectedSessions").toString();
			String expiredSessions=conn.getAttribute(Sessions,"expiredSessions").toString();
			System.out.println("active Sessions = "+activeSessions);
			System.out.println("rejectedSessions = "+rejectedSessions);
			System.out.println("expiredSessions = "+expiredSessions);
			System.out.println();
			
			
			//For Request Processor
			System.out.println("Request Processor");
			System.out.println("~~~~~~~~");
			ObjectName rqpr = null;
			if( serverInfo.startsWith("Apache Tomcat/7.") ) {
				rqpr = new ObjectName("Catalina:type=GlobalRequestProcessor,name=\"http-bio-8080\"");
			} else if ( serverInfo.startsWith("Apache Tomcat/6.") ) {
				rqpr = new ObjectName("Catalina:type=GlobalRequestProcessor,name=http-8080");
			}
			String requestCount=conn.getAttribute(rqpr,"requestCount").toString();
			String errorCount=conn.getAttribute(rqpr,"errorCount").toString();
			String bytesSent=conn.getAttribute(rqpr,"bytesSent").toString();
			System.out.println("requestCount = "+requestCount);
			System.out.println("errorCount = "+errorCount);
			System.out.println("Bytes Sent = "+bytesSent);
			System.out.println();
			//% error count
			
			/*ObjectName rqpr2 = new ObjectName("Catalina:type=DataSource,path=/QAMonitor,host=localhost,class=javax.sql.DataSource,name=/QAMonitor");
			String requestCount1=conn.getAttribute(rqpr2,"numIdle").toString();
			System.out.println(requestCount1);*/
			
			
			// For ThreadPool
			System.out.println("Thread Pool");
			System.out.println("~~~~~~~~");
			ObjectName threadPool = null;
			if( serverInfo.startsWith("Apache Tomcat/7.") ) {
				threadPool = new ObjectName("Catalina:type=ThreadPool,name=\"http-bio-8080\"");
			} else if ( serverInfo.startsWith("Apache Tomcat/6.") ) {
				threadPool = new ObjectName("Catalina:type=ThreadPool,name=http-8080");
			}
			String currentThreadsBusy=conn.getAttribute(threadPool,"currentThreadsBusy").toString();
			String currentThreadCount=conn.getAttribute(threadPool,"currentThreadCount").toString();
			System.out.println("currentThreadsBusy = "+currentThreadsBusy);
			System.out.println("currentThreadCount = "+currentThreadCount);
			System.out.println();
			
			
			// For HeapMemoryUsage
			System.out.println("JVM Heap Memory");
			System.out.println("~~~~~~~~");
			// Tomcat 6: same
			// Tomcat 7: 
			ObjectName Memory = new ObjectName("java.lang:type=Memory");
			CompositeData composite = (CompositeData) conn.getAttribute(Memory, "HeapMemoryUsage");
			String ComittedMem = composite.get("committed").toString();
			String MaxMem = composite.get("max").toString();
			String Used = composite.get("used").toString();
			System.out.println("Commited Memory in bytes = "+ComittedMem);
			System.out.println("Maximum Memory in bytes = "+MaxMem);
			System.out.println("Used Memeory in bytes = "+Used);
			Float pused =Float.parseFloat(Used)*100/Float.parseFloat(MaxMem);
			System.out.println("Percentage Used Memory = "+pused+"%");
			System.out.println();
			
			
			
		} catch (Exception e) {
			e.printStackTrace();
		}  
	}
}
