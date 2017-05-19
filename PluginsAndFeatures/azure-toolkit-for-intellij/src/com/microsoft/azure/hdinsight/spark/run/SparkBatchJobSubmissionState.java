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

import com.intellij.debugger.engine.RemoteDebugProcessHandler;
import com.intellij.execution.DefaultExecutionResult;
import com.intellij.execution.ExecutionException;
import com.intellij.execution.ExecutionResult;
import com.intellij.execution.Executor;
import com.intellij.execution.configurations.RemoteConnection;
import com.intellij.execution.configurations.RemoteState;
import com.intellij.execution.configurations.RunProfileState;
import com.intellij.execution.impl.ConsoleViewImpl;
import com.intellij.execution.runners.ProgramRunner;
import com.intellij.openapi.project.Project;
import com.microsoft.azure.hdinsight.sdk.cluster.IClusterDetail;
import com.microsoft.azure.hdinsight.spark.common.SparkSubmissionParameter;
import com.microsoft.azure.hdinsight.spark.common.SparkSubmitAdvancedConfigModel;
import com.microsoft.azure.hdinsight.spark.common.SparkSubmitModel;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;

/**
 * Created by zhwe on 5/18/2017.
 */
public class SparkBatchJobSubmissionState implements RunProfileState, RemoteState {
    private final Project myProject;
    private SparkSubmissionParameter submissionParameter;
    private SparkSubmitModel submitModel;
    private RemoteConnection remoteConnection;

    private SparkSubmitAdvancedConfigModel submitAdvancedConfigModel;

    private IClusterDetail clusterDetail;

    public SparkBatchJobSubmissionState(Project project, SparkSubmissionParameter submissionParameter, SparkSubmitModel submitModel, IClusterDetail detail, SparkSubmitAdvancedConfigModel advModel) {
        this.myProject = project;
        this.submissionParameter = submissionParameter;
        this.submitModel = submitModel;
        this.clusterDetail = detail;
        this.submitAdvancedConfigModel = advModel;
    }

    public void setRemoteConnection(RemoteConnection remoteConnection) {
        this.remoteConnection = remoteConnection;
    }

    public SparkSubmitModel getSubmitModel() {
        return submitModel;
    }

    public IClusterDetail getClusterDetail() {
        return clusterDetail;
    }

    public SparkSubmitAdvancedConfigModel getSubmitAdvancedConfigModel() {
        return submitAdvancedConfigModel;
    }



    public SparkSubmissionParameter getSubmissionParameter() {
        return submissionParameter;
    }

    @Nullable
    @Override
    public ExecutionResult execute(Executor executor, @NotNull ProgramRunner programRunner) throws ExecutionException {
        ConsoleViewImpl consoleView = new ConsoleViewImpl(myProject, false);
        RemoteDebugProcessHandler process = new RemoteDebugProcessHandler(myProject);
        consoleView.attachToProcess(process);
        return new DefaultExecutionResult(consoleView, process);
    }

    @Override
    public RemoteConnection getRemoteConnection() {
        return this.remoteConnection;
    }
}
