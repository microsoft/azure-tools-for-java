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

import com.intellij.execution.DefaultExecutionResult;
import com.intellij.execution.ExecutionException;
import com.intellij.execution.ExecutionResult;
import com.intellij.execution.Executor;
import com.intellij.execution.configurations.*;
import com.intellij.execution.executors.DefaultDebugExecutor;
import com.intellij.execution.executors.DefaultRunExecutor;
import com.intellij.execution.impl.ConsoleViewImpl;
import com.intellij.execution.process.KillableColoredProcessHandler;
import com.intellij.execution.process.OSProcessHandler;
import com.intellij.execution.process.ProcessAdapter;
import com.intellij.execution.process.ProcessEvent;
import com.intellij.execution.runners.ProgramRunner;
import com.intellij.execution.ui.ConsoleView;
import com.intellij.execution.ui.ConsoleViewContentType;
import com.intellij.execution.util.JavaParametersUtil;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleManager;
import com.intellij.openapi.project.Project;
import com.intellij.util.PathUtil;
import com.microsoft.azure.hdinsight.common.HDInsightUtil;
import com.microsoft.azure.hdinsight.spark.common.SparkBatchJobConfigurableModel;
import com.microsoft.azure.hdinsight.spark.common.SparkLocalRunConfigurableModel;
import com.microsoft.azure.hdinsight.spark.common.SparkSubmissionParameter;
import com.microsoft.azure.hdinsight.spark.common.SparkSubmitModel;
import com.microsoft.azure.hdinsight.spark.mock.SparkLocalRunner;
import com.microsoft.azure.hdinsight.spark.ui.SparkJobLogConsoleView;
import com.microsoft.azure.hdinsight.spark.ui.SparkLocalRunConfigurable;
import com.microsoft.azuretools.telemetry.AppInsightsClient;
import com.microsoft.intellij.hdinsight.messages.HDInsightBundle;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.SystemUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.file.Paths;
import java.util.*;

/**
 * Spark Batch Job Submission Run profile state
 */
public class SparkBatchJobSubmissionState implements RunProfileState,
                                                     RemoteState,
                                                     SparkBatchRemoteRunProfileState {
    @NotNull
    private UUID uuid = UUID.randomUUID();

    private final Project myProject;
    private RemoteConnection remoteConnection;
    @NotNull
    private SparkBatchJobConfigurableModel jobModel;
    private final boolean isExecutor;

    // Properties for executing
    @Nullable
    private SparkBatchJobProcessCtrlLogOut remoteProcessCtrlLogHandler;
    @Nullable
    private ExecutionResult executionResult;
    @Nullable
    private ConsoleView consoleView;

    public SparkBatchJobSubmissionState(@NotNull Project project,
                                        @NotNull SparkBatchJobConfigurableModel jobModel,
                                        boolean isExecutor) {
        this.myProject = project;
        this.jobModel = jobModel;
        this.isExecutor = isExecutor;
    }

    @NotNull
    public String getUuid() {
        return uuid.toString();
    }

    public void setRemoteConnection(RemoteConnection remoteConnection) {
        this.remoteConnection = remoteConnection;
    }

    public SparkSubmitModel getSubmitModel() {
        return jobModel.getSubmitModel();
    }

    @Nullable
    @Override
    public ExecutionResult execute(Executor executor, @NotNull ProgramRunner programRunner) throws ExecutionException {
        AppInsightsClient.create(HDInsightBundle.message("SparkProjectCompileSuccess"), null, new HashMap<String, String>() {{
            put("Executor", executor.getId());
            put("ActionUuid", getUuid());
        }});

        try {
            if (executor instanceof SparkBatchJobRunExecutor || executor instanceof SparkBatchJobDebugExecutor) {
                if (getRemoteProcessCtrlLogHandler() == null || getExecutionResult() == null || getConsoleView() == null) {
                    throw new ExecutionException("Spark Batch Job execution result is not ready");
                }

                ExecutionResult result = getExecutionResult();
                ConsoleView ctrlMessageView = getConsoleView();
                getRemoteProcessCtrlLogHandler().getCtrlSubject().subscribe(
                        messageWithType -> {
                            switch (messageWithType.getKey()) {
                                case Info:
                                    ctrlMessageView.print("INFO: " + messageWithType.getValue() + "\n", ConsoleViewContentType.SYSTEM_OUTPUT);
                                    break;
                                case Warning:
                                case Log:
                                    ctrlMessageView.print("LOG: " + messageWithType.getValue() + "\n", ConsoleViewContentType.SYSTEM_OUTPUT);
                                    break;
                                default:
                                    ctrlMessageView.print("ERROR: " + messageWithType.getValue() + "\n", ConsoleViewContentType.ERROR_OUTPUT);
                            }
                        },
                        err -> {
                            StringWriter errWriter = new StringWriter();
                            err.printStackTrace(new PrintWriter(errWriter));

                            String errMessage = Optional.ofNullable(err.getMessage())
                                    .orElse(err.toString()) + "\n stack trace: " + errWriter.getBuffer().toString();

                            createAppInsightEvent(executor, new HashMap<String, String>() {{
                                put("IsSubmitSucceed", "false");
                                put("SubmitFailedReason", HDInsightUtil.normalizeTelemetryMessage(errMessage));
                            }});

                            ctrlMessageView.print("ERROR: " + errMessage, ConsoleViewContentType.ERROR_OUTPUT);
                        },
                        () -> {
                            if (!isExecutor) {
                                createAppInsightEvent(executor, new HashMap<String, String>() {{
                                    put("IsSubmitSucceed", "true");
                                }});
                            }
                        });

                programRunner.onProcessStarted(null, result);

                return result;
            } else if (executor instanceof DefaultRunExecutor || executor instanceof DefaultDebugExecutor) {
                // Spark Local Run/Debug
                ConsoleViewImpl consoleView = new SparkJobLogConsoleView(myProject);
                OSProcessHandler processHandler = new KillableColoredProcessHandler(
                        createCommandlineForLocal(jobModel.getLocalRunConfigurableModel(), executor instanceof DefaultDebugExecutor));

                processHandler.addProcessListener(new ProcessAdapter() {
                    @Override
                    public void processTerminated(ProcessEvent event) {
                        createAppInsightEvent(executor, new HashMap<String, String>() {{
                            put("IsSubmitSucceed", "true");
                            put("ExitCode", Integer.toString(event.getExitCode()));
                        }});
                    }
                });

                consoleView.attachToProcess(processHandler);

                return new DefaultExecutionResult(consoleView, processHandler);
            }
        } catch (ExecutionException ee) {
            createAppInsightEvent(executor, new HashMap<String, String>() {{
                put("IsSubmitSucceed", "false");
                put("SubmitFailedReason", HDInsightUtil.normalizeTelemetryMessage(ee.getMessage()));
            }});

            throw ee;
        }

        return null;
    }

    private GeneralCommandLine createCommandlineForLocal(SparkLocalRunConfigurableModel localRunConfigurableModel, Boolean isDebug) throws ExecutionException {
        JavaParameters params = new JavaParameters();
        JavaParametersUtil.configureConfiguration(params, localRunConfigurableModel);

        Module mainModule = ModuleManager.getInstance(myProject).findModuleByName(myProject.getName());

        if (mainModule != null) {
            params.configureByModule(mainModule, JavaParameters.JDK_AND_CLASSES_AND_TESTS);
        } else {
            JavaParametersUtil.configureProject(myProject, params, JavaParameters.JDK_AND_CLASSES_AND_TESTS, null);
        }

        params.setWorkingDirectory(
                Paths.get(localRunConfigurableModel.getDataRootDirectory(), "__default__", "user", "current").toString());

        // Add jmockit as -javaagent
        String jmockitJarPath = params.getClassPath().getPathList().stream()
                .filter(path -> path.toLowerCase().matches(".*\\Wjmockit-.*\\.jar"))
                .findFirst()
                .orElseThrow(() -> new ExecutionException("Dependence jmockit not found"));
        String javaAgentParam = "-javaagent:" + jmockitJarPath;
        params.getVMParametersList().add(javaAgentParam);

        if (isDebug) {
            // TODO: Add onthrow and onuncaught with Breakpoint UI settings later
            String debugConnection = String.format("-agentlib:jdwp=transport=dt_socket,server=n,address=127.0.0.1:%s,suspend=y", getRemoteConnection().getAddress());

            params.getVMParametersList().add(debugConnection);
        }

        params.getClassPath().add(PathUtil.getJarPathForClass(SparkLocalRunner.class));

        params.getProgramParametersList()
                .addAt(0,
                        Optional.ofNullable(localRunConfigurableModel.getRunClass())
                                .filter(mainClass -> !mainClass.trim().isEmpty())
                                .orElseThrow(() -> new ExecutionException("Spark job's main class isn't set")));

        params.getProgramParametersList()
                .addAt(0,
                        "--master local[" + (localRunConfigurableModel.isIsParallelExecution() ? 2 : 1) + "]");

        if (SystemUtils.IS_OS_WINDOWS) {
            if (!Optional.ofNullable(params.getEnv().get(SparkLocalRunConfigurable.HADOOP_HOME_ENV))
                    .map(hadoopHome -> Paths.get(hadoopHome, "bin", SparkLocalRunConfigurable.WINUTILS_EXE_NAME).toString())
                    .map(File::new)
                    .map(File::exists)
                    .orElse(false)) {
                throw new ExecutionException(
                        "winutils.exe should be in %HADOOP_HOME%\\bin\\ directory for Windows platform, please config it at 'Run/Debug Configuration -> Locally Run -> WINUTILS.exe location'.");
            }
        }

        params.setMainClass(SparkLocalRunner.class.getCanonicalName());
        return params.toCommandLine();
    }

    @Override
    public RemoteConnection getRemoteConnection() {
        if (this.remoteConnection == null) {
            setRemoteConnection(new RemoteConnection(true, "127.0.0.1", "0", true));
        }

        return this.remoteConnection;
    }

    public void createAppInsightEvent(@NotNull Executor executor, @Nullable final Map<String, String> addedEventProps) {
        HashMap<String, String> postEventProps = new HashMap<String, String>() {{
            put("Executor", executor.getId());
            put("ActionUuid", getUuid());
        }};

        // Merge added props, but not overwrite Executor and ActionUuid properties.
        Optional.ofNullable(addedEventProps)
                .ifPresent(propsToAdd -> propsToAdd.forEach((k, v) -> postEventProps.merge(k, v, (vOld, vNew) -> vOld)));

        switch (executor.getId()) {
            case "Run":
                AppInsightsClient.create(HDInsightBundle.message("SparkRunConfigLocalRunButtonClick"), null, postEventProps);
                break;
            case "Debug":
                AppInsightsClient.create(HDInsightBundle.message("SparkRunConfigLocalDebugButtonClick"), null, postEventProps);
                break;
            case SparkBatchJobRunExecutor.EXECUTOR_ID:
                AppInsightsClient.create(HDInsightBundle.message("SparkRunConfigRunButtonClick"), null, postEventProps);
                break;
            case SparkBatchJobDebugExecutor.EXECUTOR_ID:
                AppInsightsClient.create(HDInsightBundle.message("SparkRunConfigDebugButtonClick"), null, postEventProps);
                break;
        }
    }

    public void checkSubmissionParameter() throws ExecutionException {
        SparkSubmissionParameter parameter = getSubmitModel().getSubmissionParameter();

        if (StringUtils.isEmpty(parameter.getClusterName())) {
            throw new ExecutionException("The HDInsight cluster to submit is not selected, please config it at 'Run/Debug configuration -> Remotely Run in Cluster'.");
        }

        if (StringUtils.isEmpty(parameter.getArtifactName())) {
            throw new ExecutionException("The artifact to submit is not selected, please config it at 'Run/Debug configuration -> Remotely Run in Cluster'.");
        }

        if (StringUtils.isBlank(parameter.getMainClassName())) {
            throw new ExecutionException("The main class name is empty, please config it at 'Run/Debug configuration -> Remotely Run in Cluster'.");
        }
    }

    @Nullable
    public SparkBatchJobProcessCtrlLogOut getRemoteProcessCtrlLogHandler() {
        return remoteProcessCtrlLogHandler;
    }

    public void setRemoteProcessCtrlLogHandler(@Nullable SparkBatchJobProcessCtrlLogOut remoteProcessCtrlLogHandler) {
        this.remoteProcessCtrlLogHandler = remoteProcessCtrlLogHandler;
    }

    @Nullable
    public ExecutionResult getExecutionResult() {
        return executionResult;
    }

    public void setExecutionResult(@Nullable ExecutionResult executionResult) {
        this.executionResult = executionResult;
    }

    @Nullable
    public ConsoleView getConsoleView() {
        return consoleView;
    }

    public void setConsoleView(@Nullable ConsoleView consoleView) {
        this.consoleView = consoleView;
    }
}
