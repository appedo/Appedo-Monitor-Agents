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
 * This class handles the Oracle database connection-pooling and gives a connection from the pool on demand.
 * 
 * @author Veeru
 *
 */
public class OracleConnector {

	private Connection con = null;

	private String connectionURL = "";
	private String driver = "";
	private String port = "";
	private String protocal = "";
	private String dbHostName = "";
	private String dbName = "";
	private String userName = "";
	private String password = "";
	private String sid = "";

	private static OracleConnector myOracleConnector = null;

	/**
	 * Avoid object creation for this class from outside
	 */
	private OracleConnector() {
	}

	/**
	 * Returns the only(singleton) object created for this Class.
	 *
	 * @param strDbName
	 * @return
	 * @throws Exception
	 */
	public static OracleConnector getmyOracleConnector(String strDbName) throws Exception {
		if( myOracleConnector == null ){
			myOracleConnector = new OracleConnector();
			myOracleConnector.connect(strDbName);
		}
		return myOracleConnector;
	}

	/**
	 * Connect the Oracle database with the details available in oracle_config.properties file.
	 * 
	 * @param strDbName
	 * @throws Exception
	 */
	private void connect(String strDbName) throws Exception {
		
		try {
			if( strDbName==null) {
				strDbName = this.dbName;
			}
			
			loadPropertyFileConstants(Constants.THIS_JAR_PATH+File.separator+"oracle_config.properties");
			//System.out.println("Connection string :" + "jdbc:"+this.protocal+"://"+this.dbHostName+":"+this.port+"/"+strDbName);
			
			Class.forName(this.driver);
			//("jdbc:oracle:thin:@myhost:1521:orcl", "scott", "tiger");
			
			// if DB-Password is not given then, consider the user as Trusted user. Try to login without password.
			if( this.password != null && this.password.length() > 0 ) {
				connectionURL = "jdbc:" + this.protocal + ":@" + this.dbHostName + ":" + this.port + ":" + this.sid;
				this.con = DriverManager.getConnection(connectionURL, this.userName, this.password);
			} else {
				throw new Exception("Please enter a valid username/password in the oracle_config.properties");
			}
			
		} catch (Exception e) {
			System.out.println("Excepton in Oracle.connect: "+e.getMessage());
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

			this.driver = "oracle.jdbc.driver.OracleDriver";
			this.protocal = "oracle:thin";
			this.dbHostName	= UtilsFactory.replaceNull(prop.getProperty("DB_HOST"), "localhost");
			this.port = prop.getProperty("LOCAL_LISTERN_PORT");
			this.userName = prop.getProperty("USER_NAME");
			this.password = prop.getProperty("USER_PASSWORD");
			this.sid = UtilsFactory.replaceNull(prop.getProperty("SID"), "orcl");
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
	 * @param strDbName
	 * @return
	 * @throws Exception
	 */
	public Connection getConnetion(String strDbName) throws Exception {
		if( this.con == null || ! isConnectionExists(this.con) ) {
			this.con = null;
			myOracleConnector.connect(strDbName);
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
			stmt.execute("SELECT 1 FROM DUAL");
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

