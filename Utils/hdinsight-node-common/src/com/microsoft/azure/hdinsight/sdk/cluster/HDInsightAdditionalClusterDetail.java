/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.hdinsight.sdk.cluster;

import com.google.gson.annotations.Expose;
import com.microsoft.azure.hdinsight.common.ClusterManagerEx;
import com.microsoft.azure.hdinsight.sdk.storage.HDStorageAccount;
import com.microsoft.azure.hdinsight.sdk.storage.IHDIStorageAccount;
import com.microsoft.azure.hdinsight.sdk.storage.StorageAccountType;
import com.microsoft.azure.hdinsight.spark.common.SparkSubmitStorageType;
import com.microsoft.azure.hdinsight.spark.common.SparkSubmitStorageTypeOptionsForCluster;
import com.microsoft.azure.toolkit.lib.common.model.Subscription;
import com.microsoft.azuretools.azurecommons.helpers.NotNull;
import com.microsoft.azuretools.azurecommons.helpers.Nullable;
import org.apache.commons.lang3.StringUtils;

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
    public Subscription getSubscription() {
        return new Subscription("[LinkedCluster]", "[NoSubscription]", "", false);
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
        if (StringUtils.isEmpty(defaultStorageRootPath)) {
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
