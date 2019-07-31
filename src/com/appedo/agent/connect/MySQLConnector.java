package com.appedo.agent.connect;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Properties;

import com.appedo.agent.utils.Constants;
import com.appedo.agent.utils.UtilsFactory;


/**
 * This class handles the MySQL database connection-pooling and gives a connection from the pool on demand.
 * 
 * @author veeru
 *
 */
public class MySQLConnector {

	private Connection con = null;
	
	private String driver = "";
	private String port = "";
	private String protocal = "";
	private String dbHostName = "";
	private String dbName = "";
	private String userName = "";
	private String password = "";
	
	private static MySQLConnector mySQLConnector = null;
	
	/**
	 * Avoid object creation for this class from outside
	 */
	private MySQLConnector() {
	}
	
	/**
	 * Returns the only(singleton) object created for this Class.
	 * 
	 * @return
	 */
	public static MySQLConnector getMySQLConnector(String strDbName) throws Exception {
		if( mySQLConnector == null ){
			mySQLConnector = new MySQLConnector();
			mySQLConnector.connect(strDbName);
		}
		return mySQLConnector;
	}
	
	/**
	 * Connect the MySQL database with the details available in mysql_config.properties file.
	 */
	private void connect(String strDbName) throws Exception {
		
		try {
			
			if( strDbName==null) {
				strDbName = this.dbName;
			}
			loadPropertyFileConstants(Constants.THIS_JAR_PATH+File.separator+"mysql_config.properties");
			
			Class.forName(this.driver);
			con = DriverManager.getConnection("jdbc:"+this.protocal+"://"+this.dbHostName+":"+this.port+"/"+strDbName, this.userName, this.password);
			
		} catch (Exception e) {
			System.out.println("Excepton in MySQL.connect: "+e.getMessage());
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
			
			this.driver = "com.mysql.jdbc.Driver";
			this.protocal = "mysql";
			this.dbHostName	= UtilsFactory.replaceNull( prop.getProperty("DB_HOST"), "localhost" );
			this.port = prop.getProperty("LOCAL_LISTERN_PORT");
			this.dbName = UtilsFactory.replaceNull( prop.getProperty("DB_NAME"), "mysql" );
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
	 * 
	 * @return
	 */
	public Connection getConnetion(String strDbName) throws Exception {
		if( this.con == null || ! isConnectionExists(this.con) ){
			mySQLConnector.connect(strDbName);
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
	 * Re-Establish the connections, if they are not active.
	 * 
	 * @throws SQLException 
	 */
	public Connection reEstablishConnection(String strDbName) throws SQLException {
		while( this.con == null || ! isConnectionExists(this.con) ) {
			try{
				Thread.sleep(3000);
				System.out.println("Trying to establish DB connection.");
				
				this.con = mySQLConnector.getConnetion(strDbName);
			} catch(Exception exConReEstablish) {
				//LogManager.errorLog(exConReEstablish);
				//LogManager.infoLog("unlable to ReEstablish DB connection: "+exConReEstablish.getMessage());
				System.out.println("Unable to ReEstablish DB connection: "+exConReEstablish.getMessage());
				
			}
		}
		
		//LogManager.infoLog("Connection re-established");
		return this.con;
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
