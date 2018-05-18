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

package com.microsoft.azure.hdinsight.spark.run;

import com.google.common.net.HostAndPort;
import com.microsoft.azure.hdinsight.common.MessageInfoType;
import com.microsoft.azure.hdinsight.common.mvc.IdeSchedulers;
import com.microsoft.azure.hdinsight.sdk.cluster.IClusterDetail;
import com.microsoft.azure.hdinsight.spark.common.*;
import com.microsoft.azure.hdinsight.spark.jobs.JobUtils;
import com.microsoft.azuretools.azurecommons.helpers.NotNull;
import com.microsoft.azuretools.azurecommons.helpers.Nullable;
import org.apache.commons.io.output.NullOutputStream;
import rx.Observable;
import rx.Subscription;
import rx.subjects.PublishSubject;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.util.AbstractMap.SimpleImmutableEntry;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

import static com.microsoft.azure.hdinsight.common.MessageInfoType.Info;

public class SparkBatchJobRemoteProcess extends Process {
    @NotNull
    private IdeSchedulers schedulers;
    @NotNull
    private SparkSubmissionParameter submissionParameter;
    @NotNull
    private String artifactPath;
    @NotNull
    private final PublishSubject<SimpleImmutableEntry<MessageInfoType, String>> ctrlSubject;
    @NotNull
    private SparkJobLogInputStream jobStdoutLogInputSteam;
    @NotNull
    private SparkJobLogInputStream jobStderrLogInputSteam;
    @Nullable
    private Subscription jobSubscription;
    @Nullable
    private SparkBatchJob sparkJob;
    @NotNull
    private final PublishSubject<SparkBatchJobSubmissionEvent> eventSubject = PublishSubject.create();
    private boolean isDestroyed = false;

    private boolean isDisconnected;

    public SparkBatchJobRemoteProcess(@NotNull IdeSchedulers schedulers,
                                      @NotNull SparkSubmissionParameter submissionParameter,
                                      @NotNull String artifactPath,
                                      @NotNull PublishSubject<SimpleImmutableEntry<MessageInfoType, String>> ctrlSubject) {
        this.schedulers = schedulers;
        this.submissionParameter = submissionParameter;
        this.artifactPath = artifactPath;
        this.ctrlSubject = ctrlSubject;

        this.jobStdoutLogInputSteam = new SparkJobLogInputStream("stdout");
        this.jobStderrLogInputSteam = new SparkJobLogInputStream("stderr");
    }

    /**
     * To Kill the remote job.
     *
     * @return is the remote Spark Job killed
     */
    public boolean killProcessTree() {
        return false;
    }

    /**
     * Is the Spark job session connected
     *
     * @return is the Spark Job log getting session still connected
     */
    public boolean isDisconnected() {
        return isDisconnected;
    }

    @Nullable
    public HostAndPort getLocalTunnel(int i) {
        return null;
    }

    @Override
    public OutputStream getOutputStream() {
        return new NullOutputStream();
    }

    @Override
    public InputStream getInputStream() {
        return jobStdoutLogInputSteam;
    }

    @Override
    public InputStream getErrorStream() {
        return jobStderrLogInputSteam;
    }

    @Override
    public int waitFor() {
        return 0;
    }

    @Override
    public int exitValue() {
        return 0;
    }

    @Override
    public void destroy() {
        getSparkJob().ifPresent(sparkBatchJob -> {
            try {
                sparkBatchJob.killBatchJob();
            } catch (IOException ignored) {
            }
        });

        this.isDestroyed = true;

        this.disconnect();
    }

    public Optional<SparkBatchJob> getSparkJob() {
        return Optional.ofNullable(sparkJob);
    }

    public Optional<Subscription> getJobSubscription() {
        return Optional.ofNullable(jobSubscription);
    }

    public SparkBatchJob createJobToSubmit(IClusterDetail cluster) {
        return new SparkBatchJob(
                URI.create(JobUtils.getLivyConnectionURL(cluster)),
                getSubmissionParameter(),
                SparkBatchSubmission.getInstance());
    }

    public void start() {
        // Build, deploy and wait for the job done.
        jobSubscription = prepareArtifact()
                .flatMap(this::submitJob)
                .flatMap(this::awaitForJobStarted)
                .flatMap(this::attachInputStreams)
                .flatMap(this::awaitForJobDone)
                .subscribe(sdPair -> {
                    if (sdPair.getKey() == SparkBatchJobState.SUCCESS) {
                        logInfo("");
                        logInfo("========== RESULT ==========");
                        logInfo("Job run successfully.");
                    } else {
                        logInfo("");
                        logInfo("========== RESULT ==========");
                        logError("Job state is " + sdPair.getKey().toString());
                        logError("Diagnostics: " + sdPair.getValue());
                    }
                }, err -> {
                    ctrlSubject.onError(err);
                    destroy();
                }, () -> {
                    disconnect();
                });
    }

    @NotNull
    public boolean isJobStarted(@NotNull SparkBatchJob job, SparkBatchJobState state) {
        return state == SparkBatchJobState.RUNNING;
    }

    @NotNull
    private Observable<SparkBatchJob> awaitForJobStarted(@NotNull SparkBatchJob job) {
        return job.getStatus()
                .map(status -> new SimpleImmutableEntry<>(
                        SparkBatchJobState.valueOf(status.getState().toUpperCase()),
                        String.join("\n", status.getLog())))
                .retry(job.getRetriesMax())
                .repeatWhen(ob -> ob
                        .doOnNext(ignored -> logInfo("The Spark job is starting..."))
                        .delay(job.getDelaySeconds(), TimeUnit.SECONDS)
                )
                .takeUntil(stateLogPair -> stateLogPair.getKey().isJobDone() || isJobStarted(job, stateLogPair.getKey()))
                .filter(stateLogPair -> stateLogPair.getKey().isJobDone() || isJobStarted(job, stateLogPair.getKey()))
                .flatMap(stateLogPair -> {
                    if (stateLogPair.getKey().isJobDone() && stateLogPair.getKey() != SparkBatchJobState.SUCCESS) {
                        return Observable.error(
                                new SparkJobException("The Spark job failed to start due to " + stateLogPair.getValue()));
                    }

                    return Observable.just(job);
                });
    }

    private Observable<SparkBatchJob> attachJobInputStream(SparkJobLogInputStream inputStream, SparkBatchJob job) {
        return Observable.just(inputStream)
                .map(stream -> stream.attachJob(job))
                .subscribeOn(schedulers.processBarVisibleAsync("Attach Spark batch job outputs " + inputStream.getLogType()))
                .retryWhen(attempts -> attempts.flatMap(err -> {
                    try {
                        final String state = job.getState();

                        if (state.equals("starting") || state.equals("not_started") || state.equals("running")) {
                            logInfo("Job is waiting for start due to cluster busy, please wait or disconnect (The job will run when the cluster is free).");

                            return Observable.timer(5, TimeUnit.SECONDS);
                        }
                    } catch (IOException ignored) {
                    }

                    return Observable.error(new SparkJobException("Spark Job Service not available, please check HDInsight cluster status.", err));
                }));
    }

    public void disconnect() {
        this.isDisconnected = true;

        this.ctrlSubject.onCompleted();
        this.eventSubject.onCompleted();

        this.getJobSubscription().ifPresent(Subscription::unsubscribe);
    }

    protected void logInfo(String message) {
        ctrlSubject.onNext(new SimpleImmutableEntry<>(Info, message));
    }

    protected void logError(String message) {
        ctrlSubject.onNext(new SimpleImmutableEntry<>(MessageInfoType.Error, message));
    }

    @NotNull
    public PublishSubject<SparkBatchJobSubmissionEvent> getEventSubject() {
        return eventSubject;
    }

    protected Observable<SparkBatchJob> startJobSubmissionLogReceiver(SparkBatchJob job) {

        return job.getSubmissionLog()
                .doOnNext(ctrlSubject::onNext)
                .doOnError(ctrlSubject::onError)
                .last()
                .map(messageTypeText -> job);

    }

    // Build and deploy artifact
    protected Observable<SimpleImmutableEntry<IClusterDetail, String>> prepareArtifact() {
        return JobUtils.deployArtifact(artifactPath, getSubmissionParameter().getClusterName(), ctrlSubject)
                       .subscribeOn(schedulers.processBarVisibleAsync("Deploy the jar file into cluster"))
                       .toObservable();
    }

    protected Observable<? extends SparkBatchJob> submitJob(SimpleImmutableEntry<IClusterDetail, String> clusterArtifactUriPair) {
        IClusterDetail cluster = clusterArtifactUriPair.getKey();
        getSubmissionParameter().setFilePath(clusterArtifactUriPair.getValue());

        sparkJob = this.createJobToSubmit(cluster);

        return sparkJob
                .submit()
                .subscribeOn(schedulers.processBarVisibleAsync("Submit the Spark batch job"))
                .flatMap(this::startJobSubmissionLogReceiver)   // To receive the Livy submission log
                .doOnNext(job -> eventSubject.onNext(new SparkBatchJobSubmittedEvent(job)));
    }

    @NotNull
    public IdeSchedulers getSchedulers() {
        return schedulers;
    }

    @NotNull
    public String getTitle() {
        return getSubmissionParameter().getMainClassName();
    }

    protected Observable<? extends SparkBatchJob> attachInputStreams(SparkBatchJob job) {
        return Observable.zip(
                attachJobInputStream((SparkJobLogInputStream) getErrorStream(), job),
                attachJobInputStream((SparkJobLogInputStream) getInputStream(), job),
                (job1, job2) -> {
                    sparkJob = job;
                    return job;
                });
    }

    protected Observable<SimpleImmutableEntry<SparkBatchJobState, String>> awaitForJobDone(SparkBatchJob runningJob) {
        return runningJob.getJobDoneObservable()
                .subscribeOn(schedulers.processBarVisibleAsync("Spark batch job " + getTitle() + " is running"))
                .flatMap(jobStateDiagnosticsPair -> runningJob
                                .getJobLogAggregationDoneObservable()
                                .subscribeOn(schedulers.processBarVisibleAsync(
                                        "Waiting for " + getTitle() + " log aggregation is done"))
                                .map(any -> jobStateDiagnosticsPair));
    }

    @NotNull
    public PublishSubject<SimpleImmutableEntry<MessageInfoType, String>> getCtrlSubject() {
        return ctrlSubject;
    }

    @NotNull
    public SparkSubmissionParameter getSubmissionParameter() {
        return submissionParameter;
    }

    @NotNull
    public String getArtifactPath() {
        return artifactPath;
    }

    public boolean isDestroyed() {
        return isDestroyed;
    }
}
