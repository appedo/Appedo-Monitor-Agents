package com.appedo.agent.bean;

import com.appedo.agent.utils.UtilsFactory;

public class JStackEntryBean {
	private String strFunctionName, strPosition;
	private Integer nHierarchy, nLineNo;
	private Long lThreadId, lPrevCollecElapsedTime;
	private long lStackCollectionTime, lLastUsedOn;
	
	public String getFunctionName() {
		return strFunctionName;
	}
	public void setFunctionName(String strFunctionName) {
		this.strFunctionName = strFunctionName;
	}
	
	public int getHierarchy() {
		return nHierarchy;
	}
	public void setHierarchy(int nHierarchy) {
		this.nHierarchy = nHierarchy;
	}
	
	public Integer getLineNo() {
		return nLineNo;
	}
	public void setLineNo(Integer nLineNo) {
		this.nLineNo = nLineNo;
	}
	
	public String getPosition() {
		return strPosition;
	}
	public void setPosition(String strPosition) {
		this.strPosition = strPosition;
	}
	
	public Long getPrevCollecElapsedTime() {
		return lPrevCollecElapsedTime;
	}
	public void setPrevCollecElapsedTime(Long lPrevCollecElapsedTime) {
		this.lPrevCollecElapsedTime = lPrevCollecElapsedTime;
	}
	
	public long getStackCollectionTime() {
		return lStackCollectionTime;
	}
	public void setStackCollectionTime(long lStackCollectionTime) {
		this.lStackCollectionTime = lStackCollectionTime;
	}
	
	public Long getThreadId() {
		return lThreadId;
	}
	public void setThreadId(long lThreadId) {
		this.lThreadId = lThreadId;
	}
	
	public long getLastUsedOn() {
		return lLastUsedOn;
	}
	public void setLastUsedOn(long lLastUsedOn) {
		this.lLastUsedOn = lLastUsedOn;
	}
	
	public String toString() {
		StringBuilder sbJSON = new StringBuilder();
		
		sbJSON	.append("{")
				.append("\"function_name\":\"").append(strFunctionName).append("\",")
				.append("\"hierarchy\":\"").append(nHierarchy).append("\",")
				.append("\"line_no\":\"").append( UtilsFactory.replaceNull(nLineNo,"") ).append("\",")
				.append("\"position\":\"").append( UtilsFactory.replaceNull(strPosition,"") ).append("\",")
				.append("\"prev_collec_elapsed_time\":\"").append( lPrevCollecElapsedTime ).append("\",")
				.append("\"stack_collection_time\":\"").append( UtilsFactory.formatDateWithTimeZone(lStackCollectionTime) ).append("\",")
				.append("\"thread_id\":\"").append(lThreadId).append("\"")
				.append("}");
		
		
		return sbJSON.toString();
	}
	
	public void destroy() {
		
	}
}
