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
import com.microsoft.azure.hdinsight.sdk.cluster.IClusterDetail;
import com.microsoft.azure.hdinsight.spark.common.*;
import com.microsoft.azure.hdinsight.spark.jobs.JobUtils;
import com.microsoft.azuretools.azurecommons.helpers.NotNull;
import com.microsoft.azuretools.azurecommons.helpers.Nullable;
import rx.Observable;
import rx.Subscription;
import rx.subjects.PublishSubject;

import java.io.IOException;
import java.net.URI;
import java.util.AbstractMap.SimpleImmutableEntry;

public class SparkBatchJobRemoteDebugProcess extends SparkBatchJobRemoteProcess {
    @NotNull
    private final SparkBatchDebugSession debugSession;
    @NotNull
    private SparkBatchRemoteDebugJobSshAuth authData;
    @Nullable
    private Subscription executorSubscription;

    public SparkBatchJobRemoteDebugProcess(@NotNull IdeSchedulers schedulers,
                                           @NotNull SparkBatchDebugSession debugSession,
                                           @NotNull SparkSubmissionParameter submissionParameter,
                                           @NotNull String artifactPath,
                                           @NotNull SparkBatchRemoteDebugJobSshAuth authData,
                                           @NotNull PublishSubject<SimpleImmutableEntry<MessageInfoType, String>> ctrlSubject) {
        super(schedulers, submissionParameter, artifactPath, ctrlSubject);
        this.debugSession = debugSession;
        this.authData = authData;
    }

    @NotNull
    @Override
    public String getTitle() {
        return super.getTitle() + " driver";
    }

    @Override
    public SparkBatchJob createJobToSubmit(IClusterDetail cluster) {
        try {
            // Create the Spark Job with special debug enabling parameters
            return SparkBatchRemoteDebugJob.factory(
                    URI.create(JobUtils.getLivyConnectionURL(cluster)).toString(),
                    getSubmissionParameter(),
                    SparkBatchSubmission.getInstance());
        } catch (DebugParameterDefinedException e) {
            throw new RuntimeException(e);
        }
    }


    @Override
    protected Observable<SimpleImmutableEntry<SparkBatchJobState, String>> awaitForJobDone(SparkBatchJob runningJob) {
        return createDebugSession((SparkBatchRemoteDebugJob) runningJob)
                .subscribeOn(getSchedulers().processBarVisibleAsync("Create Spark batch job debug session"))
                .flatMap(super::awaitForJobDone);
    }

    @NotNull
    protected SparkBatchDebugJobJdbPortForwardedEvent createEventWithJdbPorForwarding(SparkBatchRemoteDebugJob job)
            throws JSchException, IOException {
        String remoteHost = job.getSparkDriverHost();
        int remotePort = job.getSparkDriverDebuggingPort();

        int localPort = debugSession
                .forwardToRemotePort(remoteHost, remotePort)
                .getForwardedLocalPort(remoteHost, remotePort);

        // Start to find executors
        executorSubscription = job.getExecutorsObservable()
                .map(hostContainerPair -> new SparkBatchJobExecutorCreatedEvent(
                        job, debugSession, hostContainerPair.getKey(), hostContainerPair.getValue()))
                .subscribe(getEventSubject()::onNext);

        return new SparkBatchDebugJobJdbPortForwardedEvent(job, debugSession, remoteHost, remotePort, localPort, true);
    }

    @Override
    public void disconnect() {
        super.disconnect();

        if (executorSubscription != null) {
            executorSubscription.unsubscribe();
        }
    }

    private Observable<SparkBatchRemoteDebugJob> createDebugSession(SparkBatchRemoteDebugJob job) {
        return Observable.fromCallable(() -> {
            try {
                SparkBatchDebugJobJdbPortForwardedEvent jdbReadyEvent = createEventWithJdbPorForwarding(job);

                // Debug session created and SSH port forwarded
                getEventSubject().onNext(jdbReadyEvent);

                return job;
            } catch (JSchException e) {
                // Rethrow it since JSch can't handle the certificate expired issue
                throw new SparkJobException(
                        "Can't create Spark Job remote debug session, " +
                        "please check whether SSH password has expired or wrong, using Putty or other SSH tool.",
                        e);
            }
        });
    }

    @Override
    public boolean isJobStarted(@NotNull SparkBatchJob job, SparkBatchJobState state) {
        try {
            return ((SparkBatchRemoteDebugJob) job).getSparkDriverDebuggingPort() > 0;
        } catch (IOException e) {
            return false;
        }
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
