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

import com.google.gson.annotations.Expose;
import com.microsoft.azure.hdinsight.common.ClusterManagerEx;
import com.microsoft.azure.hdinsight.sdk.common.HDIException;
import com.microsoft.azure.hdinsight.sdk.storage.HDStorageAccount;
import com.microsoft.azure.hdinsight.sdk.storage.IHDIStorageAccount;
import com.microsoft.azure.hdinsight.spark.common.SparkSubmitStorageType;
import com.microsoft.azure.hdinsight.spark.common.SparkSubmitStorageTypeOptionsForCluster;
import com.microsoft.azuretools.authmanage.models.SubscriptionDetail;
import com.microsoft.azuretools.azurecommons.helpers.NotNull;
import com.microsoft.azuretools.azurecommons.helpers.Nullable;

import java.io.IOException;
import java.net.URI;
import java.util.List;
import java.util.Optional;

public class HDInsightAdditionalClusterDetail implements IClusterDetail, LivyCluster, YarnCluster {
    @NotNull
    private String clusterName;
    @NotNull
    private String userName;
    @NotNull
    private String password;

    @Expose
    @Nullable
    private HDStorageAccount defaultStorageAccount;

    public HDInsightAdditionalClusterDetail(@NotNull String clusterName,
                                            @NotNull String userName,
                                            @NotNull String password,
                                            @Nullable HDStorageAccount storageAccount) {
        this.clusterName = clusterName;
        this.userName = userName;
        this.password = password;
        defaultStorageAccount = storageAccount;
    }

    @Override
    public boolean isEmulator() { return false; }

    @Override
    public boolean isConfigInfoAvailable() {
        return false;
    }

    @Override
    public String getName() {
        return clusterName;
    }

    @Override
    public String getTitle() {
        return Optional.ofNullable(getSparkVersion())
                .filter(ver -> !ver.trim().isEmpty())
                .map(ver -> getName() + " (Spark: " + ver + " Linked)")
                .orElse(getName() + " [Linked]");
    }

    @Override
    public String getState() {
        return null;
    }

    @Override
    public String getLocation() {
        return null;
    }

    @Override
    public String getConnectionUrl() {
        return ClusterManagerEx.getInstance().getClusterConnectionString(this.clusterName);
    }

    public String getLivyConnectionUrl() {
        return URI.create(getConnectionUrl()).resolve("livy/").toString();
    }

    public String getYarnNMConnectionUrl() {
        return URI.create(getConnectionUrl()).resolve("yarnui/ws/v1/cluster/apps/").toString();
    }

    @Override
    public String getCreateDate() {
        return null;
    }

    @Override
    public ClusterType getType() {
        return null;
    }

    @Override
    public String getResourceGroup(){
        return null;
    }

    @Override
    public String getVersion() {
        return null;
    }

    @Override
    public String getSparkVersion() {
        return null;
    }

    @Override
    public SubscriptionDetail getSubscription() {
        return new SubscriptionDetail("[LinkedCluster]", "[NoSubscription]", "", false);
    }

    @Override
    public int getDataNodes() {
        return 0;
    }

    @Override
    public String getHttpUserName() throws HDIException {
        return userName;
    }

    @Override
    public String getHttpPassword() throws HDIException {
        return password;
    }

    @Override
    public String getOSType() {
        return null;
    }

    @Override
    @Nullable
    public IHDIStorageAccount getStorageAccount() throws HDIException {
        return defaultStorageAccount;
    }

    @Override
    public List<HDStorageAccount> getAdditionalStorageAccounts() {
        return null;
    }

    @Override
    public void getConfigurationInfo() throws IOException, HDIException {

    }

    @Override
    public SparkSubmitStorageType getDefaultStorageType() {
        return SparkSubmitStorageType.SPARK_INTERACTIVE_SESSION;
    }

    @Override
    public SparkSubmitStorageTypeOptionsForCluster getStorageOptionsType() {
        return SparkSubmitStorageTypeOptionsForCluster.HdiAdditionalClusterWithUndetermineStorage;
    }
}
