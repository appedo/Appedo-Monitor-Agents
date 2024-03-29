package com.appedo.agent.manager;

import org.jboss.as.controller.client.ModelControllerClient;
import org.jboss.dmr.ModelNode;

public class DomainModeModel
  {
   public void getWebSubsystemRuntimeDetails(ModelControllerClient client,String hostControllerName,String serverName)
    {
       System.out.println("\n\n\n************************************************");
       System.out.println("Web Subsystem Runtime Details");
       System.out.println("************************************************");
       try{
             ModelNode op = new ModelNode();
             op.get("operation").set("read-resource");
             ModelNode address = op.get("address");
             address.add("host", hostControllerName);
             address.add("server", serverName);
             address.add("subsystem", "web");
             address.add("connector", "http");
             op.get("recursive").set(true);
             op.get("include-runtime").set(true);
             op.get("operations").set(true);

             ModelNode returnVal = client.execute(op);
             String result=returnVal.get("result").toString();
             String path="/host="+hostControllerName+"/server="+serverName+"/subsystem=web/connector=http";
             if(result.equals("undefined"))
                {
                   System.out.println("\n\tCheck ["+path+"] ** HostController HostController or Server may not be Running");
                }
             else
                {
                   System.out.println(returnVal.get("result").toString());
                }
          }
        catch(Exception e)
          {
             e.printStackTrace();
             System.out.println("getWebSubsystemRuntimeDetails Failed: "+e);
          }
    }

   public void testNonXADataSource(ModelControllerClient client,String hostControllerName,String serverName,String dataSourceName)
    {
       System.out.println("\n\n\n************************************************");
       System.out.println("DataSource \""+dataSourceName+"\" TestResults");
       System.out.println("************************************************");

       try{
             ModelNode op = new ModelNode();
             op.get("operation").set("test-connection-in-pool");
             ModelNode address = op.get("address");
             address.add("host", hostControllerName);
             address.add("server", serverName);
             address.add("subsystem", "datasources");
             address.add("data-source", dataSourceName);
             op.get("operations").set(true);

             ModelNode returnVal = client.execute(op);
             String result=returnVal.get("result").toString();
             String path="/host="+hostControllerName+"/server="+serverName+"/subsystem=datasources/data-source="+dataSourceName;
             if(result.equals("[true]"))
                {
                   System.out.println("\n\tNon XA DataSource \""+dataSourceName+"\" is RUNNING Successfully.");
                }
             else if(result.equals("[false]"))
                {
                   System.out.println("\n\tNon XA DataSource \""+dataSourceName+"\" is TEST FAILED Successfully. Check PATH exist or not? ["+path+"]");
                }
             else if(result.equals("undefined"))
                {
                   System.out.println("\n\tDataSource \""+dataSourceName+"\" might not be deployed on this ["+path+"]  ** HostController or Server may not be Running"+"Please Check the DataSource \""+dataSourceName+"\" name, It may be INCORRECT. \n\tCheck PATH exist or not? ["+path+"]");
                }
          }
        catch(Exception e) {
             e.printStackTrace();
             System.out.println("testNonXADataSource Failed: "+e);
          }
    }

   public void monitorApplicationStatistics(ModelControllerClient client,String hostControllerName,String serverName,String applicationName)
    {
       System.out.println("\n\n\n******************************************************");
       System.out.println("Application \""+applicationName+"\" RuntimeStatistics");
       System.out.println("******************************************************");
       try{
             ModelNode op = new ModelNode();
             op.get("operation").set("read-resource");
             ModelNode address = op.get("address");
             address.add("host", hostControllerName);
             address.add("server", serverName);
             address.add("deployment", applicationName);
             op.get("recursive").set(true);
             op.get("include-runtime").set(true);
             op.get("include-defaults").set(true);
             op.get("operations").set(true);

             ModelNode returnVal = client.execute(op);
             String result=returnVal.get("result").toString();
             String path="/host="+hostControllerName+"/server="+serverName+"/deployment="+applicationName;
             if(result.equals("undefined"))
                {
                   System.out.println("\n\tApplication \""+applicationName+ "\" might not have deployed on this host="+hostControllerName+"/server="+serverName+"  ** HostController or Server may not be Running. Check PATH exist or not? ["+path+"]");
                }
             else
                {
                   System.out.println("\n\tMonitoring \""+applicationName+ "\""+result);
                }
          }
        catch(Exception e)
          {
             e.printStackTrace();
             System.out.println("testNonXADataSource Failed: "+e);
          }
    }
  }