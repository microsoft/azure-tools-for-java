/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.hdinsight.spark.run;

import com.jcraft.jsch.JSchException;
import com.microsoft.azure.hdinsight.common.mvc.IdeSchedulers;
import com.microsoft.azure.hdinsight.sdk.cluster.IClusterDetail;
import com.microsoft.azure.hdinsight.spark.common.*;
import com.microsoft.azuretools.azurecommons.helpers.NotNull;
import rx.Observable;
import rx.subjects.PublishSubject;

import java.io.IOException;
import java.io.InputStream;
import java.util.AbstractMap.SimpleImmutableEntry;

public class SparkBatchJobRemoteDebugExecutorProcess extends SparkBatchJobRemoteDebugProcess {
    @NotNull
    private final SparkBatchRemoteDebugJob parentJob;
    @NotNull
    private final String host;
    @NotNull
    private final String logUrl;
    @NotNull
    private final SparkJobExecutorLogInputStream stdOutInputStream;
    @NotNull
    private final SparkJobExecutorLogInputStream stdErrInputStream;

    public SparkBatchJobRemoteDebugExecutorProcess(@NotNull IdeSchedulers schedulers,
                                                   @NotNull SparkBatchRemoteDebugJob parentJob,
                                                   @NotNull String host,
                                                   @NotNull SparkBatchDebugSession debugSshSession,
                                                   @NotNull String logBaseUrl) {
        // Needn't artifact path for executor since no deployment
        super(schedulers, debugSshSession, parentJob, "", "Executor " + host, debugSshSession.getAuth(), PublishSubject.create());

        this.parentJob = parentJob;
        this.host = host;
        this.logUrl = logBaseUrl;
        this.stdOutInputStream = new SparkJobExecutorLogInputStream("stdout", logBaseUrl);
        this.stdErrInputStream = new SparkJobExecutorLogInputStream("stderr", logBaseUrl);
    }

    @NotNull
    @Override
    public String getTitle() {
        return super.getTitle().replace("driver", "executor " + host);
    }

    @Override
    protected Observable<? extends ISparkBatchJob> prepareArtifact() {
        return Observable.just(parentJob);
    }

    @Override
    protected Observable<? extends ISparkBatchJob> submitJob(ISparkBatchJob parentJob) {
        return Observable.just(parentJob);
    }

    @Override
    public InputStream getInputStream() {
        return stdOutInputStream;
    }

    @Override
    public InputStream getErrorStream() {
        return stdErrInputStream;
    }

    @NotNull
    @Override
    protected Observable<SparkBatchDebugJobJdbPortForwardedEvent> createEventWithJdbPorForwarding(
            SparkBatchRemoteDebugJob job) {
        return Observable.fromCallable(() -> {
            int remotePort = job.getYarnContainerJDBListenPort(logUrl);

            int localPort = getDebugSession()
                    .forwardToRemotePort(host, remotePort)
                    .getForwardedLocalPort(host, remotePort);

            return new SparkBatchDebugJobJdbPortForwardedEvent(job, getDebugSession(), host, remotePort, localPort, false);
        });
    }
}
