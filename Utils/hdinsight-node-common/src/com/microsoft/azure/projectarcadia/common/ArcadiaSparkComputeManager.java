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
import com.microsoft.azure.hdinsight.sdk.common.ODataParam;
import com.microsoft.azure.hdinsight.sdk.rest.azure.synapse.models.WorkspaceInfoListResult;
import com.microsoft.azure.toolkit.lib.Azure;
import com.microsoft.azure.toolkit.lib.auth.AzureAccount;
import com.microsoft.azure.toolkit.lib.common.event.AzureEventBus;
import com.microsoft.azure.toolkit.lib.common.model.Subscription;
import com.microsoft.azuretools.adauth.AuthException;
import com.microsoft.azuretools.authmanage.CommonSettings;
import com.microsoft.azuretools.azurecommons.helpers.NotNull;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.http.NameValuePair;
import rx.Observable;
import rx.schedulers.Schedulers;

import java.net.URI;
import java.util.Collections;
import java.util.List;

import static rx.Observable.concat;
import static rx.Observable.from;

public class ArcadiaSparkComputeManager implements ClusterContainer, ILogger {
    private static class LazyHolder {
        static final ArcadiaSparkComputeManager INSTANCE = new ArcadiaSparkComputeManager();
    }

    public static ArcadiaSparkComputeManager getInstance() {
        return LazyHolder.INSTANCE;
    }

    private static final String REST_SEGMENT_SUBSCRIPTION = "/subscriptions/";
    private static final String REST_SEGMENT_RESOURCES = "resources";
    private static final String SYNAPSE_WORKSPACE_FILTER = "resourceType eq 'Microsoft.Synapse/workspaces'";
    private static final String LIST_WORKSPACE_API_VERSION = "2017-03-01";

    @NotNull
    private ImmutableSortedSet<? extends ArcadiaWorkSpace> workSpaces = ImmutableSortedSet.of();

    public ArcadiaSparkComputeManager() {
        AzureEventBus.once("account.logged_out.account", (t, e) -> workSpaces = ImmutableSortedSet.of());
        AzureEventBus.on("account.subscription_changed.account", new AzureEventBus.EventListener(e -> workSpaces = ImmutableSortedSet.of()));
    }

    @NotNull
    @Override
    public ImmutableSortedSet<? extends IClusterDetail> getClusters() {
        if (!Azure.az(AzureAccount.class).isLoggedIn()) {
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
        if (!Azure.az(AzureAccount.class).isLoggedIn()) {
            return this;
        }

        try {
            return fetchWorkSpaces().toBlocking().singleOrDefault(this);
        } catch (Exception ignored) {
            log().warn("Got exceptions when refreshing Synapse workspaces. " + ExceptionUtils.getStackTrace(ignored));
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
                                    String errMsg = String.format("Got exceptions when refreshing spark pools. Workspace: %s. %s",
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

    public List<NameValuePair> getSynapseWorkspaceFilter() {
        return Collections.singletonList(ODataParam.filter(SYNAPSE_WORKSPACE_FILTER));
    }

    @NotNull
    private Observable<List<ArcadiaWorkSpace>> getWorkSpacesRequest() {
        if (!Azure.az(AzureAccount.class).isLoggedIn()) {
            return Observable.error(new AuthException(
                    "Can't get Synapse workspaces since user doesn't sign in, please sign in by Azure Explorer."));
        }

        return Observable.fromCallable(() -> Azure.az(AzureAccount.class).account().getSelectedSubscriptions())
                .flatMap(Observable::from)
                .map(sub -> Pair.of(
                        sub,
                        URI.create(getSubscriptionsUri(sub.getId()).toString() + "/")
                                .resolve(REST_SEGMENT_RESOURCES))
                )
                .doOnNext(subAndWorkspaceUriPair -> log().debug("Pair(Subscription, WorkSpaceListUri): " + subAndWorkspaceUriPair.toString()))
                .flatMap(subAndWorkSpaceUriPair ->
                        buildHttp(subAndWorkSpaceUriPair.getLeft())
                                .withUuidUserAgent()
                                .get(subAndWorkSpaceUriPair.getRight().toString(), getSynapseWorkspaceFilter(), null, WorkspaceInfoListResult.class)
                                .flatMap(resp -> Observable.from(resp.items()))
                                .onErrorResumeNext(err -> {
                                    log().warn("Got exceptions when listing workspace by subscription ID. " + ExceptionUtils.getStackTrace(err));
                                    return Observable.empty();
                                })
                                // Filter workspaces only in provisioning state or success state
                                .map(workspace -> new ArcadiaWorkSpace(subAndWorkSpaceUriPair.getLeft(), workspace))
                                // Run the time-consuming task concurrently in IO thread
                                .flatMap(arcadiaWorkSpace -> arcadiaWorkSpace
                                        .get()
                                        .onErrorResumeNext(err -> {
                                            log().warn(String.format("Got exceptions when getting workspace %s details. %s",
                                                    arcadiaWorkSpace.getName(), ExceptionUtils.getStackTrace(err)));
                                            return Observable.empty();
                                        })
                                        .subscribeOn(Schedulers.io()))
                                .filter(ArcadiaWorkSpace::isRunning)
                                .subscribeOn(Schedulers.io())
                )
                .toList();
    }

    @NotNull
    private ArcadiaSparkComputeManager updateWithResponse(@NotNull List<ArcadiaWorkSpace> arcadiaWorkspace) {
        this.workSpaces = ImmutableSortedSet.copyOf(arcadiaWorkspace);
        return this;
    }

    @NotNull
    private AzureHttpObservable buildHttp(@NotNull Subscription subscriptionDetail) {
        return new AzureHttpObservable(subscriptionDetail, LIST_WORKSPACE_API_VERSION);
    }

    @NotNull
    public Observable<? extends ArcadiaSparkCompute> findCompute(final @NotNull String tenantId,
                                                                 final @NotNull String workspaceName,
                                                                 final @NotNull String computeName) {
        return concat(from(getClusters()), fetchClusters().flatMap(computeManager -> from(computeManager.getClusters())))
                .map(ArcadiaSparkCompute.class::cast)
                .filter(compute -> compute.getWorkSpace().getName().equals(workspaceName)
                        && compute.getSubscription().getTenantId().equals(tenantId)
                        && compute.getName().equals(computeName));
    }

    @NotNull
    public Observable<? extends ArcadiaWorkSpace> findWorkspace(final @NotNull String tenantId,
                                                                final @NotNull String workspaceName) {
        return concat(from(getWorkspaces()), fetchWorkSpaces().flatMap(manager -> from(manager.getWorkspaces())))
                .filter(workspace -> workspace.getSubscription().getTenantId().equals(tenantId)
                        && workspace.getName().equals(workspaceName));
    }
}
