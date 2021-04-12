/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.hdinsight.spark.run;

import com.jcraft.jsch.JSchException;
import com.microsoft.azure.hdinsight.common.mvc.IdeSchedulers;
import com.microsoft.azure.hdinsight.spark.common.*;
import com.microsoft.azure.hdinsight.spark.common.log.SparkLogLine;
import com.microsoft.azuretools.azurecommons.helpers.NotNull;
import rx.Observable;
import rx.subjects.PublishSubject;

import java.net.UnknownServiceException;
import java.util.AbstractMap.SimpleImmutableEntry;

public class SparkBatchJobRemoteDebugProcess extends SparkBatchJobRemoteProcess {
    @NotNull
    private final SparkBatchDebugSession debugSession;
    @NotNull
    private final SparkBatchRemoteDebugJobSshAuth authData;

    public SparkBatchJobRemoteDebugProcess(@NotNull IdeSchedulers schedulers,
                                           @NotNull SparkBatchDebugSession debugSession,
                                           @NotNull ISparkBatchDebugJob sparkDebugJob,
                                           @NotNull String artifactPath,
                                           @NotNull String title,
                                           @NotNull SparkBatchRemoteDebugJobSshAuth authData,
                                           @NotNull PublishSubject<SparkLogLine> ctrlSubject) {
        super(schedulers, sparkDebugJob, artifactPath, title, ctrlSubject);
        this.debugSession = debugSession;
        this.authData = authData;
    }

    @NotNull
    @Override
    public String getTitle() {
        return super.getTitle() + " driver";
    }

    @Override
    Observable<SimpleImmutableEntry<String, String>> awaitForJobDone(ISparkBatchJob runningJob) {
        return createDebugSession((SparkBatchRemoteDebugJob) runningJob)
                .subscribeOn(getSchedulers().processBarVisibleAsync("Create Spark batch job debug session"))
                .flatMap(super::awaitForJobDone);
    }

    @NotNull
    protected Observable<SparkBatchDebugJobJdbPortForwardedEvent> createEventWithJdbPorForwarding(
            SparkBatchRemoteDebugJob job) {
        return Observable.zip(job.getSparkDriverHost(), job.getSparkDriverDebuggingPort(), SimpleImmutableEntry::new)
                .flatMap(remoteHostPortPair ->  {
                    final String remoteHost = remoteHostPortPair.getKey();
                    final int remotePort = remoteHostPortPair.getValue();

                    final int localPort;
                    try {
                        localPort = debugSession
                                .forwardToRemotePort(remoteHost, remotePort)
                                .getForwardedLocalPort(remoteHost, remotePort);

                        return Observable.just(new SparkBatchDebugJobJdbPortForwardedEvent(
                                job, debugSession, remoteHost, remotePort, localPort, true));
                    } catch (final JSchException | UnknownServiceException e) {
                        return Observable.error(e);
                    }
                });
    }

    @Override
    public void disconnect() {
        super.disconnect();
    }

    private Observable<SparkBatchRemoteDebugJob> createDebugSession(SparkBatchRemoteDebugJob job) {
        return createEventWithJdbPorForwarding(job)
                // Rethrow it since JSch can't handle the certificate expired issue
                .doOnError(e -> Observable.error(new SparkJobException(
                        "Can't create Spark Job remote debug session, " +
                        "please check whether SSH password has expired or wrong, using Putty or other SSH tool.",
                        e)))
                .map(jdbReadyEvent -> {
                    // Debug session created and SSH port forwarded
                    getEventSubject().onNext(jdbReadyEvent);

                    return job;
                });
    }

    @NotNull
    public SparkBatchRemoteDebugJobSshAuth getAuthData() {
        return authData;
    }

    @NotNull
    public SparkBatchDebugSession getDebugSession() {
        return debugSession;
    }
}
