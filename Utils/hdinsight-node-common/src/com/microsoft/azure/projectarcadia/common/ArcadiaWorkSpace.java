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

package com.microsoft.azure.projectarcadia.common;

import com.google.common.collect.ImmutableSortedSet;
import com.microsoft.azure.hdinsight.common.logger.ILogger;
import com.microsoft.azure.hdinsight.sdk.cluster.ClusterContainer;
import com.microsoft.azure.hdinsight.sdk.cluster.IClusterDetail;
import com.microsoft.azure.hdinsight.sdk.common.AzureHttpObservable;
import com.microsoft.azure.hdinsight.sdk.rest.azure.synapse.models.ApiVersion;
import com.microsoft.azure.hdinsight.sdk.rest.azure.synapse.models.BigDataPoolResourceInfoListResult;
import com.microsoft.azure.hdinsight.sdk.rest.azure.synapse.models.Workspace;
import com.microsoft.azure.hdinsight.sdk.rest.azure.synapse.models.WorkspaceProvisioningState;
import com.microsoft.azuretools.authmanage.CommonSettings;
import com.microsoft.azuretools.authmanage.models.SubscriptionDetail;
import com.microsoft.azuretools.azurecommons.helpers.NotNull;
import com.microsoft.azuretools.azurecommons.helpers.Nullable;
import org.apache.commons.lang3.exception.ExceptionUtils;
import rx.Observable;

import java.net.URI;
import java.util.Optional;

public class ArcadiaWorkSpace implements ClusterContainer, Comparable<ArcadiaWorkSpace>, ILogger {
    private static final String REST_SEGMENT_SPARK_COMPUTES = "/bigDataPools";

    @NotNull
    private final SubscriptionDetail subscription;

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

    public ArcadiaWorkSpace(@NotNull SubscriptionDetail subscription, @NotNull Workspace workspaceResponse) {
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
                        .map(sparkCompute -> new ArcadiaSparkCompute(this, sparkCompute))
                        .iterator());
        return this;
    }

    @NotNull
    @Override
    public ClusterContainer refresh() {
        try {
            return fetchClusters().toBlocking().singleOrDefault(this);
        } catch (Exception ignored) {
            log().warn("Got Exceptions when refreshing Arcadia spark computes. " + ExceptionUtils.getStackTrace(ignored));
            return this;
        }
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
    public SubscriptionDetail getSubscription() {
        return subscription;
    }

    public Workspace getWorkspaceResponse() {
        return workspaceResponse;
    }

    @Override
    public int compareTo(@NotNull ArcadiaWorkSpace other) {
        if (this == other) {
            return 0;
        }

        return this.getTitleForNode().compareTo(other.getTitleForNode());
    }
}
