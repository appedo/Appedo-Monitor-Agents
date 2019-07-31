package com.appedo.agent.utils;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.io.UnsupportedEncodingException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.URLDecoder;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;

import com.appedo.agent.bean.JStackBean;
import com.appedo.agent.bean.JStackEntryBean;
import com.appedo.agent.utils.Constants.OSType;

import net.sf.json.JSONObject;

public class UtilsFactory{
	
	/**
	 * Returns the current date-time in the format of yyyy-MM-dd HH:mm:ss.S
	 * 
	 * @return
	 */
	public static String nowFormattedDate(){
		String opDate = "";
		try{
			Calendar calNow = Calendar.getInstance();
			DateFormat opFormatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.S");
			opDate = opFormatter.format(calNow.getTime());
		}catch(Exception e){
			System.out.println("Exception in nowFormattedDate(): "+e.getMessage());
			e.printStackTrace();
		}
		return opDate;
	}
	
	/**
	 * Returns the current date-time in yyyy-MM-dd'T'HH:mm:ss.SSSXXX format, say "2015-08-19T13:58:32.431+05:30"
	 * 
	 * @return
	 */
	public static String formatDateWithTimeZone(long lDate){
		String opDate = "";
		try{
			DateFormat opFormatter = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSXXX");
			opDate = opFormatter.format(lDate);
		}catch(Exception e){
			System.out.println("Exception in formatDateWithTimeZone(): "+e.getMessage());
			e.printStackTrace();
		}
		return opDate;
	}
	
	public static void printDebugLog(boolean toPrint, String message) {
		System.out.print(toPrint?"DEBUG: "+message+"\n" : "");
	}
	
	/**
	 * Returns the current date-time in yyyy-MM-dd'T'HH:mm:ss.SSSXXX format, say "2015-08-19T13:58:32.431+05:30"
	 * 
	 * @return
	 */
	public static String formatDateWithTimeZone(Date lDate){
		String opDate = "";
		try{
			DateFormat opFormatter = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSXXX");
			opDate = opFormatter.format(lDate);
		}catch(Exception e){
			System.out.println("Exception in formatDateWithTimeZone(): "+e.getMessage());
			e.printStackTrace();
		}
		return opDate;
	}
	
	public static boolean isStartingWith(String[] saCheck, String strTarget){

		for( int i=0; i < saCheck.length; i++ ){
			if( strTarget.startsWith(saCheck[i]) ){
				return true;
			}
		}
		
		return false;
	}
	
	public static boolean isExists(String[] saCheck, String strTarget){

		for( int i=0; i < saCheck.length; i++ ){
			if( strTarget.equals(saCheck[i]) ){
				return true;
			}
		}
		
		return false;
	}
	
	/**
	 * Round up the given decimal digits.
	 * 
	 * @param value
	 * @param places
	 * @return
	 */
	public static Double round(Double value, int places) {
		if (places < 0) throw new IllegalArgumentException();
		
		BigDecimal bd = new BigDecimal(value);
		bd = bd.setScale(places, RoundingMode.HALF_UP);
		return bd.doubleValue();
	}
	
	/**
	 * This method checks whether 'val1' is null OR length of val1 = 0, 
	 * if so, it will return val2 String, irrespective of val2's value.
	 * Otherwise, it will return val1.
	 * 
	 * @param val1 
	 * @param val2 
	 * @return String
	 */
	public static String replaceNull(Object val1, String val2) {
		if (val1 == null || val1.toString().length() == 0)
			return val2;
		else
			return val1.toString();
	}
	
	/**
	 * Returns Properties object for the file given.
	 * 
	 * @param configFilePath
	 * @return
	 * @throws Exception
	 */
	public static Properties getPropertiesFile(String configFilePath)throws Exception{
		Properties prop = null;
		try{
			prop = new Properties();
			InputStream is = new FileInputStream( configFilePath );
			prop.load(is);
		}catch(Exception e){
			System.out.println("Exception in Property file load: "+e.getMessage());
			e.printStackTrace();
			throw e;
		}
		return prop;
	}
	/**
	 * detect the operating system from the os.name System property and cache
	 * the result
	 * 
	 * @returns - the operating system detected
	 */
	public static OSType getOperatingSystemType() {
		OSType detectedOS;
		
		String OS = System.getProperty("os.name", "generic").toLowerCase(Locale.ENGLISH);
		if ((OS.indexOf("mac") >= 0) || (OS.indexOf("darwin") >= 0)) {
			detectedOS = OSType.MacOS;
		} else if (OS.indexOf("win") >= 0) {
			detectedOS = OSType.Windows;
		} else if (OS.indexOf("nux") >= 0) {
			detectedOS = OSType.Linux;
		} else {
			detectedOS = OSType.Other;
		}
		
		return detectedOS;
	}
	
	/**
	 * Find the absolute path of the JAR.
	 * 
	 * If the execution is done through IDE, then the "bin" folder's home path is returned.	
	 * 
	 * @return
	 * @throws UnsupportedEncodingException
	 */
	public static String getThisJarPath() throws UnsupportedEncodingException {
		String strThisJARPath = null;
		
		strThisJARPath = UtilsFactory.class.getProtectionDomain().getCodeSource().getLocation().getPath();
		String OS = System.getProperty("os.name").toLowerCase();
		if (OS.contains("wind")) {
			strThisJARPath = strThisJARPath.substring(1);
		} else if (OS.contains("nix") || OS.contains("nux") || OS.contains("aix")) {
			strThisJARPath = strThisJARPath.substring(0);
		}
		
		// While executing JAR through command-prompt, need to trim till last /File-Separator
		// Eg.: C:/Users/ram/Downloads/appedo_mysql_monitor/appedo_mysql_agent_pvt.jar
		if( strThisJARPath.endsWith(".jar") ) {
			strThisJARPath = strThisJARPath.substring(0, strThisJARPath.lastIndexOf("/"));
		}
		// Otherwise considering that the execution is from some IDE like Eclipse, trim the last folder, which is suppose to be 'bin' folder
		// Eg.: E:/Ramkumar/workspace_github/Appedo-Monitor-Agents/bin/
		else {
			System.out.println("Assuming this is started from IDE. Removed 'bin' from the path");
			strThisJARPath = strThisJARPath.substring(0, strThisJARPath.length()-5);
		}
		
		strThisJARPath = new File(URLDecoder.decode(strThisJARPath, "UTF-8")).getAbsolutePath();
		
		return strThisJARPath;
	}
	
	/**
	 * Closes the nested collection variable.
	 * 
	 * @param objCollection
	 */
	public static void clearCollectionHieracy(Object objCollection){
		try{
			if( objCollection == null ) {

			} else if( objCollection instanceof JStackBean ) {
				JStackBean jsbCollection = (JStackBean)objCollection;
				jsbCollection.destroy();
				
			} else if( objCollection instanceof JStackEntryBean ) {
				JStackEntryBean jsbCollection = (JStackEntryBean)objCollection;
				jsbCollection.destroy();
				
			} else if( objCollection instanceof Map ) {
				Map mapCollection = (Map)objCollection;
				Iterator it = mapCollection.keySet().iterator();
				while( it.hasNext() ){
					Object str = it.next();
					clearCollectionHieracy( mapCollection.get(str) );
				}
				mapCollection.clear();
				// mapCollection = null;
			} else if( objCollection instanceof List ) {
				List listCollection = (List)objCollection;
				for( int i=0; i < listCollection.size(); i++ ){
					clearCollectionHieracy( listCollection.get(i) );
				}
				listCollection.clear();
				// listCollection = null;
			} else if( objCollection instanceof StringBuilder ) {
				StringBuilder sbCollection = (StringBuilder)objCollection;
				sbCollection.setLength(0);
			} else if( objCollection instanceof StringBuffer ) {
				StringBuffer sbCollection = (StringBuffer)objCollection;
				sbCollection.setLength(0);
			}
			
			// objCollection = null;
		}catch(Throwable t){
			System.out.println("Exception while clearCollectionHieracy: "+t.getMessage());
			t.printStackTrace();
		}
	}
	
	/**
	 * Returns true if given value is present in the array
	 * 
	 * @param saArray
	 * @param strValue
	 * @return
	 */
	public static boolean contains(String[] saArray, String strValue) {
		for (int i = 0; i < saArray.length; i++) {
			if (saArray[i].equalsIgnoreCase(strValue)) return true;
		}
		
		return false;
	}
	
	/**
	 * Returns the index of the value.
	 * if given value is not present in the array, returns -1.
	 * 
	 * @param saArray
	 * @param strValue
	 * @return
	 */
	public static int contains(String[] saArray, String strValue, Class classObject) {
		for (int i = 0; i < saArray.length; i++) {
			if (saArray[i].equals(strValue)) {
				return i;
			}
		}
		
		return -1;
	}
	
	public static void close(InputStream inputStream) {
		try {
			if (inputStream != null) {
				inputStream.close();
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	

	public static void close(Reader reader) {
		try {
			if (reader != null) {
				reader.close();
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	public static byte[] getBytes(InputStream is) throws IOException {

		int len;
		int size = 1024;
		byte[] buf;

		if (is instanceof ByteArrayInputStream) {
			size = is.available();
			buf = new byte[size];
			len = is.read(buf, 0, size);
		} else {
			ByteArrayOutputStream bos = new ByteArrayOutputStream();
			buf = new byte[size];
			while ((len = is.read(buf, 0, size)) != -1)
				bos.write(buf, 0, len);
			buf = bos.toByteArray();
		}
		return buf;
	}
	
	static String jsonString = "{\r\n" + 
			"\"Tasks\":[\r\n" + 
			"{\"Key\":\"total\", \"CounterName\":\"Total running Processes\", \"Desc\":\"Processes running in totals\"},\r\n" + 
			"{\"Key\":\"running\", \"CounterName\":\"Processes running\", \"Desc\":\"Processes running\"},\r\n" + 
			"{\"Key\":\"sleeping\", \"CounterName\":\"Processes sleeping\", \"Desc\":\"Processes sleeping\"},\r\n" + 
			"{\"Key\":\"stopped\", \"CounterName\":\"Processes stopped\", \"Desc\":\"Processes stopped\"},\r\n" + 
			"{\"Key\":\"zombie\", \"CounterName\":\"Processes waiting to be stop\", \"Desc\":\"Processes waiting to be stoppati from the parent process\"}\r\n" + 
			"],\r\n" + 
			"\"Cpu\":[\r\n" + 
			"{\"Key\":\"us\", \"CounterName\":\"User Total CPU%\", \"Desc\":\"Percentage of the CPU for user processes\"},\r\n" + 
			"{\"Key\":\"sy\", \"CounterName\":\"System Total CPU%\", \"Desc\":\"Percentage of the CPU for system processes\"},\r\n" + 
			"{\"Key\":\"ni\", \"CounterName\":\"Nice CPU%\", \"Desc\":\"Percentage of the CPU processes with priority upgrade nice\"},\r\n" + 
			"{\"Key\":\"id\", \"CounterName\":\"Idle CPU%\", \"Desc\":\"CPU usage as a percentage by idle processes\"},\r\n" + 
			"{\"Key\":\"wa\", \"CounterName\":\"CPU% of Processes wa for I/O\", \"Desc\":\"Percentage of the CPU processes waiting for I/O operations\"},\r\n" + 
			"{\"Key\":\"hi\", \"CounterName\":\"Hardware CPU%\", \"Desc\":\"Percentage of the CPU serving hardware interrupts\"},\r\n" + 
			"{\"Key\":\"si\", \"CounterName\":\"Software CPU%\", \"Desc\":\"Percentage of the CPU serving software interrupts\"},\r\n" + 
			"{\"Key\":\"st\", \"CounterName\":\"Steal Time of CPU%\", \"Desc\":\"CPU usage as a percentage by steal time\"}\r\n" + 
			"], \r\n" + 
			"\"Mem\":[\r\n" + 
			"{\"Key\":\"total\", \"CounterName\":\"Total system memory\", \"Desc\":\"Total system memory\"},\r\n" + 
			"{\"Key\":\"free\", \"CounterName\":\"Free memory\", \"Desc\":\"Free memory\"},\r\n" + 
			"{\"Key\":\"used\", \"CounterName\":\"Memory used\", \"Desc\":\"Memory used\"},\r\n" + 
			"{\"Key\":\"buffers\", \"CounterName\":\"Buffer cache\", \"Desc\":\"Buffer cache\"},\r\n" + 
			"{\"Key\":\"buff/cache\", \"CounterName\":\"Buffer cache\", \"Desc\":\"Buffer cache\"}\r\n" + 
			"], \r\n" + 
			"\"Swap\":[\r\n" + 
			"{\"Key\":\"total\", \"CounterName\":\"Total swap available\", \"Desc\":\"Total swap available\"},\r\n" + 
			"{\"Key\":\"used\", \"CounterName\":\"Total swap free\", \"Desc\":\"Total swap free\"},\r\n" + 
			"{\"Key\":\"free\", \"CounterName\":\"Total swap used\", \"Desc\":\"Total swap used\"},\r\n" + 
			"{\"Key\":\"avail\", \"CounterName\":\"Available memory\", \"Desc\":\"Available memory\"},\r\n" + 
			"{\"Key\":\"cached\", \"CounterName\":\"Available memory\", \"Desc\":\"Available memory\"}\r\n" + 
			"]\r\n" + 
			"}";
	
	//JSONObject joCouterData = new JSONObject();
	public static JSONObject joCounters = JSONObject.fromObject(jsonString);
	
	
	
	/**
	 * Returns the given bytes unit into KiloBytes unit
	 * 
	 * @param lByte
	 * @return
	 *
	public static long convertByteToKB(long lByte) {
		return lByte / 1024;
	}
	
	/**
	 * Returns the given bytes unit into MegaBytes unit
	 * 
	 * @param lByte
	 * @return
	 *
	public static long convertByteToMB(long lByte) {
		return lByte / 1024 / 1024;
	}
	
	/**
	 * Returns the given KiloBytes unit into MegaBytes unit
	 * 
	 * @param lByte
	 * @return
	 *
	public static long convertKByteToMB(long lKiloByte) {
		return lKiloByte / 1024;
	}*/
	
}
