/**
 * Copyright (c) Microsoft Corporation
 * <p/>
 * All rights reserved.
 * <p/>
 * MIT License
 * <p/>
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated
 * documentation files (the "Software"), to deal in the Software without restriction, including without limitation
 * the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and
 * to permit persons to whom the Software is furnished to do so, subject to the following conditions:
 * <p/>
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of
 * the Software.
 * <p/>
 * THE SOFTWARE IS PROVIDED *AS IS*, WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO
 * THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT,
 * TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package com.microsoft.azure.hdinsight.sdk.cluster;

import com.microsoft.azure.hdinsight.common.ClusterManagerEx;
import com.microsoft.azure.hdinsight.common.logger.ILogger;
import com.microsoft.azure.hdinsight.sdk.cluster.HDInsightNewAPI.ClusterOperationNewAPIImpl;
import com.microsoft.azure.hdinsight.sdk.cluster.HDInsightNewAPI.HDInsightUserRoleType;
import com.microsoft.azure.hdinsight.sdk.common.HDIException;
import com.microsoft.azure.hdinsight.sdk.storage.*;
import com.microsoft.azure.hdinsight.spark.common.SparkSubmitStorageType;
import com.microsoft.azure.hdinsight.spark.common.SparkSubmitStorageTypeOptionsForCluster;
import com.microsoft.azuretools.authmanage.models.SubscriptionDetail;
import com.microsoft.azuretools.azurecommons.helpers.AzureCmdException;
import com.microsoft.azuretools.azurecommons.helpers.NotNull;
import com.microsoft.azuretools.azurecommons.helpers.Nullable;
import com.microsoft.azuretools.telemetry.AppInsightsClient;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;

import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

public class ClusterDetail implements IClusterDetail, LivyCluster, YarnCluster, ILogger {

    private static final String ADL_HOME_PREFIX = "adl://home";
    private static final String ADLS_HOME_HOST_NAME = "dfs.adls.home.hostname";
    private static final String ADLS_HOME_MOUNTPOINT = "dfs.adls.home.mountpoint";

    private final String WorkerNodeName = "workernode";
    private final String DefaultFS = "fs.defaultFS";
    private final String FSDefaultName = "fs.default.name";
    private final String StorageAccountKeyPrefix = "fs.azure.account.key.";
    private final String ResourceGroupStartTag = "resourceGroups/";
    private final String ResourceGroupEndTag = "/providers/";

    private SubscriptionDetail subscription;
    private ClusterRawInfo clusterRawInfo;
    private IClusterOperation clusterOperation;

    private int dataNodes;
    @Nullable
    private String userName;
    @Nullable
    private String passWord;
    private IHDIStorageAccount defaultStorageAccount;
    private List<HDStorageAccount> additionalStorageAccounts;
    private boolean isConfigInfoAvailable = false;
    @Nullable
    private Map<String, String> coresiteMap = null;

    public ClusterDetail(SubscriptionDetail paramSubscription,
                         ClusterRawInfo paramClusterRawInfo,
                         IClusterOperation clusterOperation){
        this.subscription = paramSubscription;
        this.clusterRawInfo = paramClusterRawInfo;
        this.clusterOperation = clusterOperation;
        ExtractInfoFromComputeProfile();
    }

    public boolean isRoleTypeReader() {
        return clusterOperation instanceof ClusterOperationNewAPIImpl
                && ((ClusterOperationNewAPIImpl) clusterOperation).getRoleType() == HDInsightUserRoleType.READER;
    }

    public boolean isEmulator () { return false; }

    public boolean isConfigInfoAvailable(){
        return isConfigInfoAvailable;
    }

    public String getName(){
        return this.clusterRawInfo.getName();
    }

    @Override
    public String getTitle() {
        StringBuilder titleStringBuilder = new StringBuilder(getName());

        String sparkVersion = getSparkVersion();
        if (StringUtils.isNotBlank(sparkVersion)) {
            titleStringBuilder.append(String.format(" (Spark: %s)", sparkVersion));
        }

        if (ClusterManagerEx.getInstance().isHdiReaderCluster(this)) {
            titleStringBuilder.append(" (Role: Reader)");
        }

        String state = getState();
        if (StringUtils.isNotBlank(state) && !state.equalsIgnoreCase("Running")) {
            titleStringBuilder.append(String.format(" (State: %s)", state));
        }

        return titleStringBuilder.toString();
    }

    @Override
    public String getSparkVersion() {
        ClusterProperties clusterProperties = clusterRawInfo.getProperties();
        if(clusterProperties == null) {
            return null;
        }

        // HDI and Spark version map
        // HDI 3.3   <-> Spark 1.5.2
        // HDI 3.4   <-> Spark 1.6.2
        // HDI 3.5   <-> Spark 1.6.2 & Spark 2.0.2
        // HDI 3.6   <-> Spark 2.0.0 & Spark 2.1.0
        String clusterVersion = clusterProperties.getClusterVersion();
        if(clusterVersion.startsWith("3.3")){
            return "1.5.2";
        } else if(clusterVersion.startsWith("3.4")) {
            return "1.6.2";
        } else if(clusterVersion.startsWith("3.5")){
            ComponentVersion componentVersion = clusterProperties.getClusterDefinition().getComponentVersion();
            return componentVersion == null ? "1.6.2" : componentVersion.getSpark();
        } else {
            ComponentVersion componentVersion = clusterProperties.getClusterDefinition().getComponentVersion();
            return componentVersion == null ? null : componentVersion.getSpark();
        }
    }

    public String getState(){
        ClusterProperties clusterProperties = this.clusterRawInfo.getProperties();
        return clusterProperties == null ? null : clusterProperties.getClusterState();
    }

    public String getLocation(){
        return this.clusterRawInfo.getLocation();
    }

    public String getConnectionUrl(){
        return ClusterManagerEx.getInstance().getClusterConnectionString(getName());
    }

    public String getCreateDate() {
        ClusterProperties clusterProperties = this.clusterRawInfo.getProperties();
        return clusterProperties == null ? null : clusterProperties.getCreatedDate();
    }

    public static ClusterType getType(@NotNull ClusterRawInfo clusterRawInfo) {
        ClusterType type =  null;
        try {
            type = ClusterType.valueOf(clusterRawInfo.getProperties().getClusterDefinition().getKind().toLowerCase());
        } catch (IllegalArgumentException e) {
            type = ClusterType.unkown;
        }
        return type == null ? ClusterType.unkown : type;
    }

    public ClusterType getType(){
        return getType(this.clusterRawInfo);
    }

    public String getResourceGroup(){
        String clusterId = clusterRawInfo.getId();
        int rgNameStart = clusterId.indexOf(ResourceGroupStartTag) + ResourceGroupStartTag.length();
        int rgNameEnd = clusterId.indexOf(ResourceGroupEndTag);
        if (rgNameStart != -1 && rgNameEnd != -1 && rgNameEnd > rgNameStart)
        {
            return clusterId.substring(rgNameStart, rgNameEnd);
        }

        return null;
    }

    public String getVersion(){
        ClusterProperties clusterProperties = this.clusterRawInfo.getProperties();
        return clusterProperties == null ? null : clusterProperties.getClusterVersion();
    }

    public SubscriptionDetail getSubscription(){
        return subscription;
    }

    public int getDataNodes(){
        return dataNodes;
    }

    @Nullable
    public String getHttpUserName() {
        try {
            getConfigurationInfo();
        } catch (Exception ex) {
            log().warn("Error getting cluster configuration info. Cluster Name: " + getName());
            log().warn(ExceptionUtils.getStackTrace(ex));
        } finally {
            return userName;
        }
    }

    @Nullable
    public String getHttpPassword() {
        try {
            getConfigurationInfo();
        } catch (Exception ex) {
            log().warn("Error getting cluster configuration info. Cluster Name: " + getName());
            log().warn(ExceptionUtils.getStackTrace(ex));
        } finally {
            return passWord;
        }
    }

    public static String getOSType(@NotNull ClusterRawInfo clusterRawInfo) {
        ClusterProperties clusterProperties = clusterRawInfo.getProperties();
        return clusterProperties == null ? null : clusterProperties.getOsType();
    }

    public String getOSType(){
        return getOSType(this.clusterRawInfo);
    }

    @Nullable
    public IHDIStorageAccount getStorageAccount() {
        try {
            getConfigurationInfo();
        } catch (Exception ex) {
            log().warn("Error getting cluster configuration info. Cluster Name: " + getName());
            log().warn(ExceptionUtils.getStackTrace(ex));
        } finally {
            return defaultStorageAccount;
        }
    }

    public List<HDStorageAccount> getAdditionalStorageAccounts(){
        try {
            getConfigurationInfo();
        } catch (Exception ex) {
            log().warn("Error getting cluster configuration info. Cluster Name: " + getName());
            log().warn(ExceptionUtils.getStackTrace(ex));
        } finally {
            return additionalStorageAccounts;
        }
    }

    private void ExtractInfoFromComputeProfile(){
        List<Role> roles = this.clusterRawInfo.getProperties().getComputeProfile().getRoles();
        for(Role role : roles){
            if(role.getName().equals(WorkerNodeName)){
                this.dataNodes = role.getTargetInstanceCount();
                break;
            }
        }
    }

    public void getConfigurationInfo() throws IOException, HDIException, AzureCmdException {
        // If exception happens, isConfigInfoAvailable is still false, which means
        // next time we call getConfigurationInfo(), load configuration codes will still be executed.
        if (!isConfigInfoAvailable()) {
            synchronized (this) {
                if (!isConfigInfoAvailable()) {
                    ClusterConfiguration clusterConfiguration =
                            clusterOperation.getClusterConfiguration(subscription, clusterRawInfo.getId());
                    if (clusterConfiguration != null && clusterConfiguration.getConfigurations() != null) {
                        Configurations configurations = clusterConfiguration.getConfigurations();
                        Gateway gateway = configurations.getGateway();
                        if (gateway != null) {
                            this.userName = gateway.getUsername();
                            this.passWord = gateway.getPassword();
                        }

                        Map<String, String> coresSiteMap = configurations.getCoresite();
                        ClusterIdentity clusterIdentity = configurations.getClusterIdentity();
                        if (coresSiteMap != null) {
                            this.coresiteMap = coresSiteMap;
                            try {
                                this.defaultStorageAccount = getDefaultStorageAccount(coresSiteMap, clusterIdentity);
                            } catch (HDIException exp) {
                                String errMsg = String.format("Encounter exception when getting storage configuration for cluster name:%s,type:%s,location:%s," +
                                                "state:%s,version:%s,osType:%s,kind:%s,spark version:%s",
                                        clusterRawInfo.getName(),
                                        clusterRawInfo.getType(),
                                        clusterRawInfo.getLocation(),
                                        clusterRawInfo.getProperties().getClusterState(),
                                        clusterRawInfo.getProperties().getClusterVersion(),
                                        clusterRawInfo.getProperties().getOsType(),
                                        clusterRawInfo.getProperties().getClusterDefinition().getKind(),
                                        clusterRawInfo.getProperties().getClusterDefinition().getComponentVersion().getSpark());
                                log().warn(errMsg, exp);
                                throw new HDIException(errMsg, exp);
                            }

                            this.additionalStorageAccounts = getAdditionalStorageAccounts(coresSiteMap);
                        }
                    }

                    isConfigInfoAvailable = true;
                }
            }
        }
    }

    @Nullable
    public String getDefaultStorageRootPath() {
        log().info("Cluster ID: " + clusterRawInfo.getId());
        Map<String, String> requestedCoresiteMap = null;

        try {
            if (!(clusterOperation instanceof ClusterOperationNewAPIImpl)) {
                requestedCoresiteMap = this.coresiteMap;
            } else {
                requestedCoresiteMap =
                        ((ClusterOperationNewAPIImpl) clusterOperation).getClusterCoreSiteRequest(clusterRawInfo.getId())
                                .toBlocking()
                                .singleOrDefault(null);
            }

            if (requestedCoresiteMap == null) {
                log().warn("Error getting cluster core-site. coresiteMap is null.");
                return null;
            }
        } catch (Exception ex) {
            log().warn("Error getting cluster core-site. " + ExceptionUtils.getStackTrace(ex));
            return null;
        }

        String containerAddress = null;
        if (requestedCoresiteMap.containsKey(DefaultFS)) {
            containerAddress = requestedCoresiteMap.get(DefaultFS);
        } else if (requestedCoresiteMap.containsKey(FSDefaultName)) {
            containerAddress = requestedCoresiteMap.get(FSDefaultName);
        } else {
            log().warn("Error getting cluster default storage account. containerAddress is null.");
            return null;
        }

        String scheme = URI.create(containerAddress).getScheme();
        if (ADL_HOME_PREFIX.equalsIgnoreCase(containerAddress)) {
            String accountName = "";
            String defaultRootPath = "";

            if (requestedCoresiteMap.containsKey(ADLS_HOME_HOST_NAME)) {
                accountName = requestedCoresiteMap.get(ADLS_HOME_HOST_NAME).split("\\.")[0];
            }
            if (requestedCoresiteMap.containsKey(ADLS_HOME_MOUNTPOINT)) {
                defaultRootPath = requestedCoresiteMap.get(ADLS_HOME_MOUNTPOINT);
            }

            return URI.create(String.format("%s://%s.azuredatalakestore.net", scheme, accountName))
                    .resolve(defaultRootPath)
                    .toString();
        } else if (Pattern.compile(StoragePathInfo.BlobPathPattern).matcher(containerAddress).matches()
                || Pattern.compile(StoragePathInfo.AdlsGen2PathPattern).matcher(containerAddress).matches()) {
            return containerAddress;
        } else {
            final Map<String, String> properties = new HashMap<>();
            properties.put("ErrorType", "Unknown HDInsight default storage type");
            properties.put("coreSiteMap", StringUtils.join(requestedCoresiteMap));
            properties.put("containerAddress", containerAddress);
            properties.put("ClusterID", this.clusterRawInfo.getId());
            AppInsightsClient.createByType(AppInsightsClient.EventType.Error, this.getClass().getSimpleName(), null, properties);

            return null;
        }
    }

    @Nullable
    private IHDIStorageAccount getDefaultStorageAccount(Map<String, String> coresiteMap, ClusterIdentity clusterIdentity) throws HDIException {
        String defaultStorageRootPath = getDefaultStorageRootPath();
        if (defaultStorageRootPath == null) {
            throw new HDIException("Failed to get default storage root path");
        }

        StoragePathInfo pathInfo = new StoragePathInfo(defaultStorageRootPath);
        switch (pathInfo.storageType) {
            case ADLS:
                return new ADLSStorageAccount(this, true, clusterIdentity, URI.create(defaultStorageRootPath));

            case BLOB:
                String storageAccountName = pathInfo.path.getHost();
                if (StringUtils.isBlank(storageAccountName)) {
                    throw new HDIException(String.format("Failed to get default storage account name from root path %s with %s storage type",
                            defaultStorageRootPath, pathInfo.storageType));
                }

                String defaultContainerName = pathInfo.path.getUserInfo();
                String defaultStorageAccountKey = StorageAccountKeyPrefix + storageAccountName;
                String storageAccountKey = null;
                if (coresiteMap.containsKey(defaultStorageAccountKey)) {
                    storageAccountKey = coresiteMap.get(defaultStorageAccountKey);
                }

                if (storageAccountKey == null) {
                   return null;
                }

                return new HDStorageAccount(this, storageAccountName, storageAccountKey, true, defaultContainerName);

            case ADLSGen2:
                String accountName = pathInfo.path.getHost();
                if (StringUtils.isBlank(accountName)) {
                    throw new HDIException(String.format("Failed to get default storage account name from root path %s with %s storage type",
                            defaultStorageRootPath, pathInfo.storageType));
                }

                String fileSystem = pathInfo.path.getUserInfo();
                return new ADLSGen2StorageAccount(this, accountName, null, true, fileSystem, pathInfo.path.getScheme());

            default:
                return null;
        }
    }

    private List<HDStorageAccount> getAdditionalStorageAccounts(Map<String, String> coresiteMap){
        if(coresiteMap.size() <= 2)
        {
            return null;
        }

        List<HDStorageAccount> storageAccounts = new ArrayList<>();
        for (Map.Entry<String, String> entry : coresiteMap.entrySet()){
            if(entry.getKey().toLowerCase().equals(DefaultFS) || entry.getKey().toLowerCase().equals(FSDefaultName)){
                continue;
            }

            if(entry.getKey().contains(StorageAccountKeyPrefix)){
                HDStorageAccount account =
                        new HDStorageAccount(this, entry.getKey().substring(StorageAccountKeyPrefix.length()), entry.getValue(), false, null);
                storageAccounts.add(account);
            }
        }

        return storageAccounts;
    }

    public String getLivyConnectionUrl() {
        return URI.create(getConnectionUrl()).resolve("livy/").toString();
    }

    public String getYarnNMConnectionUrl() {
        return URI.create(getConnectionUrl()).resolve("yarnui/ws/v1/cluster/apps/").toString();
    }

    @Override
    public SparkSubmitStorageType getDefaultStorageType() {
        SparkSubmitStorageType type = getStorageOptionsType().getOptionTypes().length == 0
                ? null
                : getStorageOptionsType().getOptionTypes()[0];
        return type;
    }

    @Override
    public SparkSubmitStorageTypeOptionsForCluster getStorageOptionsType() {
        StorageAccountType type = StorageAccountType.UNKNOWN;

        if (getStorageAccount() == null) {
            try {
                getConfigurationInfo();
            } catch (IOException | HDIException | AzureCmdException ignored) {
            }
        }

        if (getStorageAccount() != null) {
            type = getStorageAccount().getAccountType();
        }

        if (isRoleTypeReader()) {
            return SparkSubmitStorageTypeOptionsForCluster.HDInsightReaderStorageTypeOptions;
        } else if (type == StorageAccountType.ADLS) {
            return SparkSubmitStorageTypeOptionsForCluster.ClusterWithAdls;
        } else if (type == StorageAccountType.BLOB) {
            return SparkSubmitStorageTypeOptionsForCluster.ClusterWithBlob;
        } else if(type == StorageAccountType.ADLSGen2){
           return SparkSubmitStorageTypeOptionsForCluster.ClusterWithAdlsGen2;
        } else {
            return SparkSubmitStorageTypeOptionsForCluster.ClusterWithUnknown;
        }
    }
}
