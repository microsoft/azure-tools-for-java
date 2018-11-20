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

package com.microsoft.azure.hdinsight.spark.common;

import com.microsoft.azure.datalake.store.ADLStoreClient;
import com.microsoft.azure.hdinsight.common.MessageInfoType;
import com.microsoft.azure.hdinsight.sdk.common.AzureDataLakeHttpObservable;
import com.microsoft.azure.hdinsight.sdk.common.AzureHttpObservable;
import com.microsoft.azure.hdinsight.sdk.common.HttpResponse;
import com.microsoft.azure.hdinsight.sdk.common.azure.serverless.AzureSparkServerlessAccount;
import com.microsoft.azure.hdinsight.sdk.rest.azure.serverless.spark.models.ApiVersion;
import com.microsoft.azure.hdinsight.sdk.rest.azure.serverless.spark.models.CreateSparkBatchJob;
import com.microsoft.azure.hdinsight.sdk.rest.azure.serverless.spark.models.CreateSparkBatchJobParameters;
import com.microsoft.azure.hdinsight.sdk.rest.azure.serverless.spark.models.SparkBatchJobResponsePayload;
import com.microsoft.azuretools.azurecommons.helpers.NotNull;
import com.microsoft.azuretools.azurecommons.helpers.Nullable;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.apache.http.entity.StringEntity;
import rx.Observable;
import rx.Observer;

import java.io.IOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.AbstractMap;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

public class CosmosServerlessSparkBatchJob extends SparkBatchJob {
    private static final String REST_SEGMENT_SPARK_BATCH_JOB = "/activityTypes/spark/batchJobs";
    @NotNull
    private final AzureSparkServerlessAccount account;
    @NotNull
    private String jobUuid;
    @NotNull
    private final Deployable jobDeploy;
    @NotNull
    private final AzureHttpObservable http;
    @NotNull
    private final String apiVersion = ApiVersion.VERSION;

    public CosmosServerlessSparkBatchJob(@NotNull AzureSparkServerlessAccount account,
                                         @NotNull Deployable jobDeploy,
                                         @NotNull CreateSparkBatchJobParameters submissionParameter,
                                         @NotNull SparkBatchSubmission sparkBatchSubmission,
                                         @NotNull Observer<AbstractMap.SimpleImmutableEntry<MessageInfoType, String>> ctrlSubject) {
        super(submissionParameter, sparkBatchSubmission, ctrlSubject);
        this.account = account;
        this.jobUuid = UUID.randomUUID().toString();
        this.jobDeploy = jobDeploy;
        this.http = new AzureDataLakeHttpObservable(account.getSubscription().getTenantId(), apiVersion);
    }

    @NotNull
    public AzureHttpObservable getHttp() {
        return http;
    }

    @NotNull
    public CreateSparkBatchJobParameters getSubmissionParameter() {
        return (CreateSparkBatchJobParameters) super.getSubmissionParameter();
    }

    @NotNull
    public String getJobUuid() {
        return jobUuid;
    }


    /**
     * Prepare spark event log path for job submission
     * @return whether spark events log path preparation succeed or not
     */
    public Observable<Boolean> prepareSparkEventsLogFolder() {
        return Observable.fromCallable(() -> {
            try {
                String path = getSubmissionParameter().sparkEventsDirectoryPath();
                String accessToken = getHttp().getAccessToken();
                ADLStoreClient storeClient = ADLStoreClient.createClient(URI.create(path).getHost(), accessToken);
                if (storeClient.checkExists(path)) {
                    return true;
                } else {
                    return storeClient.createDirectory(path);
                }
            } catch (Exception ex) {
                throw new IOException("Spark events log path preparation failed", ex);
            }
        });
    }

    /**
     * Create Serverless Spark batch job
     */
    @NotNull
    public CreateSparkBatchJob prepareCreateSparkBatchJob(CreateSparkBatchJobParameters parameters) {
        return new CreateSparkBatchJob()
                // TODO: Replace name with a meaningful name
                .withName("ServerlessSession_" + getJobUuid())
                .withProperties(parameters);
    }

    @NotNull
    public Observable<com.microsoft.azure.hdinsight.sdk.rest.azure.serverless.spark.models.SparkBatchJob>
    createSparkBatchJobRequest(@NotNull String jobUuid, @NotNull CreateSparkBatchJobParameters parameters) {
        String url = getConnectUri().toString() + "/" + jobUuid;

        CreateSparkBatchJob putBody = prepareCreateSparkBatchJob(parameters);

        String json = putBody.convertToJson()
                .orElseThrow(() -> new IllegalArgumentException("Bad Spark Batch Job arguments to put"));

        StringEntity entity = new StringEntity(json, StandardCharsets.UTF_8);

        return getHttp()
                .withUuidUserAgent()
                .put(url, entity, null, null,
                        com.microsoft.azure.hdinsight.sdk.rest.azure.serverless.spark.models.SparkBatchJob.class);
    }

    /**
     * Get Serverless Spark batch job detail
     */
    @NotNull
    public Observable<com.microsoft.azure.hdinsight.sdk.rest.azure.serverless.spark.models.SparkBatchJob>
    getSparkBatchJobRequest(@NotNull String jobUuid) {
        String url = getConnectUri().toString() + "/" + jobUuid;

        return getHttp()
                .withUuidUserAgent()
                .get(url, null, null,
                        com.microsoft.azure.hdinsight.sdk.rest.azure.serverless.spark.models.SparkBatchJob.class);
    }

    /**
     * Kill Serverless Spark batch job
     */
    @NotNull
    public Observable<HttpResponse> killSparkBatchJobRequest(@NotNull String jobUuid) {
        String url = getConnectUri().toString() + "/" + jobUuid;

        return getHttp()
                .withUuidUserAgent()
                .delete(url, null, null);
    }

    @NotNull
    @Override
    public Observable<? extends ISparkBatchJob> submit() {
        return prepareSparkEventsLogFolder()
                .flatMap(isSucceed -> {
                            if (isSucceed) {
                                return createSparkBatchJobRequest(getJobUuid(), getSubmissionParameter());
                            } else {
                                return Observable.error(new IOException("Spark events log path preparation failed."));
                            }
                        })
                .flatMap(sparkBatchJob -> getSparkBatchJobRequest(getJobUuid()))
                .doOnNext(sparkBatchJob -> {
                    if (sparkBatchJob != null && sparkBatchJob.properties() != null && sparkBatchJob.properties().responsePayload() != null) {
                        this.setBatchId(sparkBatchJob.properties().responsePayload().getId());
                    }
                })
                .map(sparkBatchJob -> this);
    }


    @NotNull
    @Override
    public Observable<? extends ISparkBatchJob> deploy(@NotNull String artifactPath) {
        return jobDeploy.deploy(artifactPath)
                .map(path -> {
                    ctrlInfo(String.format("Upload to Azure Datalake store %s successfully", path));
                    getSubmissionParameter().setFilePath(path);
                    return this;
                });
    }

    @Override
    public Observable<? extends ISparkBatchJob> killBatchJob() {
        return killSparkBatchJobRequest(getJobUuid())
                .map(resp -> this)
                .onErrorReturn(err -> {
                    String errMsg = String.format("Failed to stop spark job. %s", ExceptionUtils.getStackTrace(err));
                    ctrlInfo(errMsg);
                    log().warn(errMsg);
                    return this;
                });
    }

    @Override
    public URI getConnectUri() {
        return account.getUri().resolve(REST_SEGMENT_SPARK_BATCH_JOB);
    }

    @Override
    public Observable<SparkBatchJobResponsePayload> getStatus() {
        return getSparkBatchJobRequest(getJobUuid())
                .map(sparkBatchJob -> sparkBatchJob.properties().responsePayload());
    }

    @Nullable
    @Override
    public String getState() throws IOException {
        return getStatus()
                .retry(getRetriesMax())
                .repeatWhen(ob -> ob.delay(getDelaySeconds(), TimeUnit.SECONDS))
                .map(sparkSubmitResponse -> sparkSubmitResponse.getState())
                .toBlocking()
                .singleOrDefault("error");
    }

    @Override
    public boolean isActive() throws IOException {
        return getStatus()
                .retry(getRetriesMax())
                .repeatWhen(ob -> ob.delay(getDelaySeconds(), TimeUnit.SECONDS))
                .map(sparkSubmitResponse -> sparkSubmitResponse.isAlive())
                .toBlocking()
                .singleOrDefault(false);
    }

    private Observable<AbstractMap.SimpleImmutableEntry<String, String>> getJobDoneObservable() {
        return getStatus()
                .repeatWhen(ob -> ob.delay(getDelaySeconds(), TimeUnit.SECONDS))
                .takeUntil(resp -> isDone(resp.getState()))
                .map(resp -> new AbstractMap.SimpleImmutableEntry<>(resp.getState(), String.join("\n", resp.getLog())));
    }

    private Observable<String> getJobLogAggregationDoneObservable() {
        // TODO: enable yarn log aggregation
        return Observable.just("SUCCEEDED");
    }

    @Override
    public Observable<AbstractMap.SimpleImmutableEntry<MessageInfoType, String>> getSubmissionLog() {
        // TODO: Replace it with HttpObservable
        return super.getSubmissionLog();
    }

        private void ctrlInfo(@NotNull String message) {
        getCtrlSubject().onNext(new AbstractMap.SimpleImmutableEntry<>(MessageInfoType.Info, message));
    }
}
