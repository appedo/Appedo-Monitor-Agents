package com.appedo.agent.manager;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Date;

import com.appedo.agent.bean.AgentCounterBean;
import com.appedo.agent.bean.LinuxUnificationBean;
import com.appedo.agent.bean.LinuxUnificationCounterBean;
import com.appedo.agent.bean.LinuxUnificationSlowQueryBean;
import com.appedo.agent.bean.SlaCounterBean;
import com.appedo.agent.connect.PostgresSQLConnector;
import com.appedo.agent.utils.Constants;
import com.appedo.agent.utils.Constants.AGENT_TYPE;
import com.appedo.agent.utils.UtilsFactory;

import net.sf.json.JSONArray;
import net.sf.json.JSONObject;

/**
 * Postgres monitoring class. This has the functionalities to get the counter values of Linux OS.
 * 
 * @author veeru
 *
 */
public class PostgresSQLMonitorManager extends AgentManager {
	
	public static PostgresSQLMonitorManager pgMonitorManager = null;
	
	private Connection con = null;
	
	long lCounterValue = 0l;
	
	
	/**
	 * Avoid the object creation for this Class from outside.
	 */
	private PostgresSQLMonitorManager() {
	}
	
	/**
	 * Returns the only object(singleton) of this Class.
	 * 
	 * @return
	 */
	public static PostgresSQLMonitorManager getPGMonitorManager(){
		if( pgMonitorManager == null ){
			pgMonitorManager = new PostgresSQLMonitorManager();
		}
			
		return pgMonitorManager;
	}
	
	/**
	 * Monitor the server and collect the counters
	 */
	public void monitorPGServer(String strGUID, String strDbName){
		getCounters(strGUID,strDbName);
	}
	
	/**
	 * Monitor the server and collect the counters
	 */
	public void monitorLinuxUnifiedPGServer(String strGUID, String strDbName, Date collectionDate){
		//getCounters(strGUID,strDbName);
		monitorPGCounters(strGUID, strDbName, collectionDate);
	}
	
	/**
	 * Send it to the Collector WebService
	 */
	public void sendPGCounters(String strGUID){
		sendCounters(strGUID);
	}
	
	/**
	 * Collect the counter with agent's types own logic or native methods
	 * 
	 */
	private void monitorPGCounters(String strGUID, String strDbName, Date collectionDate) {
		
		JSONArray jaSelectedCounters = null, jaSlaCounters = null;
		JSONObject joSelectedCounter = null;
		
		LinuxUnificationBean beanLinuxUnification =null;
		LinuxUnificationBean beanSLA = null;
		LinuxUnificationCounterBean beanLinuxCounters = null;
		LinuxUnificationSlowQueryBean beanSlowQuery = null;
		
		Statement stmt = null;
		Statement stmtSlowQry = null;
		ResultSet rst = null;
		ResultSet rstSlowQry = null;
		String query = null, strExecutionType = null;
		int nCounterId;
		boolean bIsDelta = false;
		StringBuilder sbSlowQuery = null;
		
		try {
			jaSelectedCounters = AgentCounterBean.getCountersBean(strGUID);
			
			beanLinuxUnification = new LinuxUnificationBean();
			beanLinuxUnification.setMod_type("PostgreSQL");
			beanLinuxUnification.setType("MetricSet");
			beanLinuxUnification.setGuid(strGUID);
			beanLinuxUnification.setdDateTime(collectionDate);
			
			jaSlaCounters = SlaCounterBean.getSLACountersBean(strGUID);
			
			if(jaSlaCounters != null && jaSlaCounters.size() > 0) {
				beanSLA = new LinuxUnificationBean();
			}
			// Singleton Connection will be returned. If that is not exists or expired then, Recreate the connection.
			con = PostgresSQLConnector.getMyPGConnector(strDbName).getConnetion(strDbName);
						
			stmt = con.createStatement();
			
			for (int i = 0; i < jaSelectedCounters.size(); i++) {
				joSelectedCounter = jaSelectedCounters.getJSONObject(i);
				nCounterId = Integer.parseInt(joSelectedCounter.getString("counter_id"));
				query = Constants.QUERY_COMMENT_PREFIX+joSelectedCounter.getString("query");
				bIsDelta = joSelectedCounter.getBoolean("isdelta");
				strExecutionType = joSelectedCounter.getString("executiontype");
				beanLinuxUnification.addNewCounter(joSelectedCounter.getString("counter_id"));
				lCounterValue = 0l;
				
				if ( strExecutionType.equalsIgnoreCase("query") ) {
					
					if(query.contains("#DB_NAME#")) {
						query = query.replace("#DB_NAME#", strDbName);
					}
					rst = stmt.executeQuery(query);
					
					if ( rst.next() ){
						lCounterValue = Long.parseLong(rst.getString(1));
						
						if(bIsDelta) {
							lCounterValue = addDeltaCounterValue_v1(nCounterId, lCounterValue);
						}
						
						// Create Bean for the LinuxUnification OS Module Counter entry(line)
						beanLinuxCounters = new LinuxUnificationCounterBean();
						beanLinuxCounters.setCounter_type(nCounterId);
						beanLinuxCounters.setException("");
						beanLinuxCounters.setProcess_name("");
						beanLinuxCounters.setCounter_value(lCounterValue);
						beanLinuxUnification.addCounterEntry(String.valueOf(nCounterId), beanLinuxCounters);
						if(SlaCounterBean.getSLACountersBean(strGUID) != null && SlaCounterBean.getSLACountersBean(strGUID).size() > 0) {
							verifySLABreach_v1(jaSlaCounters, nCounterId, lCounterValue, beanSLA);
						}
					}
				}
			}
			
			if(beanLinuxUnification.isCountersValueAvailable()) {
				LogManagerExtended.logJStackOutput("metrics###"+beanLinuxUnification.toString("MetricSet"));
				LogManagerExtended.databaseInfoLog("metrics###"+beanLinuxUnification.toString("MetricSet"));
			}
			
			if(beanSLA != null) {
				if(beanSLA.isSLACountersValueAvailable()) {
					beanSLA.setMod_type("PostgreSQL");
					beanSLA.setType("SLASet");
					beanSLA.setGuid(strGUID);
					beanSLA.setdDateTime(collectionDate);
					
					LogManagerExtended.logJStackOutput("metrics###"+beanSLA.toString("SLASet"));
					LogManagerExtended.databaseInfoLog("metrics###"+beanSLA.toString("SLASet"));
				}
			}
			
			//collect SlowQuerySet 
			if(checkPgStatTable(con)) {
				sbSlowQuery = new StringBuilder();
				stmtSlowQry = con.createStatement();
				
				//added NOT LIKE `QUERY_COMMENT_PREFIX`, since in slow query log, to avoid APPEDO queries used for monitoring
				sbSlowQuery	.append(Constants.QUERY_COMMENT_PREFIX)
							.append("select query, calls, total_time as duration_ms ")
							.append("from pg_stat_statements ")
							.append("where dbid= (select datid from pg_stat_database where datname='").append(strDbName).append("') ")
							.append("  AND query NOT LIKE '").append(Constants.QUERY_COMMENT_PREFIX).append("%' ")
							.append("order by  total_time desc limit 10 ");
				
				rstSlowQry = stmtSlowQry.executeQuery(sbSlowQuery.toString());
				while ( rstSlowQry.next() ){
					beanSlowQuery = new LinuxUnificationSlowQueryBean();
					
					String result = rstSlowQry.getString("query").replaceAll("['\"]", "");
					result = result.replaceAll(System.lineSeparator(), "");
					
					beanSlowQuery.setQuery(result);
					//beanSlowQuery.setQuery(rstSlowQry.getString("query").replaceAll("['\"]", ""));
					beanSlowQuery.setCalls(rstSlowQry.getInt("calls"));
					beanSlowQuery.setDuration_ms(rstSlowQry.getInt("duration_ms"));
					
					beanLinuxUnification.addSlowQueryEntry(beanSlowQuery);
				}
				
				if(beanLinuxUnification.isSlowQueriesAvailable()) {
					LogManagerExtended.logJStackOutput("metrics###"+beanLinuxUnification.toString("SlowQuerySet"));
					LogManagerExtended.databaseInfoLog("metrics###"+beanLinuxUnification.toString("SlowQuerySet"));
				}
			}else {
				LogManagerExtended.databaseInfoLog("pg_stat_statements Table is not exists.");
			}
		}catch (Exception e) {
			LogManagerExtended.databaseInfoLog("Exception in monitorPGCounters : "+ e);
		}finally {
			PostgresSQLConnector.close(rst);
			rst = null;
			PostgresSQLConnector.close(stmt);
			stmt = null;
			
			UtilsFactory.clearCollectionHieracy(sbSlowQuery);
			sbSlowQuery = null;
		}
	}
	
	/**
	 * Collect the counter with agent's types own logic or native methods
	 * 
	 */
	public void getCounters(String strGUID,String strDbName){
		
		int nCounterId ;		
		Statement stmt = null;
		ResultSet rst = null;
		Statement stmtSlowQry = null;
		ResultSet rstSlowQry = null;
		String query = null;
		// create variable's to capture execution_type & is_delta
		boolean bIsDelta = false;
		//String strExecutionType = "";

		StringBuilder sbSlowQuery = null;
		
		try{
			// reset the counter collector variable in AgentManager.
			resetCounterMap(strGUID);
			
			// Singleton Connection will be returned. If that is not exists or expired then, Recreate the connection.
			con = PostgresSQLConnector.getMyPGConnector(strDbName).getConnetion(strDbName);
			
			stmt = con.createStatement();
			JSONArray joSelectedCounters = AgentCounterBean.getCountersBean(strGUID);
			//System.out.println("joSelectedCounters : " + joSelectedCounters.toString());
			
			for(int i=0; i<joSelectedCounters.size(); i++){
				lCounterValue = 0l;
				query = null;
				
				JSONObject joSelectedCounter = joSelectedCounters.getJSONObject(i);				
				nCounterId = Integer.parseInt(joSelectedCounter.getString("counter_id"));
				// added prefix `QUERY_COMMENT_PREFIX`, since in slow query log, to avoid APPEDO queries used for monitoring
				query = Constants.QUERY_COMMENT_PREFIX+joSelectedCounter.getString("query");
				bIsDelta = joSelectedCounter.getBoolean("isdelta");
				//strExecutionType = joSelectedCounter.getString("executiontype");
				
				// get execution type & is_delta
				
				if(query.contains("#DB_NAME#")) {
					query = query.replace("#DB_NAME#", strDbName);
				}
				
				rst = stmt.executeQuery(query);
				while ( rst.next() ){
					
					lCounterValue = Long.parseLong(rst.getString(1));
					
					if(bIsDelta) {
						lCounterValue = addDeltaCounterValue(nCounterId, lCounterValue);
					}else {
						addCounterValue(nCounterId, lCounterValue);
					}
					// TODO: Static Counter correction required.

	            	// Verify SLA Breach
					// JSONObject joSLACounter = null;
					ArrayList<JSONObject> joSLACounter = null; // Need to change variable name as alSLACounters
					joSLACounter = verifySLABreach(strGUID, SlaCounterBean.getSLACountersBean(strGUID), nCounterId, lCounterValue);
					
					// if breached then add it to Collector's collection
					if( joSLACounter != null ) {
						addSlaCounterValue(joSLACounter);
					}
				}
			}
			
			// Get Slow Queries
			JSONObject joSlowQry = new JSONObject();
			JSONObject joQry = null;
			JSONArray  jaSlowQry = new JSONArray();
			long lDbId = -1l;
			
			if(checkPgStatTable(con)) {
				lDbId = getDBId(con,strDbName);
				
				sbSlowQuery = new StringBuilder();
				stmtSlowQry = con.createStatement();
				//query = "SELECT array_to_json(array_agg(row_to_json(t)))  FROM ( select replace(query, E'\'', '') as query, calls, total_time as duration_ms from pg_stat_statements where dbid = (select datid from pg_stat_database where datid="+lDbId+")  order by  total_time   ) t  ";
				
				// added NOT LIKE `QUERY_COMMENT_PREFIX`, since in slow query log, to avoid APPEDO queries used for monitoring
				sbSlowQuery	.append(Constants.QUERY_COMMENT_PREFIX)
							.append("select query, calls, total_time as duration_ms ")
							.append("from pg_stat_statements ")
							.append("where dbid= (select datid from pg_stat_database where datname='").append(strDbName).append("') ")
							.append("  AND query NOT LIKE '").append(Constants.QUERY_COMMENT_PREFIX).append("%' ")
							.append("order by  total_time desc limit 10 ");
				//myString.replaceAll("'", "\\'");
				//System.out.println(query);
				rstSlowQry = stmtSlowQry.executeQuery(sbSlowQuery.toString());
				while ( rstSlowQry.next() ){
					joQry = new JSONObject();
					joQry.put("query", rstSlowQry.getString("query").replaceAll("['\"]", ""));
					joQry.put("calls", rstSlowQry.getInt("calls"));
					joQry.put("duration_ms", rstSlowQry.getInt("duration_ms"));
					
//					Package aPackage = Constants.class.getPackage();
					//joSlowQry.put("1001", strGUID);// guid
//					joSlowQry.put("1002", UtilsFactory.nowFormattedDate());// guid
//					joSlowQry.put("1004", aPackage.getSpecificationVersion()+ "-" +aPackage.getImplementationVersion()); // agent version
					//joSlowQry.put("slowQueries", rstSlowQry.getString("array_to_json"));// all queries
					
					jaSlowQry.add(joQry);
				}
				joSlowQry.put("1001", strGUID);// guid
				joSlowQry.put("slowQueries", jaSlowQry.toString());// all queries
				addSlowQryCounterValue(joSlowQry.toString());
				
			}else {
				System.out.println("Table not exist");
			}
			
			// Check the talbe			
					
			//addSlowQryCounterValue(joSlowQry);
		} catch(Throwable th) {
			System.out.println("Exception in monitorMyPGServer: "+th.getMessage());
			th.printStackTrace();
			reportGlobalError(th.getMessage());
		} finally {
			// queue the counter
			try {
				queueCounterValues();
			} catch (Exception e) {
				System.out.println("Exception in queueCounterValues(): "+e.getMessage());
				e.printStackTrace();
			}
			PostgresSQLConnector.close(rst);
			rst = null;
			PostgresSQLConnector.close(stmt);
			stmt = null;
			
			UtilsFactory.clearCollectionHieracy(sbSlowQuery);
			sbSlowQuery = null;
		}
		
	}
	
	/**
	 * to check pg_stat_statements table 
	 * @param con
	 * @return
	 */
	public boolean checkPgStatTable(Connection con) {
		Statement stmtQry = null;
		// added prefix since to avoid in slow query log
		String strQry = Constants.QUERY_COMMENT_PREFIX+"SELECT EXISTS (SELECT relname FROM pg_class WHERE relname='pg_stat_statements') as relname";
		ResultSet rstQry = null;
		boolean bReturn = false;
		
		try {
			stmtQry = con.createStatement();
			rstQry = stmtQry.executeQuery(strQry);
			while ( rstQry.next() ){
				bReturn = rstQry.getBoolean("relname");
			}
		}catch(Exception e) {
			System.out.println("Exception in checkPgStatTable :" + e.getMessage());
		}finally {
			PostgresSQLConnector.close(rstQry);
			rstQry = null;
			PostgresSQLConnector.close(stmtQry);
			stmtQry = null;
		}
		return bReturn;
	}
	
	
	/**
	 * to check pg_stat_statements table 
	 * @param con
	 * @return
	 */
	public Long getDBId(Connection con, String strDbName) {
		Statement stmtQry = null;
		// added prefix since to avoid in slow query log
		String strQry = Constants.QUERY_COMMENT_PREFIX+"select datid from pg_stat_database where datname='"+strDbName+"'";
		ResultSet rstQry = null;
		Long bReturn = -1l;
		
		try {
			stmtQry = con.createStatement();
			rstQry = stmtQry.executeQuery(strQry);
			while ( rstQry.next() ){
				bReturn = rstQry.getLong("datid");
			}
		}catch(Exception e) {
			System.out.println("Exception in checkPgStatTable :" + e.getMessage());
		}finally {
			PostgresSQLConnector.close(rstQry);
			rstQry = null;
			PostgresSQLConnector.close(stmtQry);
			stmtQry = null;
		}
		return bReturn;
	}
	
	/**
	 * Send the collected counter-sets to Collector WebService, by calling parent's sendCounter method
	 */
	public void sendCounters(String strGUID) {
		
		// send the collected counters to Collector WebService through parent sender function
		sendCounterToCollector(strGUID, AGENT_TYPE.POSTGRES);
		sendSlowQryToCollector(strGUID,AGENT_TYPE.POSTGRES);
		sendSlaCounterToCollector(strGUID,AGENT_TYPE.POSTGRES);
	}
	
	
	private String getErrorString(InputStream errorStream) {
		InputStreamReader isrError = null;
		BufferedReader rError = null;
		String line = null;
		StringBuilder sbError = new StringBuilder();
		
		try{
			isrError = new InputStreamReader(errorStream);
			rError = new BufferedReader(isrError);
			sbError.setLength(0);
			while ((line = rError.readLine()) != null) {
				sbError.append(line).append(" ");
			}
			if( sbError.length() > 0 ){
				sbError.deleteCharAt(sbError.length()-1);
				
				System.out.println("sbError in CPU: "+sbError);
			}
		} catch ( Exception e ) {
			System.out.println("Exception in getErrorString: "+e.getMessage());
			e.printStackTrace();
		} finally {
			try{
				isrError.close();
			} catch(Exception e) {
				System.out.println("Exception in isrError.close(): "+e.getMessage());
				e.printStackTrace();
			}
			isrError = null;
			try{
				rError.close();
			} catch(Exception e) {
				System.out.println("Exception in rError.destroy(): "+e.getMessage());
				e.printStackTrace();
			}
			rError = null;
		}
		
		return sbError.toString();
	}
	
	@Override
	protected void finalize() throws Throwable {
		clearCounterMap();
		
		super.finalize();
	}
}
