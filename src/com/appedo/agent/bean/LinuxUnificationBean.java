package com.appedo.agent.bean;

import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashMap;

import com.appedo.agent.utils.UtilsFactory;

public class LinuxUnificationBean {
	private String mod_type, type, guid;
	private long lDateTime;
	private Date dDateTime;
	
	private LinkedHashMap<String, ArrayList<LinuxUnificationCounterBean> > hmCounterEntries = null;
	private LinkedHashMap<String, ArrayList<LinuxUnificationSLACounterBean> > hmSLACounterEntries = null;
	
	private ArrayList<LinuxUnificationSlowQueryBean> alSlowQuerys = new ArrayList<LinuxUnificationSlowQueryBean>();
	
	public LinuxUnificationBean() {
		hmCounterEntries = new LinkedHashMap<String, ArrayList<LinuxUnificationCounterBean> >();
		hmSLACounterEntries = new LinkedHashMap<String, ArrayList<LinuxUnificationSLACounterBean> >();
	}
	
	public String getMod_type() {
		return mod_type;
	}

	public void setMod_type(String mod_type) {
		this.mod_type = mod_type;
	}

	public String getType() {
		return type;
	}

	public void setType(String type) {
		this.type = type;
	}

	public String getGuid() {
		return guid;
	}

	public void setGuid(String guid) {
		this.guid = guid;
	}

	public long getlDateTime() {
		return lDateTime;
	}

	public void setlDateTime(long lDateTime) {
		this.lDateTime = lDateTime;
	}

	public Date getdDateTime() {
		return dDateTime;
	}

	public void setdDateTime(Date dDateTime) {
		this.dDateTime = dDateTime;
	}
	
	public void addNewCounter(String strCounterId) {
		hmCounterEntries.put(strCounterId, new ArrayList<LinuxUnificationCounterBean>());
	}
	
	public void addNewSLACounter(String strSLAId) {
		hmSLACounterEntries.put(strSLAId, new ArrayList<LinuxUnificationSLACounterBean>());
	}
	
	public boolean isCountersValueAvailable() {
		// Group all the StackEntries
		ArrayList<LinuxUnificationCounterBean> beanCountersAll = new ArrayList<LinuxUnificationCounterBean>();
		
		for(ArrayList<LinuxUnificationCounterBean> alCounters: hmCounterEntries.values()) {
			beanCountersAll.addAll( alCounters );
		}
		
		return beanCountersAll.size()>0?true:false;
	}
	
	public boolean isSLACountersValueAvailable() {
		// Group all the StackEntries
		ArrayList<LinuxUnificationSLACounterBean> beanSLACountersAll = new ArrayList<LinuxUnificationSLACounterBean>();
		
		for(ArrayList<LinuxUnificationSLACounterBean> alSLACounters: hmSLACounterEntries.values()) {
			beanSLACountersAll.addAll( alSLACounters );
		}
		
		return beanSLACountersAll.size()>0?true:false;
	}
	
	public boolean isSlowQueriesAvailable() {
		return this.alSlowQuerys.size()>0?true:false;
	}
	
	public void addCounterEntry(String strCounterId, LinuxUnificationCounterBean beanLinuxUnificationCounter) {
		ArrayList<LinuxUnificationCounterBean> alCounterEntries = hmCounterEntries.get(strCounterId);
		
		alCounterEntries.add(0, beanLinuxUnificationCounter);
	}
	
	public void addSLACounterEntry(String strCounterId, LinuxUnificationSLACounterBean beanLinuxUnificationSLACounter) {
		ArrayList<LinuxUnificationSLACounterBean> alSLACounterEntries = hmSLACounterEntries.get(strCounterId);
		
		alSLACounterEntries.add(0, beanLinuxUnificationSLACounter);
	}
	
	public void addSlowQueryEntry(LinuxUnificationSlowQueryBean beanLinuxUnificationSlowQuery) {
		this.alSlowQuerys.add(beanLinuxUnificationSlowQuery);
	}
	
	public String toString(String typeSet) {
		
		StringBuilder sbJSON = new StringBuilder();
		
		if(typeSet.contains("SlowQuerySet")) {
			
			sbJSON	.append("{")
					.append("\"mod_type\":\"").append(mod_type).append("\",")
					.append("\"type\":\"").append(typeSet).append("\",")
					.append("\"guid\":\"").append(guid).append("\",")
					.append("\"datetime\":\"").append( UtilsFactory.formatDateWithTimeZone(dDateTime) ).append("\",")
					.append("\"SlowQuerySet\":").append( this.alSlowQuerys ).append("")
					.append("}");
					
		}else if(typeSet.contains("SLASet")) {
			// Group all the SLAcounterEntries
			ArrayList<LinuxUnificationSLACounterBean> beanAllSLACounters = new ArrayList<LinuxUnificationSLACounterBean>();
			
			for(ArrayList<LinuxUnificationSLACounterBean> alcounters : hmSLACounterEntries.values()) {
				beanAllSLACounters.addAll(alcounters);
			}
			
			sbJSON	.append("{")
					.append("\"mod_type\":\"").append(mod_type).append("\",")
					.append("\"type\":\"").append(type).append("\",")
					.append("\"guid\":\"").append(guid).append("\",")
					.append("\"datetime\":\"").append( UtilsFactory.formatDateWithTimeZone(dDateTime) ).append("\",")
					.append("\"SLASet\":").append( beanAllSLACounters ).append("")
					.append("}");
					
		}else {
			// Group all the counterEntries
			ArrayList<LinuxUnificationCounterBean> beanAllCounters = new ArrayList<LinuxUnificationCounterBean>();
			
			for(ArrayList<LinuxUnificationCounterBean> alcounters : hmCounterEntries.values()) {
				beanAllCounters.addAll(alcounters);
			}
			sbJSON	.append("{")
					.append("\"mod_type\":\"").append(mod_type).append("\",")
					.append("\"type\":\"").append(type).append("\",")
					.append("\"guid\":\"").append(guid).append("\",")
					.append("\"datetime\":\"").append( UtilsFactory.formatDateWithTimeZone(dDateTime) ).append("\",")
					.append("\"MetricSet\":").append( beanAllCounters ).append("")
					.append("}");
		}
		
		return sbJSON.toString();
	}
	
	public void destroy() {
		UtilsFactory.clearCollectionHieracy(hmCounterEntries);
	}
}
