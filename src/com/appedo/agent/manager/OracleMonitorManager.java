package com.appedo.agent.manager;

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
import com.appedo.agent.connect.OracleConnector;
import com.appedo.agent.utils.Constants;
import com.appedo.agent.utils.Constants.AGENT_TYPE;
import com.appedo.agent.utils.UtilsFactory;

import net.sf.json.JSONArray;
import net.sf.json.JSONObject;


public class OracleMonitorManager extends AgentManager {
	
	public static OracleMonitorManager OracleMonitorManager = null;
	
	private Connection con = null;
	
	double lCounterValue = 0l;
	
	/**
	 * Avoid the object creation for this Class from outside.
	 */
	private OracleMonitorManager() {
	}
	
	/**
	 * Returns the only object(singleton) of this Class.
	 * 
	 * @return
	 */
	public static OracleMonitorManager getOracleMonitorManager(){
		if( OracleMonitorManager == null ){
			OracleMonitorManager = new OracleMonitorManager();
		}
		
		return OracleMonitorManager;
	}
	
	/**
	 * Monitor the server and collect the counters
	 */
	public Date monitorOracleServer(String strGUID, String strDbName, Date dateSlowQueryLastReadOn){
		return getCounters(strGUID, strDbName, dateSlowQueryLastReadOn);
	}
	
	/**
	 * Monitor the Oracle database and collect the counters & slowQueries.
	 */
	public void monitorOracleServer(String strGUID, Date collectionDate) {
		monitorOracleCounters(strGUID, collectionDate);
	}
	
	/**
	 * Send it to the Collector WebService
	 */
	public void sendOracleCounters(String strGUID){
		sendCounters(strGUID);
	}
	
	/**
	 * Collect the counter with agent's types own logic or native methods
	 */
	public Date getCounters(String strGUID, String strDbName, Date dateSlowQueryLastReadOn) {
		int nCounterId = 0;		
		Statement stmt = null;
		ResultSet rst = null;
		Statement stmtSlowQry = null;
		ResultSet rstSlowQry = null;
		String query = null;
		// create variable's to capture execution_type & is_delta
		boolean bIsDelta = false, bCounterWorked = false;
		//String strExecutionType = "";

		StringBuilder sbSlowQuery = null;
		
		try{
			// reset the counter collector variable in AgentManager.
			resetCounterMap(strGUID);
			
			// Singleton Connection will be returned. If that is not exists or expired then, Recreate the connection.
			con = OracleConnector.getmyOracleConnector(strDbName).getConnetion(strDbName);
			
			stmt = con.createStatement();
			JSONArray joSelectedCounters = AgentCounterBean.getCountersBean(strGUID);
			//System.out.println("joSelectedCounters : " + joSelectedCounters.toString());
			
			for(int i=0; i<joSelectedCounters.size(); i++){
				lCounterValue = 0l;
				query = null;
				bCounterWorked = false;
				
				try {
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
					if ( bCounterWorked = rst.next() ) {
						
						lCounterValue = Double.parseDouble((rst.getString(1)));
						
						if(bIsDelta) {
							lCounterValue = addDeltaCounterValue(nCounterId, lCounterValue);
						} else {
							addCounterValue(nCounterId, lCounterValue);
						}
						// TODO: Static Counter correction required.
					}
				} catch(Throwable th) {
					System.out.println("Exception in getCounters: "+th.getMessage());
					th.printStackTrace();
					reportCounterError(nCounterId, th.getMessage());
				}
				
	            // Verify SLA Breach
				if( bCounterWorked ) {
					try {
						// JSONObject joSLACounter = null;
						ArrayList<JSONObject> joSLACounter = null; // Need to change variable name as alSLACounters
						joSLACounter = verifySLABreach(strGUID, SlaCounterBean.getSLACountersBean(strGUID), nCounterId, lCounterValue);
						
						// if breached then add it to Collector's collection
						if( joSLACounter != null ) {
							addSlaCounterValue(joSLACounter);
						}
					} catch(Throwable th) {
						System.out.println("Exception in getCounters.sla-operations: "+th.getMessage());
						th.printStackTrace();
					}
				}
			}
			
			
			// Get the slow queries
			JSONObject joSlowQry = new JSONObject();
			JSONObject joQry = null;
			JSONArray  jaSlowQry = new JSONArray();
			
			try {
				if( dateSlowQueryLastReadOn.getTime() <= (new Date().getTime() - Constants.SLOW_QUERY_READ_FREQUENCY_MILLISECONDS) ) {
					
					// Check whether Slow-Query fetching View is available
					if ( ! checkSqlAreaView(con) ) {
						throw new Exception("Oracle queries-history View : Does not have permission (or) not configured.");
					} else {
						// Update the time, to validate for next time
						dateSlowQueryLastReadOn = new Date();
						
						sbSlowQuery = new StringBuilder();
						stmtSlowQry = con.createStatement();
						
						// Use Slow-Query fetching query, if defined in config.properties.
						if( Constants.ORCL_SQ_QUERY != null && Constants.ORCL_SQ_QUERY.length() > 0 ) {
							sbSlowQuery.append( Constants.ORCL_SQ_QUERY );
						} else {
							// added prefix `QUERY_COMMENT_PREFIX`, since in slow query log, to avoid APPEDO queries used for monitoring
							sbSlowQuery	.append(Constants.QUERY_COMMENT_PREFIX)
										.append("SELECT * FROM ( ")
										.append("  SELECT sql_fulltext, round((a.elapsed_time/1000/a.executions),2) elapsedtime, a.executions ")
										.append("  FROM v$sqlarea a ")
										.append("  WHERE a.executions > 0 ");
							if( Constants.OMIT_AGENT_QUERIES_IN_SLOW_QUERIES ) {
								sbSlowQuery.append("  AND sql_fulltext NOT LIKE '").append(Constants.QUERY_COMMENT_PREFIX).append("%' ");
							} 
							sbSlowQuery	.append("  ORDER BY 2 DESC ")
										.append(") ")
										.append("WHERE rownum <= 10 ");
						}
						
						rstSlowQry = stmtSlowQry.executeQuery(sbSlowQuery.toString());
						while ( rstSlowQry.next() ){
							joQry = new JSONObject();					
							joQry.put("query", rstSlowQry.getString("SQL_FULLTEXT").replaceAll("'", ""));
							joQry.put("calls", rstSlowQry.getInt("EXECUTIONS"));
							joQry.put("duration_ms", rstSlowQry.getInt("ELAPSEDTIME"));
							
							jaSlowQry.add(joQry);
						}
						joSlowQry.put("1001", strGUID);	// guid
						joSlowQry.put("slowQueries", jaSlowQry.toString());	// all queries
						
						addSlowQryCounterValue(joSlowQry.toString());
					}
				}
			} catch(Throwable th) {
				System.out.println("Exception in getCounters.slow-query: "+th.getMessage());
				th.printStackTrace();
			}
		} catch(Throwable th) {
			System.out.println("Exception in monitorMyOracleServer: "+th.getMessage());
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
			
			OracleConnector.close(rst);
			rst = null;
			
			OracleConnector.close(stmtSlowQry);
			stmtSlowQry = null;
			
			OracleConnector.close(stmt);
			stmt = null;

			UtilsFactory.clearCollectionHieracy(sbSlowQuery);
			sbSlowQuery = null;
		}
		
		return dateSlowQueryLastReadOn;
	}
	
	private void monitorOracleCounters(String strGUID, Date collectionDate) {
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
			beanLinuxUnification.setMod_type("Oracle");
			beanLinuxUnification.setType("MetricSet");
			beanLinuxUnification.setGuid(strGUID);
			beanLinuxUnification.setdDateTime(collectionDate);
			
			jaSlaCounters = SlaCounterBean.getSLACountersBean(strGUID);
			
			if(jaSlaCounters != null && jaSlaCounters.size() > 0) {
				beanSLA = new LinuxUnificationBean();
			}
			
			con = OracleConnector.getmyOracleConnector("").getConnetion("");
			
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
					rst = stmt.executeQuery(query);
					
					if ( rst.next() ){
						lCounterValue = Double.parseDouble(rst.getString(1));
						
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
					beanSLA.setMod_type("Oracle");
					beanSLA.setType("SLASet");
					beanSLA.setGuid(strGUID);
					beanSLA.setdDateTime(collectionDate);
					
					LogManagerExtended.logJStackOutput("metrics###"+beanSLA.toString("SLASet"));
					LogManagerExtended.databaseInfoLog("metrics###"+beanSLA.toString("SLASet"));
				}
			}
			
			//collecting oracle slow queries
			if(validateSqlAreaView(con)) {
				sbSlowQuery = new StringBuilder();
				stmtSlowQry = con.createStatement();
				
				// Use Slow-Query fetching query, if defined in config.properties.
				if( Constants.ORCL_SQ_QUERY != null && Constants.ORCL_SQ_QUERY.length() > 0 ) {
					sbSlowQuery.append( Constants.ORCL_SQ_QUERY );
				}else {
					sbSlowQuery	.append(Constants.QUERY_COMMENT_PREFIX)
								.append("SELECT * FROM ( ")
								.append("  SELECT sql_fulltext, round((a.elapsed_time/1000/a.executions),2) elapsedtime, a.executions ")
								.append("  FROM v$sqlarea a ")
								.append("  WHERE a.executions > 0 ");
					if( Constants.OMIT_AGENT_QUERIES_IN_SLOW_QUERIES ) {
						sbSlowQuery.append("  AND sql_fulltext NOT LIKE '").append(Constants.QUERY_COMMENT_PREFIX).append("%' ");
					} 
					sbSlowQuery	.append("  ORDER BY 2 DESC ")
								.append(") ")
								.append("WHERE rownum <= 10 ");
				}
				
				rstSlowQry = stmtSlowQry.executeQuery(sbSlowQuery.toString());
				while ( rstSlowQry.next() ){
					beanSlowQuery = new LinuxUnificationSlowQueryBean();
					
					String result = rstSlowQry.getString("SQL_FULLTEXT").replaceAll("['\"]", "");
					result = result.replaceAll(System.lineSeparator(), "");
					
					beanSlowQuery.setQuery(result);
					beanSlowQuery.setCalls(rstSlowQry.getInt("EXECUTIONS"));
					beanSlowQuery.setDuration_ms(rstSlowQry.getInt("ELAPSEDTIME"));
					
					beanLinuxUnification.addSlowQueryEntry(beanSlowQuery);
				}
				
				if(beanLinuxUnification.isSlowQueriesAvailable()) {
					LogManagerExtended.logJStackOutput("metrics###"+beanLinuxUnification.toString("SlowQuerySet"));
					LogManagerExtended.databaseInfoLog("metrics###"+beanLinuxUnification.toString("SlowQuerySet"));
				}
			}else {
				LogManagerExtended.databaseInfoLog("Oracle queries-history View : Does not have permission (or) not configured.");
			}
		}catch (Exception e) {
			LogManagerExtended.databaseInfoLog("Exception in monitorOracleCounters : "+ e);
		}
		
	}
	
	public JSONObject getModuleInformation() throws Exception{
		JSONObject joModuleData = null;
		Statement stmt = null;
		ResultSet rst = null;
		try {
			this.con = OracleConnector.getmyOracleConnector("").getConnetion("");
			stmt = con.createStatement();
			
			rst = stmt.executeQuery("SELECT version FROM V$INSTANCE");
			
			if(rst.next()) {
				joModuleData = new JSONObject();
				joModuleData.put("moduleName", "Oracle-"+rst.getString("VERSION"));
				joModuleData.put("moduleTypeName", "Oracle");
				joModuleData.put("VERSION_ID", rst.getString("VERSION"));
			}
			
		}catch (Exception e) {
			LogManagerExtended.databaseInfoLog("Exception in getModuleInformation :"+e);
			throw e;
		}
		return joModuleData;
	}
	
	public boolean validateSqlAreaView(Connection con) {
		Statement stmtQry = null;
		ResultSet rstQry = null;
		boolean bReturn = false;
		
		String strQry = null;
		
		if( ! Constants.ORCL_SQ_DISABLE_DBAVIEW_VALIDATION) {
			
			try {
				/**
				 * user_views - All views owned by Current-User.
				 * all_views - All views accessible to the current user. Means, except "sys" user's views.
				 * dba_views - All views in the database, including "sys" user's views.
				 */
				// added prefix to avoid in slow query log
				strQry = Constants.QUERY_COMMENT_PREFIX+"SELECT count(*) as isExists FROM dba_views WHERE view_name = 'V_$SQLAREA'";
				
				stmtQry = con.createStatement();
				rstQry = stmtQry.executeQuery(strQry);
				if(rstQry.next()){
					bReturn = rstQry.getInt("isExists")>0;
				}
			} catch(Throwable th) {
				System.out.println("Exception in checkSqlAreaView :" + th.getMessage());
				th.printStackTrace();
				
				bReturn = false;
			} finally {
				OracleConnector.close(rstQry);
				rstQry = null;
				
				OracleConnector.close(stmtQry);
				stmtQry = null;
			}
		}
		
		return bReturn;
	}
	
	public boolean checkSqlAreaView(Connection con) {
		Statement stmtQry = null;
		ResultSet rstQry = null;
		boolean bReturn = false;
		
		String strQry = null;
		
		if( ! Constants.ORCL_SQ_DISABLE_DBAVIEW_VALIDATION ) {
			
			try {
				/**
				 * user_views - All views owned by Current-User.
				 * all_views - All views accessible to the current user. Means, except "sys" user's views.
				 * dba_views - All views in the database, including "sys" user's views.
				 */
				// added prefix to avoid in slow query log
				strQry = Constants.QUERY_COMMENT_PREFIX+"SELECT count(*) as isExists FROM dba_views WHERE view_name = 'V_$SQLAREA'";
				
				stmtQry = con.createStatement();
				rstQry = stmtQry.executeQuery(strQry);
				if(rstQry.next()){
					bReturn = rstQry.getInt("isExists")>0;
				}
			} catch(Throwable th) {
				System.out.println("Exception in checkSqlAreaView :" + th.getMessage());
				th.printStackTrace();
				
				bReturn = false;
			} finally {
				OracleConnector.close(rstQry);
				rstQry = null;
				
				OracleConnector.close(stmtQry);
				stmtQry = null;
			}
		} else {
			bReturn = true;
		}
		
		return bReturn;
	}
	
	/**
	 * Send the collected counter-sets to Collector WebService, by calling parent's sendCounter method
	 */
	public void sendCounters(String strGUID) {
		
		// send the collected counters to Collector WebService through parent sender function
		sendCounterToCollector(strGUID, AGENT_TYPE.ORACLE);
		sendSlowQryToCollector(strGUID,AGENT_TYPE.ORACLE);
		sendSlaCounterToCollector(strGUID,AGENT_TYPE.ORACLE);
	}
	
	@Override
	protected void finalize() throws Throwable {
		clearCounterMap();
		
		super.finalize();
	}
}

