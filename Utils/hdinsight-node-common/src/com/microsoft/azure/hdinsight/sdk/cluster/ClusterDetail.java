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
import com.microsoft.azuretools.azurecommons.helpers.Nullable;
import org.apache.commons.lang3.exception.ExceptionUtils;

import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ClusterDetail implements IClusterDetail, LivyCluster, YarnCluster, ILogger {

    private static final String ADL_HOME_PREFIX = "adl://home";
    private static final String ADLS_HOME_HOST_NAME = "dfs.adls.home.hostname";
    private static final String ADLS_HOME_MOUNTPOINT = "dfs.adls.home.mountpoint";

    private final String WorkerNodeName = "workernode";
    private final String DefaultFS = "fs.defaultFS";
    private final String FSDefaultName = "fs.default.name";
    private final String StorageAccountKeyPrefix = "fs.azure.account.key.";
    public static final String StorageAccountNamePattern = "^(?<scheme>abfs[s]?|wasb[s]?)://(?<container>.*)@(?<fullAccountName>.*)$";
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
        return Optional.ofNullable(getSparkVersion())
                .filter(ver -> !ver.trim().isEmpty())
                .map(ver -> getName() + " (Spark: " + ver + ")")
                .orElse(getName());
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

    public ClusterType getType(){
        ClusterType type =  null;
        try {
            type = ClusterType.valueOf(this.clusterRawInfo.getProperties().getClusterDefinition().getKind().toLowerCase());
        } catch (IllegalArgumentException e) {
            type = ClusterType.unkown;
        }
        return type == null ? ClusterType.unkown : type;
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

    public String getOSType(){
        ClusterProperties clusterProperties = this.clusterRawInfo.getProperties();
        return clusterProperties == null ? null : clusterProperties.getOsType();
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
                            this.defaultStorageAccount = getDefaultStorageAccount(coresSiteMap, clusterIdentity);
                            this.additionalStorageAccounts = getAdditionalStorageAccounts(coresSiteMap);
                        }
                    }

                    isConfigInfoAvailable = true;
                }
            }
        }
    }

    private IHDIStorageAccount getDefaultStorageAccount(Map<String, String> coresiteMap, ClusterIdentity clusterIdentity) throws HDIException{
        String containerAddress = null;
        if(coresiteMap.containsKey(DefaultFS)){
            containerAddress = coresiteMap.get(DefaultFS);
        }else if(coresiteMap.containsKey(FSDefaultName)){
            containerAddress = coresiteMap.get(FSDefaultName);
        }

        if(containerAddress == null){
            throw new HDIException("Failed to get default storage account");
        }

        String scheme = URI.create(containerAddress).getScheme();

        //for adls
        if(ADL_HOME_PREFIX.equalsIgnoreCase(containerAddress)) {
            String accountName = "";
            String defaultRootPath = "";

            if(coresiteMap.containsKey(ADLS_HOME_HOST_NAME)) {
                accountName = coresiteMap.get(ADLS_HOME_HOST_NAME).split("\\.")[0];
            }
            if(coresiteMap.containsKey(ADLS_HOME_MOUNTPOINT)) {
                defaultRootPath = coresiteMap.get(ADLS_HOME_MOUNTPOINT);
            }

            URI rootURI = URI.create(String.format("%s://%s.azuredatalakestore.net", scheme, accountName)).resolve(defaultRootPath);
            return new ADLSStorageAccount(this,true, clusterIdentity, rootURI);
        } else if (Pattern.compile(StorageAccountNamePattern).matcher(containerAddress).matches()) {
            String storageAccountName = getStorageAccountName(containerAddress);
            if(storageAccountName == null){
                throw new HDIException("Failed to get default storage account name");
            }

            String defaultContainerName = getDefaultContainerName(containerAddress);

            String keyNameOfDefaultStorageAccountKey = StorageAccountKeyPrefix + storageAccountName;
            String storageAccountKey = null;
            if(coresiteMap.containsKey(keyNameOfDefaultStorageAccountKey)){
                storageAccountKey = coresiteMap.get(keyNameOfDefaultStorageAccountKey);
            }

            if (!scheme.startsWith(ADLSGen2StorageAccount.DefaultScheme) && storageAccountKey == null) {
                throw new HDIException("Failed to get default storage account key");
            }

            if (scheme.startsWith(ADLSGen2StorageAccount.DefaultScheme)) {
                try {
                    return new ADLSGen2StorageAccount(this, storageAccountName, storageAccountKey, true, defaultContainerName, scheme);
                } catch (Exception ex) {
                    throw new HDIException("Failed to init ADLSGEN2 account", ex);
                }
            } else {
                return new HDStorageAccount(this, storageAccountName, storageAccountKey, true, defaultContainerName);
            }

        } else {
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

    private String getStorageAccountName(String containerAddress){
        Pattern r = Pattern.compile(StorageAccountNamePattern);
        Matcher m = r.matcher(containerAddress);
        if(m.find())
        {
            return m.group(3);
        }

        return null;
    }

    private String getDefaultContainerName(String containerAddress){
        Pattern r = Pattern.compile(StorageAccountNamePattern);
        Matcher m = r.matcher(containerAddress);
        if(m.find())
        {
            return m.group(2);
        }

        return null;
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
        StorageAccountTypeEnum type = StorageAccountTypeEnum.UNKNOWN;

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
        } else if (type == StorageAccountTypeEnum.ADLS) {
            return SparkSubmitStorageTypeOptionsForCluster.ClusterWithAdls;
        } else if (type == StorageAccountTypeEnum.BLOB) {
            return SparkSubmitStorageTypeOptionsForCluster.ClusterWithBlob;
        } else if(type == StorageAccountTypeEnum.ADLSGen2){
           return SparkSubmitStorageTypeOptionsForCluster.ClusterWithAdlsGen2;
        } else {
            return SparkSubmitStorageTypeOptionsForCluster.ClusterWithUnknown;
        }
    }
}
