package com.appedo.agent.manager;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;

import net.sf.json.JSONArray;
import net.sf.json.JSONObject;

import com.appedo.agent.bean.AgentCounterBean;
import com.appedo.agent.bean.SlaCounterBean;
import com.appedo.agent.connect.MySQLConnector;
import com.appedo.agent.utils.Constants;
import com.appedo.agent.utils.UtilsFactory;
import com.appedo.agent.utils.Constants.AGENT_TYPE;

/**
 * MySQL Database server monitoring class. This has the functionalities to get the counter values of MySQL Database Server.
 * 
 * @author veeru
 *  *
 */
public class MySQLMonitorManager extends AgentManager {
	
	private static MySQLMonitorManager mySQLMonitorManager = null;
	
	private String MYSQL_VERSION = null;
	
	private Connection con = null;
	
	long lCounterValue = 0l;
	
	
	/**
	 * Avoid the object creation for this Class from outside.
	 */
	private MySQLMonitorManager() {		

	}
	
	/**
	 * Returns the only object(singleton) of this Class.
	 * 
	 * @return
	 */
	public static MySQLMonitorManager getMySQLMonitorManager(){
		if( mySQLMonitorManager == null ){
			mySQLMonitorManager = new MySQLMonitorManager();
		}
		
		return mySQLMonitorManager;
	}
	
	/**
	 * Monitor the server and collect the counters
	 */
	public void monitorMySQLServer(String strGUID, String strDbName){
		getCounters(strGUID,strDbName);
	}
	
	/**
	 * Send it to the Collector WebService
	 */
	public void sendMySQLCounters(String strGUID){
		sendCounters(strGUID);
	}
	
	/**
	 * Finds the MySQL's version and saves it in the a Application variable
	 */
	private void setMySQLVersion() {
		Statement stmt = null;
		ResultSet rst = null;
		
		try{
			stmt = con.createStatement();
			
			rst = stmt.executeQuery("SHOW VARIABLES LIKE 'version'");
			while ( rst.next() ){
				MYSQL_VERSION = rst.getString(2);
			}
		} catch(Exception e) {
			System.out.println("Exception in setMySQLVersion: "+e.getMessage());
			e.printStackTrace();
		} finally {
			MySQLConnector.close(rst);
			rst = null;
			MySQLConnector.close(stmt);
			stmt = null;
		}
	}
	
	/**
	 * Returns the configured MySQL server's version.
	 * 
	 * @return
	 */
	public String getMySQLVersion() {
		return MYSQL_VERSION;
	}
	
	/**
	 * Collect the counter with agent's types own logic or native methods
	 * 
	 */
	public void getCounters(String strGUID,String strDbName){
		int nCounterId ;		
		Statement stmt = null;
		ResultSet rst = null;		
		String strCountername = null;
		String query = null;
		boolean bIsDelta = false;
		//String strExecutionType = "";
		Statement stmtSlowQry = null;
		ResultSet rstSlowQry = null;
		
		StringBuilder sbSlowQuery = null;
		
		try{
			// reset the counter collector variable in AgentManager.
			resetCounterMap(strGUID);
			
			// establish connection 
			if( con == null || ! MySQLConnector.isConnectionExists(con) ){
				con = MySQLConnector.getMySQLConnector(strDbName).reEstablishConnection(strDbName);
				
				// w.r.t. the MySQL version; get the Counter Codes
				if( MYSQL_VERSION == null ){
					setMySQLVersion();
				}
			}
			
			stmt = con.createStatement();
			JSONArray joSelectedCounters = AgentCounterBean.getCountersBean(strGUID);
			
			for(int i=0; i<joSelectedCounters.size(); i++){
				lCounterValue = 0l;
				query = null;
				
				JSONObject joSelectedCounter = joSelectedCounters.getJSONObject(i);				
				nCounterId = Integer.parseInt(joSelectedCounter.getString("counter_id"));
				// prefix `/* APPEDO */` added, since in slow query log, to avoid our `APPEDO` queries used for monitor
				query = Constants.QUERY_COMMENT_PREFIX+joSelectedCounter.getString("query");
				bIsDelta = joSelectedCounter.getBoolean("isdelta");
				//strExecutionType = joSelectedCounter.getString("executiontype");
				
				//System.out.println("MySQL Monitor query: "+query);
				rst = stmt.executeQuery(query);
				while ( rst.next() ){
					strCountername = rst.getString(1);
//					System.out.println("strCountername :" + strCountername);
					if( strCountername.equals("Compression") || strCountername.equals("Innodb_have_atomic_builtins") || strCountername.equals("Rpl_status") || strCountername.equals("Slave_running") || strCountername.equals("Ssl_session_cache_mode") || strCountername.equals("")  || strCountername.equals("")  || strCountername.equals("")  || strCountername.equals("") ){
						//hmCounters.put( strCountername, rst.getString(2) );
					}else {
						lCounterValue = rst.getLong(2);
						
						if(bIsDelta) {
							lCounterValue = addDeltaCounterValue(nCounterId, lCounterValue);
						}else {
							addCounterValue( nCounterId, lCounterValue );
						}
						// TODO: Static Counter correction required.

		            	// Verify SLA Breach
						// JSONObject joSLACounter = null;
						ArrayList<JSONObject> joSLACounter = null; // Need to change variable name as alSLACounters
						joSLACounter = verifySLABreach(strGUID, SlaCounterBean.getSLACountersBean(strGUID), nCounterId, lCounterValue );
						
						// if breached then add it to Collector's collection
						if( joSLACounter != null ) {
							addSlaCounterValue(joSLACounter);
						}
					}
				}
			}
			
			// add the slow queries here
			JSONObject joSlowQry = new JSONObject();
			JSONObject joQry = null;
			JSONArray  jaSlowQry = new JSONArray();
			
			
			if(checkMysqlLogTable(con)) {
				sbSlowQuery = new StringBuilder();
				
				stmtSlowQry = con.createStatement();
				//query = "SELECT array_to_json(array_agg(row_to_json(t)))  FROM ( select replace(query, E'\'', '') as query, calls, total_time as duration_ms from pg_stat_statements where dbid = (select datid from pg_stat_database where datid="+lDbId+")  order by  total_time   ) t  ";
				
				// added NOT LIKE `/* APPEDO */ %`, since in slow query log, to avoid our `APPEDO` queries used for monitoring
				sbSlowQuery	.append(Constants.QUERY_COMMENT_PREFIX)
							.append("select start_time, round(TIME_TO_SEC(query_time)*1000) as query_time, sql_text ")
							.append("from mysql.slow_log ")
							.append("where sql_text NOT LIKE '").append(Constants.QUERY_COMMENT_PREFIX).append("%' ");
				if ( strDbName != null ) {
					sbSlowQuery.append("AND db = '").append(strDbName).append("' ");
				}
				sbSlowQuery.append("order by query_time desc limit 20 ");
//+ "select query, calls, total_time as duration_ms from pg_stat_statements where dbid= (select datid from pg_stat_database where datname='"+strDbName+"')  order by  total_time desc limit 10 ";
				//myString.replaceAll("'", "\\'");
				//System.out.println(query);
				rstSlowQry = stmtSlowQry.executeQuery(sbSlowQuery.toString());
				while ( rstSlowQry.next() ){
					joQry = new JSONObject();
					joQry.put("query", rstSlowQry.getString("sql_text").replaceAll("'", ""));
					joQry.put("duration_ms",rstSlowQry.getInt("query_time"));
					joQry.put("calls","1");
					joQry.put("stime",rstSlowQry.getString("start_time"));
					jaSlowQry.add(joQry);
				}
				joSlowQry.put("1001", strGUID);// guid
				joSlowQry.put("slowQueries", jaSlowQry.toString());// all queries
				addSlowQryCounterValue(joSlowQry.toString());
				
			}else {
				System.out.println("Mysql Slow query table does not have permission or mysql slow query not configuered.");
			}
		} catch(Throwable th) {
			System.out.println("rst.getString(1): "+strCountername);
			System.out.println("Exception in monitorMySQLServer: "+th.getMessage());
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
			MySQLConnector.close(rst);
			rst = null;
			MySQLConnector.close(stmt);
			stmt = null;
			
			UtilsFactory.clearCollectionHieracy(sbSlowQuery);
			sbSlowQuery = null;
		}
	}
	
	public boolean checkMysqlLogTable(Connection con) {
		Statement stmtQry = null;
		// added prefix since to avoid in slow query log
		String strQry = Constants.QUERY_COMMENT_PREFIX+"SELECT * FROM information_schema.tables WHERE table_schema = 'mysql' AND table_name = 'slow_log' LIMIT 1;";
		ResultSet rstQry = null;
		boolean bReturn = false;
		
		try {
			stmtQry = con.createStatement();
			rstQry = stmtQry.executeQuery(strQry);
			if(!rstQry.wasNull()){
				bReturn = true;
			}
		}catch(Exception e) {
			System.out.println("Exception in checkMysqlLogTable :" + e.getMessage());
		}finally {
			MySQLConnector.close(rstQry);
			rstQry = null;
			MySQLConnector.close(stmtQry);
			stmtQry = null;
		}
		return bReturn;
	}
	
	/**
	 * Send the collected counter-sets to Collector WebService, by calling parent's sendCounter method
	 */
	public void sendCounters(String strGUID) {

		// send the collected counters to Collector WebService through parent sender function
		sendCounterToCollector(strGUID,AGENT_TYPE.MYSQL);
		sendSlowQryToCollector(strGUID,AGENT_TYPE.MYSQL);
		sendSlaCounterToCollector(strGUID,AGENT_TYPE.MYSQL);
	}
	
	@Override
	protected void finalize() throws Throwable {
		clearCounterMap();
		
		super.finalize();
	}
	
}
