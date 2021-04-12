/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.hdinsight.spark.common;

import com.microsoft.azure.hdinsight.spark.common.log.SparkLogLine;
import com.microsoft.azuretools.azurecommons.helpers.NotNull;
import com.microsoft.azuretools.azurecommons.helpers.Nullable;
import rx.Observable;

import java.io.File;
import java.net.URI;
import java.net.URL;
import java.util.AbstractMap.SimpleImmutableEntry;

import static com.microsoft.azure.hdinsight.common.MessageInfoType.HtmlPersistentMessage;
import static com.microsoft.azure.hdinsight.spark.common.log.SparkLogLine.TOOL;

public class ArcadiaSparkBatchJob extends SparkBatchJob {
    @NotNull
    private final  Deployable deployDelegate;

    public ArcadiaSparkBatchJob(final @NotNull SparkSubmissionParameter submissionParameter,
                                final @NotNull SparkBatchSubmission sparkBatchSubmission,
                                final @NotNull Deployable deployDelegate) {
        super(submissionParameter, sparkBatchSubmission);
        this.deployDelegate = deployDelegate;
    }

    @NotNull
    @Override
    public Observable<? extends ISparkBatchJob> deploy(@NotNull String artifactPath) {
        return deployDelegate.deploy(new File(artifactPath), getCtrlSubject())
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
                         .doOnNext(state -> ctrlInfo("The Spark Batch job has been submitted to "
                                                             + "Apache Spark Pool for Azure Synapse "
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
    public Observable<SparkLogLine> getSubmissionLog() {
        // No batches/{id}/log API support yet
        final URL jobHistoryWebUrl = getJobHistoryWebUrl();
        String trackingJobMsg = "Track the batch job by opening ";
        if (jobHistoryWebUrl != null) {
            trackingJobMsg += "<a href=\"" + jobHistoryWebUrl + "\">Spark Job History Server</a> and ";
        }
        trackingJobMsg += "<a href=\"" + getJobDetailsWebUrl() + "\">Spark Job Details UI</a> in Browser";
        ctrlLog(TOOL, HtmlPersistentMessage, trackingJobMsg);

        return Observable.empty();
    }

    @NotNull
    @Override
    protected Observable<SimpleImmutableEntry<String, String>> getJobDoneObservable() {
        // TODO: Monitoring the Job status
        return Observable.empty();
    }

    @NotNull
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

    @Override
    public ArcadiaSparkBatchJob clone() {
        return new ArcadiaSparkBatchJob(
                this.getSubmissionParameter(),
                this.getSubmission(),
                this.deployDelegate
        );
    }
}
