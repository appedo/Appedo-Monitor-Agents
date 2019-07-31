package com.appedo.agent.bean;

import com.appedo.agent.utils.UtilsFactory;

public class LinuxUnificationCounterBean {
	private long counter_type;
	private double counter_value;
	private String exception, process_name;
	
	
	public long getCounter_type() {
		return counter_type;
	}
	public void setCounter_type(long counter_type) {
		this.counter_type = counter_type;
	}
	public double getCounter_value() {
		return counter_value;
	}
	public void setCounter_value(double counter_value) {
		this.counter_value = counter_value;
	}
	public String getException() {
		return exception;
	}
	public void setException(String exception) {
		this.exception = exception;
	}
	public String getProcess_name() {
		return process_name;
	}
	public void setProcess_name(String process_name) {
		this.process_name = process_name;
	}
	
	public String toString() {
		StringBuilder sbJSON = new StringBuilder();
		
		sbJSON	.append("{")
				.append("\"counter_type\":\"").append(counter_type).append("\",")
				.append("\"counter_value\":\"").append(counter_value).append("\",")
				.append("\"exception\":\"").append( UtilsFactory.replaceNull(exception,"") ).append("\",")
				.append("\"process_name\":\"").append( UtilsFactory.replaceNull(process_name,"") ).append("\"")
				.append("}");
				
		return sbJSON.toString();
	}

}
