/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.hdinsight.sdk.common.azure.serverless;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSortedSet;
import com.microsoft.azure.hdinsight.common.logger.ILogger;
import com.microsoft.azure.hdinsight.sdk.cluster.ClusterContainer;
import com.microsoft.azure.hdinsight.sdk.cluster.IClusterDetail;
import com.microsoft.azure.hdinsight.sdk.common.AzureDataLakeHttpObservable;
import com.microsoft.azure.hdinsight.sdk.common.AzureHttpObservable;
import com.microsoft.azure.hdinsight.sdk.common.HttpResponse;
import com.microsoft.azure.hdinsight.sdk.common.ODataParam;
import com.microsoft.azure.hdinsight.sdk.rest.azure.datalake.analytics.accounts.models.DataLakeAnalyticsAccount;
import com.microsoft.azure.hdinsight.sdk.rest.azure.datalake.analytics.accounts.models.DataLakeAnalyticsAccountBasic;
import com.microsoft.azure.hdinsight.sdk.rest.azure.datalake.analytics.accounts.models.DataLakeStoreAccountInformation;
import com.microsoft.azure.hdinsight.sdk.rest.azure.datalake.analytics.job.models.JobInfoListResult;
import com.microsoft.azure.hdinsight.sdk.rest.azure.datalake.analytics.job.models.JobState;
import com.microsoft.azure.hdinsight.sdk.rest.azure.serverless.spark.models.*;
import com.microsoft.azure.hdinsight.spark.common.SparkSubmitStorageType;
import com.microsoft.azure.hdinsight.spark.common.SparkSubmitStorageTypeOptionsForCluster;
import com.microsoft.azure.toolkit.lib.common.model.Subscription;
import com.microsoft.azuretools.authmanage.AuthMethodManager;
import com.microsoft.azuretools.azurecommons.helpers.NotNull;
import com.microsoft.azuretools.azurecommons.helpers.Nullable;
import com.microsoft.azuretools.sdkmanage.AzureManager;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.NameValuePair;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.entity.StringEntity;
import rx.Observable;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.List;

import static com.microsoft.azure.hdinsight.sdk.rest.azure.datalake.analytics.job.models.ApiVersion.VERSION;

public class AzureSparkServerlessAccount implements IClusterDetail, ClusterContainer, ILogger {
    private static final String REST_SEGMENT_SPARK_RESOURCEPOOLS = "/activityTypes/spark/resourcePools";
    private static final String REST_SEGMENT_SPARK_BATCH_JOB = "/activityTypes/spark/batchJobs";
    private static final String REST_SEGMENT_JOB_LIST = "/Jobs";
    private static final String REST_SEGMENT_JOB_MANAGEMENT_TENANTID = "/#@";
    private static final String REST_SEGMENT_JOB_MANAGEMENT_RESOURCE = "/resource";

    private static final String REST_SEGMENT_JOB_MANAGEMENT_SUFFIX = "/jobManagement";

    @NotNull
    private final Subscription subscription;

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

    public AzureSparkServerlessAccount(@NotNull Subscription subscription, @NotNull URI uri, @NotNull String name) {
        this.subscription = subscription;
        this.http = new AzureDataLakeHttpObservable(subscription.getTenantId(), this.apiVersion);
        this.uri = uri;
        this.name = name;
    }

    //
    // Getters / setters
    //

    @NotNull
    public Subscription getSubscription() {
        return subscription;
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
        final AzureManager azureManager = AuthMethodManager.getInstance().getAzureManager();
        if (azureManager == null) {
            log().warn("Azure manager is null");
            return null;
        }

        final String url = azureManager.getPortalUrl()
            + REST_SEGMENT_JOB_MANAGEMENT_TENANTID
            + subscription.getTenantId()
            + REST_SEGMENT_JOB_MANAGEMENT_RESOURCE
            + getId()
            + REST_SEGMENT_JOB_MANAGEMENT_SUFFIX;
        return URI.create(url);
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

    /**
     * Get Cosmos Serverless Spark batch job list
     * @return Cosmos Serverless Spark batch job list
     */
    public Observable<SparkBatchJobList> getSparkBatchJobList() {
        URI url = getUri().resolve(REST_SEGMENT_SPARK_BATCH_JOB);

        return getHttp()
                .withUuidUserAgent()
                // Fetch the jobs sorted by submitTime in descending order
                .get(url.toString(), ImmutableList.of(ODataParam.orderby("submitTime desc")), null,
                     SparkBatchJobList.class);
    }

    /**
     * Prepare for creating Cosmos Serverless Spark batch job
     * @return the CreateSparkBatchJob object which contains parameters of the job
     */
    @NotNull
    public CreateSparkBatchJob prepareCreateSparkBatchJob(@NotNull String jobUuid,
                                                          @NotNull CreateSparkBatchJobParameters parameters) {
        return new CreateSparkBatchJob()
                .withName(parameters.getName())
                .withProperties(parameters);
    }

    /**
     * Create Cosmos Serverless Spark batch job
     * @return the SparkBatchJob object which contains the property of the job
     */
    public Observable<SparkBatchJob> createSparkBatchJobRequest(@NotNull String jobUuid,
                                                                @NotNull CreateSparkBatchJobParameters parameters) {
        String url = getUri().resolve(REST_SEGMENT_SPARK_BATCH_JOB).toString() + "/" + jobUuid;
        CreateSparkBatchJob putBody = prepareCreateSparkBatchJob(jobUuid, parameters);

        String json = putBody.convertToJson()
                .orElseThrow(() -> new IllegalArgumentException("Bad Cosmos Serverless Spark Batch Job arguments to put"));

        StringEntity entity = new StringEntity(json, StandardCharsets.UTF_8);

        return getHttp()
                .withUuidUserAgent()
                .put(url, entity, null, null, SparkBatchJob.class);
    }

    /**
     * Get Cosmos Serverless Spark batch job detail info
     * @return the SparkBatchJob object which contains the property of the job
     */
    public Observable<SparkBatchJob> getSparkBatchJobRequest(@NotNull String jobUuid) {
        String url = getUri().resolve(REST_SEGMENT_SPARK_BATCH_JOB).toString() + "/" + jobUuid;

        return getHttp()
                .withUuidUserAgent()
                .get(url, null, null, SparkBatchJob.class);
    }

    /**
     * Get Cosmos Serverless Spark batch job detail info with raw http response
     * @param jobUuid
     * @return Http response for the request
     */
    public Observable<HttpResponse> getSparkBatchJobWithRawHttpResponse(@NotNull String jobUuid) {
        String url = getUri().resolve(REST_SEGMENT_SPARK_BATCH_JOB).toString() + "/" + jobUuid;

        return getHttp()
                .withUuidUserAgent()
                .requestWithHttpResponse(new HttpGet(url), null, null, null);
    }

    /**
     * Kill Cosmos Serverless Spark batch job
     * @return http response of the killing request
     */
    public Observable<HttpResponse> killSparkBatchJobRequest(@NotNull String jobUuid) {
        String url = getUri().resolve(REST_SEGMENT_SPARK_BATCH_JOB).toString() + "/" + jobUuid;

        return getHttp()
                .withUuidUserAgent()
                .delete(url, null, null);
    }

    public Observable<Integer> getJobDegreeOfParallelism() {
        return getJobs()
                .flatMap(jobList -> Observable.from(jobList.value()))
                .map(jobInfo -> jobInfo.degreeOfParallelism())
                .defaultIfEmpty(0)
                .reduce((a, b) -> a + b);
    }

    // TODO: handle job list pagination
    public Observable<JobInfoListResult> getJobs() {
        URI url = getUri().resolve(REST_SEGMENT_JOB_LIST);
        List<NameValuePair> parameters = Collections.singletonList(
                ODataParam.filter(String.format("state eq '%s'", JobState.RUNNING.toString())));

        return new AzureDataLakeHttpObservable(subscription.getTenantId(), VERSION)
                .withUuidUserAgent()
                .get(url.toString(), parameters, null, JobInfoListResult.class);
    }

    @NotNull
    public Observable<AzureSparkServerlessAccount> get() {
        return getResourcePoolsRequest()
                .map(this::updateWithResponse)
                .defaultIfEmpty(this);
    }

    private Observable<SparkResourcePoolList> getResourcePoolsRequest() {
        URI url = getUri().resolve(REST_SEGMENT_SPARK_RESOURCEPOOLS);

        return getHttp()
                .withUuidUserAgent()
                .get(url.toString(), null, null, SparkResourcePoolList.class);
    }

    private AzureSparkServerlessAccount updateWithResponse(SparkResourcePoolList sparkResourcePoolList) {
        clusters = ImmutableSortedSet.copyOf(sparkResourcePoolList.value().stream()
                .map(analyticsActivity -> new AzureSparkCosmosCluster(this, analyticsActivity.id().toString())
                        .updateWithAnalyticsActivity(analyticsActivity))
                .iterator());

        return this;
    }

    /**
     * Get clusters with "Ended" and "Finalizing" state filtered
     * @return cluster set with "Ended" and "Ending" state filtered
     */
    @NotNull
    @Override
    public ImmutableSortedSet<? extends IClusterDetail> getClusters() {
        return ImmutableSortedSet.copyOf(getRawClusters().stream()
                 .filter(cluster -> ((AzureSparkCosmosCluster) cluster).isRunning()).iterator());
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

    @NotNull
    public String getName() {
        return name;
    }

    @Override
    public String getTitle() {
        return name;
    }

    @Override
    public String getConnectionUrl() {
        return null;
    }

    @Override
    public SparkSubmitStorageType getDefaultStorageType() {
        return SparkSubmitStorageType.ADLA_ACCOUNT_DEFAULT_STORAGE;
    }

    @Override
    public SparkSubmitStorageTypeOptionsForCluster getStorageOptionsType() {
        return SparkSubmitStorageTypeOptionsForCluster.ServerlessClusterWithAccountDefault;
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
}
