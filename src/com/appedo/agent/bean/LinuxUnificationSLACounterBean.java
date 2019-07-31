package com.appedo.agent.bean;

public class LinuxUnificationSLACounterBean {

	private String breached_severity;
	private int counter_id, sla_id;
	private long critical_threshold_value, warning_threshold_value;
	private boolean is_above, percentage_calculation;
	private double received_value;
	
	
	public String getBreached_severity() {
		return breached_severity;
	}
	public void setBreached_severity(String breached_severity) {
		this.breached_severity = breached_severity;
	}
	public int getCounter_id() {
		return counter_id;
	}
	public void setCounter_id(int counter_id) {
		this.counter_id = counter_id;
	}
	public int getSla_id() {
		return sla_id;
	}
	public void setSla_id(int sla_id) {
		this.sla_id = sla_id;
	}
	public long getCritical_threshold_value() {
		return critical_threshold_value;
	}
	public void setCritical_threshold_value(long critical_threshold_value) {
		this.critical_threshold_value = critical_threshold_value;
	}
	public long getWarning_threshold_value() {
		return warning_threshold_value;
	}
	public void setWarning_threshold_value(long warning_threshold_value) {
		this.warning_threshold_value = warning_threshold_value;
	}
	public boolean isIs_above() {
		return is_above;
	}
	public void setIs_above(boolean is_above) {
		this.is_above = is_above;
	}
	public boolean isPercentage_calculation() {
		return percentage_calculation;
	}
	public void setPercentage_calculation(boolean percentage_calculation) {
		this.percentage_calculation = percentage_calculation;
	}
	public double getReceived_value() {
		return received_value;
	}
	public void setReceived_value(double received_value) {
		this.received_value = received_value;
	}
	
	public String toString() {
		StringBuilder sbJSON = new StringBuilder();
		
		sbJSON	.append("{")
				.append("\"breached_severity\":\"").append(breached_severity).append("\",")
				.append("\"counter_id\":\"").append(counter_id).append("\",")
				.append("\"critical_threshold_value\":\"").append(critical_threshold_value).append("\",")
				.append("\"is_above\":\"").append(is_above).append("\",")
				.append("\"percentage_calculation\":\"").append(percentage_calculation).append("\",")
				.append("\"received_value\":\"").append(received_value).append("\",")
				.append("\"sla_id\":\"").append(sla_id).append("\",")
				.append("\"warning_threshold_value\":\"").append(warning_threshold_value).append("\"")
				.append("}");
				
		return sbJSON.toString();
	}
}
