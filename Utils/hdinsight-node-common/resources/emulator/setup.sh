#!/bin/bash
unzip -u service.zip &>>~/setup.log
echo "Configure VM network and service......"
rsync -a service/ambari-server /var/lib/
rsync -a service/sbin /opt/livy/

sed -i '/PEERDNS=/c\PEERDNS=yes' /etc/sysconfig/network-scripts/ifcfg-eth0
sed -i '/DNS.=/d' /etc/sysconfig/network-scripts/ifcfg-eth0
/etc/init.d/network restart &>>~/setup.log

# Fix the resolve file
# Install R
# yum update
echo "Installing R and other packages......"
yum install R -y &>>~/setup.log
yum install jq -y &>>~/setup.log

livy_root_dir=/opt/livy
livy_package_name=livy-server-0.2.0-SNAPSHOT-livy-server
livy_package=$livy_package_name.zip
livy_package_md5=$livy_package_name.md5
livy_url=https://github.com/wezhang/livy/releases/download/sparkR-20160822.0
livy_package_url=$livy_url/$livy_package
livy_package_md5_url=$livy_url/$livy_package_md5

mkdir -p /opt/livy 

pushd $livy_root_dir &>/dev/null

# Download the livy
if ! md5sum -c $livy_root_dir/$livy_package_md5 2>/dev/null; then
  wget $livy_package_md5_url -q -O $livy_root_dir/$livy_package_md5
  wget $livy_package_url -q -O $livy_root_dir/$livy_package
fi

unzip -u $livy_package &>>~/setup.log

chmod +x sbin/*.sh

popd

grep -q -F 'export SPARK_HOME=/usr/hdp/current/spark-client' /etc/environment || echo 'export SPARK_HOME=/usr/hdp/current/spark-client' >> /etc/environment
source /etc/environment

# Reset admin password and restart ambari service
echo "Reset admin password for ambari......"
ambari-admin-password-reset << EOF
admin
admin
EOF

echo "Waiting for the ambari server is ready for 5 minutes..."
for i in `seq 1 60`; do curl  --fail -u admin:admin -s -H "X-Requested-By:ambari" "http://localhost:8080" > /dev/null && break || sleep 5; done

echo "Configure properties for hive......"
# configure hive properties
#/var/lib/ambari-server/resources/scripts/configs.sh -u admin -p admin set localhost Sandbox hive-site 'hive.server2.thrift.http.path' '/'  &>>~/setup.log
#/var/lib/ambari-server/resources/scripts/configs.sh -u admin -p admin set localhost Sandbox hive-site 'hive.server2.transport.mode' 'http' &>>~/setup.log
#/var/lib/ambari-server/resources/scripts/configs.sh -u admin -p admin set localhost Sandbox webhcat-site 'templeton.hive.properties' 'hive.exec.pre.hooks=org.apache.hadoop.hive.ql.hooks.ATSHook, hive.exec.failure.hooks=org.apache.hadoop.hive.ql.hooks.ATSHook, hive.exec.post.hooks=org.apache.hadoop.hive.ql.hooks.ATSHook' &>>~/setup.log

# Add the LIVY_MASTER to ambari recovery
sed -i '/LIVY_MASTER/!s/\(recovery.enabled_components=.*\)/\1,LIVY_MASTER/' /etc/ambari-server/conf/ambari.properties

# Add livy service to ambari
echo "Add livy service in ambari......"
curl --silent -u admin:admin -H 'X-Requested-By: ambari' -X POST -d '{"ServiceInfo":{"service_name":"LIVY"}}' http://localhost:8080/api/v1/clusters/Sandbox/services 
sleep 5

# Apply conf to cluster
echo "Apply configuration for livy......"
curl --silent -u admin:admin -H 'X-Requested-By: ambari' -X PUT -d '{"Clusters":{"desired_config":[{"type":"livy-defaults","tag":"version1473323324517","properties":{"livy.repl.driverClassPath":"/opt/livy/livy-server-0.2.0-SNAPSHOT/repl-jars","livy.server.session.timeout":"2073600000"},"service_config_version_note":"Initial configurations for Livy"},{"type":"livy-log4j-properties","tag":"version1473323324517","properties":{"content":"\n# The default Livy logging configuration.\nlog4j.rootLogger=INFO,console,DRFA,ETW,FilterLog\n\nlog4j.appender.console=org.apache.log4j.ConsoleAppender\nlog4j.appender.console.target=System.err\nlog4j.appender.console.layout=org.apache.log4j.PatternLayout\nlog4j.appender.console.layout.ConversionPattern=%d{yy/MM/dd HH:mm:ss} %p %c{1}: %m%n\n\nlog4j.logger.org.eclipse.jetty=INFO\n\nlog4j.appender.DRFA=org.apache.log4j.DailyRollingFileAppender\nlog4j.appender.DRFA.target=System.err\nlog4j.appender.DRFA.File=/var/log/livy/livy.log\n# Rollver at midnight\nlog4j.appender.DRFA.DatePattern=.yyyy-MM-dd\nlog4j.appender.DRFA.layout=org.apache.log4j.PatternLayout\n# Pattern format: Date LogLevel LoggerName LogMessage\nlog4j.appender.DRFA.layout.ConversionPattern=%d{yy/MM/dd HH:mm:ss} %p %c{1}: %m%n\n\n#EtwLog Appender\n#sends Spark logs to customer storage account\nlog4j.appender.ETW=com.microsoft.log4jappender.EtwAppender\nlog4j.appender.ETW.source=HadoopServiceLog\nlog4j.appender.ETW.component=sparklivy\nlog4j.appender.ETW.layout=org.apache.log4j.TTCCLayout\nlog4j.appender.ETW.OSType=Linux\n\n# FilterLog Appender\n# Sends filtered HDP service logs to our storage account\nlog4j.appender.FilterLog=com.microsoft.log4jappender.FilterLogAppender\nlog4j.appender.FilterLog.source=CentralFilteredHadoopServiceLogs\nlog4j.appender.FilterLog.component=sparklivy\nlog4j.appender.FilterLog.layout=org.apache.log4j.TTCCLayout\nlog4j.appender.FilterLog.Threshold=INFO\nlog4j.appender.FilterLog.whitelistFileName=NA\nlog4j.appender.FilterLog.OSType=Linux"},"service_config_version_note":"Initial configurations for Livy"}]}}' http://localhost:8080/api/v1/clusters/Sandbox &>>~/setup.log
sleep 5

# Add Component to service
echo "Add component to livy......"
curl --silent -u admin:admin -H 'X-Requested-By: ambari' -X POST http://localhost:8080/api/v1/clusters/Sandbox/services/LIVY/components/LIVY_MASTER
sleep 5

# Create Host component
echo "Create host component......"
curl --silent -u admin:admin -H 'X-Requested-By: ambari' -X POST -d '{"RequestInfo":{"query":"Hosts/host_name=sandbox.hortonworks.com"},"Body":{"host_components":[{"HostRoles":{"component_name":"LIVY_MASTER"}}]}}' http://localhost:8080/api/v1/clusters/Sandbox/hosts
sleep 5

# Install Service till it is done
echo "Waiting for installing livy service..."
count=0
status=""
while [ "$status" != "COMPLETED" -a $count -lt 5 ]
do
	echo $status, $count &>>~/setup.log
	if [ "$status" != "IN_PROGRESS" ]
	then 
		url=`curl --fail --silent -u admin:admin -H 'X-Requested-By: ambari' -X PUT -d '{"RequestInfo":{"context":"Install Services","operation_level":{"level":"CLUSTER","cluster_name":"Sandbox"}},"Body":{"ServiceInfo":{"state":"INSTALLED"}}}' http://localhost:8080/api/v1/clusters/Sandbox/services/LIVY | jq -r '.href'`
		count=$((count+1))
		echo $url &>>~/setup.log
	fi
	sleep 5
	status=`curl --fail --silent -u admin:admin $url | jq -r '.Requests.request_status'`
done


# Start Service
echo "Waiting for starting livy service..."
count=0
status=""
while [ "$status" != "COMPLETED" -a $count -lt 5 ]
do
	echo $status, $count &>>~/setup.log
	if [ "$status" != "IN_PROGRESS" ]
	then
		url=`curl --fail --silent -u admin:admin -H 'X-Requested-By: ambari' -X PUT -d '{"RequestInfo":{"context":"Start Added Services","operation_level":{"level":"CLUSTER","cluster_name":"Sandbox"}},"Body":{"ServiceInfo":{"state":"STARTED"}}}' http://localhost:8080/api/v1/clusters/Sandbox/services/LIVY | jq -r '.href'`
		count=$((count+1))
		echo $url &>>~/setup.log
	fi
	sleep 5
	status=`curl --fail --silent -u admin:admin $url | jq -r '.Requests.request_status'`
done

# restart Hive component till it is done
#echo "Waiting for restarting hive service..."
#count=0
#status=""
#while [ "$status" != "COMPLETED" -a $count -lt 5 ]
#do
#	echo $status, $count &>>~/setup.log
#	if [ "$status" != "IN_PROGRESS" ]
#	then 
#		url=`curl --fail --silent -u admin:admin -H 'X-Requested-By: ambari' -X POST -d '{"RequestInfo":{"command":"RESTART","context":"Restart all components for HIVE","operation_level":{"level":"SERVICE","cluster_name":"Sandbox","service_name":"HIVE"}},"Requests/resource_filters":[{"service_name":"HIVE","component_name":"HIVE_CLIENT","hosts":"sandbox.hortonworks.com"},{"service_name":"HIVE","component_name":"HIVE_METASTORE","hosts":"sandbox.hortonworks.com"},{"service_name":"HIVE","component_name":"HIVE_SERVER","hosts":"sandbox.hortonworks.com"},{"service_name":"HIVE","component_name":"MYSQL_SERVER","hosts":"sandbox.hortonworks.com"},{"service_name":"HIVE","component_name":"WEBHCAT_SERVER","hosts":"sandbox.hortonworks.com"}]}' http://localhost:8080/api/v1/clusters/Sandbox/requests | jq -r '.href'`
#		count=$((count+1))
#		echo $url &>>~/setup.log
#	fi
#	sleep 5
#	status=`curl --fail --silent -u admin:admin $url | jq -r '.Requests.request_status'`
#done
#
# Verfiy livy service
code=`curl --fail --silent -I http://localhost:8998/ | grep HTTP/1.1 | awk {'print $2'}`
if [ "$code" = "200" ]
then
	echo "Successfully setup VM"
	exit 0
else
	echo "Failed to setup VM, check setup.log on VM"
	exit 1
fi
