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
import com.microsoft.azure.hdinsight.common.logger.ILogger;
import com.microsoft.azure.hdinsight.common.mvc.IdeSchedulers;
import com.microsoft.azure.hdinsight.spark.common.ISparkBatchJob;
import com.microsoft.azure.hdinsight.spark.common.SparkJobUploadArtifactException;
import com.microsoft.azuretools.azurecommons.helpers.NotNull;
import com.microsoft.azuretools.azurecommons.helpers.Nullable;
import org.apache.commons.io.output.NullOutputStream;
import rx.Observable;
import rx.Subscription;
import rx.subjects.PublishSubject;

import java.io.InputStream;
import java.io.OutputStream;
import java.util.AbstractMap.SimpleImmutableEntry;
import java.util.Optional;

import static com.microsoft.azure.hdinsight.common.MessageInfoType.Info;

public class SparkBatchJobRemoteProcess extends Process implements ILogger {
    @NotNull
    private IdeSchedulers schedulers;
    @NotNull
    private String artifactPath;
    @NotNull
    private final String title;
    @NotNull
    private final PublishSubject<SimpleImmutableEntry<MessageInfoType, String>> ctrlSubject;
    @NotNull
    private SparkJobLogInputStream jobStdoutLogInputSteam;
    @NotNull
    private SparkJobLogInputStream jobStderrLogInputSteam;
    @Nullable
    private Subscription jobSubscription;
    @NotNull
    private final ISparkBatchJob sparkJob;
    @NotNull
    private final PublishSubject<SparkBatchJobSubmissionEvent> eventSubject = PublishSubject.create();
    private boolean isDestroyed = false;

    private boolean isDisconnected;

    public SparkBatchJobRemoteProcess(@NotNull IdeSchedulers schedulers,
                                      @NotNull ISparkBatchJob sparkJob,
                                      @NotNull String artifactPath,
                                      @NotNull String title,
                                      @NotNull PublishSubject<SimpleImmutableEntry<MessageInfoType, String>> ctrlSubject) {
        this.schedulers = schedulers;
        this.sparkJob = sparkJob;
        this.artifactPath = artifactPath;
        this.title = title;
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
        if (!isDestroyed()) {
            getSparkJob().killBatchJob()
                    .doOnError(err -> getCtrlSubject().onError(err))
                    .doOnEach(job -> {
                        this.isDestroyed = true;
                        this.disconnect();
                    })
                    .subscribe(
                            job -> log().trace("Killed Spark batch job " + job.getBatchId()),
                            err -> log().warn("Got error when killing Spark batch job", err),
                            () -> {}
                    );
        }
    }

    @NotNull
    public ISparkBatchJob getSparkJob() {
        return sparkJob;
    }

    public Optional<Subscription> getJobSubscription() {
        return Optional.ofNullable(jobSubscription);
    }

    public void start() {
        // Build, deploy and wait for the job done.
        jobSubscription = prepareArtifact()
                .flatMap(this::submitJob)
                .flatMap(this::awaitForJobStarted)
                .flatMap(this::attachInputStreams)
                .flatMap(this::awaitForJobDone)
                .subscribe(sdPair -> {
                    if (sparkJob.isSuccess(sdPair.getKey())) {
                        ctrlInfo("");
                        ctrlInfo("========== RESULT ==========");
                        ctrlInfo("Job run successfully.");
                    } else {
                        ctrlInfo("");
                        ctrlInfo("========== RESULT ==========");
                        ctrlError("Job state is " + sdPair.getKey());
                        ctrlError("Diagnostics: " + sdPair.getValue());
                    }
                }, err -> {
                    ctrlSubject.onError(err);
                    destroy();
                }, () -> {
                    disconnect();
                });
    }

    @NotNull
    private Observable<? extends ISparkBatchJob> awaitForJobStarted(@NotNull ISparkBatchJob job) {
        return job.awaitStarted()
                .map(state -> job);
    }

    private Observable<? extends ISparkBatchJob> attachJobInputStream(SparkJobLogInputStream inputStream, ISparkBatchJob job) {
        return Observable.just(inputStream)
                .map(stream -> stream.attachJob(job))
                .subscribeOn(schedulers.processBarVisibleAsync("Attach Spark batch job outputs " + inputStream.getLogType()));
    }

    public void disconnect() {
        if (!isDisconnected()) {
            this.isDisconnected = true;

            this.ctrlSubject.onCompleted();
            this.eventSubject.onCompleted();

            this.getJobSubscription().ifPresent(Subscription::unsubscribe);
        }
    }

    protected void ctrlInfo(String message) {
        ctrlSubject.onNext(new SimpleImmutableEntry<>(Info, message));
    }

    protected void ctrlError(String message) {
        ctrlSubject.onNext(new SimpleImmutableEntry<>(MessageInfoType.Error, message));
    }

    @NotNull
    public PublishSubject<SparkBatchJobSubmissionEvent> getEventSubject() {
        return eventSubject;
    }

    protected Observable<ISparkBatchJob> startJobSubmissionLogReceiver(ISparkBatchJob job) {
        return job.getSubmissionLog()
                .doOnNext(ctrlSubject::onNext)
                // "ctrlSubject::onNext" lead to uncaught exception
                // while "ctrlError" only print error message in console view
                .doOnError(err -> ctrlError(err.getMessage()))
                .lastOrDefault(null)
                .map((@Nullable SimpleImmutableEntry<MessageInfoType, String> messageTypeText) -> job);
    }

    // Build and deploy artifact
    protected Observable<? extends ISparkBatchJob> prepareArtifact() {
        return getSparkJob()
                .deploy(artifactPath)
                .onErrorResumeNext(err -> {
                    Throwable rootCause = err.getCause() != null ? err.getCause() : err;
                    return Observable.error(new SparkJobUploadArtifactException("Failed to upload Spark application artifacts: " + rootCause.getMessage(), rootCause));
                })
                .subscribeOn(schedulers.processBarVisibleAsync("Deploy the jar file into cluster"));
    }

    protected Observable<? extends ISparkBatchJob> submitJob(ISparkBatchJob sparkJob) {
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
        return title;
    }

    private Observable<? extends ISparkBatchJob> attachInputStreams(ISparkBatchJob job) {
        return Observable.zip(
                attachJobInputStream((SparkJobLogInputStream) getErrorStream(), job),
                attachJobInputStream((SparkJobLogInputStream) getInputStream(), job),
                (job1, job2) -> job);
    }

    Observable<SimpleImmutableEntry<String, String>> awaitForJobDone(ISparkBatchJob runningJob) {
        return runningJob.awaitDone()
                .subscribeOn(schedulers.processBarVisibleAsync("Spark batch job " + getTitle() + " is running"))
                .doOnNext(jobStateDiagnosticsPair ->
                        getCtrlSubject().onNext(new SimpleImmutableEntry(
                                MessageInfoType.Telemetry,
                                jobStateDiagnosticsPair.getKey() + "`````" + jobStateDiagnosticsPair.getValue())))
                .flatMap(jobStateDiagnosticsPair -> runningJob
                        .awaitPostDone()
                        .subscribeOn(schedulers.processBarVisibleAsync(
                                "Waiting for " + getTitle() + " log aggregation is done"))
                        .map(any -> jobStateDiagnosticsPair));
    }

    @NotNull
    public PublishSubject<SimpleImmutableEntry<MessageInfoType, String>> getCtrlSubject() {
        return ctrlSubject;
    }

    public boolean isDestroyed() {
        return isDestroyed;
    }
}
