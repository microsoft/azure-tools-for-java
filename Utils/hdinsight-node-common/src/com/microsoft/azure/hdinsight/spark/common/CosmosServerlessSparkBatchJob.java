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
                                         @NotNull SparkBatchSubmission sparkBatchSubmission) {
        super(account, submissionParameter, sparkBatchSubmission, null, null, null);
        this.account = account;
        this.jobUuid = UUID.randomUUID().toString();
        this.jobDeploy = jobDeploy;
        setDelaySeconds(5);
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
        return jobDeploy.deploy(new File(artifactPath), getCtrlSubject())
                .map(path -> {
                    ctrlInfo(String.format("Upload to Azure Datalake store %s successfully", path));
                    getSubmissionParameter().setFilePath(path);
                    return this;
                });
    }

    @Override
    public Observable<? extends ISparkBatchJob> killBatchJob() {
        ctrlInfo("Try to kill spark job...");
        return getAccount().killSparkBatchJobRequest(getJobUuid())
                .map(resp -> this)
                .onErrorReturn(err -> {
                    String errHint = "Failed to kill spark job.";
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
                .flatMap(batchResp ->
                        getJobSchedulerState(batchResp) == null
                                ? Observable.error(new IOException("Failed to get scheduler state of the job."))
                                : Observable.just(batchResp)
                )
                .retryWhen(err ->
                        err.zipWith(Observable.range(1, getRetriesMax()), (n, i) -> i)
                                .delay(getDelaySeconds(), TimeUnit.SECONDS)
                )
                .repeatWhen(ob -> ob.delay(GET_JOB_DONE_REPEAT_DELAY_MILLISECONDS, TimeUnit.MILLISECONDS))
                .takeUntil(this::isJobEnded)
                .filter(this::isJobEnded)
                .map(batchResp -> {
                    return new AbstractMap.SimpleImmutableEntry<>(
                            Optional.ofNullable(getJobState(batchResp)).orElse("unknown"),
                            getJobLog(batchResp));
                });
    }

    @Override
    public Observable<String> awaitStarted() {
        return getSparkBatchJobRequest()
                .flatMap(batchResp ->
                        getJobSchedulerState(batchResp) == null
                                ? Observable.error(new IOException("Failed to get scheduler state of the job."))
                                : Observable.just(batchResp)
                )
                .retryWhen(err ->
                        err.zipWith(Observable.range(1, getRetriesMax()), (n, i) -> i)
                                .delay(getDelaySeconds(), TimeUnit.SECONDS)
                )
                .repeatWhen(ob ->
                        ob.doOnNext(ignore -> ctrlInfo("The Spark job is starting..."))
                                .delay(getDelaySeconds(), TimeUnit.SECONDS))
                .takeUntil(batchResp -> isJobEnded(batchResp) || isJobRunning(batchResp))
                .filter(batchResp -> isJobEnded(batchResp) || isJobRunning(batchResp))
                .doOnNext(batchResp -> {
                    String sparkMasterUI = getMasterUI(batchResp);
                    if (sparkMasterUI != null) {
                        ctrlHyperLink(sparkMasterUI + "?adlaAccountName=" + getAccount().getName());
                    }
                })
                .flatMap(batchResp -> {
                    if (isJobRunning(batchResp) || isJobSuccess(batchResp)) {
                        return Observable.just(getJobState(batchResp));
                    } else if (isJobFailed(batchResp)) {
                        String errorMsg = "The Spark job failed to start due to:\n" + getJobLog(batchResp);
                        log().warn(errorMsg);
                        return Observable.error(new SparkJobException(errorMsg));
                    } else {
                        // Job scheduler state is ENDED;
                        return Observable.just("unknown");
                    }
                });
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
    public boolean isSuccess(@NotNull String state) {
        return state.equalsIgnoreCase(SparkBatchJobState.SUCCESS.toString());
    }

    public boolean isJobSuccess(
            @NotNull com.microsoft.azure.hdinsight.sdk.rest.azure.serverless.spark.models.SparkBatchJob batchResp) {
        String jobState = getJobState(batchResp);
        return jobState != null && isSuccess(jobState);
    }

    public boolean isJobFailed(
            @NotNull com.microsoft.azure.hdinsight.sdk.rest.azure.serverless.spark.models.SparkBatchJob batchResp) {
        String jobState = getJobState(batchResp);
        return jobState != null && isDone(jobState) && !isSuccess(jobState);
    }

    public boolean isJobEnded(
            @NotNull com.microsoft.azure.hdinsight.sdk.rest.azure.serverless.spark.models.SparkBatchJob batchResp) {
        String jobSchedulerState = getJobSchedulerState(batchResp);
        String jobRunningState = getJobState(batchResp);
        // Sometimes the job is not even started but go to ENDED state,
        // so we only get SchedulerState.ENDED state but empty jobRunningState
        return (jobSchedulerState != null && jobSchedulerState.equalsIgnoreCase(SchedulerState.ENDED.toString()))
                || (jobRunningState != null && super.isDone(jobRunningState));
    }

    public boolean isJobRunning(
            @NotNull com.microsoft.azure.hdinsight.sdk.rest.azure.serverless.spark.models.SparkBatchJob batchResp) {
        return batchResp.properties() != null
                && batchResp.properties().responsePayload() != null
                && batchResp.properties().responsePayload().getState() != null
                && isRunning(batchResp.properties().responsePayload().getState());
    }

    @Nullable
    public String getJobSchedulerState(
            @NotNull com.microsoft.azure.hdinsight.sdk.rest.azure.serverless.spark.models.SparkBatchJob batchResp) {
        return batchResp.schedulerState() != null
                ? batchResp.schedulerState().toString()
                : null;
    }

    @Nullable
    public String getJobState(
            @NotNull com.microsoft.azure.hdinsight.sdk.rest.azure.serverless.spark.models.SparkBatchJob batchResp) {
        return batchResp.properties() != null
                && batchResp.properties().responsePayload() != null
                && StringUtils.isNotEmpty(batchResp.properties().responsePayload().getState())
                ? batchResp.properties().responsePayload().getState()
                : null;
    }

    @Nullable
    public String getJobLog(
            @NotNull com.microsoft.azure.hdinsight.sdk.rest.azure.serverless.spark.models.SparkBatchJob batchResp) {
        return batchResp.properties() != null
                && batchResp.properties().responsePayload() != null
                && batchResp.properties().responsePayload().getLog() != null
                ? String.join("\n", batchResp.properties().responsePayload().getLog())
                : null;
    }

    @Nullable
    public String getMasterUI(
            @NotNull com.microsoft.azure.hdinsight.sdk.rest.azure.serverless.spark.models.SparkBatchJob batchResp) {
        return batchResp.properties() != null
                ? batchResp.properties().sparkMasterUI()
                : null;
    }

    @Nullable
    public String getLivyAPI(
            @NotNull com.microsoft.azure.hdinsight.sdk.rest.azure.serverless.spark.models.SparkBatchJob batchResp) {
        return batchResp.properties() != null
                ? batchResp.properties().livyServerAPI()
                : null;
    }

    @NotNull
    @Override
    public Observable<AbstractMap.SimpleImmutableEntry<MessageInfoType, String>> getSubmissionLog() {
        ImmutableSet<String> ignoredEmptyLines = ImmutableSet.of("stdout:", "stderr:", "yarn diagnostics:");
        final int GET_LIVY_URL_REPEAT_DELAY_MILLISECONDS = 3000;
        final int MAX_LOG_LINES_PER_REQUEST = 128;
        final int GET_LOG_REPEAT_DELAY_MILLISECONDS = 1000;
        // We need to repeatly call getSparkBatchJobRequest() since "livyServerApi" field does not always exist in response but
        // only appeared for a while and before that we can't get the "livyServerApi" field.
        ctrlInfo("Trying to get livy URL...");
        return getSparkBatchJobRequest()
                .flatMap(batchResp ->
                        getJobSchedulerState(batchResp) == null
                                ? Observable.error(new IOException("Failed to get scheduler state of the job."))
                                : Observable.just(batchResp)
                )
                .retryWhen(err ->
                        err.zipWith(Observable.range(1, getRetriesMax()), (n, i) -> i)
                                .delay(getDelaySeconds(), TimeUnit.SECONDS)
                )
                .repeatWhen(ob -> ob.delay(GET_LIVY_URL_REPEAT_DELAY_MILLISECONDS, TimeUnit.MILLISECONDS))
                .takeUntil(batchResp -> isJobEnded(batchResp) || StringUtils.isNotEmpty(getLivyAPI(batchResp)))
                .filter(batchResp -> isJobEnded(batchResp) || StringUtils.isNotEmpty(getLivyAPI(batchResp)))
                .flatMap(job -> {
                    if (isJobEnded(job)) {
                        String jobState = getJobState(job);
                        String schedulerState = getJobSchedulerState(job);
                        String message = String.format("Job scheduler state: %s. Job running state: %s.", schedulerState, jobState);
                        return Observable.just(new AbstractMap.SimpleImmutableEntry<>(MessageInfoType.Info, message));
                    } else {
                        return Observable.just(job)
                                .doOnNext(batchResp -> {
                                    ctrlInfo("Successfully get livy URL: " + batchResp.properties().livyServerAPI());
                                    ctrlInfo("Trying to retrieve livy submission logs...");
                                    // After test we find batch id won't be provided until the job is in running state
                                    // However, since only one spark job will be run on the cluster, the batch ID should always be 0
                                    setBatchId(0);
                                })
                                .map(batchResp -> batchResp.properties().livyServerAPI())
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
                                                                .map(batchResp -> Triple.of(logLines, getJobState(batchResp), getJobSchedulerState(batchResp)));
                                                    }
                                                })
                                                .onErrorResumeNext(errors ->
                                                        getSparkBatchJobRequest()
                                                                .delay(getDelaySeconds(), TimeUnit.SECONDS)
                                                                .map(batchResp -> Triple.of(new ArrayList<>(), getJobState(batchResp), getJobSchedulerState(batchResp)))
                                                )
                                                .repeatWhen(ob -> ob.delay(GET_LOG_REPEAT_DELAY_MILLISECONDS, TimeUnit.MILLISECONDS))
                                                // Continuously get livy log until job is not in Starting state or job is in Ended scheduler state
                                                .takeUntil(logAndStatesTriple -> {
                                                    String jobRunningState = logAndStatesTriple.getMiddle();
                                                    String jobSchedulerState = logAndStatesTriple.getRight();
                                                    return jobRunningState != null && !jobRunningState.equalsIgnoreCase(SparkBatchJobState.STARTING.toString())
                                                            || jobSchedulerState != null && jobSchedulerState.equalsIgnoreCase(SchedulerState.ENDED.toString());
                                                })
                                                .flatMap(logAndStatesTriple -> {
                                                    String jobRunningState = logAndStatesTriple.getMiddle();
                                                    String jobSchedulerState = logAndStatesTriple.getRight();
                                                    if (jobRunningState != null && !jobRunningState.equalsIgnoreCase(SparkBatchJobState.STARTING.toString())
                                                            || jobSchedulerState != null && jobSchedulerState.equalsIgnoreCase(SchedulerState.ENDED.toString())) {
                                                        String message = String.format("Job scheduler state: %s. Job running state: %s.", jobSchedulerState, jobRunningState);
                                                        return Observable.just(new AbstractMap.SimpleImmutableEntry<>(MessageInfoType.Info, message));
                                                    } else {
                                                        return Observable.from(logAndStatesTriple.getLeft())
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

    @Override
    public CosmosServerlessSparkBatchJob clone() {
        return new CosmosServerlessSparkBatchJob(
                this.getAccount(),
                this.jobDeploy,
                this.getSubmissionParameter(),
                this.getSubmission()
        );
    }
}
