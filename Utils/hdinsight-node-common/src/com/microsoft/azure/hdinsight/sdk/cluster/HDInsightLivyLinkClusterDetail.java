/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.hdinsight.sdk.cluster;

import com.microsoft.azure.hdinsight.sdk.common.HDIException;
import com.microsoft.azure.hdinsight.spark.common.SparkSubmitStorageType;
import com.microsoft.azure.hdinsight.spark.common.SparkSubmitStorageTypeOptionsForCluster;
import com.microsoft.azure.toolkit.lib.common.model.Subscription;
import com.microsoft.azuretools.azurecommons.helpers.NotNull;
import com.microsoft.azuretools.azurecommons.helpers.Nullable;

import java.net.URI;
import java.util.Optional;

public class HDInsightLivyLinkClusterDetail implements IClusterDetail, LivyCluster, YarnCluster {
    @NotNull
    private final URI livyEndpoint;
    @Nullable
    private final URI yarnEndpoint;
    @NotNull
    private String clusterName;
    @Nullable
    private String userName;
    @Nullable
    private String password;

    public HDInsightLivyLinkClusterDetail(@NotNull URI livyEndpoint,
                                          @Nullable URI yarnEndpoint,
                                          @NotNull String clusterName,
                                          @Nullable String userName,
                                          @Nullable String password) {
        this.clusterName = clusterName;
        this.userName = userName;
        this.password = password;
        this.livyEndpoint = livyEndpoint;
        this.yarnEndpoint = yarnEndpoint;
    }

    @Override
    @NotNull
    public String getConnectionUrl() {
        return livyEndpoint.toString().endsWith("/") ? livyEndpoint.toString() : livyEndpoint.toString() + "/";
    }

    @Override
    @NotNull
    public String getLivyConnectionUrl() {
        return getConnectionUrl();
    }

    @Override
    @Nullable
    public String getYarnNMConnectionUrl() {
        return Optional.ofNullable(yarnEndpoint)
                .filter(endpoint -> endpoint != null)
                .map(endpoint -> endpoint.toString().endsWith("/") ? endpoint.toString() : endpoint.toString() + "/")
                .map(url -> url + "ws/v1/cluster/apps/")
                .orElse(null);
    }

    @Override
    @NotNull
    public String getName() {
        return clusterName;
    }

    @Override
    @NotNull
    public String getTitle() {
        return Optional.ofNullable(getSparkVersion())
                .filter(ver -> !ver.trim().isEmpty())
                .map(ver -> getName() + " (Spark: " + ver + " Linked)")
                .orElse(getName() + " [Linked]");
    }

    @Override
    @NotNull
    public Subscription getSubscription() {
        return new Subscription("[LinkedCluster]", "[NoSubscription]", "", false);
    }

    @Override
    @Nullable
    public String getHttpUserName() throws HDIException {
        return userName;
    }

    @Override
    @Nullable
    public String getHttpPassword() throws HDIException {
        return password;
    }

    @Override
    public SparkSubmitStorageType getDefaultStorageType() {
        return SparkSubmitStorageType.SPARK_INTERACTIVE_SESSION;
    }

    @Override
    public SparkSubmitStorageTypeOptionsForCluster getStorageOptionsType() {
        return SparkSubmitStorageTypeOptionsForCluster.HdiAdditionalClusterWithUndetermineStorage;
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
