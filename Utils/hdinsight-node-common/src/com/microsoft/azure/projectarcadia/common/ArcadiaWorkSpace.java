/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.projectarcadia.common;

import com.google.common.collect.ImmutableSortedSet;
import com.microsoft.azure.hdinsight.common.logger.ILogger;
import com.microsoft.azure.hdinsight.sdk.cluster.ClusterContainer;
import com.microsoft.azure.hdinsight.sdk.cluster.IClusterDetail;
import com.microsoft.azure.hdinsight.sdk.common.AzureHttpObservable;
import com.microsoft.azure.hdinsight.sdk.rest.azure.synapse.models.*;
import com.microsoft.azure.synapsesoc.common.SynapseCosmosSparkPool;
import com.microsoft.azure.toolkit.lib.common.model.Subscription;
import com.microsoft.azuretools.authmanage.CommonSettings;
import com.microsoft.azuretools.azurecommons.helpers.NotNull;
import com.microsoft.azuretools.azurecommons.helpers.Nullable;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import rx.Observable;

import java.net.URI;
import java.util.Optional;

public class ArcadiaWorkSpace implements ClusterContainer, Comparable<ArcadiaWorkSpace>, ILogger {
    private static final String REST_SEGMENT_SPARK_COMPUTES = "/bigDataPools";

    @NotNull
    private final Subscription subscription;

    @NotNull
    private Workspace workspaceResponse;

    @NotNull
    private ImmutableSortedSet<? extends IClusterDetail> clusters = ImmutableSortedSet.of();

    @NotNull
    private final URI uri;

    @NotNull
    private final String name;

    @NotNull
    private final AzureHttpObservable http;

    public ArcadiaWorkSpace(@NotNull Subscription subscription, @NotNull Workspace workspaceResponse) {
        this.subscription = subscription;
        this.workspaceResponse = workspaceResponse;
        this.name = workspaceResponse.name();
        this.uri = URI.create(CommonSettings.getAdEnvironment().resourceManagerEndpoint()).resolve(workspaceResponse.id());
        this.http = new AzureHttpObservable(subscription, ApiVersion.VERSION);
    }

    @NotNull
    @Override
    public ImmutableSortedSet<? extends IClusterDetail> getClusters() {
        return ImmutableSortedSet.copyOf(
                this.clusters.stream().filter(cluster -> ((ArcadiaSparkCompute) cluster).isRunning()).iterator());
    }

    @NotNull
    public Observable<ArcadiaWorkSpace> fetchClusters() {
        return getSparkComputesRequest()
                .map(this::updateWithResponse)
                .defaultIfEmpty(this);
    }

    @NotNull
    private Observable<BigDataPoolResourceInfoListResult> getSparkComputesRequest() {
        String url = getUri().toString() + REST_SEGMENT_SPARK_COMPUTES;

        return getHttp()
                .withUuidUserAgent()
                .get(url, null, null, BigDataPoolResourceInfoListResult.class);
    }

    private ArcadiaWorkSpace updateWithResponse(@NotNull BigDataPoolResourceInfoListResult response) {
        this.clusters =
                ImmutableSortedSet.copyOf(response.items().stream()
                        .map(sparkCompute ->
                                StringUtils.isBlank(this.workspaceResponse.adlaResourceId())
                                        ? new ArcadiaSparkCompute(this, sparkCompute)
                                        : new SynapseCosmosSparkPool(
                                        this, sparkCompute, this.workspaceResponse.adlaResourceId()))
                        .iterator());
        return this;
    }

    @NotNull
    @Override
    public ClusterContainer refresh() {
        try {
            return fetchClusters().toBlocking().singleOrDefault(this);
        } catch (Exception ignored) {
            log().warn("Got Exceptions when refreshing Apache Spark Pool for Azure Synapse. " + ExceptionUtils.getStackTrace(ignored));
            return this;
        }
    }

    public Observable<ArcadiaWorkSpace> get() {
        return getHttp()
                .withUuidUserAgent()
                .get(getUri().toString(), null, null, Workspace.class)
                .doOnNext(workspaceResponse -> this.workspaceResponse = workspaceResponse)
                .map(workspace -> this);
    }

    public boolean isRunning() {
        if (getProvisioningState() == null) {
            return false;
        }

        return getProvisioningState().equals(WorkspaceProvisioningState.PROVISIONING)
                || getProvisioningState().equals(WorkspaceProvisioningState.SUCCEEDED);
    }

    @Nullable
    public WorkspaceProvisioningState getProvisioningState() {
        return WorkspaceProvisioningState.fromString(this.workspaceResponse.provisioningState());
    }

    @NotNull
    public String getState() {
        return Optional.ofNullable(getProvisioningState()).map(state -> state.toString()).orElse("Unknown");
    }

    @NotNull
    public String getTitleForNode() {
        if (getState().equalsIgnoreCase(WorkspaceProvisioningState.SUCCEEDED.toString())) {
            return getName();
        } else {
            return String.format("%s [%s]", getName(), getState());
        }
    }

    @NotNull
    public String getName() {
        return this.name;
    }

    @NotNull
    public URI getUri() {
        return this.uri;
    }

    @NotNull
    public String getId() {
        return this.workspaceResponse.id();
    }

    @Nullable
    public String getSparkUrl() {
        return this.workspaceResponse.connectivityEndpoints().getOrDefault("dev", null);
    }

    @Nullable
    public String getWebUrl() {
        return this.workspaceResponse.connectivityEndpoints().getOrDefault("web", null);
    }

    @NotNull
    public AzureHttpObservable getHttp() {
        return http;
    }

    @NotNull
    public Subscription getSubscription() {
        return subscription;
    }

    public Workspace getWorkspaceResponse() {
        return workspaceResponse;
    }

    @Nullable
    public DataLakeStorageAccountDetails getStorageAccountDetails() {
        return getWorkspaceResponse().defaultDataLakeStorage();
    }

    @Override
    public int compareTo(@NotNull ArcadiaWorkSpace other) {
        if (this == other) {
            return 0;
        }

        return this.getTitleForNode().compareTo(other.getTitleForNode());
    }
}
