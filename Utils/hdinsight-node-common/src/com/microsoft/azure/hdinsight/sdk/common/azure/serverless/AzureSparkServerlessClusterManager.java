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

import com.google.common.collect.ImmutableSortedSet;
import com.microsoft.azure.AzureEnvironment;
import com.microsoft.azure.hdinsight.common.logger.ILogger;
import com.microsoft.azure.hdinsight.sdk.cluster.IClusterDetail;
import com.microsoft.azure.hdinsight.sdk.cluster.ClusterContainer;
import com.microsoft.azure.hdinsight.sdk.common.AzureHttpObservable;
import com.microsoft.azure.hdinsight.sdk.common.AzureManagementHttpObservable;
import com.microsoft.azure.hdinsight.sdk.common.ODataParam;
import com.microsoft.azure.hdinsight.sdk.rest.azure.datalake.analytics.accounts.api.GetAccountsListResponse;
import com.microsoft.azure.hdinsight.sdk.rest.azure.datalake.analytics.accounts.models.ApiVersion;
import com.microsoft.azure.hdinsight.sdk.rest.azure.datalake.analytics.accounts.models.DataLakeAnalyticsAccountBasic;
import com.microsoft.azuretools.authmanage.AuthMethodManager;
import com.microsoft.azuretools.authmanage.CommonSettings;
import com.microsoft.azuretools.authmanage.models.SubscriptionDetail;
import com.microsoft.azuretools.azurecommons.helpers.NotNull;
import com.microsoft.azuretools.azurecommons.helpers.Nullable;
import com.microsoft.azuretools.sdkmanage.AzureManager;
import org.apache.commons.lang3.tuple.Pair;
import rx.Observable;

import java.io.IOException;
import java.net.URI;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class AzureSparkServerlessClusterManager implements ClusterContainer,
                                                           ILogger {
    // Lazy singleton initialization
    private static class LazyHolder {
        static final AzureSparkServerlessClusterManager INSTANCE =
                new AzureSparkServerlessClusterManager();
    }
    public static AzureSparkServerlessClusterManager getInstance() {
        return LazyHolder.INSTANCE;
    }

    //
    // Fields
    //
    private static final String REST_SEGMENT_SUBSCRIPTION = "/subscriptions/";
    private static final String REST_SEGMENT_ADL_ACCOUNT = "providers/Microsoft.DataLakeAnalytics/accounts";

    // FIXME!!!
    private static final String ACCOUNT_FILTER = CommonSettings.getAdEnvironment().endpoints().getOrDefault("dataLakeSparkAccountFilter", "");

    @NotNull
    private final HashMap<String, AzureHttpObservable> httpMap = new HashMap<>();

    @NotNull
    private AzureEnvironment azureEnv = CommonSettings.getAdEnvironment();

    @NotNull
    private ImmutableSortedSet<? extends AzureSparkServerlessAccount> accounts= ImmutableSortedSet.of();

    public AzureSparkServerlessClusterManager() {
        this.httpMap.put("common", new AzureHttpObservable(ApiVersion.VERSION));
    }

    //
    // Getters / setters
    //

    @NotNull
    public HashMap<String, AzureHttpObservable> getHttpMap() {
        return httpMap;
    }

    @NotNull
    public ImmutableSortedSet<? extends AzureSparkServerlessAccount> getAccounts() {
        return accounts;
    }

    @Nullable
    public AzureManager getAzureManager() {
        try {
            return AuthMethodManager.getInstance().getAzureManager();
        } catch (IOException e) {
            log().info("Can't get Azure manager now. error: " + e);

            return null;
        }
    }

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

        return get().toBlocking().singleOrDefault(this);
    }

    public Observable<AzureSparkServerlessClusterManager> get() {
        return getAzureDataLakeAccountsRequest()
                .map(this::updateWithResponse)
                .defaultIfEmpty(this);
    }

    public Observable<AzureSparkServerlessClusterManager> fetchClusters() {
        return get()
                .map(AzureSparkServerlessClusterManager::getAccounts)
                .flatMap(Observable::from)
                .flatMap(AzureSparkServerlessAccount::get)
                .toSortedList()
                .map(accounts -> this)
                .defaultIfEmpty(this);
    }

    private Observable<Set<Pair<SubscriptionDetail, List<DataLakeAnalyticsAccountBasic>>>> getAzureDataLakeAccountsRequest() {
        if (getAzureManager() == null) {
            return Observable.empty();
        }

        // Loop subscriptions to get all accounts
        return Observable
                .fromCallable(() -> getAzureManager().getSubscriptionManager().getSubscriptionDetails()
                        .stream()
                        .map(sub -> Pair.of(
                                sub,
                                URI.create(getSubscriptionsUri(sub.getSubscriptionId()).toString() + "/")
                                        .resolve(REST_SEGMENT_ADL_ACCOUNT)))
                        .collect(Collectors.toSet()))  // Output pair list of Subscription detail and ADL account URI
                .doOnNext(pair -> log().debug("Pair(Subscription, AccountsListUri): " + pair.toString()))
                .flatMap(Observable::from)             // Send URI to next one by one
                .map(subUriPair -> Pair.of(
                        subUriPair.getLeft(),
                        getHttp(subUriPair.getLeft()).withUuidUserAgent(false)
                                .get(subUriPair.getRight().toString(),
                                        Collections.singletonList(ODataParam.filter(ACCOUNT_FILTER)),
                                        null,
                                        // FIXME!!! Needs to support paging
                                        GetAccountsListResponse.class)))
                .toList()
                .map(adlAccountsSet -> adlAccountsSet.stream()
                        .flatMap(subAccountPair -> {
                            try {
                                // May produce NoElementException
                                return Stream.of(Pair.of(subAccountPair.getLeft(),
                                                         subAccountPair.getRight().toBlocking().single().items()));
                            } catch (Exception e) {
                                log().warn("Can't get Azure Spark Serverless Account " + e);
                                return Stream.empty();
                            }
                        })
                        .collect(Collectors.toSet()))
                .doOnNext(pairs -> log().debug("Pair(Subscription, AccountBasics) sets: " + pairs.toString()));
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
    private URI getSubscriptionsUri(@NotNull String subscriptionId) {
        return URI.create(azureEnv.resourceManagerEndpoint())
                .resolve(REST_SEGMENT_SUBSCRIPTION)
                .resolve(subscriptionId);
    }

    @NotNull
    private AzureSparkServerlessClusterManager updateWithResponse(Set<Pair<SubscriptionDetail, List<DataLakeAnalyticsAccountBasic>>> accountsResponse) {
        accounts = ImmutableSortedSet.copyOf(accountsResponse
                .stream()
                .flatMap(subAccountsMapPair ->
                        subAccountsMapPair.getRight()   // accountBasic lists
                                .stream()               // accountBasic stream
                                .map(accountBasic ->    // collect to AzureSparkServerlessAccount stream
                                        new AzureSparkServerlessAccount(subAccountsMapPair.getLeft(),
                                                                        // endpoint property is account's base URI
                                                                        URI.create("https://" + accountBasic.endpoint()),
                                                                        accountBasic.name())
                                                .setBasicResponse(accountBasic)))
                .iterator());

        return this;
    }

}
