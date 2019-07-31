# whenever agent version changed, need to change here

nohup java -Djavax.net.ssl.trustStore=cacerts_appedo_agent -Djavax.net.ssl.trustStorePassword=changeit -jar appedo_linux_agent.jar > logs/appedo_linux_agent_$(date +%Y-%m-%d_%H%M).log 2>&1 &