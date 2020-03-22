/*
 * Copyright (c) Microsoft Corporation
 *
 * All rights reserved.
 *
 * MIT License
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated
 * documentation files (the "Software"), to deal in the Software without restriction, including without limitation
 * the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and
 * to permit persons to whom the Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of
 * the Software.
 *
 * THE SOFTWARE IS PROVIDED *AS IS*, WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO
 * THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT,
 * TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package com.microsoft.azure.hdinsight.sdk.cluster;

import com.google.gson.annotations.Expose;
import com.microsoft.azure.hdinsight.common.ClusterManagerEx;
import com.microsoft.azure.hdinsight.sdk.storage.HDStorageAccount;
import com.microsoft.azure.hdinsight.sdk.storage.IHDIStorageAccount;
import com.microsoft.azure.hdinsight.sdk.storage.StorageAccountType;
import com.microsoft.azure.hdinsight.spark.common.SparkSubmitStorageType;
import com.microsoft.azure.hdinsight.spark.common.SparkSubmitStorageTypeOptionsForCluster;
import com.microsoft.azuretools.adauth.StringUtils;
import com.microsoft.azuretools.authmanage.models.SubscriptionDetail;
import com.microsoft.azuretools.azurecommons.helpers.NotNull;
import com.microsoft.azuretools.azurecommons.helpers.Nullable;

import java.net.URI;
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

    @Nullable
    private String defaultStorageRootPath;

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
    public SubscriptionDetail getSubscription() {
        return new SubscriptionDetail("[LinkedCluster]", "[NoSubscription]", "", false);
    }

    @Override
    public int getDataNodes() {
        return 0;
    }

    @Override
    @NotNull
    public String getHttpUserName() {
        return userName;
    }

    @Override
    @NotNull
    public String getHttpPassword() {
        return password;
    }

    @Override
    public SparkSubmitStorageType getDefaultStorageType() {
        SparkSubmitStorageType type = getStorageOptionsType().getOptionTypes().length == 0
                ? null
                : getStorageOptionsType().getOptionTypes()[0];
        return type;
    }

    @Nullable
    @Override
    public String getDefaultStorageRootPath() {
        return defaultStorageRootPath;
    }

    public void setDefaultStorageRootPath(@Nullable String defaultStorageRootPath) {
        this.defaultStorageRootPath = defaultStorageRootPath;
    }

    @Override
    @Nullable
    public IHDIStorageAccount getStorageAccount() {
        return defaultStorageAccount;
    }

    @Override
    public SparkSubmitStorageTypeOptionsForCluster getStorageOptionsType() {
        // for cluster which is not reader
        if (StringUtils.isNullOrEmpty(defaultStorageRootPath)) {
            return SparkSubmitStorageTypeOptionsForCluster.HdiAdditionalClusterWithUndetermineStorage;
        }

        StorageAccountType type = StorageAccountType.parseUri(URI.create(defaultStorageRootPath));
        switch (type) {
            case BLOB:
                return SparkSubmitStorageTypeOptionsForCluster.HdiAdditionalClusterForReaderWithBlob;
            case ADLSGen2:
                return SparkSubmitStorageTypeOptionsForCluster.HdiAdditionalClusterForReaderWithADLSGen2;
            case ADLS:
                return SparkSubmitStorageTypeOptionsForCluster.HdiAdditionalClusterForReaderWithADLSGen1;
            default:
                return SparkSubmitStorageTypeOptionsForCluster.HdiAdditionalClusterWithUndetermineStorage;
        }
    }

    @Override
    public int hashCode() {
        return getName().hashCode();
    }

    @Override
    public boolean equals(Object o) {
        if (o == this) {
            return true;
        }

        if (!(o instanceof IClusterDetail)) {
            return false;
        }

        return o.hashCode() == this.hashCode();
    }
}
