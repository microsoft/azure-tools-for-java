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
 * Spark Batch Job Submission Run profile state
 */
public class SparkBatchJobSubmissionState implements RunProfileState, RemoteState {
    private final Project myProject;
    private SparkSubmitModel submitModel;
    private RemoteConnection remoteConnection;

    public SparkBatchJobSubmissionState(Project project, SparkSubmitModel submitModel) {
        this.myProject = project;
        this.submitModel = submitModel;
    }

    public void setRemoteConnection(RemoteConnection remoteConnection) {
        this.remoteConnection = remoteConnection;
    }

    public SparkSubmitModel getSubmitModel() {
        return submitModel;
    }

    @Nullable
    @Override
    public ExecutionResult execute(Executor executor, @NotNull ProgramRunner programRunner) throws ExecutionException {
        if (programRunner instanceof SparkBatchJobDebuggerRunner) {
            ConsoleViewImpl consoleView = new ConsoleViewImpl(myProject, false);
            SparkBatchJobDebugProcessHandler process = new SparkBatchJobDebugProcessHandler(myProject);

            consoleView.attachToProcess(process);

            ExecutionResult result = new DefaultExecutionResult(consoleView, process);
            programRunner.onProcessStarted(null, result);

            return result;
        } else if (programRunner instanceof SparkBatchJobRunner) {
            SparkBatchJobRunner jobRunner = (SparkBatchJobRunner) programRunner;
            jobRunner.submitJob(getSubmitModel());
        }

        return null;
    }

    @Override
    public RemoteConnection getRemoteConnection() {
        return this.remoteConnection;
    }
}
