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
import com.microsoft.azure.hdinsight.sdk.common.AzureHttpObservable;
import com.microsoft.azure.hdinsight.sdk.common.azure.serverless.AzureSparkServerlessAccount;
import com.microsoft.azure.hdinsight.sdk.rest.azure.serverless.spark.models.CreateSparkBatchJobParameters;
import com.microsoft.azure.hdinsight.sdk.rest.azure.serverless.spark.models.SparkBatchJobResponsePayload;
import com.microsoft.azuretools.azurecommons.helpers.NotNull;
import com.microsoft.azuretools.azurecommons.helpers.Nullable;
import org.apache.commons.lang3.exception.ExceptionUtils;
import rx.Observable;
import rx.Observer;

import java.io.IOException;
import java.net.URI;
import java.util.AbstractMap;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

public class CosmosServerlessSparkBatchJob extends SparkBatchJob {
    @NotNull
    private final AzureSparkServerlessAccount account;
    @NotNull
    private String jobUuid;
    @NotNull
    private final Deployable jobDeploy;

    public CosmosServerlessSparkBatchJob(@NotNull AzureSparkServerlessAccount account,
                                         @NotNull Deployable jobDeploy,
                                         @NotNull CreateSparkBatchJobParameters submissionParameter,
                                         @NotNull SparkBatchSubmission sparkBatchSubmission,
                                         @NotNull Observer<AbstractMap.SimpleImmutableEntry<MessageInfoType, String>> ctrlSubject) {
        super(submissionParameter, sparkBatchSubmission, ctrlSubject);
        this.account = account;
        this.jobUuid = UUID.randomUUID().toString();
        this.jobDeploy = jobDeploy;
    }

    @NotNull
    public AzureSparkServerlessAccount getAccount() {
        return account;
    }

    @NotNull
    public AzureHttpObservable getHttp() {
        return getAccount().getHttp();
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
                ADLStoreClient storeClient = ADLStoreClient.createClient(URI.create(account.getStorageRootPath()).getHost(), accessToken);
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

    @NotNull
    @Override
    public Observable<? extends ISparkBatchJob> submit() {
        return prepareSparkEventsLogFolder()
                .flatMap(isSucceed -> {
                            if (isSucceed) {
                                return getAccount().createSparkBatchJobRequest(getJobUuid(), getSubmissionParameter());
                            } else {
                                return Observable.error(new IOException("Spark events log path preparation failed."));
                            }
                        })
                .flatMap(sparkBatchJob -> getAccount().getSparkBatchJobRequest(getJobUuid()))
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
        return getAccount().killSparkBatchJobRequest(getJobUuid())
                .map(resp -> this)
                .onErrorReturn(err -> {
                    String errMsg = String.format("Failed to stop spark job. %s", ExceptionUtils.getStackTrace(err));
                    ctrlInfo(errMsg);
                    log().warn(errMsg);
                    return this;
                });
    }

    @Override
    public Observable<SparkBatchJobResponsePayload> getStatus() {
        return getAccount().getSparkBatchJobRequest(getJobUuid())
                .flatMap(sparkBatchJob -> {
                    if (sparkBatchJob.properties() == null) {
                        return Observable.empty();
                    } else {
                        return Observable.just(sparkBatchJob.properties().responsePayload());
                    }
                });
    }

    @Nullable
    @Override
    public String getState() throws IOException {
        return getStatus()
                .retry(getRetriesMax())
                .repeatWhen(ob -> ob.delay(getDelaySeconds(), TimeUnit.SECONDS))
                .map(sparkSubmitResponse -> sparkSubmitResponse.getState())
                .toBlocking()
                .singleOrDefault("unknown");
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

    @Override
    protected Observable<AbstractMap.SimpleImmutableEntry<String, String>> getJobDoneObservable() {
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
        return Observable.create(ob -> {
            ob.onNext(new AbstractMap.SimpleImmutableEntry<>(MessageInfoType.Info, "Submission log not supported yet"));
        });
    }

    private void ctrlInfo(@NotNull String message) {
        getCtrlSubject().onNext(new AbstractMap.SimpleImmutableEntry<>(MessageInfoType.Info, message));
    }
}
