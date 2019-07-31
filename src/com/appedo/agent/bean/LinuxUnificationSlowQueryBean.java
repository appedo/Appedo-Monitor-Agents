package com.appedo.agent.bean;

import com.appedo.agent.utils.UtilsFactory;

public class LinuxUnificationSlowQueryBean {

	private long duration_ms;
	private int calls;
	private String  query;
	
	
	public long getDuration_ms() {
		return duration_ms;
	}
	public void setDuration_ms(long Duration_ms) {
		this.duration_ms = Duration_ms;
	}
	public double getCalls() {
		return calls;
	}
	public void setCalls(int Calls) {
		this.calls = Calls;
	}
	public String getQuery() {
		return query;
	}
	public void setQuery(String SlowQueries) {
		this.query = SlowQueries;
	}
	
	public String toString() {
		StringBuilder sbJSON = new StringBuilder();
		
		sbJSON	.append("{")
				.append("\"duration_ms\":\"").append(duration_ms).append("\",")
				.append("\"calls\":\"").append(calls).append("\",")
				.append("\"query\":\"").append( UtilsFactory.replaceNull(query,"") ).append("\"")
				.append("}");
				
		return sbJSON.toString();
	}

}
