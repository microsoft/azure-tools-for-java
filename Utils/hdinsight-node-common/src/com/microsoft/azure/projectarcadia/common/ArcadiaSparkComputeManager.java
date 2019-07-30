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
import com.microsoft.azure.hdinsight.sdk.rest.azure.projectarcadia.models.ApiVersion;
import com.microsoft.azure.hdinsight.sdk.rest.azure.projectarcadia.models.GetWorkspaceListResponse;
import com.microsoft.azure.hdinsight.sdk.rest.azure.projectarcadia.models.Workspace;
import com.microsoft.azuretools.adauth.AuthException;
import com.microsoft.azuretools.authmanage.AuthMethodManager;
import com.microsoft.azuretools.authmanage.CommonSettings;
import com.microsoft.azuretools.authmanage.models.SubscriptionDetail;
import com.microsoft.azuretools.azurecommons.helpers.NotNull;
import com.microsoft.azuretools.azurecommons.helpers.Nullable;
import com.microsoft.azuretools.sdkmanage.AzureManager;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.apache.commons.lang3.tuple.Pair;
import rx.Observable;
import rx.schedulers.Schedulers;

import java.io.IOException;
import java.net.URI;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ArcadiaSparkComputeManager implements ClusterContainer, ILogger {
    private static class LazyHolder {
        static final ArcadiaSparkComputeManager INSTANCE = new ArcadiaSparkComputeManager();
    }

    public static ArcadiaSparkComputeManager getInstance() {
        return LazyHolder.INSTANCE;
    }

    private static final String REST_SEGMENT_SUBSCRIPTION = "/subscriptions/";
    private static final String REST_SEGMENT_WORKSPACES = "providers/Microsoft.ProjectArcadia/workspaces";

    @NotNull
    private ImmutableSortedSet<? extends ArcadiaWorkSpace> workSpaces = ImmutableSortedSet.of();

    @NotNull
    private final Map<String, AzureHttpObservable> httpMap = new HashMap<>();

    @NotNull

    @Nullable
    public AzureManager getAzureManager() {
        try {
            return AuthMethodManager.getInstance().getAzureManager();
        } catch (IOException e) {
            log().info("Failed to get AzureManager. Error: " + e);

            return null;
        }
    }

    public ArcadiaSparkComputeManager() {
        AuthMethodManager.getInstance().addSignOutEventListener(() -> workSpaces = ImmutableSortedSet.of());
        AzureManager azureManager = getAzureManager();
        if (azureManager != null) {
            azureManager.getSubscriptionManager().addListener(ev -> workSpaces = ImmutableSortedSet.of());
        }
    }

    @NotNull
    @Override
    public ImmutableSortedSet<? extends IClusterDetail> getClusters() {
        if (getAzureManager() == null) {
            return ImmutableSortedSet.of();
        }

        return ImmutableSortedSet.copyOf(
                getWorkspaces().stream()
                        .flatMap(workSpace -> workSpace.getClusters().stream())
                        .iterator()
        );
    }

    @NotNull
    @Override
    public ClusterContainer refresh() {
        if (getAzureManager() == null) {
            return this;
        }

        try {
            return fetchWorkSpaces().toBlocking().singleOrDefault(this);
        } catch (Exception ignored) {
            log().warn("Got exceptions when refreshing Arcadia workspaces. " + ExceptionUtils.getStackTrace(ignored));
            return this;
        }
    }

    @NotNull
    public Observable<ArcadiaSparkComputeManager> fetchClusters() {
        return fetchWorkSpaces()
                .map(ArcadiaSparkComputeManager::getWorkspaces)
                .flatMap(Observable::from)
                .flatMap(workSpace ->
                        workSpace.fetchClusters()
                                .onErrorResumeNext(err -> {
                                    String errMsg = String.format("Got exceptions when refreshing spark computes. Workspace: %s. %s",
                                            workSpace.getName(), ExceptionUtils.getStackTrace(err));
                                    log().warn(errMsg);
                                    return Observable.empty();
                                })
                                .subscribeOn(Schedulers.io())
                )
                .map(workspace -> this)
                .defaultIfEmpty(this);
    }

    @NotNull
    public ImmutableSortedSet<? extends ArcadiaWorkSpace> getWorkspaces() {
        this.workSpaces =
                ImmutableSortedSet.copyOf(this.workSpaces.stream().filter(workspace -> workspace.isRunning()).iterator());
        return this.workSpaces;
    }

    @NotNull
    private URI getSubscriptionsUri(@NotNull String subscriptionId) {
        return URI.create(CommonSettings.getAdEnvironment().resourceManagerEndpoint())
                .resolve(REST_SEGMENT_SUBSCRIPTION)
                .resolve(subscriptionId);
    }

    @NotNull
    public Observable<ArcadiaSparkComputeManager> fetchWorkSpaces() {
        return getWorkSpacesRequest()
                .map(this::updateWithResponse)
                .defaultIfEmpty(this);
    }

    @NotNull
    private Observable<List<Pair<SubscriptionDetail, Workspace>>> getWorkSpacesRequest() {
        AzureManager azureManager = getAzureManager();
        if (azureManager == null) {
            return Observable.error(new AuthException(
                    "Can't get Arcadia workspaces since user doesn't sign in, please sign in by Azure Explorer."));
        }

        return Observable.fromCallable(() -> azureManager.getSubscriptionManager().getSelectedSubscriptionDetails())
                .flatMap(Observable::from)
                .map(sub -> Pair.of(
                        sub,
                        URI.create(getSubscriptionsUri(sub.getSubscriptionId()).toString() + "/")
                                .resolve(REST_SEGMENT_WORKSPACES))
                )
                .doOnNext(subAndWorkspaceUriPair -> log().debug("Pair(Subscription, WorkSpaceListUri): " + subAndWorkspaceUriPair.toString()))
                .flatMap(subAndWorkSpaceUriPair ->
                        getHttp(subAndWorkSpaceUriPair.getLeft())
                                .withUuidUserAgent()
                                .get(subAndWorkSpaceUriPair.getRight().toString(), null, null, GetWorkspaceListResponse.class)
                                .flatMap(resp -> Observable.from(resp.items()))
                                .onErrorResumeNext(err -> {
                                    log().warn("Got exceptions when listing workspace by subscription ID. " + ExceptionUtils.getStackTrace(err));
                                    return Observable.empty();
                                })
                                .map(workSpace -> Pair.of(subAndWorkSpaceUriPair.getLeft(), workSpace))
                                .subscribeOn(Schedulers.io())
                )
                .toList()
                .doOnNext(subAndWorkSpacePair -> log().debug("Pair(Subscription, WorkSpace) list: " + subAndWorkSpacePair.toString()));
    }

    @NotNull
    private ArcadiaSparkComputeManager updateWithResponse(@NotNull List<Pair<SubscriptionDetail, Workspace>> workSpacesResponse) {
        this.workSpaces =
                ImmutableSortedSet.copyOf(
                        workSpacesResponse
                                .stream()
                                .map(subAndWorkSpacePair ->
                                        new ArcadiaWorkSpace(subAndWorkSpacePair.getLeft(), subAndWorkSpacePair.getRight()))
                                .iterator()
                );
        return this;
    }

    @NotNull
    private synchronized AzureHttpObservable getHttp(@NotNull SubscriptionDetail subscriptionDetail) {
        if (httpMap.containsKey(subscriptionDetail.getSubscriptionId())) {
            return httpMap.get(subscriptionDetail.getSubscriptionId());
        }

        AzureHttpObservable subHttp = new AzureHttpObservable(subscriptionDetail, ApiVersion.VERSION);
        httpMap.put(subscriptionDetail.getSubscriptionId(), subHttp);

        return subHttp;
    }
}
