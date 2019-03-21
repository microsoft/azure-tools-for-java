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

import com.google.common.collect.ImmutableSet;
import com.microsoft.azure.datalake.store.ADLStoreClient;
import com.microsoft.azure.hdinsight.common.MessageInfoType;
import com.microsoft.azure.hdinsight.sdk.common.AzureHttpObservable;
import com.microsoft.azure.hdinsight.sdk.common.azure.serverless.AzureSparkServerlessAccount;
import com.microsoft.azure.hdinsight.sdk.rest.azure.serverless.spark.models.CreateSparkBatchJobParameters;
import com.microsoft.azure.hdinsight.sdk.rest.azure.serverless.spark.models.SchedulerState;
import com.microsoft.azuretools.azurecommons.helpers.NotNull;
import com.microsoft.azuretools.azurecommons.helpers.Nullable;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.apache.commons.lang3.tuple.Triple;
import org.apache.http.Header;
import org.apache.http.NameValuePair;
import org.apache.http.message.BasicHeader;
import org.apache.http.message.BasicNameValuePair;
import rx.Observable;
import rx.Observer;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static com.microsoft.azure.hdinsight.common.MessageInfoType.Log;

public class CosmosServerlessSparkBatchJob extends SparkBatchJob {
    @NotNull
    private final AzureSparkServerlessAccount account;
    @NotNull
    private String jobUuid;
    @NotNull
    private final Deployable jobDeploy;

    // Parameters for getting Livy submission log
    private int logStartIndex = 0;

    public CosmosServerlessSparkBatchJob(@NotNull AzureSparkServerlessAccount account,
                                         @NotNull Deployable jobDeploy,
                                         @NotNull CreateSparkBatchJobParameters submissionParameter,
                                         @NotNull SparkBatchSubmission sparkBatchSubmission,
                                         @NotNull Observer<AbstractMap.SimpleImmutableEntry<MessageInfoType, String>> ctrlSubject) {
        super(account, submissionParameter, sparkBatchSubmission, ctrlSubject, null, null, null);
        this.account = account;
        this.jobUuid = UUID.randomUUID().toString();
        this.jobDeploy = jobDeploy;
    }

    public int getLogStartIndex() {
        return logStartIndex;
    }

    public void setLogStartIndex(int logStartIndex) {
        this.logStartIndex = logStartIndex;
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
                ADLStoreClient storeClient = ADLStoreClient.createClient(URI.create(getAccount().getStorageRootPath()).getHost(), accessToken);
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
                                String errorMsg = "Spark events log path preparation failed.";
                                log().warn(errorMsg);
                                return Observable.error(new IOException(errorMsg));
                            }
                        })
                // For HDInsight job , we can get batch ID immediatelly after we submit job,
                // but for Serverless job, some time are needed for environment setup before batch ID is available
                .map(sparkBatchJob -> this);
    }

    @NotNull
    @Override
    public Observable<? extends ISparkBatchJob> deploy(@NotNull String artifactPath) {
        URI dest = URI.create(account.getStorageRootPath());
        return jobDeploy.deploy(new File(artifactPath), dest)
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
                    String errHint = "Failed to stop spark job.";
                    ctrlInfo(errHint + " " + err.getMessage());
                    log().warn(errHint + ExceptionUtils.getStackTrace(err));
                    return this;
                });
    }

    private Observable<com.microsoft.azure.hdinsight.sdk.rest.azure.serverless.spark.models.SparkBatchJob> getSparkBatchJobRequest() {
        return getAccount().getSparkBatchJobRequest(getJobUuid());
    }

    @Override
    protected Observable<AbstractMap.SimpleImmutableEntry<String, String>> getJobDoneObservable() {
        // Refer parent class "SparkBatchJob" for delay interval
        final int GET_JOB_DONE_REPEAT_DELAY_MILLISECONDS = 1000;
        return getSparkBatchJobRequest()
                .repeatWhen(ob -> ob.delay(GET_JOB_DONE_REPEAT_DELAY_MILLISECONDS, TimeUnit.MILLISECONDS))
                .takeUntil(sparkBatchJob -> {
                    String jobState = getJobState(sparkBatchJob);
                    return isJobEnded(sparkBatchJob)
                            || (jobState != null && isDone(jobState));
                })
                .filter(sparkBatchJob -> {
                    String jobState = getJobState(sparkBatchJob);
                    return isJobEnded(sparkBatchJob)
                            || (jobState != null && isDone(jobState));
                })
                .map(sparkBatchJob -> {
                    String jobState = getJobState(sparkBatchJob);
                    if (jobState != null && isDone(jobState)) {
                        return new AbstractMap.SimpleImmutableEntry<>(
                                jobState,
                                getJobLog(sparkBatchJob));
                    } else {
                        return new AbstractMap.SimpleImmutableEntry<>(
                                sparkBatchJob.schedulerState().toString(),
                                getJobLog(sparkBatchJob));
                    }
                });
    }

    @Override
    public Observable<String> awaitStarted() {
        ctrlInfo("The Spark job is starting...");
        return getSparkBatchJobRequest()
                .doOnNext(sparkBatchJob -> {
                    if (sparkBatchJob.properties() != null && sparkBatchJob.properties().sparkMasterUI() != null) {
                        ctrlHyperLink(sparkBatchJob.properties().sparkMasterUI() + "?adlaAccountName=" + getAccount().getName());
                    }
                })
                .flatMap(job ->
                        getSparkBatchJobRequest()
                                .repeatWhen(ob -> ob.delay(getDelaySeconds(), TimeUnit.SECONDS))
                                .takeUntil(sparkBatchJob -> {
                                    String jobState = getJobState(sparkBatchJob);
                                    return isJobEnded(sparkBatchJob)
                                            || (jobState != null && (isDone(jobState) || isRunning(jobState)));
                                })
                                .filter(sparkBatchJob -> {
                                    String jobState = getJobState(sparkBatchJob);
                                    return isJobEnded(sparkBatchJob)
                                            || (jobState != null && (isDone(jobState) || isRunning(jobState)));
                                })
                                .flatMap(sparkBatchJob -> {
                                    String jobState = getJobState(sparkBatchJob);
                                    if (jobState != null && (isDone(jobState) || isRunning(jobState))) {
                                        if (isDone(jobState) && !isSuccess(jobState)) {
                                            String errorMsg = "The Spark job failed to start due to:\n" + getJobLog(sparkBatchJob);
                                            log().warn(errorMsg);
                                            return Observable.error(new SparkJobException(errorMsg));
                                        } else {
                                            return Observable.just(jobState);
                                        }
                                    } else {
                                        return Observable.just(sparkBatchJob.schedulerState().toString());
                                    }
                                })
                );
    }

    @NotNull
    @Override
    protected Observable<String> getJobLogAggregationDoneObservable() {
        // TODO: enable yarn log aggregation
        return Observable.just("SUCCEEDED");
    }

    @NotNull
    public Observable<SparkJobLog> getSubmissionLogRequest(@NotNull String livyUrl,
                                                           int batchId,
                                                           int startIndex,
                                                           int maxLinePerGet) {
        String requestUrl = String.format("%s/batches/%d/log", livyUrl, batchId);
        List<NameValuePair> parameters = Arrays.asList(
                new BasicNameValuePair("from", String.valueOf(startIndex)),
                new BasicNameValuePair("size", String.valueOf(maxLinePerGet)));
        List<Header> headers = Arrays.asList(
                new BasicHeader("x-ms-kobo-account-name", getAccount().getName()));
        return getHttp()
                .withUuidUserAgent()
                .get(requestUrl, parameters, headers, SparkJobLog.class);
    }

    @Override
    public boolean isSuccess(String state) {
        return state.equalsIgnoreCase(SparkBatchJobState.SUCCESS.toString());
    }

    public boolean isJobEnded(
            @NotNull com.microsoft.azure.hdinsight.sdk.rest.azure.serverless.spark.models.SparkBatchJob sparkBatchJob) {
        return sparkBatchJob.schedulerState().toString().equalsIgnoreCase(SchedulerState.ENDED.toString());
    }

    @Nullable
    public String getJobState(
            @NotNull com.microsoft.azure.hdinsight.sdk.rest.azure.serverless.spark.models.SparkBatchJob sparkBatchJob) {
        if(sparkBatchJob.properties() != null
                && sparkBatchJob.properties().responsePayload() != null
                && StringUtils.isNotEmpty(sparkBatchJob.properties().responsePayload().getState())) {
            return sparkBatchJob.properties().responsePayload().getState();
        } else {
            return null;
        }
    }

    @Nullable
    public String getJobLog(
            @NotNull com.microsoft.azure.hdinsight.sdk.rest.azure.serverless.spark.models.SparkBatchJob sparkBatchJob) {
        if(sparkBatchJob.properties() != null
                && sparkBatchJob.properties().responsePayload() != null
                && sparkBatchJob.properties().responsePayload().getLog() != null) {
            return String.join("\n", sparkBatchJob.properties().responsePayload().getLog());
        } else {
            return null;
        }
    }

    @NotNull
    @Override
    public Observable<AbstractMap.SimpleImmutableEntry<MessageInfoType, String>> getSubmissionLog() {
        ImmutableSet<String> ignoredEmptyLines = ImmutableSet.of("stdout:", "stderr:", "yarn diagnostics:");
        final int GET_LIVY_URL_REPEAT_DELAY_MILLISECONDS = 3000;
        final int MAX_LOG_LINES_PER_REQUEST = 128;
        final int GET_LOG_REPEAT_DELAY_MILLISECONDS = 200;
        // We need to repeatly call getSparkBatchJobRequest() since "livyServerApi" field does not always exist in response but
        // only appeared for a while and before that we can't get the "livyServerApi" field.
        ctrlInfo("Trying to get livy URL...");
        return getSparkBatchJobRequest()
                .repeatWhen(ob -> ob.delay(GET_LIVY_URL_REPEAT_DELAY_MILLISECONDS, TimeUnit.MILLISECONDS))
                .takeUntil(sparkBatchJob ->
                        isJobEnded(sparkBatchJob)
                                || (sparkBatchJob.properties() != null
                                && StringUtils.isNotEmpty(sparkBatchJob.properties().livyServerAPI())))
                .filter(sparkBatchJob ->
                        isJobEnded(sparkBatchJob)
                                || (sparkBatchJob.properties() != null
                                && StringUtils.isNotEmpty(sparkBatchJob.properties().livyServerAPI())))
                .flatMap(job -> {
                    if (isJobEnded(job)) {
                        String message = "Job is in " + job.schedulerState().toString() + " state.";
                        return Observable.just(new AbstractMap.SimpleImmutableEntry<>(Log, message));
                    } else {
                        return Observable.just(job)
                                .doOnNext(sparkBatchJob -> {
                                    ctrlInfo("Successfully get livy URL: " + sparkBatchJob.properties().livyServerAPI());
                                    ctrlInfo("Trying to retrieve livy submission logs...");
                                    // After test we find batch id won't be provided until the job is in running state
                                    // However, since only one spark job will be run on the cluster, the batch ID should always be 0
                                    setBatchId(0);
                                })
                                .map(sparkBatchJob -> sparkBatchJob.properties().livyServerAPI())
                                // Get submission log
                                .flatMap(livyUrl ->
                                        Observable.defer(() -> getSubmissionLogRequest(livyUrl, getBatchId(), getLogStartIndex(), MAX_LOG_LINES_PER_REQUEST))
                                                .map(sparkJobLog -> Optional.ofNullable(sparkJobLog.getLog()).orElse(Collections.<String>emptyList()))
                                                .doOnNext(logs -> setLogStartIndex(getLogStartIndex() + logs.size()))
                                                .map(logs -> logs.stream()
                                                        .filter(logLine -> !ignoredEmptyLines.contains(logLine.trim().toLowerCase()))
                                                        .collect(Collectors.toList()))
                                                .flatMap(logLines -> {
                                                    if (logLines.size() > 0) {
                                                        return Observable.just(Triple.of(logLines, SparkBatchJobState.STARTING.toString(), SchedulerState.SCHEDULED.toString()));
                                                    } else {
                                                        return getSparkBatchJobRequest()
                                                                .flatMap(sparkBatchJob -> {
                                                                    String batchJobState = Optional.ofNullable(getJobState(sparkBatchJob)).orElse(SparkBatchJobState.STARTING.toString());
                                                                    return Observable.just(Triple.of(logLines, batchJobState, sparkBatchJob.schedulerState().toString()));
                                                                });
                                                    }
                                                })
                                                .onErrorResumeNext(err ->
                                                        getSparkBatchJobRequest()
                                                                .flatMap(sparkBatchJob -> {
                                                                    String batchJobState = Optional.ofNullable(getJobState(sparkBatchJob)).orElse(SparkBatchJobState.STARTING.toString());
                                                                    return Observable.just(Triple.of(Collections.<String>emptyList(), batchJobState, sparkBatchJob.schedulerState().toString()));
                                                                })
                                                )
                                                .repeatWhen(ob -> ob.delay(GET_LOG_REPEAT_DELAY_MILLISECONDS, TimeUnit.MILLISECONDS))
                                                // Continuously get Livy log until job state is not in STARTING state or job scheduler state is ENDED
                                                .takeUntil(logAndStateTriple ->
                                                        !logAndStateTriple.getMiddle().equalsIgnoreCase(SparkBatchJobState.STARTING.toString())
                                                                || logAndStateTriple.getRight().equalsIgnoreCase(SchedulerState.ENDED.toString())
                                                )
                                                .flatMap(logAndStateTriple -> {
                                                    if (!logAndStateTriple.getMiddle().equalsIgnoreCase(SparkBatchJobState.STARTING.toString())) {
                                                        String message = "Job is in " + logAndStateTriple.getMiddle() + " state.";
                                                        return Observable.just(new AbstractMap.SimpleImmutableEntry<>(Log, message));
                                                    } else if (logAndStateTriple.getRight().equalsIgnoreCase(SchedulerState.ENDED.toString())) {
                                                        String message = "Job is in " + logAndStateTriple.getRight() + " state.";
                                                        return Observable.just(new AbstractMap.SimpleImmutableEntry<>(Log, message));
                                                    } else {
                                                        return Observable.from(logAndStateTriple.getLeft())
                                                                .map(line -> new AbstractMap.SimpleImmutableEntry<>(Log, line));
                                                    }
                                                })
                                );
                    }
                });
    }

    @Override
    public Observable<AbstractMap.SimpleImmutableEntry<String, Long>> getDriverLog(String type, long logOffset, int size) {
        return Observable.empty();
    }

    private void ctrlInfo(@NotNull String message) {
        getCtrlSubject().onNext(new AbstractMap.SimpleImmutableEntry<>(MessageInfoType.Info, message));
    }

    private void ctrlHyperLink(@NotNull String url) {
        getCtrlSubject().onNext(new AbstractMap.SimpleImmutableEntry<>(MessageInfoType.Hyperlink, url));
    }
}
