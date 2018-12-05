package com.microsoft.azure.sqlbigdata.sdk.cluster;

import com.microsoft.azure.hdinsight.sdk.cluster.IClusterDetail;
import com.microsoft.azure.hdinsight.sdk.cluster.LivyCluster;
import com.microsoft.azure.hdinsight.sdk.cluster.YarnCluster;
import com.microsoft.azure.hdinsight.sdk.common.HDIException;
import com.microsoft.azuretools.authmanage.models.SubscriptionDetail;
import com.microsoft.azuretools.azurecommons.helpers.NotNull;
import com.microsoft.azuretools.azurecommons.helpers.Nullable;

import java.net.URI;
import java.util.Optional;

public class SqlBigDataLivyLinkClusterDetail implements IClusterDetail, LivyCluster, YarnCluster {
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

    public SqlBigDataLivyLinkClusterDetail(@NotNull URI livyEndpoint,
                                           @Nullable URI yarnEndpoint,
                                           @NotNull String clusterName,
                                           @NotNull String userName,
                                           @NotNull String password) {
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
        if (yarnEndpoint == null) {
            return null;
        } else {
            return yarnEndpoint.toString().endsWith("/") ? yarnEndpoint.toString() : yarnEndpoint.toString() + "/";
        }
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
