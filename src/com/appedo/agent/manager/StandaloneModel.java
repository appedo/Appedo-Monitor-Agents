package com.appedo.agent.manager;

import java.io.IOException;
import java.util.Collections;
import java.util.List;

import org.jboss.as.cli.CommandContext;
import org.jboss.as.cli.CommandContextFactory;
import org.jboss.as.cli.CommandLineException;
import org.jboss.as.controller.client.ModelControllerClient;
import org.jboss.as.controller.client.helpers.ClientConstants;
import org.jboss.dmr.ModelNode;

import com.appedo.agent.utils.Constants;
import com.appedo.agent.utils.UtilsFactory;

public class StandaloneModel {
	
	public ModelNode monitorTransactionStatistics(ModelControllerClient client) {
		ModelNode returnVal = null;
		
		try{
			ModelNode op = new ModelNode();
			op.get("operation").set("read-resource");
			ModelNode address = op.get("address");
			address.add("subsystem", "transactions");
			op.get("include-runtime").set(true);
			
			returnVal = client.execute(op);
			String result = returnVal.get("result").toString();
			
			if(result.equals("undefined"))
				System.out.println("Check [/subsystem=transactions] ** Server may not be Running");
			//else
			//	System.out.println(returnVal.get("result").toString());
		} catch(Exception e) {
			System.out.println("Exception in monitorTransactionStatistics: "+e.getMessage());
			e.printStackTrace();
		}
		
		return returnVal;
	}
	
	public ModelNode getWebSubsystemRuntimeDetails(ModelControllerClient client){
		ModelNode returnVal = null;
		
		try{
			ModelNode op = new ModelNode();
			op.get("operation").set("read-resource");
			op.get("recursive").set(true);
			op.get("include-runtime").set(true);
			op.get("operations").set(true);
			
			ModelNode address = op.get("address");
			address.add("subsystem", "web");
			address.add("connector", "http");
			
			
			returnVal = client.execute(op);
			String result = returnVal.get("result").toString();
			
			if(result.equals("undefined")){
				System.out.println("\n\tCheck [/subsystem=web/connector=http] ** Server may not be Running");
			}
		}catch(Exception e){
			System.out.println("Exception in getWebSubsystemRuntimeDetails(): "+e.getMessage());
			e.printStackTrace();
		}
		
		return returnVal;
	}
	
	/**
	 * 
	 * list of all jboss web subSystem Attributes
	 * 
	 * @param client
	 * @return ModelNode
	 */
	public ModelNode getWebSubsystemRuntimeAttributes(ModelControllerClient client){
		ModelNode returnVal = null;
		
		try{
			ModelNode op = new ModelNode();
			op.get("operation").set("read-resource-description");
			op.get("recursive").set(true);
			op.get("include-runtime").set(true);
			op.get("operations").set(true);
			
			ModelNode address = op.get("address");
			address.add("subsystem", "web");
			address.add("connector", "http");
			
			
			returnVal = client.execute(op);
			String result=returnVal.get("result").toString();
			
			if(result.equals("undefined")){
				System.out.println("\n\tCheck [/subsystem=web/connector=http] ** Server may not be Running");
			}
		}catch(Exception e){
			System.out.println("Exception in getWebSubsystemRuntimeAttributes(): "+e.getMessage());
			e.printStackTrace();
		}
		
		return returnVal;
	}
	
	public ModelNode getTransactionRunTimeAttributes(ModelControllerClient client) {
		ModelNode returnVal = null;
		
		try{
			ModelNode op = new ModelNode();
			op.get("operation").set("read-resource-description");
			op.get("recursive").set(true);
			op.get("include-runtime").set(true);
			op.get("operations").set(true);
			
			ModelNode address = op.get("address");
			address.add("subsystem", "transactions");
			
			
			returnVal = client.execute(op);
			String result = returnVal.get("result").toString();
			
			if(result.equals("undefined"))
				System.out.println("Check [/subsystem=transactions] ** Server may not be Running");
			//else
			//	System.out.println(returnVal.get("result").toString());
		} catch(Exception e) {
			System.out.println("Exception in monitorTransactionStatistics: "+e.getMessage());
			e.printStackTrace();
		}
		
		return returnVal;
	}
	
	/**
	 * Get value of the CLI command
	 * 
	 * @param client
	 * @param queryString
	 * @param attributeName
	 * @return
	 * @throws Throwable
	 */
	public String getRuntimeDetails(ModelControllerClient client, String queryString, String attributeName) throws Throwable {
		ModelNode returnVal = null;
		String result = null;
		
		try{
			ModelNode op = new ModelNode();
			op.get("operation").set("read-attribute");
			op.get("name").set(attributeName);
			
			ModelNode address = op.get("address");
			
			UtilsFactory.printDebugLog(Constants.IS_DEBUG, "queryString: "+queryString);
			UtilsFactory.printDebugLog(Constants.IS_DEBUG, "attributeName: "+attributeName);
			
			for ( String segment : queryString.split("/")) {
				if( segment.length() > 0 ) {
					String[] elements = segment.split("=");
					address.add(elements[0],elements[1]);
				}
			}
			
			// Get JSON output
			returnVal = client.execute(op);
			UtilsFactory.printDebugLog(Constants.IS_DEBUG, "returnVal: "+returnVal);
			
			// Get required output from above JSON
			result = returnVal.get("result").toString();
			
			if( result.equals("undefined") ) {
				System.out.println("\n\tCheck ["+queryString+"] ** Server may not be Running");
			} else if( result.endsWith("L") ) {
				result = result.substring(0, result.length()-1);
			}
			
			UtilsFactory.printDebugLog(Constants.IS_DEBUG, "result: "+result);
			
		} catch(Throwable th){
			System.out.println("Exception in getRuntimeDetails(): "+th.getMessage());
			throw th;
		}
		
		return result;
	}
	
	public ModelNode getJbossDetails(ModelControllerClient client) {
		ModelNode returnVal = null;
		
		try{
			ModelNode op = new ModelNode();
			op.get("operation").set("read-resource");
			op.get("include-runtime").set(true);
			
			returnVal = client.execute(op);
			String result = returnVal.get("result").toString();
			
			if(result.equals("undefined"))
				System.out.println("Check [/subsystem=transactions] ** Server may not be Running");
			
		} catch(Exception e) {
			System.out.println("Exception in monitorTransactionStatistics: "+e.getMessage());
			e.printStackTrace();
		}
		
		return returnVal;
	}
	
	/**
	 * List all applications which can be monitored under the given params (Host, Port).
	 * 
	 * @param client
	 * @param applicationName
	 * @return
	 */
	public ModelNode monitorApplicationStatistics(ModelControllerClient client, String applicationName){
		ModelNode returnVal = null;
		
		try{
			ModelNode op = new ModelNode();
			op.get("operation").set("read-resource");
			final ModelNode address = op.get("address");
			address.add("deployment", applicationName+".war");
			op.get("recursive").set(true);
			op.get("include-runtime").set(true);
			op.get("include-defaults").set(true);
			op.get("operations").set(true);
			
			returnVal = client.execute(op);
			String result=returnVal.get("result").toString();
			if(result.equals("undefined")) {
				System.out.println("Application \""+applicationName+ "\" might not have deployed. ** Server may not be Running. Check PATH exist or not? [/deployment="+applicationName+"]");
			} else {
				System.out.println("Monitoring \""+applicationName+": "+result);
			}
		} catch(Exception e){
			System.out.println("monitorApplicationStatistics Failed: "+e);
			e.printStackTrace();
		}
		
		return returnVal;
	}
	
   
   public void  getPlatformJvm(ModelControllerClient client) {
		
		try{
			ModelNode operation = new ModelNode();
			operation.get("operation").set("read-resource");
			operation.get("recursive").set(true);
			operation.get("include-runtime").set(true);
			ModelNode address = operation.get("address");
			address.add("core-service", "platform-mbean");
			ModelNode returnVal = null;
			returnVal= client.execute(operation);
			
			String result = returnVal.get("result").toString();
//			ModelNode resutltNode = returnVal.get("result"); 
			
			System.out.println("result: "+result);
		} catch(Exception ee) {
			System.out.println("Exception in getPlatformJvm: "+ee.getMessage());
		}
	}
	
   /**
	* List all applications which can be monitored under the given params (Host, Port).
	* 
	* @param client
	*/
	public void listAllApplications(final ModelControllerClient client /*, final String deploymentName */ ) {
		final ModelNode op = new ModelNode();
		op.get("operation").set("read-children-names");
		op.get("child-type").set(ClientConstants.DEPLOYMENT);
		final ModelNode result;
		
		try {
			result = client.execute(op);
			System.out.println("Available Applications: ");
			// Check to make sure there is an outcome
			if (result.hasDefined(ClientConstants.OUTCOME)) {
				if (result.get(ClientConstants.OUTCOME).asString().equals(ClientConstants.SUCCESS)) {
					final List<ModelNode> deployments = (result.hasDefined(ClientConstants.RESULT) ? result.get(ClientConstants.RESULT).asList() : Collections.<ModelNode>emptyList());
					for (ModelNode n : deployments) {
						System.out.println(n.asString());
						
						/* Check whether given application is deployed
						if (n.asString().equals(deploymentName)) {
							return true;
						} */
					}
					
					if( deployments.size() == 0 ) {
						System.out.println("No Application found.");
					}
				} else if (result.get(ClientConstants.OUTCOME).asString().equals(ClientConstants.FAILURE_DESCRIPTION)) {
					throw new IllegalStateException(String.format("A failure occurred when checking existing deployments. Error: %s",
							(result.hasDefined(ClientConstants.FAILURE_DESCRIPTION) ? result.get(ClientConstants.FAILURE_DESCRIPTION).asString() : "Unknown")));
				}
			} else {
				throw new IllegalStateException(String.format("An unexpected response was found checking the deployment. Result: %s", result));
			}
		} catch (IOException e) {
			throw new IllegalStateException(String.format("Could not execute operation '%s'", op), e);
		}
	}
	
	public void runCommands(final ModelControllerClient client) throws CommandLineException {
		ModelNode deployRequest = null;
		String request="/server-group=*/deployment=*/:read-resource(recursive=false,proxies=true,include-runtime=true,include-defaults=true)";
		CommandContext ctx = CommandContextFactory.getInstance().newCommandContext("ramkumar","smith*123".toCharArray());
		ctx.connectController("localhost", 9999);
		deployRequest = ctx.buildRequest(request);
		
		System.out.println("Running command ---- ");
		try {
			ModelNode response = client.execute(deployRequest);
			System.out.println("nt response = > "+response);
			/*
			nt response = > {
				"outcome" => "failed",
				"failure-description" => "JBAS014883: No resource definition is registered for address [
				(\"server-group\" => \"*\"),
				(\"deployment\" => \"*\")
			]",
				"rolled-back" => true
			}
			*/
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	public void printChildrenResource(final ModelControllerClient client) throws IOException {
		final ModelNode operation = new ModelNode();
		
		operation.get("operation").set("read-children-resources");
		operation.get("child-type").set(ClientConstants.DEPLOYMENT);
		
		final ModelNode address = operation.get("address");
		
		address.add("deployment", "MMDisplay.war");
		
		System.out.println("child-resource: "+client.execute(operation));
	}
	
	/**
	 * Print the JNDI details
	 * 
	 * @param client
	 */
	public void printJNDIDetails(final ModelControllerClient client) {
		try {
			ModelNode op = new ModelNode();
			op.get("operation").set("jndi-view");
			ModelNode address = op.get("address");
			address.add("subsystem", "naming");
			op.get("recursive").set(true);
			op.get("operations").set(true);
			
			ModelNode returnVal = client.execute(op);
			System.out.println("printJNDIDetails: "+returnVal.get("result").toString());
		} catch (Exception e) {
			System.out.println("Exception in printJNDIDetails: "+e.getMessage());
			e.printStackTrace();
		}
	}
}