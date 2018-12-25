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

import com.microsoft.azure.hdinsight.sdk.common.HDIException;
import com.microsoft.azure.hdinsight.spark.common.SparkSubmitStorageType;
import com.microsoft.azure.hdinsight.spark.common.SparkSubmitStorageTypeOptionsForCluster;
import com.microsoft.azuretools.authmanage.models.SubscriptionDetail;
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
    public SubscriptionDetail getSubscription() {
        return new SubscriptionDetail("[LinkedCluster]", "[NoSubscription]", "", false);
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
}
