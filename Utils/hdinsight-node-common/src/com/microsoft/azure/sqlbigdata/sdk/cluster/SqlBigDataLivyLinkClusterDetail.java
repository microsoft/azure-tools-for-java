package com.microsoft.azure.sqlbigdata.sdk.cluster;

import com.microsoft.azure.hdinsight.sdk.cluster.IClusterDetail;
import com.microsoft.azure.hdinsight.sdk.cluster.LivyCluster;
import com.microsoft.azure.hdinsight.sdk.cluster.SparkCluster;
import com.microsoft.azure.hdinsight.sdk.cluster.YarnCluster;
import com.microsoft.azure.hdinsight.sdk.common.HDIException;
import com.microsoft.azuretools.authmanage.models.SubscriptionDetail;
import com.microsoft.azuretools.azurecommons.helpers.NotNull;
import com.microsoft.azuretools.azurecommons.helpers.Nullable;

import java.net.URI;
import java.util.Optional;

public class SqlBigDataLivyLinkClusterDetail implements IClusterDetail, LivyCluster, YarnCluster {
    @NotNull
    private String host;
    private int knoxPort;
    @NotNull
    private String selectedCluster;
    @NotNull
    private String clusterName;
    @NotNull
    private String userName;
    @NotNull
    private String password;

    public SqlBigDataLivyLinkClusterDetail(@NotNull String host,
                                           int knoxPort,
                                           @NotNull String selectedCluster,
                                           @NotNull String clusterName,
                                           @NotNull String userName,
                                           @NotNull String password) {
        this.host = host;
        this.knoxPort = knoxPort;
        this.selectedCluster = selectedCluster;
        this.clusterName = clusterName;
        this.userName = userName;
        this.password = password;
    }

    @NotNull
    public String getHost() {
        return host;
    }

    @NotNull
    public String getSelectedCluster() {
        return selectedCluster;
    }

    public int getKnoxPort() {
        return knoxPort;
    }

    @Override
    @NotNull
    public String getConnectionUrl() {
        return String.format("https://%s:%d/gateway/%s/livy/v1/", host, knoxPort, selectedCluster);
    }

    @Override
    @NotNull
    public String getLivyConnectionUrl() {
        return getConnectionUrl();
    }

    @Override
    @NotNull
    public String getYarnNMConnectionUrl() {
        return String.format("https://%s:%d/gateway/%s/yarn/", host, knoxPort, selectedCluster);
    }

    @NotNull
    public String getSparkHistoryUrl() {
        return String.format("https://%s:%d/gateway/%s/sparkhistory/", host, knoxPort, selectedCluster);
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

}
