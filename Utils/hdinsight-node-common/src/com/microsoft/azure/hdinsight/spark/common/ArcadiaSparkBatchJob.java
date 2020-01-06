/*
 * Copyright (c) Microsoft Corporation
 * <p/>
 * All rights reserved.
 * <p/>
 * MIT License
 * <p/>
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated
 * documentation files (the "Software"), to deal in the Software without restriction, including without limitation
 * the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and
 * to permit persons to whom the Software is furnished to do so, subject to the following conditions:
 * <p/>
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of
 * the Software.
 * <p/>
 * THE SOFTWARE IS PROVIDED *AS IS*, WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO
 * THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT,
 * TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package com.microsoft.azure.hdinsight.spark.common;

import com.microsoft.azure.hdinsight.common.MessageInfoType;
import com.microsoft.azuretools.azurecommons.helpers.NotNull;
import com.microsoft.azuretools.azurecommons.helpers.Nullable;
import rx.Observable;
import rx.Observer;

import java.io.File;
import java.net.URI;
import java.net.URL;
import java.util.AbstractMap.SimpleImmutableEntry;

public class ArcadiaSparkBatchJob extends SparkBatchJob {
    private final @NotNull Deployable deployDelegate;

    public ArcadiaSparkBatchJob(final @NotNull SparkSubmissionParameter submissionParameter,
                                final @NotNull SparkBatchSubmission sparkBatchSubmission,
                                final @NotNull Deployable deployDelegate,
                                final @NotNull Observer<SimpleImmutableEntry<MessageInfoType, String>> ctrlSubject) {
        super(submissionParameter, sparkBatchSubmission, ctrlSubject);
        this.deployDelegate = deployDelegate;
    }

    @NotNull
    @Override
    public Observable<? extends ISparkBatchJob> deploy(@NotNull String artifactPath) {
        return deployDelegate.deploy(new File(artifactPath))
                .map(uploadedUri -> {
                    ctrlInfo(String.format("File %s has been uploaded to %s.", artifactPath, uploadedUri));

                    getSubmissionParameter().setFilePath(uploadedUri);

                    return this;
                });
    }

    @NotNull
    @Override
    public Observable<String> awaitStarted() {
        // No submission log and driver log fetching supports
        return Observable.just("no_waiting")
                .doOnNext(state -> ctrlInfo("The Spark Batch job has been submitted to Synapse Spark pool"
                        + getConnectUri().toString()
                        + " with the following parameters: "
                        + getSubmissionParameter().serializeToJson()));
    }

    @NotNull
    @Override
    public Observable<SimpleImmutableEntry<String, Long>> getDriverLog(String type, long logOffset, int size) {
        return Observable.empty();
    }

    @NotNull
    @Override
    public Observable<SimpleImmutableEntry<MessageInfoType, String>> getSubmissionLog() {
        // No batches/{id}/log API support yet
        URL jobHistoryWebUrl = getJobHistoryWebUrl();
        String trackingJobMsg = "Track the batch job by opening ";
        if (jobHistoryWebUrl != null) {
            trackingJobMsg += "<a href=\"" + jobHistoryWebUrl + "\">Spark Job History Server</a> and ";
        }
        trackingJobMsg += "<a href=\"" + getJobDetailsWebUrl() + "\">Spark Job Details UI</a> in Browser";
        getCtrlSubject().onNext(new SimpleImmutableEntry<>(MessageInfoType.HtmlPersistentMessage, trackingJobMsg));

        return Observable.empty();
    }

    @NotNull
    @Override
    protected Observable<SimpleImmutableEntry<String, String>> getJobDoneObservable() {
        // TODO: Monitoring the Job status
        return Observable.empty();
    }

    @Nullable
    @Override
    public URI getConnectUri() {
        return getArcadiaSubmission().getLivyUri().resolve("batches");
    }

    @NotNull
    private SparkBatchArcadiaSubmission getArcadiaSubmission() {
        return (SparkBatchArcadiaSubmission) super.getSubmission();
    }

    @Nullable
    private URL getJobHistoryWebUrl() {
        return getArcadiaSubmission().getHistoryServerUrl(getBatchId());
    }

    @NotNull
    private URL getJobDetailsWebUrl() {
        return getArcadiaSubmission().getJobDetailsWebUrl(getBatchId());
    }

    private void ctrlInfo(@NotNull String message) {
        getCtrlSubject().onNext(new SimpleImmutableEntry<>(MessageInfoType.Info, message));
    }
}
