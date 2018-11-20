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
import com.microsoft.azure.hdinsight.common.logger.ILogger;
import com.microsoft.azure.hdinsight.sdk.cluster.ClusterContainer;
import com.microsoft.azure.hdinsight.sdk.cluster.ClusterType;
import com.microsoft.azure.hdinsight.sdk.cluster.IClusterDetail;
import com.microsoft.azure.hdinsight.sdk.common.AzureDataLakeHttpObservable;
import com.microsoft.azure.hdinsight.sdk.common.AzureHttpObservable;
import com.microsoft.azure.hdinsight.sdk.common.HDIException;
import com.microsoft.azure.hdinsight.sdk.common.ODataParam;
import com.microsoft.azure.hdinsight.sdk.rest.azure.datalake.analytics.accounts.models.DataLakeAnalyticsAccount;
import com.microsoft.azure.hdinsight.sdk.rest.azure.datalake.analytics.accounts.models.DataLakeAnalyticsAccountBasic;
import com.microsoft.azure.hdinsight.sdk.rest.azure.datalake.analytics.accounts.models.DataLakeStoreAccountInformation;
import com.microsoft.azure.hdinsight.sdk.rest.azure.datalake.analytics.job.models.JobInfoListResult;
import com.microsoft.azure.hdinsight.sdk.rest.azure.datalake.analytics.job.models.JobState;
import com.microsoft.azure.hdinsight.sdk.rest.azure.serverless.spark.models.ApiVersion;
import com.microsoft.azure.hdinsight.sdk.rest.azure.serverless.spark.models.SparkResourcePoolList;
import com.microsoft.azure.hdinsight.sdk.rest.azure.serverless.spark.models.SparkResourcePoolState;
import com.microsoft.azure.hdinsight.sdk.storage.HDStorageAccount;
import com.microsoft.azure.hdinsight.sdk.storage.IHDIStorageAccount;
import com.microsoft.azuretools.authmanage.AuthMethodManager;
import com.microsoft.azuretools.authmanage.models.SubscriptionDetail;
import com.microsoft.azuretools.azurecommons.helpers.AzureCmdException;
import com.microsoft.azuretools.azurecommons.helpers.NotNull;
import com.microsoft.azuretools.azurecommons.helpers.Nullable;
import com.microsoft.azuretools.sdkmanage.AzureManager;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.NameValuePair;
import rx.Observable;

import java.io.IOException;
import java.net.URI;
import java.util.Collections;
import java.util.List;

public class AzureSparkServerlessAccount implements IClusterDetail, ClusterContainer, Comparable<AzureSparkServerlessAccount>, ILogger {
    private static final String REST_SEGMENT_SPARK_RESOURCEPOOLS = "/activityTypes/spark/resourcePools";
    private static final String REST_SEGMENT_JOB_LIST = "/Jobs";
    private static final String REST_SEGMENT_JOB_MANAGEMENT_TENANTID = "/#@";
    private static final String REST_SEGMENT_JOB_MANAGEMENT_RESOURCE = "/resource";

    private static final String REST_SEGMENT_JOB_MANAGEMENT_SUFFIX = "/jobManagement";

    @NotNull
    private final SubscriptionDetail subscription;

    @NotNull
    private final AzureHttpObservable http;

    @NotNull
    private final String apiVersion = ApiVersion.VERSION;

    @NotNull
    private final URI uri;

    @Nullable
    private DataLakeAnalyticsAccountBasic basicResponse;

    @Nullable
    private String id;

    @NotNull
    private ImmutableSortedSet<? extends IClusterDetail> clusters = ImmutableSortedSet.of();

    @NotNull
    private String name;

    @Nullable
    private DataLakeAnalyticsAccount detailResponse;

    public AzureSparkServerlessAccount(@NotNull SubscriptionDetail subscription, @NotNull URI uri, @NotNull String name) {
        this.subscription = subscription;
        this.http = new AzureDataLakeHttpObservable(subscription.getTenantId(), this.apiVersion);
        this.uri = uri;
        this.name = name;
    }

    //
    // Getters / setters
    //

    @NotNull
    public SubscriptionDetail getSubscription() {
        return subscription;
    }

    @Override
    public int getDataNodes() {
        return 0;
    }

    @Override
    public String getHttpUserName() throws HDIException {
        return null;
    }

    @Override
    public String getHttpPassword() throws HDIException {
        return null;
    }

    @Override
    public String getOSType() {
        return null;
    }

    @Override
    public String getResourceGroup() {
        return null;
    }

    @Override
    public IHDIStorageAccount getStorageAccount() throws HDIException {
        return null;
    }

    @Override
    public List<HDStorageAccount> getAdditionalStorageAccounts() {
        return null;
    }

    @Override
    public void getConfigurationInfo() throws IOException, HDIException, AzureCmdException {

    }

    @Override
    public String getSparkVersion() {
        return null;
    }

    @NotNull
    public AzureHttpObservable getHttp() {
        return http;
    }

    @NotNull
    public URI getUri() {
        return uri;
    }

    @Nullable
    public URI getJobManagementURI() {
        if (getId() == null || subscription.getTenantId() == null) {
            log().warn(String.format("Can't get account ID or tenantID. AccountID:%s, tenantID:%s", getId(),
                    subscription.getTenantId()));
            return null;
        }

        try {
            AzureManager azureManager = AuthMethodManager.getInstance().getAzureManager();
            if (azureManager == null) {
                log().warn("Azure manager is null");
                return null;
            }

            String url = azureManager.getPortalUrl()
                    + REST_SEGMENT_JOB_MANAGEMENT_TENANTID
                    + subscription.getTenantId()
                    + REST_SEGMENT_JOB_MANAGEMENT_RESOURCE
                    + getId()
                    + REST_SEGMENT_JOB_MANAGEMENT_SUFFIX;
            return URI.create(url);
        } catch (IOException ex) {
            log().warn("Can't get Azure Manager now. Error: " + ex);
            return null;
        }
    }

    @Nullable
    public String getId() {
        return id;
    }

    public AzureSparkServerlessAccount setId(@Nullable String id) {
        this.id = id;
        return this;
    }

    //
    // RestFUL API operations
    //

    public Observable<Integer> getJobDegreeOfParallelism() {
        return getJobs()
                .flatMap(jobList -> Observable.from(jobList.value()))
                .map(jobInfo -> jobInfo.degreeOfParallelism())
                .defaultIfEmpty(0)
                .reduce((a, b) -> a + b);
    }

    // TODO: handle job list pagination
    public Observable<JobInfoListResult> getJobs() {
        URI uri = getUri().resolve(REST_SEGMENT_JOB_LIST);
        List<NameValuePair> parameters = Collections.singletonList(
                ODataParam.filter(String.format("state eq '%s'",JobState.RUNNING.toString())));

        return new AzureDataLakeHttpObservable(subscription.getTenantId(),
                com.microsoft.azure.hdinsight.sdk.rest.azure.datalake.analytics.job.models.ApiVersion.VERSION)
                .withUuidUserAgent()
                .get(uri.toString(), parameters, null, JobInfoListResult.class);
    }

    @NotNull
    public Observable<AzureSparkServerlessAccount> get() {
        return getResourcePoolsRequest()
                .map(this::updateWithResponse)
                .defaultIfEmpty(this);
    }

    private Observable<SparkResourcePoolList> getResourcePoolsRequest() {
        URI uri = getUri().resolve(REST_SEGMENT_SPARK_RESOURCEPOOLS);

        return getHttp()
                .withUuidUserAgent()
                .get(uri.toString(), null, null, SparkResourcePoolList.class);
    }

    private AzureSparkServerlessAccount updateWithResponse(SparkResourcePoolList sparkResourcePoolList) {
        clusters = ImmutableSortedSet.copyOf(sparkResourcePoolList.value().stream()
                .map(analyticsActivity -> new AzureSparkServerlessCluster(this, analyticsActivity.id().toString())
                        .updateWithAnalyticsActivity(analyticsActivity))
                .iterator());

        return this;
    }

    /**
     * Get clusters with "Ended" and "Ending" state filtered
     * @return cluster set with "Ended" and "Ending" state filtered 
     */
    @NotNull
    @Override
    public ImmutableSortedSet<? extends IClusterDetail> getClusters() {
        return ImmutableSortedSet.copyOf(getRawClusters().stream().filter(cluster -> {
            String clusterState = cluster.getState();
            return !clusterState.equals(SparkResourcePoolState.ENDED.toString()) &&
                    !clusterState.equals(SparkResourcePoolState.ENDING.toString());
        }).iterator());
    }

    /**
     * Get raw clusters without filtering
     * @return raw cluster set without filtering
     */
    @NotNull
    public ImmutableSortedSet<? extends IClusterDetail> getRawClusters() {
        return clusters;
    }

    @NotNull
    @Override
    public ClusterContainer refresh() {
        return get().toBlocking().singleOrDefault(this);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }

        return obj != null && StringUtils.equalsIgnoreCase(id, ((AzureSparkServerlessAccount) obj).id);
    }

    @Nullable
    public DataLakeAnalyticsAccountBasic getBasicResponse() {
        return basicResponse;
    }

    public AzureSparkServerlessAccount setBasicResponse(@Nullable DataLakeAnalyticsAccountBasic basicResponse) {
        this.basicResponse = basicResponse;
        setId(this.basicResponse != null ? this.basicResponse.id() : null);
        return this;
    }

    @Override
    public boolean isEmulator() {
        return false;
    }

    @Override
    public boolean isConfigInfoAvailable() {
        return false;
    }

    @NotNull
    public String getName() {
        return name;
    }

    @Override
    public String getTitle() {
        return name;
    }

    @Override
    public String getState() {
        return null;
    }

    @Override
    public String getLocation() {
        return null;
    }

    @Override
    public String getConnectionUrl() {
        return null;
    }

    @Override
    public String getCreateDate() {
        return null;
    }

    @Override
    public ClusterType getType() {
        return null;
    }

    @Override
    public String getVersion() {
        return null;
    }

    public int getSystemMaxDegreeOfParallelism() {
        return this.detailResponse != null ? this.detailResponse.systemMaxDegreeOfParallelism() : 0;
    }

    public AzureSparkServerlessAccount setDetailResponse(@Nullable DataLakeAnalyticsAccount detailResponse) {
        this.detailResponse = detailResponse;

        return this;
    }

    @Nullable
    public DataLakeAnalyticsAccount getDetailResponse() {
        return detailResponse;
    }

    @Nullable
    public String getStorageRootPath() {
        String storageRootPath = null;
        DataLakeAnalyticsAccount accountDetail = this.getDetailResponse();
        if (accountDetail != null) {
            // find default storage account name and suffix
            String defaultStorageAccountName = accountDetail.defaultDataLakeStoreAccount();
            storageRootPath = accountDetail.dataLakeStoreAccounts()
                    .stream()
                    .filter(info -> info.name().equals(defaultStorageAccountName))
                    .findFirst()
                    .map(DataLakeStoreAccountInformation::suffix)
                    .map(suffix -> String.format("adl://%s.%s/", defaultStorageAccountName, suffix))
                    .orElse(null);
        }
        return storageRootPath;
    }

    @Override
    public int compareTo(@NotNull AzureSparkServerlessAccount other) {
        if (this == other) {
            return 0;
        }

        return getName().compareTo(other.getName());
    }
}
