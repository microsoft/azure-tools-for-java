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

import com.jcraft.jsch.JSchException;
import com.microsoft.azure.hdinsight.common.MessageInfoType;
import com.microsoft.azure.hdinsight.common.mvc.IdeSchedulers;
import com.microsoft.azure.hdinsight.spark.common.*;
import com.microsoft.azuretools.azurecommons.helpers.NotNull;
import rx.Observable;
import rx.subjects.PublishSubject;

import java.net.UnknownServiceException;
import java.util.AbstractMap.SimpleImmutableEntry;

public class SparkBatchJobRemoteDebugProcess extends SparkBatchJobRemoteProcess {
    @NotNull
    private final SparkBatchDebugSession debugSession;
    @NotNull
    private SparkBatchRemoteDebugJobSshAuth authData;

    public SparkBatchJobRemoteDebugProcess(@NotNull IdeSchedulers schedulers,
                                           @NotNull SparkBatchDebugSession debugSession,
                                           @NotNull ISparkBatchDebugJob sparkDebugJob,
                                           @NotNull String artifactPath,
                                           @NotNull String title,
                                           @NotNull SparkBatchRemoteDebugJobSshAuth authData,
                                           @NotNull PublishSubject<SimpleImmutableEntry<MessageInfoType, String>> ctrlSubject) {
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
                    String remoteHost = remoteHostPortPair.getKey();
                    int remotePort = remoteHostPortPair.getValue();

                    int localPort = 0;
                    try {
                        localPort = debugSession
                                .forwardToRemotePort(remoteHost, remotePort)
                                .getForwardedLocalPort(remoteHost, remotePort);

                        return Observable.just(new SparkBatchDebugJobJdbPortForwardedEvent(
                                job, debugSession, remoteHost, remotePort, localPort, true));
                    } catch (JSchException | UnknownServiceException e) {
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
