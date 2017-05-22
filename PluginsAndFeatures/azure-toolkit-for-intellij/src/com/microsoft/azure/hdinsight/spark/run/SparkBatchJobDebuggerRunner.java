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
 *
 */

package com.microsoft.azure.hdinsight.spark.run;

import com.intellij.debugger.impl.GenericDebuggerRunner;
import com.intellij.debugger.impl.GenericDebuggerRunnerSettings;
import com.intellij.execution.ExecutionException;
import com.intellij.execution.configurations.ConfigurationInfoProvider;
import com.intellij.execution.configurations.RemoteConnection;
import com.intellij.execution.configurations.RunProfile;
import com.intellij.execution.configurations.RunProfileState;
import com.intellij.execution.executors.DefaultDebugExecutor;
import com.intellij.execution.runners.ExecutionEnvironment;
import com.microsoft.azure.hdinsight.sdk.cluster.IClusterDetail;
import com.microsoft.azure.hdinsight.spark.common.SparkBatchDebugSession;
import com.microsoft.azure.hdinsight.spark.common.SparkSubmissionParameter;
import com.microsoft.azure.hdinsight.spark.common.SparkSubmitAdvancedConfigModel;
import com.microsoft.azure.hdinsight.spark.common.SparkSubmitModel;
import com.microsoft.azure.hdinsight.spark.run.configuration.RemoteDebugRunConfiguration;
import com.microsoft.tooling.msservices.components.DefaultLoader;
import org.apache.commons.lang.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import rx.exceptions.Exceptions;

import java.net.URI;

public class SparkBatchJobDebuggerRunner extends GenericDebuggerRunner {
    @Override
    public boolean canRun(@NotNull String executorId, @NotNull RunProfile profile) {
        // Only support debug now, will enable run in future
        return DefaultDebugExecutor.EXECUTOR_ID.equals(executorId) && profile instanceof RemoteDebugRunConfiguration;
    }

    @NotNull
    @Override
    public String getRunnerId() {
        return "SparkBatchJobDebug";
    }

    @Override
    public GenericDebuggerRunnerSettings createConfigurationData(ConfigurationInfoProvider settingsProvider) {
        return null;
    }

    @Override
    protected void execute(@NotNull ExecutionEnvironment environment, @Nullable Callback callback, @NotNull RunProfileState state) throws ExecutionException {
        SparkBatchJobSubmissionState submissionState = (SparkBatchJobSubmissionState) state;
        SparkSubmitModel submitModel = submissionState.getSubmitModel();
        SparkSubmissionParameter submissionParameter = submitModel.getSubmissionParameter();
        IClusterDetail clusterDetail = submitModel.getSelectedClusterDetail();
        SparkSubmitAdvancedConfigModel advModel = submitModel.getAdvancedConfigModel();

        submitModel
                .remoteDebugCompileRxOp(submissionParameter)
                .flatMap((artifact) -> submitModel.deployArtifactRxOp(
                        clusterDetail,
                        artifact.getName()))
                .map((selectedClusterDetail) -> {
                    // Create Batch Spark Debug Job
                    try {
                        return submitModel.tryToCreateBatchSparkDebugJob(selectedClusterDetail);
                    } catch (Exception e) {
                        throw Exceptions.propagate(e);
                    }
                })
                .map((remoteDebugJob) -> {
                    try {
                        URI connectUri = new URI(clusterDetail.getConnectionUrl());
                        String segs[] = connectUri.getHost().split("\\.");
                        segs[0] = segs[0].concat("-ssh");
                        String sshServer = StringUtils.join(segs, ".");

                        SparkBatchDebugSession session = SparkBatchDebugSession.factory(
                                sshServer, advModel.sshUserName);

                        String driverHost = remoteDebugJob.getSparkDriverHost();
                        int driverDebugPort = remoteDebugJob.getSparkDriverDebuggingPort();

                        switch (advModel.sshAuthType) {
                            case UseKeyFile:
                                session.setPrivateKeyFile(advModel.sshKyeFile);
                                break;
                            case UsePassword:
                                session.setPassword(advModel.sshPassword);
                                break;
                            default:
                                throw new SparkSubmitAdvancedConfigModel.UnknownSSHAuthTypeException(
                                        "Unknown type: " + advModel.sshAuthType.name());
                        }
                        session.open().forwardToRemotePort(driverHost, driverDebugPort);

                        int localPort = session.getForwardedLocalPort(driverHost, driverDebugPort);

                        submissionState.setRemoteConnection(new RemoteConnection(
                                true,
                                "localhost",
                                Integer.toString(localPort),
                                false));

                        super.execute(environment, callback, submissionState);

                        return session;
                    } catch (Exception e) {
                        throw Exceptions.propagate(e);
                    }
                })

                .subscribe(
                        (session) -> {
                            try {
                               // super.execute(environment, callback, submissionState);
                            } catch (Exception exception) {
                                DefaultLoader.getUIHelper().logError("Spark batch Job remote debug failed, got exception: ", exception);
                            }

                        },
                        (exception) -> {
                            DefaultLoader.getUIHelper().logError("Spark batch Job remote debug failed, got exception: ", exception);
                        });

    }
}
