package com.appedo.agent.manager;


import java.io.IOException;
import java.net.MalformedURLException;
import java.util.HashMap;
import java.util.Hashtable;

import javax.management.MBeanServerConnection;
import javax.management.MalformedObjectNameException;
import javax.management.ObjectName;
import javax.management.remote.JMXConnector;
import javax.management.remote.JMXConnectorFactory;
import javax.management.remote.JMXServiceURL;
import javax.naming.Context;

public class CompleteWebLogicMonitoring {

	private static MBeanServerConnection connection;
	private static JMXConnector connector;
	private static final ObjectName service;
	static String ServerName ="AdminServer";
	static String JDBCServerName ="examples-demo";
	static String wlUname= "weblogic";
	static String wlPassword="appedo@123";
	static String wlPort="7001";
	static String wlhostname="localhost";

	static {
		try {
			//com.bea:Name=RuntimeService,Type=weblogic.management.mbeanservers.runtime.RuntimeServiceMBean
			service = new ObjectName("com.bea:Name=DomainRuntimeService,Type=weblogic.management.mbeanservers.domainruntime.DomainRuntimeServiceMBean");
			//service = new ObjectName("com.bea:Name=RuntimeService,Type=weblogic.management.mbeanservers.runtime.RuntimeServiceMBean");

		}catch (MalformedObjectNameException e) {
			throw new AssertionError(e.getMessage());
		}
	}
	
	public static void initConnection(String hostname, String portString, String username, String password) throws IOException, MalformedURLException {
		String protocol = "t3";
		Integer portInteger = Integer.valueOf(portString);
		int port = portInteger.intValue();
		String jndiroot = "/jndi/";
		String mserver = "weblogic.management.mbeanservers.domainruntime"; 
		//String mserver = "weblogic.management.mbeanservers.runtime";
		JMXServiceURL serviceURL = new JMXServiceURL(protocol, hostname,port, jndiroot + mserver);
		Hashtable h = new Hashtable();
		h.put(Context.SECURITY_PRINCIPAL, username);
		h.put(Context.SECURITY_CREDENTIALS, password);
		h.put(JMXConnectorFactory.PROTOCOL_PROVIDER_PACKAGES,"weblogic.management.remote");
		connector = JMXConnectorFactory.connect(serviceURL, h);
		connection = connector.getMBeanServerConnection();
	}

	public static ObjectName[] getServerRuntimes() throws Exception {
		System.out.println("Service:"+service);
		return (ObjectName[]) connection.getAttribute(service,"ServerRuntimes");
	}
	
	public static void main(String args[]) throws Exception {
		String hostname = wlhostname;
		String portString = wlPort;
		String username = wlUname;
		String password = wlPassword;
		initConnection(hostname, portString, username, password);
		HashMap<String, String> CounterMap = new HashMap<String, String>(0);
		ObjectName[] serverRT = getServerRuntimes();
		int length = (int) serverRT.length;
		for (int i = 0; i < length; i++) {
			String name = (String) connection.getAttribute(serverRT[i],"Name");
			if(name.equalsIgnoreCase(ServerName))
			{
				//Get Thread Pool Counters
				ObjectName threadRT =  (ObjectName) connection.getAttribute(serverRT[i],"ThreadPoolRuntime");
				CounterMap.put("ThreadPool::Completed Request Count", connection.getAttribute(threadRT, "CompletedRequestCount").toString());
				CounterMap.put("ThreadPool::Execute Thread Total Count",connection.getAttribute(threadRT, "ExecuteThreadTotalCount").toString());
				CounterMap.put("ThreadPool::Execute Thread Idle Count",connection.getAttribute(threadRT, "ExecuteThreadIdleCount").toString());
				CounterMap.put("ThreadPool::Hogging Thread Count",connection.getAttribute(threadRT, "HoggingThreadCount").toString());
				CounterMap.put("ThreadPool::Pending User Request Count",connection.getAttribute(threadRT, "PendingUserRequestCount").toString());
				CounterMap.put("ThreadPool::Queue Length",connection.getAttribute(threadRT, "QueueLength").toString());
				CounterMap.put("ThreadPool::Shared Capacity For WorkManagers",connection.getAttribute(threadRT, "SharedCapacityForWorkManagers").toString());
				CounterMap.put("ThreadPool::Standby Thread Count",connection.getAttribute(threadRT, "StandbyThreadCount").toString());
				CounterMap.put("ThreadPool::Throughput" , connection.getAttribute(threadRT, "Throughput").toString());

				//Get JVM Runtime Counters
				ObjectName jvmRT =  (ObjectName) connection.getAttribute(serverRT[i],"JVMRuntime");
				CounterMap.put("JVMRuntime::Heap Free Current",connection.getAttribute(jvmRT, "HeapFreeCurrent").toString());
				CounterMap.put("JVMRuntime::Heap Free Percent",connection.getAttribute(jvmRT, "HeapFreePercent").toString());
				CounterMap.put("JVMRuntime::Heap Size Current",connection.getAttribute(jvmRT, "HeapSizeCurrent").toString());
				CounterMap.put("JVMRuntime::Heap Size Max",connection.getAttribute(jvmRT, "HeapSizeMax").toString());
				CounterMap.put("JVMRuntime::Uptime",connection.getAttribute(jvmRT, "Uptime").toString());


				//Get JMS Runtime Counters
				ObjectName jmsRuntime= (ObjectName) connection.getAttribute(serverRT[i],"JMSRuntime");
				CounterMap.put("JMSRuntime::Connections Current Count",connection.getAttribute(jmsRuntime, "ConnectionsCurrentCount").toString());
				CounterMap.put("JMSRuntime::Connections High Count",connection.getAttribute(jmsRuntime, "ConnectionsHighCount").toString());
				CounterMap.put("JMSRuntime::Connections Total Count",connection.getAttribute(jmsRuntime, "ConnectionsTotalCount").toString());
				CounterMap.put("JMSRuntime::JMSServers Current Count",connection.getAttribute(jmsRuntime, "JMSServersCurrentCount").toString());
				CounterMap.put("JMSRuntime::JMSServers High Count",connection.getAttribute(jmsRuntime, "JMSServersHighCount").toString());
				CounterMap.put("JMSRuntime::JMSServers Total Count",connection.getAttribute(jmsRuntime, "JMSServersTotalCount").toString());


				//Get JDBC Runtime Counters
				ObjectName[] appRT =(ObjectName[]) connection.getAttribute(new ObjectName("com.bea:Name="+name+",ServerRuntime="+name+",Location="+name+",Type=JDBCServiceRuntime"),"JDBCDataSourceRuntimeMBeans");
				int appLength = (int) appRT.length;
				for (int x = 0; x < appLength; x++) {
					String JDBCName =(String)connection.getAttribute(appRT[x], "Name");
					if(JDBCName.equalsIgnoreCase(JDBCServerName))
					{
						CounterMap.put("JDBCRuntime::Active Connections Current Count",connection.getAttribute(appRT[x], "ActiveConnectionsCurrentCount").toString());
						CounterMap.put("JDBCRuntime::Active Connections Average Count",connection.getAttribute(appRT[x], "ActiveConnectionsAverageCount").toString());
						CounterMap.put("JDBCRuntime::Active Connections Average Count",connection.getAttribute(appRT[x], "ActiveConnectionsAverageCount").toString());
						CounterMap.put("JDBCRuntime::Connections Total Count",connection.getAttribute(appRT[x], "ConnectionsTotalCount").toString());
						CounterMap.put("JDBCRuntime::CurrCapacity",connection.getAttribute(appRT[x], "CurrCapacity").toString());
						CounterMap.put("JDBCRuntime::CurrCapacity High Count",connection.getAttribute(appRT[x], "CurrCapacityHighCount").toString());
						CounterMap.put("JDBCRuntime::HighestNum Available",connection.getAttribute(appRT[x], "HighestNumAvailable").toString());
						CounterMap.put("JDBCRuntime::HighestNum Available",connection.getAttribute(appRT[x], "HighestNumAvailable").toString());
						CounterMap.put("JDBCRuntime::Leaked Connection Count",connection.getAttribute(appRT[x], "LeakedConnectionCount").toString());
						CounterMap.put("JDBCRuntime::Wait Seconds High Count", connection.getAttribute(appRT[x], "WaitSecondsHighCount").toString());
						CounterMap.put("JDBCRuntime::Waiting For Connection Current Count",connection.getAttribute(appRT[x], "WaitingForConnectionCurrentCount").toString());
						CounterMap.put("JDBCRuntime::Waiting For Connection Failure Total",connection.getAttribute(appRT[x], "WaitingForConnectionFailureTotal").toString());
						CounterMap.put("JDBCRuntime::Waiting For Connection Total",connection.getAttribute(appRT[x], "WaitingForConnectionTotal").toString());
						CounterMap.put("JDBCRuntime::Waiting For Connection High Count",connection.getAttribute(appRT[x], "WaitingForConnectionHighCount").toString());
					}
				}

				
				//Get JTA Runtime counters
				ObjectName jtaRuntime= (ObjectName) connection.getAttribute(serverRT[i],"JTARuntime");
				CounterMap.put("JTARuntime::Transaction Total Count",connection.getAttribute(jtaRuntime,"TransactionTotalCount").toString());
				CounterMap.put("JTARuntime::Transactions Committed Total Count", connection.getAttribute(jtaRuntime,"TransactionCommittedTotalCount").toString());
				CounterMap.put("JTARuntime::HTransaction Rolled Back Total Count", connection.getAttribute(jtaRuntime,"TransactionRolledBackTotalCount").toString());
				CounterMap.put("JTARuntime::Active Transactions Total Count", connection.getAttribute(jtaRuntime,"ActiveTransactionsTotalCount").toString());
				CounterMap.put("JTARuntime::Transaction RolledBack Timeout Total Count", connection.getAttribute(jtaRuntime,"TransactionRolledBackTimeoutTotalCount").toString());
				CounterMap.put("JTARuntime::Transaction RolledBack Resource Total Count", connection.getAttribute(jtaRuntime,"TransactionRolledBackResourceTotalCount").toString());
				CounterMap.put("JTARuntime::Transactions Rolled Back for Application Errors Total Count", connection.getAttribute(jtaRuntime,"TransactionRolledBackAppTotalCount").toString());
				CounterMap.put("JTARuntime::Transaction RolledBack System Total Count", connection.getAttribute(jtaRuntime,"TransactionRolledBackSystemTotalCount").toString());
				CounterMap.put("JTARuntime::Transaction Abandoned Total Count", connection.getAttribute(jtaRuntime,"TransactionAbandonedTotalCount").toString());
				CounterMap.put("JTARuntime::Transaction Heuristics Total Count", connection.getAttribute(jtaRuntime,"TransactionHeuristicsTotalCount").toString());
				CounterMap.put("JTARuntime::Transaction No Resources Committed Total Count", connection.getAttribute(jtaRuntime,"TransactionNoResourcesCommittedTotalCount").toString());
				CounterMap.put("JTARuntime::Transaction One Resource One Phase Committed Total Count",connection.getAttribute(jtaRuntime,"TransactionOneResourceOnePhaseCommittedTotalCount").toString().toString());

				connector.close();
				System.out.println(CounterMap);
				
				//System.out.println(CounterMap.size());
				
			}
		}
	}
}

