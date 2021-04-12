/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.hdinsight.spark.common;

import com.microsoft.azure.hdinsight.spark.common.log.SparkLogLine;
import com.microsoft.azuretools.azurecommons.helpers.NotNull;
import rx.Observable;

import java.net.URI;
import java.util.AbstractMap;
import java.util.AbstractMap.SimpleImmutableEntry;

public interface ISparkBatchJob extends Cloneable, SparkClientControlMessage {
    /**
     * Getter of the base connection URI for HDInsight Spark Job service
     *
     * @return the base connection URI for HDInsight Spark Job service
     */
    URI getConnectUri();

    /**
     * Getter of the LIVY Spark batch job ID got from job submission
     *
     * @return the LIVY Spark batch job ID
     */
    int getBatchId();

    /**
     * Getter of the maximum retry count in RestAPI calling
     *
     * @return the maximum retry count in RestAPI calling
     */
    int getRetriesMax();

    /**
     * Setter of the maximum retry count in RestAPI calling
     * @param retriesMax the maximum retry count in RestAPI calling
     */
    void setRetriesMax(int retriesMax);

    /**
     * Getter of the delay seconds between tries in RestAPI calling
     *
     * @return the delay seconds between tries in RestAPI calling
     */
    int getDelaySeconds();

    /**
     * Setter of the delay seconds between tries in RestAPI calling
     * @param delaySeconds the delay seconds between tries in RestAPI calling
     */
    void setDelaySeconds(int delaySeconds);

    /**
     * Kill the batch job specified by ID
     *
     * @return the current instance observable for chain calling,
     *         Observable Error: IOException exceptions for networking connection issues related
     */
    Observable<? extends ISparkBatchJob> killBatchJob();

    /**
     * Get Spark batch job driver host by ID
     *
     * @return Spark driver node host observable
     *         Observable Error: IOException exceptions for the driver host not found
     */
    Observable<String> getSparkDriverHost();

    /**
     * Get Spark job driver log observable
     *
     * @param type the log type, such as `stderr`, `stdout`
     * @param logOffset the log offset that fetching would start from
     * @param size the fetching size, -1 for all.
     * @return the log and its starting offset pair observable
     */
    @NotNull
    Observable<SimpleImmutableEntry<String, Long>> getDriverLog(@NotNull String type, long logOffset, int size);

    /**
     * Get Spark job specified container log observable
     *
     * @param containerLogUrl the container log URL
     * @param type the log type, such as `stderr`, `stdout`
     * @param logOffset the log offset that fetching would start from
     * @param size the fetching size, -1 for all.
     * @return the log and its starting offset pair observable
     */
    @NotNull
    Observable<AbstractMap.SimpleImmutableEntry<String, Long>> getContainerLog(@NotNull String containerLogUrl, @NotNull String type, long logOffset, int size);

    /**
     * Get Spark job submission log observable
     *
     * @return the log type and content pair observable
     */
    @NotNull
    Observable<SparkLogLine> getSubmissionLog();

    /**
     * Await the job started observable
     *
     * @return the job state string
     */
    @NotNull
    Observable<String> awaitStarted();

    /**
     * Await the job done observable
     *
     * @return the job state string and its diagnostics message
     */
    @NotNull
    Observable<SimpleImmutableEntry<String, String>> awaitDone();

    /**
     * Await the job post actions done, such as the log aggregation
     * @return the job post action status string
     */
    @NotNull
    Observable<String> awaitPostDone();

    /**
     * Deploy the job artifact into cluster
     *
     * @param artifactPath the artifact to deploy
     * @return ISparkBatchJob observable
     *         Observable Error: IOException;
     */
    @NotNull
    Observable<? extends ISparkBatchJob> deploy(@NotNull String artifactPath);

    /**
     * Create a batch Spark job and submit the job into cluster
     *
     * @return ISparkBatchJob observable
     *         Observable Error: IOException;
     */
    @NotNull
    Observable<? extends ISparkBatchJob> submit();

    /**
     * Is the job done, success or failure
     *
     * @return true for success or failure
     */
    boolean isDone(@NotNull String state);

    /**
     * Is the job running
     *
     * @return true for running
     */
    boolean isRunning(@NotNull String state);

    /**
     * Is the job finished with success
     *
     * @return true for success
     */
    boolean isSuccess(@NotNull String state);

    /**
     * a clone of this instance
     * @return the clone of this instance
     */
    ISparkBatchJob clone();
}
