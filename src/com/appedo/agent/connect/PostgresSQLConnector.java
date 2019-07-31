package com.appedo.agent.connect;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Properties;

import com.appedo.agent.manager.LogManagerExtended;
import com.appedo.agent.utils.Constants;
import com.appedo.agent.utils.UtilsFactory;

import net.sf.json.JSONObject;

/**
 * This class handles the PG database connection-pooling and gives a connection from the pool on demand.
 * 
 * @author veeru
 *
 */
public class PostgresSQLConnector {

	private Connection con = null;
	
	private String connectionURL = "";
	private String driver = "";
	private String port = "";
	private String protocal = "";
	private String dbHostName = "";
	private String dbName = "";
	private String userName = "";
	private String password = "";
	
	private static PostgresSQLConnector myPGConnector = null;
	private static HashMap<String, PostgresSQLConnector> hmPGConnector = new HashMap<String, PostgresSQLConnector>();
	/**
	 * Avoid object creation for this class from outside
	 */
	private PostgresSQLConnector() {
	}
	
	/**
	 * Returns the only(singleton) object created for this Class.
	 * 
	 * @return
	 */
	public static PostgresSQLConnector getMyPGConnector(String strDbName) throws Exception {
		if( hmPGConnector.containsKey(strDbName)){
			myPGConnector = hmPGConnector.get(strDbName);
		}else {
			myPGConnector = new PostgresSQLConnector();
			myPGConnector.connect(strDbName);
			hmPGConnector.put(strDbName, myPGConnector);
		}
		return myPGConnector;
	}
	
	public static PostgresSQLConnector getMyPGConnector() throws Exception {
		if( myPGConnector == null ){
			myPGConnector = new PostgresSQLConnector();
		}
		return myPGConnector;
	}
	
	public boolean getPGConfigFile() {
		boolean bISPostgresSQLconnect = false;
		try {
			bISPostgresSQLconnect = loadPropertyFileConstants(Constants.THIS_JAR_PATH+File.separator+"pg_config.properties");
		}catch (Exception e) {
			System.out.println("Excepton in getPGConfigFile : "+e.getMessage());
		}
		return bISPostgresSQLconnect;
	}
	
	public ArrayList<String> getAllDataBaseName() throws Exception {
		ArrayList<String> alDBName = null;
		try {
			//getConnetion();
			alDBName = listAllDataBaseNames();
		}catch (Exception e) {
			LogManagerExtended.databaseInfoLog("Excepton in getAllDataBaseName "+e.getMessage());
			throw e;
		}
		
		return alDBName;
	}
	
	public ArrayList<String> getAllDataBaseName_v1() throws Exception {
		
		ArrayList<String> alDBName = null;
		try {			
			loadPropertyFileConstants(Constants.THIS_JAR_PATH+File.separator+"pg_config.properties");
			
			Class.forName(this.driver);
			
			// if DB-Password is not given then, consider the user as Trusted user. Try to login without password.
			if( this.password != null && this.password.length() > 0 ) {
				connectionURL = "jdbc:"+this.protocal+"://"+this.dbHostName+":"+this.port+"/?";
				this.con = DriverManager.getConnection(connectionURL, this.userName, this.password);
			} else {
				connectionURL = "jdbc:"+this.protocal+"://"+this.dbHostName+":"+this.port+"/?user="+this.userName;
				this.con = DriverManager.getConnection(connectionURL);
			}
			alDBName = listAllDataBaseNames();
		} catch (Exception e) {
			LogManagerExtended.databaseInfoLog("Excepton in PG.connect: "+e.getMessage());
			throw e;
		}
		return alDBName;
	}
	
	public JSONObject getModuleInformation() throws Exception{
		JSONObject joModuleData = null;
		try {
			DatabaseMetaData metadata = this.con.getMetaData();
			joModuleData = new JSONObject();
			joModuleData.put("moduleTypeName", metadata.getDatabaseProductName());
			joModuleData.put("VERSION_ID", metadata.getDatabaseProductVersion());
		}catch (Exception e) {
			LogManagerExtended.databaseInfoLog("Exception in getModuleInformation :"+e);
		}finally {
			this.con = null;
		}
		return joModuleData;
	}
	
	public ArrayList<String> listAllDataBaseNames() {
		ArrayList<String> alDBName = new ArrayList<String>();
        try {
            PreparedStatement ps = this.con.prepareStatement("SELECT datname FROM pg_database WHERE datistemplate = false;");
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
            	alDBName.add(rs.getString(1));
            }
            rs.close();
            ps.close();

        } catch (Exception e) {
        	LogManagerExtended.databaseInfoLog("Exceptin in listDownAllDatabases : "+ e);
            e.printStackTrace();
        }
        return alDBName;
    }
	
	/**
	 * Connect the PG database with the details available in mysql_config.properties file.
	 */
	private void connect(String strDbName) throws Exception {
		
		try {
			if( strDbName==null) {
				strDbName = this.dbName;
			}
			
			loadPropertyFileConstants(Constants.THIS_JAR_PATH+File.separator+"pg_config.properties");
			
			Class.forName(this.driver);
			
			// if DB-Password is not given then, consider the user as Trusted user. Try to login without password.
			if( this.password != null && this.password.length() > 0 ) {
				connectionURL = "jdbc:"+this.protocal+"://"+this.dbHostName+":"+this.port+"/"+strDbName;
				this.con = DriverManager.getConnection(connectionURL, this.userName, this.password);
			} else {
				connectionURL = "jdbc:"+this.protocal+"://"+this.dbHostName+":"+this.port+"/"+strDbName+"?user="+this.userName;
				this.con = DriverManager.getConnection(connectionURL);
			}
		} catch (Exception e) {
			System.out.println("Excepton in PG.connect: "+e.getMessage());
			throw e;
		}
	}
	
	/**
	 * Connect the PG database with the details available in mysql_config.properties file.
	 */
	private void connect() throws Exception {
		
		try {
			loadPropertyFileConstants(Constants.THIS_JAR_PATH+File.separator+"pg_config.properties");
			
			Class.forName(this.driver);
			
			// if DB-Password is not given then, consider the user as Trusted user. Try to login without password.
			if( this.password != null && this.password.length() > 0 ) {
				connectionURL = "jdbc:"+this.protocal+"://"+this.dbHostName+":"+this.port+"/?";
				this.con = DriverManager.getConnection(connectionURL, this.userName, this.password);
			} else {
				connectionURL = "jdbc:"+this.protocal+"://"+this.dbHostName+":"+this.port+"/?user="+this.userName;
				this.con = DriverManager.getConnection(connectionURL);
			}
		} catch (Exception e) {
			LogManagerExtended.databaseInfoLog("Excepton in PG.connect: "+e.getMessage());
			throw e;
		}
	}
	
	
	/**
	 * Load the properties in the given file to the connection details variable.
	 * 
	 * @param strFilePath
	 * @return
	 * @throws Exception
	 */
	private boolean loadPropertyFileConstants(String strFilePath) throws Exception {
		Properties prop = new Properties();
		InputStream is = null;
		
		try{
			is = new FileInputStream(strFilePath);
			prop.load(is);
			
			this.driver = "org.postgresql.Driver";
			this.protocal = "postgresql";
			this.dbHostName	= UtilsFactory.replaceNull(prop.getProperty("DB_HOST"), "localhost");
			this.port = prop.getProperty("LOCAL_LISTERN_PORT");
			this.userName = prop.getProperty("USER_NAME");
			this.password = prop.getProperty("USER_PASSWORD");
		} catch(Exception e){
			System.out.println("Exception in loadSessionConstants: "+e.getMessage());
			throw e;
		} finally {
			is.close();
			is = null;
		}
		
		return true;
	}
	
	/**
	 * Return a connection object from the connection-pool.
	 * Singleton Connection will be returned. If that is not exists or expired then, Recreate the connection.
	 * 
	 * @return
	 */
	public Connection getConnetion(String strDbName) throws Exception {
		if( this.con == null || ! isConnectionExists(this.con) ) {
			this.con = null;
			myPGConnector.connect(strDbName);
		}
		
		return this.con;
	}
	
	/**
	 * Return a connection object from the connection-pool.
	 * Singleton Connection will be returned. If that is not exists or expired then, Recreate the connection.
	 * 
	 * @return
	 */
	public Connection getConnetion() throws Exception {
		if( this.con == null || ! isConnectionExists(this.con) ) {
			this.con = null;
			//myPGConnector.connect();
			myPGConnector.connect("postgres");
		}
		
		return this.con;
	}
	
	/**
	 * Check whether connection is alive or not.
	 * 
	 * @param con
	 * @return
	 * @throws SQLException
	 */
	public static boolean isConnectionExists(Connection con) throws SQLException {
		Statement stmt = null;
		
		try{
			stmt = con.createStatement();
			stmt.execute("SELECT 1");
			return true;
		} catch (Exception e) {
			return false;
		} finally {
			close(stmt);
			stmt = null;
		} 
	}
	
	/**
	 * Close a ResultSet if it has an object in its reference
	 * 
	 * @param rst
	 * @return
	 */
	public static boolean close(ResultSet rst){
		try{
			if( rst != null )
				rst.close();
		}catch(Exception e){
			System.out.println("Exception while closing ResultSet: "+e.getMessage());
			e.printStackTrace();
			return false;
		}
		return true;
	}
	
	/**
	 * Close a Statement if it has an object in its reference
	 * 
	 * @param sta
	 * @return
	 */
	public static boolean close(Statement sta){
		try{
			if( sta != null )
				sta.close();
		}catch(Exception e){
			System.out.println("Exception while closing Statement: "+e.getMessage());
			e.printStackTrace();
			return false;
		}
		return true;
	}
	
	/**
	 * Close a Connection if it has an object in its reference
	 * 
	 * @param conn
	 * @return
	 */
	public static boolean close(Connection conn){
		try{
			if( conn != null )
				conn.close();
		}catch(Exception e){
			System.out.println("Exception while closing Connection: "+e.getMessage());
			e.printStackTrace();
			return false;
		}
		return true;
	}
}

