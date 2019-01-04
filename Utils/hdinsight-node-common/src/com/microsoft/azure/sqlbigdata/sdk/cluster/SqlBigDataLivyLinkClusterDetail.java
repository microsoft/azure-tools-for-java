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

import java.util.stream.Stream;

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
        return String.format("https://%s:%d/gateway/default/livy/v1/", host, knoxPort);
    }

    @Override
    @NotNull
    public String getLivyConnectionUrl() {
        return getConnectionUrl();
    }

    @Override
    @NotNull
    public String getYarnNMConnectionUrl() {
        return String.format("https://%s:%d/gateway/default/yarn/", host, knoxPort);
    }

    @NotNull
    public String getSparkHistoryUrl() {
        return String.format("https://%s:%d/gateway/default/sparkhistory/", host, knoxPort);
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
        // Extract application ID from internal URL
        // url example: http://mssql-master-pool-0.service-master-pool:8088/proxy/application_1544743878531_0035/
        String appId = Stream.of(url.split("/")).filter(str -> str.startsWith("application_")).findFirst().orElse(null);
        return appId == null
                ? String.format("https://%s:%d/gateway/default/yarn/", host, knoxPort)
                : String.format("https://%s:%d/gateway/default/yarn/cluster/app/%s", host, knoxPort, appId);
    }
}
