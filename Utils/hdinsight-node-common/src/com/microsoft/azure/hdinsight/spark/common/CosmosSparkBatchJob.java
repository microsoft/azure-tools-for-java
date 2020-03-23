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

import com.microsoft.azure.hdinsight.common.MessageInfoType;
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
                        (cluster, appId) -> Pair.of(
                                state,
                                cluster.getSparkHistoryUiUri() == null ?
                                        null :
                                        cluster.getSparkMasterUiUri().toString() + "?adlaAccountName=" + cluster.getAccount().getName())))
                .map(stateJobUriPair -> {
                    if (stateJobUriPair.getRight() != null) {
                        getCtrlSubject().onNext(new SimpleImmutableEntry<>(MessageInfoType.Hyperlink,
                                                                           stateJobUriPair.getRight()));
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

    private void ctrlInfo(@NotNull String message) {
        getCtrlSubject().onNext(new SimpleImmutableEntry<>(MessageInfoType.Info, message));
    }

    @Override
    public CosmosSparkBatchJob clone() {
        return new CosmosSparkBatchJob(
                this.getSubmissionParameter(),
                this.getAzureSubmission()
        );
    }
}
