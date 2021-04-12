/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.hdinsight.spark.common;

import com.microsoft.azure.hdinsight.sdk.common.azure.serverless.AzureSparkCosmosCluster;
import com.microsoft.azure.hdinsight.sdk.common.azure.serverless.AzureSparkCosmosClusterManager;
import com.microsoft.azure.hdinsight.spark.jobs.JobUtils;
import com.microsoft.azuretools.azurecommons.helpers.NotNull;
import com.microsoft.azuretools.azurecommons.helpers.Nullable;
import org.apache.commons.lang3.tuple.Pair;
import rx.Observable;

import java.io.File;
import java.net.URI;
import java.util.AbstractMap.SimpleImmutableEntry;
import java.util.NoSuchElementException;
import java.util.Objects;

public class CosmosSparkBatchJob extends SparkBatchJob {
    public CosmosSparkBatchJob(@NotNull SparkSubmissionParameter submissionParameter,
                               @NotNull SparkBatchAzureSubmission azureSubmission) {
        super(submissionParameter, azureSubmission);
    }

    @NotNull
    private Observable<? extends AzureSparkCosmosCluster> getCosmosSparkCluster() {
        return AzureSparkCosmosClusterManager.getInstance()
                .findCluster(getAzureSubmission().getAccountName(), getAzureSubmission().getClusterId())
                .onErrorResumeNext(err -> Observable.error(err instanceof NoSuchElementException ?
                        new SparkJobNotConfiguredException(String.format(
                                "Can't find the target cluster %s(ID: %s) from account %s",
                                getSubmissionParameter().getClusterName(),
                                getAzureSubmission().getClusterId(),
                                getAzureSubmission().getAccountName())) :
                        err));
    }


    @NotNull
    @Override
    public Observable<? extends ISparkBatchJob> deploy(@NotNull String artifactPath) {
        return getCosmosSparkCluster()
                .flatMap(cluster -> {
                    try {
                        if (cluster.getStorageAccount() == null) {
                            // TODO: May use interaction session to upload
                            return Observable.empty();
                        }

                        File localFile = new File(artifactPath);

                        URI remoteUri = URI.create(cluster.getStorageAccount().getDefaultContainerOrRootPath())
                                .resolve("SparkSubmission/")
                                .resolve(JobUtils.getFormatPathByDate() + "/")
                                .resolve(localFile.getName());

                        ctrlInfo(String.format("Begin uploading file %s to Azure Datalake store %s ...", artifactPath, remoteUri));

                        getSubmissionParameter().setFilePath(remoteUri.toString());

                        return cluster.uploadToStorage(localFile, remoteUri);
                    } catch (Exception e) {
                        return Observable.error(e);
                    }
                })
                .doOnNext(size -> ctrlInfo(String.format("Upload to Azure Datalake store %d bytes successfully.", size)))
                .map(path -> this);
    }

    @Nullable
    @Override
    public URI getConnectUri() {
        return getAzureSubmission().getLivyUri() == null ? null : getAzureSubmission().getLivyUri().resolve("/batches");
    }

    @NotNull
    @Override
    public Observable<String> awaitStarted() {
        return super.awaitStarted()
                .flatMap(state -> Observable.zip(
                        getCosmosSparkCluster(), getSparkJobApplicationIdObservable().defaultIfEmpty(null),
                        (cluster, appId) -> {
                            final URI sparkHistoryUiUri = cluster.getSparkHistoryUiUri();
                            return Pair.of(
                                    state,
                                    sparkHistoryUiUri == null ?
                                    null :
                                    sparkHistoryUiUri + "?adlaAccountName=" + cluster.getAccount().getName());
                        }))
                .map(stateJobUriPair -> {
                    if (stateJobUriPair.getRight() != null) {
                        ctrlHyperLink(stateJobUriPair.getRight());
                    }

                    return stateJobUriPair.getKey();
                });
    }

    @NotNull
    @Override
    public Observable<SimpleImmutableEntry<String, Long>> getDriverLog(@NotNull String type, long logOffset, int size) {
        if (getConnectUri() == null) {
            return Observable.error(new SparkJobNotConfiguredException("Can't get Spark job connection URI, " +
                    "please configure Spark cluster which the Spark job will be submitted."));
        }

        // FIXME!!!
//        return getStatus()
//                .map(status -> new SimpleImmutableEntry<>(String.join("", status.getLog()), logOffset));
        return Observable.empty();
    }

    @Override
    protected Observable<String> getSparkJobDriverLogUrlObservable() {
        return Observable.just(Objects.requireNonNull(getConnectUri()).toString() + "/" + getBatchId() + "/log");
    }

    @Override
    public Observable<String> awaitPostDone() {
        return Observable.empty();
    }

    @NotNull
    private SparkBatchAzureSubmission getAzureSubmission() {
        return (SparkBatchAzureSubmission) getSubmission();
    }

    @Override
    public CosmosSparkBatchJob clone() {
        return new CosmosSparkBatchJob(
                this.getSubmissionParameter(),
                this.getAzureSubmission()
        );
    }
}
