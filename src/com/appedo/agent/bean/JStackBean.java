package com.appedo.agent.bean;

import java.util.ArrayList;
import java.util.LinkedHashMap;

import com.appedo.agent.utils.UtilsFactory;

public class JStackBean {
	private String strModType, strType, strGUID;
	private long lDateTime;
	
	private LinkedHashMap<String, Long> hmStackEntriesTimetaken = null;
	private LinkedHashMap<String, ArrayList<JStackEntryBean> > hmStackEntries = null;
	
	public JStackBean() {
		hmStackEntriesTimetaken = new LinkedHashMap<String, Long>();
		hmStackEntries = new LinkedHashMap<String, ArrayList<JStackEntryBean> >();
	}
	
	
	public String getModType() {
		return strModType;
	}
	public void setModType(String strModType) {
		this.strModType = strModType;
	}
	
	public String getType() {
		return strType;
	}
	public void setType(String strType) {
		this.strType = strType;
	}
	
	public String getGUID() {
		return strGUID;
	}
	public void setGUID(String strGUID) {
		this.strGUID = strGUID;
	}
	
	public long getDateTime() {
		return lDateTime;
	}
	public void setDateTime(long lDateTime) {
		this.lDateTime = lDateTime;
	}
	
	public void addThreadTimeTaken(String strThreadId, long nTimeTaken) {
		hmStackEntriesTimetaken.put(strThreadId, nTimeTaken);
	}
	public long getThreadTimeTaken(String strThreadId) {
		return hmStackEntriesTimetaken.get(strThreadId);
	}
	
	public void addNewThread(String strThreadId) {
		hmStackEntries.put(strThreadId, new ArrayList<JStackEntryBean>());
	}
	public ArrayList<JStackEntryBean> getStackEntries(String strThreadId) {
		ArrayList<JStackEntryBean> alStackEntries = hmStackEntries.get(strThreadId);
		
		return alStackEntries;
	}
	public JStackEntryBean getStackEntry(String strThreadId, int nPosition) {
		ArrayList<JStackEntryBean> alStackEntries = hmStackEntries.get(strThreadId);
		
		return alStackEntries.get(nPosition);
	}
	public void addStackReverseEntry(String strThreadId, JStackEntryBean beanJStackEntry) {
		ArrayList<JStackEntryBean> alStackEntries = hmStackEntries.get(strThreadId);
		
		alStackEntries.add(0, beanJStackEntry);
	}
	
	/**
	 * Find whether any Stack-Trace entries is available in the data-structure.
	 * 
	 * @return
	 */
	public boolean isStackTraceAvailable() {
		// Group all the StackEntries
		ArrayList<JStackEntryBean> beanJSEAll = new ArrayList<JStackEntryBean>();
		
		for(ArrayList<JStackEntryBean> alJSE: hmStackEntries.values()) {
			beanJSEAll.addAll( alJSE );
		}
		
		return beanJSEAll.size()>0?true:false;
	}
	
	/**
	 * Print the whole data-structure.
	 * All threads stack-trace are merged into single stack array-list.
	 */
	@Override
	public String toString() {
		// Group all the StackEntries
		ArrayList<JStackEntryBean> beanJSEAll = new ArrayList<JStackEntryBean>();
		
		for(ArrayList<JStackEntryBean> alJSE: hmStackEntries.values()) {
			beanJSEAll.addAll( alJSE );
		}
		
		
		StringBuilder sbJSON = new StringBuilder();
		
		sbJSON	.append("{")
				.append("\"mod_type\":\"").append(strModType).append("\",")
				.append("\"type\":\"").append(strType).append("\",")
				.append("\"guid\":\"").append(strGUID).append("\",")
				.append("\"datetime\":\"").append( UtilsFactory.formatDateWithTimeZone(lDateTime) ).append("\",")
				.append("\"JSTACK\":").append( beanJSEAll ).append("")
				.append("}");
		
		
		return sbJSON.toString();
	}
	
	public static int getMismatchOn(ArrayList<JStackEntryBean> prevCollection, ArrayList<JStackEntryBean> currentCollection) {
		int nIndex = 0;
		for(; nIndex < currentCollection.size(); nIndex++) {
			if( nIndex < prevCollection.size() ) {
				if( ( ! currentCollection.get(nIndex).getFunctionName().equals( prevCollection.get(nIndex).getFunctionName() ) ) 
					|| ( currentCollection.get(nIndex).getLineNo() != null && currentCollection.get(nIndex).getLineNo() != null 
						&& currentCollection.get(nIndex).getLineNo().intValue() != prevCollection.get(nIndex).getLineNo().intValue()
					)
				) {
					return --nIndex;
				}
			} else {
				return --nIndex;
			}
		}
		
		return --nIndex;
	}
	
	public void destroy() {
		UtilsFactory.clearCollectionHieracy(hmStackEntries);
		
		UtilsFactory.clearCollectionHieracy(hmStackEntriesTimetaken);
	}
}
