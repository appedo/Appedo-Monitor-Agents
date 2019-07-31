package com.appedo.agent.bean;

import java.util.HashMap;

import net.sf.json.JSONArray;

public class SlaCounterBean {
private static HashMap<String,Object>  hmSlaCountersBean = new HashMap<String,Object>();
	
	/**
	 * @return the hmCountersBean
	 */
	public static JSONArray getSLACountersBean(String strGUID) {
		
		return (JSONArray) hmSlaCountersBean.get(strGUID);
	}
	
	/**
	 * @param hmCountersBean the hmCountersBean to set
	 */
	public static void setJoSlaCountersBean(String strGUID, JSONArray jaSlaCounterSet) {
		hmSlaCountersBean.put(strGUID, jaSlaCounterSet);
	}
}
