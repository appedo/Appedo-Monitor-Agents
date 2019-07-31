package com.appedo.agent.manager;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.ArrayList;

import com.appedo.agent.utils.Constants;
import com.appedo.agent.utils.Constants.OSType;
import com.appedo.agent.utils.UtilsFactory;

/**
 * Command Executor: Executes the command given and gives the output (or) error.
 * 
 * @author Ramkumar
 */
public class CommandLineExecutor {
	
	private Process pProcstat = null;
	
	/**
	 * Execute the given command.
	 * 
	 * @param strQuery
	 * @return
	 * @throws Exception
	 */
	public boolean executeCommand(String strQuery) throws Exception {
		ProcessBuilder pbProcStat = null;
		String strExecutor = null, strExecuteOption = null;
		
		try {
			if( Constants.THIS_OS_TYPE == OSType.Windows ) {
				strExecutor = "cmd";
				strExecuteOption = "/C";
			} else if( Constants.THIS_OS_TYPE == OSType.Linux ) {
				strExecutor = "bash";
				strExecuteOption = "-c";
			} else {
				strExecutor = "";
				strExecuteOption = "";
			}
			
			// Close existing Process Objects
			closeExistingObjects();
			
			// Make a process to execute the given Query
			pbProcStat = new ProcessBuilder(strExecutor, strExecuteOption, strQuery);
			pProcstat = pbProcStat.start();
			
		} catch (Exception e) {
			throw e;
		}
		
		return true;
	}
	
	/**
	 * Not to be used anywhere.
	 * Wrapper function for JBoss, Glassfish, Weblogic, etc. To be replaced as-like used in TomcatMonitorManger
	 * 
	 * Execute the command and return the output.
	 * 
	 * @param strQuery
	 * @return
	 */
	public static ArrayList<String> execute(String strQuery) {
		ArrayList<String> al = new ArrayList<String>();
		String[] saOutput = null;
		
		CommandLineExecutor cmdExecutor = new CommandLineExecutor();
		
		try{
			cmdExecutor.executeCommand(strQuery);
			saOutput = cmdExecutor.getOutput().toString().split("\n");
			
			for( String strOutput: saOutput ) {
				al.add(strOutput);
			}
		} catch(Exception ex) {
			System.out.println("Exception in getErrorString: "+ex.getMessage());
			ex.printStackTrace();
		} finally {
			cmdExecutor.closeExistingObjects();
		}
		
		return al;
	}
	
	/**
	 * Returns the output of the command executed.
	 * 
	 * @return StringBuilder
	 */
	public StringBuilder getOutput() {
		return getOutput(null);
	}
	
	/**
	 * Returns the output of the command executed.
	 * Appends the output into StringBuilder and also adds into ArrayList.
	 * 
	 * @return StringBuilder
	 */
	public StringBuilder getOutput(ArrayList<String> alOutputLines) {
		InputStreamReader isrOutput = null;
		BufferedReader rOutput = null;
		String line = null;
		StringBuilder sbOutput = new StringBuilder();
		
		try{
			isrOutput = new InputStreamReader( pProcstat.getInputStream() );
			rOutput = new BufferedReader(isrOutput);
			while ((line = rOutput.readLine()) != null) {
				sbOutput.append(line).append("\n");
				if( alOutputLines != null ) {
					alOutputLines.add(line);
				}
			}
			
			System.out.println("Waiting for cmd to complete...");
			pProcstat.waitFor();
			
			// check for pending execution
			while ((line = rOutput.readLine()) != null) {
				sbOutput.append(line).append("\n");
				if( alOutputLines != null ) {
					alOutputLines.add(line);
				}
			}
			if( sbOutput.length() > 0 ){
				sbOutput.deleteCharAt(sbOutput.length()-1);
			}
		} catch ( Exception e ) {
			System.out.println("Exception in getErrorString: "+e.getMessage());
			e.printStackTrace();
		} finally {
			UtilsFactory.close(isrOutput);
			isrOutput = null;
			
			UtilsFactory.close(rOutput);
			rOutput = null;
		}
		
		return sbOutput;
	}
	
	/**
	 * Returns the Error of the command executed.
	 * 
	 * @return StringBuilder
	 */
	public StringBuilder getErrors() {
		InputStreamReader isrError = null;
		BufferedReader rError = null;
		String line = null;
		StringBuilder sbError = new StringBuilder();
		
		try{
			isrError = new InputStreamReader( pProcstat.getErrorStream() );
			rError = new BufferedReader(isrError);
			while ((line = rError.readLine()) != null) {
				sbError.append(line).append("\n");
			}
			
			System.out.println("Waiting for cmd to complete...");
			pProcstat.waitFor();
			
			while ((line = rError.readLine()) != null) {
				sbError.append(line).append("\n");
			}
			
			if( sbError.length() > 0 ){
				sbError.deleteCharAt(sbError.length()-1);
			}
			
		} catch ( Throwable th) {
			System.out.println("Exception in getErrorString: "+th.getMessage());
			th.printStackTrace();
		} finally {
			UtilsFactory.close(isrError);
			isrError = null;
			
			UtilsFactory.close(rError);
			rError = null;
		}
		
		return sbError;
	}
	
	/**
	 * Close the Process object used to execute the command.
	 */
	private void closeExistingObjects() {
		try {
			if (pProcstat != null) {
				pProcstat.destroy();
			}
		} catch (Exception e) {
			System.out.println("Exception in closeExistingObjects: "+e.getMessage());
			e.printStackTrace();
		}
		
		pProcstat = null;
	}
	
	/**
	 * Close the Process object used to execute the command.
	 */
	public void close() {
		closeExistingObjects();
	}
	
	@Override
	protected void finalize() throws Throwable {
		closeExistingObjects();
		
		super.finalize();
	}
}
