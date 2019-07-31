package com.appedo.agent.bean;

import java.util.HashMap;

import net.sf.json.JSONArray;

public class AgentCounterBean {
	
	private static HashMap<String,Object>  hmCountersBean = new HashMap<String,Object>();
	
	private static String strSlowQry;
	
	public static JSONArray getCountersBean(String strGUID) {		
		return (JSONArray) hmCountersBean.get(strGUID);
	}
	public static void setCountersBean(String strGUID, JSONArray jaNewCounterSet) {
		hmCountersBean.put(strGUID, jaNewCounterSet);
	}
	
	public static String getLastCollectedSlowQueries() {
		return strSlowQry;
	}
	public static void setLastCollectedSlowQueries(String strSlowQry) {
		AgentCounterBean.strSlowQry = strSlowQry;
	}
}
