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
package com.microsoft.azure.sqlbigdata.sdk.cluster;

import com.microsoft.azure.hdinsight.sdk.cluster.IClusterDetail;
import com.microsoft.azure.hdinsight.sdk.cluster.InternalUrlMapping;
import com.microsoft.azure.hdinsight.sdk.cluster.LivyCluster;
import com.microsoft.azure.hdinsight.sdk.cluster.YarnCluster;
import com.microsoft.azure.hdinsight.sdk.common.HDIException;
import com.microsoft.azure.hdinsight.spark.common.SparkSubmitStorageType;
import com.microsoft.azure.hdinsight.spark.common.SparkSubmitStorageTypeOptionsForCluster;
import com.microsoft.azuretools.authmanage.models.SubscriptionDetail;
import com.microsoft.azuretools.azurecommons.helpers.NotNull;
import com.microsoft.azuretools.azurecommons.helpers.Nullable;
import org.apache.commons.lang3.StringUtils;

import java.net.URI;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class SqlBigDataLivyLinkClusterDetail implements IClusterDetail, LivyCluster, YarnCluster, InternalUrlMapping {
    @NotNull
    private String host;
    private int knoxPort;
    @NotNull
    private String clusterName;
    @NotNull
    private String userName;
    @NotNull
    private String password;

    public SqlBigDataLivyLinkClusterDetail(@NotNull String host,
                                           int knoxPort,
                                           @Nullable String clusterName,
                                           @NotNull String userName,
                                           @NotNull String password) {
        this.host = host;
        this.knoxPort = knoxPort;
        this.clusterName = StringUtils.isBlank(clusterName) ? host : clusterName;
        this.userName = userName;
        this.password = password;
    }

    @NotNull
    public String getHost() {
        return host;
    }

    public int getKnoxPort() {
        return knoxPort;
    }

    @Override
    @NotNull
    public String getConnectionUrl() {
        return String.format("https://%s:%d/gateway/default/", host, knoxPort);
    }

    @Override
    @NotNull
    public String getLivyConnectionUrl() {
        return URI.create(getConnectionUrl()).resolve("livy/v1/").toString();
    }

    @Override
    @NotNull
    public String getYarnNMConnectionUrl() {
        return URI.create(getConnectionUrl()).resolve("yarn/ws/v1/cluster/apps/").toString();
    }

    @Override
    @NotNull
    public String getYarnUIUrl() {
        return URI.create(getConnectionUrl()).resolve("yarn/").toString();
    }

    @NotNull
    public String getSparkHistoryUrl() {
        return URI.create(getConnectionUrl()).resolve("sparkhistory/").toString();
    }

    @Override
    @NotNull
    public String getName() {
        return clusterName;
    }

    @Override
    @NotNull
    public String getTitle() {
        return getName();
    }

    @Override
    @Nullable
    public SubscriptionDetail getSubscription() {
        return null;
    }

    @Override
    @NotNull
    public String getHttpUserName() throws HDIException {
        return userName;
    }

    @Override
    @NotNull
    public String getHttpPassword() throws HDIException {
        return password;
    }

    @Override
    public SparkSubmitStorageType getDefaultStorageType() {
        return SparkSubmitStorageType.SPARK_INTERACTIVE_SESSION;
    }

    @Override
    public SparkSubmitStorageTypeOptionsForCluster getStorageOptionsType() {
       return SparkSubmitStorageTypeOptionsForCluster.BigDataClusterWithWebHdfs;
    }

    @Override
    @NotNull
    public String mapInternalUrlToPublic(@NotNull String url) {
        // url example: http://mssql-master-pool-0.service-master-pool:8088/proxy/application_1544743878531_0035/
        Matcher yarnUiMatcher = Pattern.compile("proxy/(application_[0-9_]+)").matcher(url);
        if (yarnUiMatcher.find()) {
            String appId = yarnUiMatcher.group(1);
            return String.format("https://%s:%d/gateway/default/yarn/cluster/app/%s", host, knoxPort, appId);
        } else {
            return url;
        }
    }
}
