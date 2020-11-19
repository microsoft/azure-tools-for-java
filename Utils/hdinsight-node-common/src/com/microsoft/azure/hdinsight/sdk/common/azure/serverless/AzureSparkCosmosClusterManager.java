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

package com.microsoft.azure.hdinsight.sdk.common.azure.serverless;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSortedSet;
import com.microsoft.azure.AzureEnvironment;
import com.microsoft.azure.hdinsight.common.logger.ILogger;
import com.microsoft.azure.hdinsight.sdk.cluster.ClusterContainer;
import com.microsoft.azure.hdinsight.sdk.cluster.IClusterDetail;
import com.microsoft.azure.hdinsight.sdk.common.AzureHttpObservable;
import com.microsoft.azure.hdinsight.sdk.common.AzureManagementHttpObservable;
import com.microsoft.azure.hdinsight.sdk.common.ODataParam;
import com.microsoft.azure.hdinsight.sdk.common.SparkAzureDataLakePoolServiceException;
import com.microsoft.azure.hdinsight.sdk.rest.azure.datalake.analytics.accounts.models.ApiVersion;
import com.microsoft.azure.hdinsight.sdk.rest.azure.datalake.analytics.accounts.models.DataLakeAnalyticsAccount;
import com.microsoft.azure.hdinsight.sdk.rest.azure.datalake.analytics.accounts.models.DataLakeAnalyticsAccountBasic;
import com.microsoft.azure.hdinsight.sdk.rest.azure.datalake.analytics.accounts.models.api.GetAccountsListResponse;
import com.microsoft.azuretools.adauth.AuthException;
import com.microsoft.azuretools.authmanage.AuthMethodManager;
import com.microsoft.azuretools.authmanage.CommonSettings;
import com.microsoft.azuretools.authmanage.models.SubscriptionDetail;
import com.microsoft.azuretools.azurecommons.helpers.NotNull;
import com.microsoft.azuretools.azurecommons.helpers.Nullable;
import com.microsoft.azuretools.sdkmanage.AzureManager;
import com.microsoft.azuretools.telemetry.TelemetryConstants;
import com.microsoft.azuretools.telemetrywrapper.ErrorType;
import com.microsoft.azuretools.telemetrywrapper.EventType;
import com.microsoft.azuretools.telemetrywrapper.EventUtil;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.commons.lang3.tuple.Triple;
import org.apache.http.NameValuePair;
import rx.Observable;

import java.io.IOException;
import java.net.URI;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static rx.Observable.*;

public class AzureSparkCosmosClusterManager implements ClusterContainer,
                                                           ILogger {
    // Lazy singleton initialization
    private static class LazyHolder {
        static final AzureSparkCosmosClusterManager INSTANCE =
                new AzureSparkCosmosClusterManager();
    }
    public static AzureSparkCosmosClusterManager getInstance() {
        return LazyHolder.INSTANCE;
    }

    //
    // Fields
    //
    private static final String REST_SEGMENT_SUBSCRIPTION = "/subscriptions/";
    private static final String REST_SEGMENT_ADL_ACCOUNT = "providers/Microsoft.DataLakeAnalytics/accounts";

    // FIXME!!!
    private static final String ACCOUNT_FILTER = CommonSettings.getAdEnvironment().endpoints()
            .getOrDefault("dataLakeSparkAccountFilter",
                    "length(name) gt 4 and substring(name, length(name) sub 4) ge '-c00' and substring(name, length(name) sub 4) le '-c99'");

    @NotNull
    private final HashMap<String, AzureHttpObservable> httpMap = new HashMap<>();

    @NotNull
    private AzureEnvironment azureEnv = CommonSettings.getAdEnvironment();

    @NotNull
    private ImmutableSortedSet<? extends AzureSparkServerlessAccount> accounts= ImmutableSortedSet.of();

    public AzureSparkCosmosClusterManager() {
        this.httpMap.put("common", new AzureHttpObservable(ApiVersion.VERSION));

        // Invalid cached accounts when signing out or changing subscription selection
        AuthMethodManager.getInstance().addSignOutEventListener(() -> accounts = ImmutableSortedSet.of());
        if (getAzureManager() != null) {
            getAzureManager().getSubscriptionManager().addListener(ev -> accounts = ImmutableSortedSet.of());
        }
    }

    //
    // Getters / setters
    //

    @Nullable
    public List<NameValuePair> getAccountFilter() {
        return Collections.singletonList(ODataParam.filter(ACCOUNT_FILTER));
    }

    @NotNull
    public HashMap<String, AzureHttpObservable> getHttpMap() {
        return httpMap;
    }

    @NotNull
    public ImmutableSortedSet<? extends AzureSparkServerlessAccount> getAccounts() {
        return accounts;
    }

    @Nullable
    public AzureSparkServerlessAccount getAccountByName(@NotNull String name) {
        return getAccounts().stream()
                .filter(account -> account.getName().equals(name))
                .findFirst()
                .orElse(null);
    }

    @Nullable
    public AzureManager getAzureManager() {
        try {
            return AuthMethodManager.getInstance().getAzureManager();
        } catch (IOException e) {
            log().info("Can't get Azure manager now, please sign in. error: " + e);

            return null;
        }
    }

    /**
     * Get the cached clusters, non-block
     *
     * @return Immutable sorted IClusterDetail set
     */
    @NotNull
    @Override
    public ImmutableSortedSet<? extends IClusterDetail> getClusters() {
        if (getAzureManager() == null) {
            return ImmutableSortedSet.of();
        }

        return ImmutableSortedSet.copyOf(accounts.stream()
                .flatMap(account -> account.getClusters().stream())
                .iterator());
    }

    @NotNull
    @Override
    public ClusterContainer refresh() {
        if (getAzureManager() == null) {
            return this;
        }

        try {
            return get().toBlocking().singleOrDefault(this);
        } catch (Exception ex) {
            log().warn("Got exceptions when refresh Apache Spark pool on Cosmos: " + ex);

            return this;
        }
    }

    public Observable<AzureSparkCosmosClusterManager> get() {
        return getAzureDataLakeAccountsRequest()
                .map(this::updateWithResponse)
                .defaultIfEmpty(this);
    }

    /**
     * Deep fetch all accounts' clusters
     *
     * @return Chained call result of this
     */
    public Observable<AzureSparkCosmosClusterManager> fetchClusters() {
        return get()
                .map(AzureSparkCosmosClusterManager::getAccounts)
                .flatMap(Observable::from)
                .flatMap(account -> account.get().onErrorReturn(err -> {
                    log().warn(String.format("Can't get the account %s details: %s", account.getName(), err));

                    return account;
                }))
                .map(account -> account.getClusters())
                .flatMap(Observable::from)
                .flatMap(cluster -> ((AzureSparkCosmosCluster)cluster).get().onErrorReturn(err -> {
                    log().warn(String.format("Can't get the cluster %s details: %s", cluster.getName(), err));

                    return (AzureSparkCosmosCluster) cluster;
                }))
                .map(clusters -> this)
                .defaultIfEmpty(this);
    }

    private Observable<List<Triple<SubscriptionDetail, DataLakeAnalyticsAccountBasic, DataLakeAnalyticsAccount>>>
    getAzureDataLakeAccountsRequest() {
        if (getAzureManager() == null) {
            return Observable.error(new AuthException(
                    "Can't get Azure Data Lake account since the user isn't signed in, please sign in by Azure Explorer."));
        }

        // Loop subscriptions to get all accounts
        return Observable
                .fromCallable(() -> getAzureManager().getSubscriptionManager().getSelectedSubscriptionDetails())
                .flatMap(Observable::from)             // Get Subscription details one by one
                .map(sub -> Pair.of(
                        sub,
                        URI.create(getSubscriptionsUri(sub.getSubscriptionId()).toString() + "/")
                           .resolve(REST_SEGMENT_ADL_ACCOUNT)))
                .doOnNext(pair -> log().debug("Pair(Subscription, AccountsListUri): " + pair.toString()))
                .map(subUriPair -> Pair.of(
                        subUriPair.getLeft(),
                        getHttp(subUriPair.getLeft())
                                .withUuidUserAgent()
                                .get(subUriPair.getRight().toString(),
                                        getAccountFilter(),
                                        null,
                                        // FIXME!!! Needs to support paging
                                        GetAccountsListResponse.class)))
                // account basic list -> account basic
                .flatMap(subAccountsObPair -> subAccountsObPair.getRight()
                                .onErrorResumeNext(err -> {
                                    log().warn(String.format("Ignore subscription %s(%s) with exception",
                                                             subAccountsObPair.getLeft().getSubscriptionName(),
                                                             subAccountsObPair.getLeft().getSubscriptionId()),
                                            err);

                                    return empty();
                                })
                                .flatMap(accountsResp -> Observable.from(accountsResp.items()))
                                .map(accountBasic -> Pair.of(subAccountsObPair.getLeft(), accountBasic)))
                .flatMap(subAccountBasicPair -> {
                    // accountBasic.id is the account detail absolute URI path
                    URI accountDetailUri = getResourceManagerEndpoint().resolve(subAccountBasicPair.getRight().id());

                    // Get account details
                    return getHttp(subAccountBasicPair.getLeft())
                            .withUuidUserAgent()
                            .get(accountDetailUri.toString(), null, null, DataLakeAnalyticsAccount.class)
                            .onErrorResumeNext(err -> {
                                log().warn("Failed to get the account detail: " + accountDetailUri, err);

                                return empty();
                            })
                            .map(accountDetail -> Triple.of(
                                    subAccountBasicPair.getLeft(), subAccountBasicPair.getRight(), accountDetail));
                })
                .toList()
                .doOnNext(triples -> log().debug("Triple(Subscription, AccountBasic, AccountDetails) list: " + triples.toString()));
    }

    @NotNull
    private synchronized AzureHttpObservable getHttp(SubscriptionDetail subscriptionDetail) {
        if (httpMap.containsKey(subscriptionDetail.getSubscriptionId())) {
            return httpMap.get(subscriptionDetail.getSubscriptionId());
        }

        AzureHttpObservable subHttp = new AzureManagementHttpObservable(subscriptionDetail, ApiVersion.VERSION);
        httpMap.put(subscriptionDetail.getSubscriptionId(), subHttp);

        return subHttp;
    }

    @NotNull
    private URI getResourceManagerEndpoint() {
        return URI.create(azureEnv.resourceManagerEndpoint());
    }

    @NotNull
    private URI getSubscriptionsUri(@NotNull String subscriptionId) {
        return getResourceManagerEndpoint()
                .resolve(REST_SEGMENT_SUBSCRIPTION)
                .resolve(subscriptionId);
    }

    @NotNull
    private AzureSparkCosmosClusterManager updateWithResponse(
            List<Triple<SubscriptionDetail, DataLakeAnalyticsAccountBasic, DataLakeAnalyticsAccount>> accountsResponse) {
        accounts = ImmutableSortedSet.copyOf(accountsResponse
                .stream()
                .map(subAccountBasicDetailTriple ->     // Triple: subscription, accountBasic, accountDetail
                        new AzureSparkServerlessAccount(
                                subAccountBasicDetailTriple.getLeft(),
                                // endpoint property is account's base URI
                                URI.create("https://" + subAccountBasicDetailTriple.getMiddle().endpoint()),
                                           subAccountBasicDetailTriple.getMiddle().name())
                                   .setBasicResponse(subAccountBasicDetailTriple.getMiddle())
                                   .setDetailResponse(subAccountBasicDetailTriple.getRight()))
                .iterator());

        return this;
    }

    public Observable<? extends AzureSparkCosmosCluster> findCluster(@NotNull String accountName, @NotNull String clusterGuid) {
        return concat(from(getAccounts()), get().flatMap(manager -> from(manager.getAccounts())))
                .filter(account -> account.getName().equals(accountName))
                .first()
                .flatMap(account -> concat(from(account.getClusters()), account.get().flatMap(acct -> from(acct.getClusters()))))
                .map(AzureSparkCosmosCluster.class::cast)
                .filter(cluster -> cluster.getGuid().equals(clusterGuid))
                .first();
    }

    public Observable<Boolean> isFeatureEnabled() {
        return concat(from(getAccounts()), get().flatMap(manager -> from(manager.getAccounts())))
                .isEmpty()
                .map(isEmpty -> !isEmpty)
                .onErrorReturn(err -> {
                    log().warn("Checking Spark on Cosmos got error: " + err);

                    return false;
                });
    }

    public void sendErrorTelemetry(@NotNull String operationName, @NotNull Throwable ex, @Nullable String clusterGuid) {
        Map<String, String> props = new HashMap<>();
        props.put("clusterGuid", clusterGuid);
        props.put("isSucceed", "false");
        if (ex instanceof SparkAzureDataLakePoolServiceException) {
            SparkAzureDataLakePoolServiceException serviceException = (SparkAzureDataLakePoolServiceException) ex;
            props.put("requestUri", serviceException.getRequestUri() != null ? serviceException.getRequestUri().toString() : "");
            props.put("statusCode", String.valueOf(serviceException.getStatusCode()));
            props.put("x-ms-request-id", serviceException.getRequestId());
            EventUtil.logErrorClassNameOnly(TelemetryConstants.SPARK_ON_COSMOS, operationName, ErrorType.serviceError, serviceException,  props, null);
        } else {
            EventUtil.logErrorClassNameOnly(TelemetryConstants.SPARK_ON_COSMOS, operationName, ErrorType.unclassifiedError, ex,  props, null);
        }
    }

    public void sendInfoTelemetry(@NotNull String operationName, @Nullable String clusterGuid) {
        Map<String, String> props = ImmutableMap.of(
                "statusCode", "200",
                "isSucceed", "true",
                "clusterGuid", clusterGuid);
        EventUtil.logEvent(EventType.info, TelemetryConstants.SPARK_ON_COSMOS, operationName, props);

    }
}
