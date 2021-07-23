/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.tooling.msservices.serviceexplorer;

import com.google.common.base.Preconditions;
import lombok.Getter;
import org.apache.commons.lang3.StringUtils;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public final class AzureIconSymbol {

    // This needs to be at the beginning for the initialization to happen correctly
    private static final ConcurrentMap<String, AzureIconSymbol> VALUES_BY_NAME = new ConcurrentHashMap<>();
    private static final String ICON_BASE_DIR = "/icons/";
    @Getter
    private final String path;

    private AzureIconSymbol(String path) {
        this.path = path;
        VALUES_BY_NAME.put(path, this);
    }

    public interface Common {

        AzureIconSymbol AZURE = fillInPath("Common/Azure.svg");
        AzureIconSymbol AZURE_ACTIVE_LOG = fillInPath("Common/AzureActiveLog.svg");
        AzureIconSymbol AZURE_RESOURCE_CONNECTOR = fillInPath("Common/AzureResourceConnector.svg");

        AzureIconSymbol REFRESH = fillInPath("Common/Refresh.svg");
        AzureIconSymbol CREATE = fillInPath("Common/Create.svg");

        AzureIconSymbol DELETE = fillInPath("Common/Delete.svg");
        AzureIconSymbol START = fillInPath("Common/Start.svg");
        AzureIconSymbol STOP = fillInPath("Common/Stop.svg");
        AzureIconSymbol RESTART = fillInPath("Common/Restart.svg");

        AzureIconSymbol OPEN_IN_PORTAL = fillInPath("Common/OpenInPortal.svg");
        AzureIconSymbol SHOW_PROPERTIES = fillInPath("Common/ShowProperties.svg");

        AzureIconSymbol APACHE_SPARK_FAILURE_DEBUG = fillInPath("Common/ApacheSparkFailureDebug.svg");
    }

    public interface MySQL {

        AzureIconSymbol MODULE = fillInPath("MySQL/MySQL.svg");

        AzureIconSymbol RUNNING = fillInPath("MySQL/MySQLRunning.svg");
        AzureIconSymbol STOPPED = fillInPath("MySQL/MySQLStopped.svg");
        AzureIconSymbol UPDATING = fillInPath("MySQL/MySQLUpdating.svg");

        AzureIconSymbol CONNECT_TO_SERVER = fillInPath("MySQL/ConnectToServer.svg");
        AzureIconSymbol BIND_INTO = fillInPath("MySQL/BindInto.svg");

    }

    public interface SqlServer {

        AzureIconSymbol MODULE = fillInPath("SqlServer/SqlServer.svg");

        AzureIconSymbol RUNNING = fillInPath("SqlServer/SqlServerRunning.svg");
        AzureIconSymbol STOPPED = fillInPath("SqlServer/SqlServerStopped.svg");
        AzureIconSymbol UPDATING = fillInPath("SqlServer/SqlServerUpdating.svg");

        AzureIconSymbol CONNECT_TO_SERVER = fillInPath("SqlServer/ConnectToServer.svg");
        AzureIconSymbol BIND_INTO = fillInPath("SqlServer/BindInto.svg");
    }

    public interface WebApp {

        AzureIconSymbol MODULE = fillInPath("WebApp/WebApp.svg");

        AzureIconSymbol RUNNING = fillInPath("WebApp/WebAppRunning.svg");
        AzureIconSymbol STOPPED = fillInPath("WebApp/WebAppStopped.svg");
        AzureIconSymbol UPDATING = fillInPath("WebApp/WebAppUpdating.svg");
        AzureIconSymbol RUNNING_ON_LINUX = fillInPath("WebApp/WebAppRunningOnLinux.svg");
        AzureIconSymbol STOPPED_ON_LINUX = fillInPath("WebApp/WebAppStoppedOnLinux.svg");
        AzureIconSymbol UPDATING_ON_LINUX = fillInPath("WebApp/WebAppUpdatingOnLinux.svg");

        AzureIconSymbol DEPLOY = fillInPath("WebApp/Deploy.svg");
    }

    public interface DeploymentSlot {

        AzureIconSymbol MODULE = fillInPath("DeploymentSlot/DeploymentSlot.svg");
        AzureIconSymbol MODULE_ON_LINUX = fillInPath("DeploymentSlot/DeploymentSlotOnLinux.svg");

        AzureIconSymbol RUNNING = fillInPath("DeploymentSlot/DeploymentSlotRunning.svg");
        AzureIconSymbol STOPPED = fillInPath("DeploymentSlot/DeploymentSlotStopped.svg");
        AzureIconSymbol UPDATING = fillInPath("DeploymentSlot/DeploymentSlotUpdating.svg");
        AzureIconSymbol RUNNING_ON_LINUX = fillInPath("DeploymentSlot/DeploymentSlotRunningOnLinux.svg");
        AzureIconSymbol STOPPED_ON_LINUX = fillInPath("DeploymentSlot/DeploymentSlotStoppedOnLinux.svg");
        AzureIconSymbol UPDATING_ON_LINUX = fillInPath("DeploymentSlot/DeploymentSlotUpdatingOnLinux.svg");
    }

    public interface FunctionApp {

        AzureIconSymbol MODULE = fillInPath("FunctionApp/FunctionApp.svg");

        AzureIconSymbol RUNNING = fillInPath("FunctionApp/FunctionAppRunning.svg");
        AzureIconSymbol STOPPED = fillInPath("FunctionApp/FunctionAppStopped.svg");
        AzureIconSymbol UPDATING = fillInPath("FunctionApp/FunctionAppUpdating.svg");

        AzureIconSymbol RUN = fillInPath("FunctionApp/Run.svg");
        AzureIconSymbol DEPLOY = fillInPath("FunctionApp/Deploy.svg");
    }

    public interface SpringCloud {

        AzureIconSymbol MODULE = fillInPath("SpringCloud/SpringCloud.svg");

        AzureIconSymbol CLUSTER = fillInPath("SpringCloud/SpringCloudApp.svg");

        AzureIconSymbol RUNNING = fillInPath("SpringCloud/SpringCloudAppRunning.svg");
        AzureIconSymbol STOPPED = fillInPath("SpringCloud/SpringCloudAppStopped.svg");
        AzureIconSymbol UPDATING = fillInPath("SpringCloud/SpringCloudAppUpdating.svg");
        AzureIconSymbol FAILED = fillInPath("SpringCloud/SpringCloudAppFailed.svg");
        AzureIconSymbol PENDING = fillInPath("SpringCloud/SpringCloudAppPending.svg");
        AzureIconSymbol UNKNOWN = fillInPath("SpringCloud/SpringCloudAppUnknown.svg");

        AzureIconSymbol DEPLOY = fillInPath("SpringCloud/Deploy.svg");
        AzureIconSymbol ADD_DEPENDENCY = fillInPath("SpringCloud/AddDependency.svg");
    }

    public interface ApacheSparkOnAzureSynapse {

        AzureIconSymbol MODULE = fillInPath("ApacheSparkOnAzureSynapse/ApacheSparkOnAzureSynapse.svg");
    }

    public interface ApacheSparkOnCosmos {

        AzureIconSymbol MODULE = fillInPath("ApacheSparkOnCosmos/ApacheSparkOnCosmos.svg");
    }

    public interface ContainerRegistry {

        AzureIconSymbol MODULE = fillInPath("ContainerRegistry/ContainerRegistry.svg");
    }

    public interface HDInsight {

        AzureIconSymbol MODULE = fillInPath("HDInsight/HDInsight.svg");
    }

    public interface RedisCache {

        AzureIconSymbol MODULE = fillInPath("RedisCache/RedisCache.svg");

        AzureIconSymbol RUNNING = fillInPath("RedisCache/RedisCacheRunning.svg");
        AzureIconSymbol STOPPED = fillInPath("RedisCache/RedisCacheStopped.svg");
        AzureIconSymbol UPDATING = fillInPath("RedisCache/RedisCacheUpdating.svg");
    }

    public interface ResourceManagement {

        AzureIconSymbol MODULE = fillInPath("ResourceManagement/ResourceManagement.svg");
    }

    public interface StorageAccount {

        AzureIconSymbol MODULE = fillInPath("StorageAccount/StorageAccount.svg");
    }

    public interface VirtualMachine {

        AzureIconSymbol MODULE = fillInPath("VirtualMachine/VirtualMachine.svg");

        AzureIconSymbol RUNNING = fillInPath("VirtualMachine/VirtualMachineRunning.svg");
        AzureIconSymbol STOPPED = fillInPath("VirtualMachine/VirtualMachineStopped.svg");
        AzureIconSymbol UPDATING = fillInPath("VirtualMachine/VirtualMachineUpdating.svg");
    }

    public interface SQLServerBigDataCluster {

        AzureIconSymbol MODULE = fillInPath("SQLServerBigDataCluster/SqlServerBigDataCluster.svg");
    }

    public interface DockerSupport {

        AzureIconSymbol MODULE = fillInPath("DockerSupport/DockerSupport.svg");

        AzureIconSymbol RUN = fillInPath("DockerSupport/Run.svg");
        AzureIconSymbol PUSH_IMAGE = fillInPath("DockerSupport/PushImage.svg");
        AzureIconSymbol RUN_ON_WEB_APP = fillInPath("DockerSupport/RunOnWebApp.svg");
    }

    public static AzureIconSymbol fromPath(String path) {
        Preconditions.checkArgument(StringUtils.isNotBlank(path), "path can not be blank.");
        AzureIconSymbol symbol = VALUES_BY_NAME.get(path);
        if (symbol != null) {
            return symbol;
        } else {
            return new AzureIconSymbol(path);
        }
    }

    @Override
    public int hashCode() {
        return this.path.hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        } else if (!(obj instanceof AzureIconSymbol)) {
            return false;
        } else if (obj == this) {
            return true;
        } else {
            AzureIconSymbol rhs = (AzureIconSymbol) obj;
            return this.path.equalsIgnoreCase(rhs.path);
        }
    }

    private static AzureIconSymbol fillInPath(String relativePath) {
        return new AzureIconSymbol(ICON_BASE_DIR + relativePath);
    }

}
