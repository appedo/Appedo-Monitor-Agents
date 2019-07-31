package com.appedo.agent.manager;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.appedo.agent.bean.JStackBean;
import com.appedo.agent.bean.JStackEntryBean;
import com.appedo.agent.utils.Constants;
import com.appedo.agent.utils.Constants.OSType;
import com.appedo.agent.utils.UtilsFactory;

import net.sf.json.JSONObject;

public class JStackMonitorManager {
	
	private static JStackMonitorManager jStackMonitorManager = null;
	
	private String[] saIgnoreClasses = new String[] {"java.", "javax.", "sun.", "com.sun.", "org.apache.", "commons.", "net.", "websocket.", "org.jboss.", "org.xnio.", "com.noelios.", "oracle.", "weblogic.", "com.mysql.", "com.postgresql." };
	
	private String pidJVM = null, pidGrepPattens = null, sudoUserName = null, threadStartRegExPattens = null;
	int nTemp = 0;
	
	private HashMap<String, Long> hmUniqueThreadIds = new HashMap<String, Long>();
	private long lNextUniqueThreadId = System.nanoTime();	// check function description of "getNewThreadId"
	
	private JStackMonitorManager(String strSvrAlias) {
		JSONObject joConfig = JSONObject.fromObject(Constants.ASD_DETAILS);
		JSONObject joSvrDetails = joConfig.getJSONObject(strSvrAlias);
		
		this.pidJVM = joSvrDetails.containsKey("jvm_pid")?joSvrDetails.getString("jvm_pid"):null;
		this.sudoUserName = joSvrDetails.containsKey("sudo_user_name")?joSvrDetails.getString("sudo_user_name"):null;
		this.pidGrepPattens = joSvrDetails.containsKey("jvm_pid_grep_patterns")?joSvrDetails.getString("jvm_pid_grep_patterns"):null;
		this.threadStartRegExPattens = joSvrDetails.containsKey("thread_start_reg_ex_pattens")?joSvrDetails.getString("thread_start_reg_ex_pattens"):null;
	}
	
	public static JStackMonitorManager getJStackMonitorManager(String strSvrAlias) {
		if( jStackMonitorManager == null ) {
			jStackMonitorManager = new JStackMonitorManager(strSvrAlias);
		}
		
		return jStackMonitorManager;
	}
	
	private ArrayList<String> collectStack() {
		CommandLineExecutor cmdExec = null;
		ArrayList<String> alOutput = null;
		
		StringBuilder sbCommands = new StringBuilder(), sbSudoAppender = new StringBuilder(), sbErrors = null;
		
		try {
			if( Constants.THIS_OS_TYPE == OSType.Linux ) {
				if( this.sudoUserName == null || this.sudoUserName.equalsIgnoreCase("null") ) {
					// no need to add anything
				} else if( this.sudoUserName.isEmpty() ) {
					sbSudoAppender.append("sudo ");
				} else {
					sbSudoAppender.append("sudo -u ").append(sudoUserName).append(" ");
				}
				
				sbCommands.append(sbSudoAppender).append("jstack -l ");
				
				if( pidJVM != null & ! pidJVM.isEmpty() ) {
					sbCommands.append(pidJVM);
				} else {
					if( pidGrepPattens == null || pidGrepPattens.isEmpty() ) {
						pidGrepPattens = "org.apache.catalina.startup.Bootstrap";
					}
					
					sbCommands.append("$(").append(sbSudoAppender).append("ps -ef | grep -e ").append(pidGrepPattens.replaceAll(",", "-e ")).append(" | grep -v grep | head -n1 | awk '{print $2}')");
				}
			}
			else if( Constants.THIS_OS_TYPE == OSType.Windows ) {
				if( pidJVM != null & ! pidJVM.isEmpty() ) {
					sbCommands.append("jstack -l ").append(pidJVM);
				} else {
					if( pidGrepPattens == null || pidGrepPattens.isEmpty() ) {
						pidGrepPattens = "WINDOWTITLE eq Tomcat";
					}
					
					sbCommands.append("for /f \"tokens=2 USEBACKQ\" %P IN (`tasklist /NH /FI \"").append(pidGrepPattens.replaceAll(",", "\" /FI \"")).append("\" ^| findstr /n . ^| findstr \"^2:\"`) Do jstack -l %P");
				}
			}
			
//			sbCommands.setLength(0);
//			sbCommands.append("type jstack_20180102_").append(++nTemp).append(".txt");
//			sbCommands.append("type C:\\Users\\ram\\Downloads\\appedo_jstack_monitor_2.1.001_test\\jstack_cmd_output.log");
			
			cmdExec = new CommandLineExecutor();
			System.out.println("sbCommands: "+sbCommands);
			cmdExec.executeCommand(sbCommands.toString());
			
			alOutput = new ArrayList<String>();
			cmdExec.getOutput(alOutput);
			
			sbErrors = cmdExec.getErrors();
			if( sbErrors.length() == 0 && alOutput.size() == 0 ) {
				System.out.println("No JVM Service is running. Waiting for service to start...");
				
				Thread.sleep(5000);
			} else if( sbErrors.length() > 0 ) {
				System.out.println("Exception while connecting JVM:\n"+sbErrors+"\nWaiting for service to start...");
				
				Thread.sleep(5000);
			}
			
		} catch(Throwable th) {
			System.out.println("Exception in collectStack: "+th.getMessage());
			th.printStackTrace();
		} finally {
			cmdExec.close();
		}
		
		return alOutput;
	}
	
	private Object[] processStack(ArrayList<String> alStackLines, long lExecutionCompletedOn, JStackBean prevJStackBean, Long lPrevExecutionCompletedOn, HashSet<String> hsPrevJVMThreadIds) {
		String patternString = UtilsFactory.replaceNull(this.threadStartRegExPattens, "\".+\"\\s(.+\\s)?prio=\\d+\\s.*tid=\\w+\\s.*nid=(\\w+)\\s([\\w+ ]+)(\\s\\[\\w+\\])?$");
		
		HashSet<String> hsJVMThreadIds = new HashSet<String>();
		
		Pattern pattern = Pattern.compile(patternString);
		Matcher matcher = null;
		boolean matches = false, bStackStarts = false;
		String strJVMThreadId = null, strUniqueThreadId = null, strFunctionName = null;
		
//		LinkedHashMap<String, String> hmThreadStatus = new LinkedHashMap<String, String>();
		ArrayList< JStackEntryBean > alStack = null;
		int nThreadMatchingHierarchy = -1, nIndex = 0;
//		Long lPrevTimeTaken = null;
		
		JStackBean beanJStack = null;
		JStackEntryBean beanJStackEntry = null;
		
		Iterator<String> iterPrevThreadIds = null;
		
		try {
			beanJStack = new JStackBean();
			beanJStack.setModType("JSTACK");
			beanJStack.setType("JSTACK");
			beanJStack.setGUID( Constants.JSTACK_AGENT_GUID );
			beanJStack.setDateTime( lExecutionCompletedOn );
			
			for(String line: alStackLines) {
				line = line.trim();
				
				if( ! bStackStarts ) {
					matcher = pattern.matcher(line);
					matches = matcher.matches();
				} else {
					matches = false;
				}
				
				// Thread's starting pattern : "Timer-3" daemon prio=6 tid=0x000000000b265000 nid=0x198c in Object.wait() [0x000000000d2ef000]
				if( matches ) {
					//System.out.println("matched: "+line);
					
					bStackStarts = true;
					
					// get the OS level Thread-Id
					strJVMThreadId = matcher.group( 2 );
					//strUniqueThreadId = (Integer.parseInt(strJVMThreadId.replace("0x", ""), 16))+"";
					strUniqueThreadId = getNewThreadId( strJVMThreadId )+"";
					
					//strThreadStatus = matcher.group( 3 );
					//System.out.println("strThreadStatus = "+strThreadId+" <> "+strThreadStatus);
					
					beanJStack.addThreadTimeTaken(strUniqueThreadId, lExecutionCompletedOn);
					beanJStack.addNewThread(strUniqueThreadId);
					//hmThreadStatus.put(strThreadId, strThreadStatus);
					
				}
				// if it empty line then, Thread stack is completed.
				else if( line.length() == 0 ) {
					
					// Arrange the Stack entries
					alStack = beanJStack.getStackEntries(strUniqueThreadId);
					nIndex = 0;
					
					if( alStack != null ) {
						
						// Update the hierarchy, as the Array elements were added in reverse order
						for(int nHierarcy=0; nHierarcy < alStack.size(); nHierarcy++ ) {
							beanJStackEntry = alStack.get(nHierarcy);
							beanJStackEntry.setHierarchy( nHierarcy );
						}
						
						// Compare the previous & current StackTrace.
						// So this block is not for Thread's first-ever sample.
						if( prevJStackBean != null && prevJStackBean.getStackEntries(strUniqueThreadId) != null ) {
							// Find max-matching position
							nThreadMatchingHierarchy = JStackBean.getMismatchOn(prevJStackBean.getStackEntries(strUniqueThreadId), alStack);
							
							// if previous-sample & current-sample has same function being called, then assign execution-time.
							if( nThreadMatchingHierarchy >= -1 ) {
								for(nIndex = 0; nIndex <= nThreadMatchingHierarchy; nIndex++) {
									/*
									// Get time taken for previous sample. Adding current execution's time will give total time taken.
									//lPrevTimeTaken = prevJStackBean.getStackEntries(strThreadId).get(nIndex).getPrevCollecElapsedTime();
									
									// Thread's first-ever sample will be given JStack-Execution time as life time (CollecElapsedTime).
									// So Second-sample's scenario, must calculate the process-time, directly, as difference between Previous-sample's sample-time and Current-sample's sample-time.
									//if( lPrevTimeTaken == null ) {
									*/
										alStack.get(nIndex).setPrevCollecElapsedTime( lExecutionCompletedOn - lPrevExecutionCompletedOn );
									/*
									//}
									// From third sample, add the latest process-time with previous-sample's execution-time.
									// This is the time for-which Function is being alive.
									else {
										alStack.get(nIndex).setPrevCollecElapsedTime(
												lPrevTimeTaken
												+ ( lExecutionCompletedOn - lPrevExecutionCompletedOn )
											);
									} */
									
									// For all function-calls, update the latest execution-time
									alStack.get(nIndex).setLastUsedOn( lExecutionCompletedOn );
								}
							}
						}
						// For Thread's first-sample and For current-sample's new function-calls,
						// assign Execution-Time
						for(;nIndex < alStack.size(); nIndex++) {
							// For non-first sample's new function-calls (i.e) nThreadMatchingHierarchy >  nIndex
							if( prevJStackBean != null && prevJStackBean.getStackEntries(strUniqueThreadId) != null ) {
								alStack.get(nIndex).setPrevCollecElapsedTime( lExecutionCompletedOn - lPrevExecutionCompletedOn );
							}
							// for Thread's first-sample
							else {
								alStack.get(nIndex).setPrevCollecElapsedTime( 0l );
							}
							
							// For all function-calls, update the latest execution-time
							alStack.get(nIndex).setLastUsedOn( lExecutionCompletedOn );
						}
					}
					
					// reset variables
					alStack = null;
					beanJStackEntry = null;
					
					bStackStarts = false;
					
					strUniqueThreadId = null;
					strJVMThreadId = null;
					
				}
				// for all other lines: check for Thread-State (or) Thread stack-tree.
				else if( bStackStarts ) {
					
					// Get Thread Status
					if( line.startsWith("java.lang.Thread.State") ) {
						/*
						 * Samples:
							waiting on condition;TIMED_WAITING (sleeping)
							runnable;RUNNABLE
							waiting on condition;RUNNABLE
							runnable
							waiting on condition
						 */
						//hmThreadStatus.put(strThreadId, strThreadStatus+";"+line.substring("java.lang.Thread.State: ".length(), line.length()) );
					}
					// update lock status
					else if( line.startsWith("- ") ) {
						//beanJStackEntry = beanJStack.getStackEntry( alStack.size()-1 );
						//beanJStackEntry.setLockDescription(line);
					}
					// add Stack entry
					else {
						
						strFunctionName = line.substring( line.indexOf("at ")+3, line.indexOf('(') );
						
						if( ! UtilsFactory.isStartingWith(saIgnoreClasses, strFunctionName) 
								&& ! UtilsFactory.isStartingWith(Constants.JSTACK_EXCLUDE_PACKAGES, strFunctionName) 
						) {
							//System.out.println(line);
							//if( strFunctionName.contains("megacrm") ) {
							//	System.out.println("JVM-Thread >> "+strJVMThreadId+" <> "+strUniqueThreadId);
							//}
							
							// Add this Thread-Id to verify whether ThreadId is repeating.
							hsJVMThreadIds.add( strJVMThreadId );
							
							// Create Bean for the StackTrace entry(line)
							beanJStackEntry = new JStackEntryBean();
							
							beanJStackEntry.setFunctionName( strFunctionName );
							beanJStackEntry.setLineNo( line.contains(":") ? Integer.parseInt( line.substring( line.indexOf(":")+1, line.indexOf(')') ) ): null );
							beanJStackEntry.setPosition("");
							beanJStackEntry.setStackCollectionTime( lExecutionCompletedOn );;
							beanJStackEntry.setThreadId( Long.parseLong(strUniqueThreadId) );
							beanJStackEntry.setLastUsedOn( lExecutionCompletedOn );
							
							beanJStack.addStackReverseEntry(strUniqueThreadId, beanJStackEntry);
						}
					}
				}
			}
			
			// Write the StackTraces, if anything is available
			if( beanJStack.isStackTraceAvailable() ) { 
				LogManagerExtended.logJStackOutput("jstack###"+beanJStack.toString());
				System.out.println(UtilsFactory.nowFormattedDate()+" :: Logged Stack-Trace.");
			} else {
				System.out.println(UtilsFactory.nowFormattedDate()+" :: No Stack-Trace package available. Probably, no activity in Application-Server; and all function-calls are default and excluded.");
			}
			
			// Identify the missing Thread-Ids, from previous Snapshot
			// And clear their Unique Thread-Id
//			System.out.println("JVM-Thread hsPrevJVMThreadIds: "+hsPrevJVMThreadIds);
//			System.out.println("JVM-Thread hsJVMThreadIds: "+hsJVMThreadIds);
			if( hsPrevJVMThreadIds != null ) {
				iterPrevThreadIds = hsPrevJVMThreadIds.iterator();
				while( iterPrevThreadIds.hasNext() ) {
					strJVMThreadId = iterPrevThreadIds.next();
					
					if( ! hsJVMThreadIds.contains( strJVMThreadId ) ) {
//						System.out.println("JVM-Thread removing : "+strJVMThreadId);
						hmUniqueThreadIds.remove(strJVMThreadId);
					}
				}
			}
			
		} finally {
			UtilsFactory.clearCollectionHieracy(prevJStackBean);
		}
		
		return new Object[] {beanJStack, lExecutionCompletedOn, hsJVMThreadIds};
	}
	
	/**
	 * Get a new ThreadId for the given JVM's machine-level Thread-Id.
	 * If the Thread-Id is already available then, return the previously assigned number.
	 * 
	 * This is created as JVM reuses the Thread from Thread-Pool, thus Thread-Id is repeated.
	 * So we can not segregate the StackTrace.
	 * 
	 * While starting this agent, System's nanoSecond reference is stored. Each time it is incremented.
	 * 
	 * @param strJVMThreadId
	 * @return
	 */
	private long getNewThreadId(String strJVMThreadId) {
		long lNewThreadId = 0;
		
		try {
			if( hmUniqueThreadIds.containsKey(strJVMThreadId) ) {
				lNewThreadId = hmUniqueThreadIds.get(strJVMThreadId);
			} else {
				lNewThreadId = ++lNextUniqueThreadId;
				hmUniqueThreadIds.put(strJVMThreadId, lNewThreadId);
//				System.out.println("JVM-Thread adding : "+strJVMThreadId+" <> "+lNewThreadId);
			}
		} catch (Throwable th) {
			System.out.println("Exception in getNewThreadId: "+th.getMessage());
			th.printStackTrace();
		}
		
		return lNewThreadId;
	}
	
	public Object[] monitorJStack(JStackBean prevJStackBean, Long lPrevExecutionCompletedOn, HashSet<String> hsPrevJVMThreadIds) {
		ArrayList<String> alStacks = null;
		long lExecutionCompletedOn;
		Object[] objJStackProcess = null;
		
		System.out.println("executing...");
		
		alStacks = collectStack();
		
		lExecutionCompletedOn = new Date().getTime();
//		System.out.println("JStack process completed <> "+ UtilsFactory.nowFormattedDate()+" <> Diff: "+(l2-lPrevExecutionCompletedOn));
		if( lPrevExecutionCompletedOn == null ) {
			System.out.println("JStack process completed in "+(-1)+" ms.");
		} else {
			System.out.println("JStack process completed in "+(lExecutionCompletedOn-lPrevExecutionCompletedOn)+" ms.");
		}
		
		objJStackProcess = processStack(alStacks, lExecutionCompletedOn, prevJStackBean, lPrevExecutionCompletedOn, hsPrevJVMThreadIds);
		
		return objJStackProcess;
	}
	
	public static void main(String[] args) {
//		ArrayList<String> alStacks = null;
//		
//		for(int i=0;i<20;i++) {
//			System.out.println("\n\nexecuting... "+i);
//			alStacks = collectStack();
//		}
		
//		processStack(alStacks, new Date() );
		
		
		/*
		 * Takes control of the main thread itself.
		 * Things even in finally block are not getting executed.
		 * Prints the "jstack -F"'s o/p in the console. So need to read that as an extra work.
		 *
		long l1, l2;
		sun.jvm.hotspot.tools.JStack js = new sun.jvm.hotspot.tools.JStack();
		l1 = new Date().getTime();
		try {
			System.out.println("Process started.");
			js.main(new String[] {"5656"});
		} catch(Throwable th) {
			th.printStackTrace();
		} finally {
			l2 = new Date().getTime();
			try {
				Thread.sleep(10000l);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
			System.out.println("Process completed <> "+ UtilsFactory.nowFormattedDate()+" <> Diff: "+(l2-l1));
		}
		*/
		
//		String str = "\"NioBlockingSelector.BlockPoller-1\" #14 daemon prio=5 os_prio=0 tid=0x0000000057f68000 nid=0x127c runnable [0x00000000596af000]";
		String str = "\"MySQL Statement Cancellation Timer\" #48 daemon prio=5 os_prio=0 tid=0x0000000057f38800 nid=0x1d90 in Object.wait() [0x000000005e60f000]";
		
		String patternString = "\".+\"\\s(.+\\s)?prio=\\d+\\s.*tid=\\w+\\s.*nid=(\\w+)\\s([\\w+.\\(\\) ]+)(\\s\\[\\w+\\])?$";
		Pattern pattern = Pattern.compile(patternString);
		Matcher matcher = null;
		matcher = pattern.matcher(str);
		
		System.out.println( matcher.matches()+" <> "+matcher.group(0)+" <> "+matcher.group(1)+" <> "+matcher.group(2)+" <> "+matcher.group(3)+" <> ");
	}
}
