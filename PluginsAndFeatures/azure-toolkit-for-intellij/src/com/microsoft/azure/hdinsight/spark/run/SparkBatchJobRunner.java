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

import com.google.common.collect.ImmutableList;
import com.intellij.execution.DefaultExecutionResult;
import com.intellij.execution.ExecutionException;
import com.intellij.execution.ExecutionResult;
import com.intellij.execution.configurations.RunConfiguration;
import com.intellij.execution.configurations.RunProfile;
import com.intellij.execution.configurations.RunProfileState;
import com.intellij.execution.runners.DefaultProgramRunner;
import com.intellij.execution.runners.ExecutionEnvironment;
import com.intellij.execution.ui.ConsoleView;
import com.intellij.execution.ui.RunContentDescriptor;
import com.intellij.openapi.actionSystem.ActionManager;
import com.intellij.openapi.actionSystem.Separator;
import com.intellij.openapi.project.Project;
import com.microsoft.azure.hdinsight.common.AbfsUri;
import com.microsoft.azure.hdinsight.common.ClusterManagerEx;
import com.microsoft.azure.hdinsight.common.WasbUri;
import com.microsoft.azure.hdinsight.common.logger.ILogger;
import com.microsoft.azure.hdinsight.sdk.cluster.IClusterDetail;
import com.microsoft.azure.hdinsight.spark.common.*;
import com.microsoft.azure.hdinsight.spark.common.log.SparkLogLine;
import com.microsoft.azure.hdinsight.spark.run.action.SparkBatchJobDisconnectAction;
import com.microsoft.azure.hdinsight.spark.run.configuration.LivySparkBatchJobRunConfiguration;
import com.microsoft.azure.hdinsight.spark.ui.SparkJobLogConsoleView;
import com.microsoft.azuretools.azurecommons.helpers.NotNull;
import com.microsoft.azuretools.azurecommons.helpers.Nullable;
import com.microsoft.azuretools.telemetrywrapper.EventType;
import com.microsoft.azuretools.telemetrywrapper.EventUtil;
import com.microsoft.azuretools.telemetrywrapper.Operation;
import com.microsoft.intellij.rxjava.IdeaSchedulers;
import com.microsoft.intellij.telemetry.TelemetryKeys;
import org.apache.commons.lang3.exception.ExceptionUtils;
import rx.Observable;
import rx.subjects.PublishSubject;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UnknownFormatConversionException;
import java.util.stream.Collectors;

import static com.microsoft.azure.hdinsight.spark.common.SparkBatchSubmission.getClusterSubmission;
import static com.microsoft.intellij.rxjava.IdeaSchedulers.updateCurrentBackgroundableTaskIndicator;

public class SparkBatchJobRunner extends DefaultProgramRunner implements SparkSubmissionRunner, ILogger {
    public static final String RUNNER_ID = "SparkJobRun";

    @NotNull
    @Override
    public String getRunnerId() {
        return RUNNER_ID;
    }

    @Override
    public boolean canRun(@NotNull String executorId, @NotNull RunProfile profile) {
        return SparkBatchJobRunExecutor.EXECUTOR_ID.equals(executorId)
                && profile.getClass() == LivySparkBatchJobRunConfiguration.class;
    }

    protected String transformToGen2Uri(String url) {
        return AbfsUri.isType(url)
               ? AbfsUri.parse(url).getUri().toString()
               : url;
    }

    protected SparkSubmissionParameter updateStorageConfigForSubmissionParameter(SparkSubmitModel submitModel) throws ExecutionException {
        // If we use virtual file system to select referenced jars or files on ADLS Gen2 storage, the selected file path will
        // be of URI schema which starts with "https://". Then job submission will fail with error like
        // "Server returned HTTP response code: 401 for URL: https://accountName.dfs.core.windows.net/fs0/Reference.jar"
        // Therefore, we need to transform the Gen2 "https" URI to "abfs" url to avoid the error.
        final SparkSubmissionParameter submissionParameter = submitModel.getSubmissionParameter();
        submissionParameter.setReferencedJars(submissionParameter.getReferencedJars().stream()
                                                   .map(this::transformToGen2Uri)
                                                   .collect(Collectors.toList()));
        submissionParameter.setReferencedFiles(submissionParameter.getReferencedFiles().stream()
                                                    .map(this::transformToGen2Uri)
                                                    .collect(Collectors.toList()));

        // If job upload storage type is Azure Blob storage, we need to put blob storage credential into livy configuration
        if (submitModel.getJobUploadStorageModel().getStorageAccountType() == SparkSubmitStorageType.BLOB) {
            try {
                final WasbUri fsRoot = WasbUri.parse(submitModel.getJobUploadStorageModel().getUploadPath());
                final String storageKey = submitModel.getJobUploadStorageModel().getStorageKey();
                final Object existingConfigEntry = submissionParameter.getJobConfig().get(SparkSubmissionParameter.Conf);
                final SparkConfigures wrappedConfig = existingConfigEntry instanceof Map
                                                      ? new SparkConfigures(existingConfigEntry)
                                                      : new SparkConfigures();
                wrappedConfig.put("spark.hadoop." + fsRoot.getHadoopBlobFsPropertyKey(), storageKey);
                submissionParameter.getJobConfig().put(SparkSubmissionParameter.Conf, wrappedConfig);
            } catch (final UnknownFormatConversionException error) {
                final String errorHint = "Azure blob storage uploading path is not in correct format";
                log().warn(String.format("%s. Uploading Path: %s. Error message: %s. Stacktrace:\n%s",
                                         errorHint, submitModel.getJobUploadStorageModel().getUploadPath(), error.getMessage(),
                                         ExceptionUtils.getStackTrace(error)));
                throw new ExecutionException(errorHint);
            } catch (final Exception error) {
                final String errorHint = "Failed to update config for linked Azure Blob storage";
                log().warn(String.format("%s. Error message: %s. Stacktrace:\n%s",
                                         errorHint, error.getMessage(), ExceptionUtils.getStackTrace(error)));
                throw new ExecutionException(errorHint);
            }
        }

        return submissionParameter;
    }

    @Override
    @NotNull
    public Observable<ISparkBatchJob> buildSparkBatchJob(@NotNull SparkSubmitModel submitModel) {
        return Observable.fromCallable(() -> {
            final String clusterName = submitModel.getSubmissionParameter().getClusterName();

            updateCurrentBackgroundableTaskIndicator(progressIndicator -> {
                progressIndicator.setFraction(0.2f);
                progressIndicator.setText("Get Spark cluster [" + clusterName + "] information from subscriptions");
            });

            final IClusterDetail clusterDetail = ClusterManagerEx.getInstance().getClusterDetailByName(clusterName)
                                                                 .orElseThrow(() -> new ExecutionException(
                                                                         "Can't find cluster named " + clusterName));

            updateCurrentBackgroundableTaskIndicator(progressIndicator -> {
                progressIndicator.setFraction(0.7f);
                progressIndicator.setText("Get the storage configuration for artifacts deployment");
            });

            final Deployable jobDeploy = SparkBatchJobDeployFactory.getInstance().buildSparkBatchJobDeploy(
                    submitModel, clusterDetail);

            final SparkSubmissionParameter submissionParameter = updateStorageConfigForSubmissionParameter(submitModel);

            updateCurrentBackgroundableTaskIndicator(progressIndicator -> {
                progressIndicator.setFraction(1.0f);
                progressIndicator.setText("All checks are passed.");
            });

            return new SparkBatchJob(clusterDetail,
                                     submissionParameter,
                                     getClusterSubmission(clusterDetail),
                                     jobDeploy);
        });
    }

    protected void addConsoleViewFilter(@NotNull ISparkBatchJob job, @NotNull ConsoleView consoleView) {
    }

    protected void sendTelemetryForParameters(@NotNull SparkSubmitModel model, @Nullable Operation operation) {
        try {
            final SparkSubmissionParameter params = model.getSubmissionParameter();
            final Map<String, String> props = new HashMap<>();

            if (params.getDriverCores() != null) {
                props.put(SparkSubmissionParameter.DriverCores, params.getDriverCores().toString());
            }

            if (params.getDriverMemory() != null) {
                props.put(SparkSubmissionParameter.DriverMemory, params.getDriverMemory());
            }

            if (params.getExecutorCores() != null) {
                props.put(SparkSubmissionParameter.ExecutorCores, params.getExecutorCores().toString());
            }

            if (params.getExecutorMemory() != null) {
                props.put(SparkSubmissionParameter.ExecutorMemory, params.getExecutorMemory());
            }

            if (params.getNumExecutors() != null) {
                props.put(SparkSubmissionParameter.NumExecutors, params.getNumExecutors().toString());
            }

            props.put("refJarsCount",
                      String.valueOf(Optional.ofNullable(params.getReferencedJars())
                                             .orElse(ImmutableList.of())
                                             .size()));
            props.put("refFilesCount",
                      String.valueOf(Optional.ofNullable(params.getReferencedFiles())
                                             .orElse(ImmutableList.of())
                                             .size()));
            props.put("commandlineArgsCount",
                      String.valueOf(Optional.ofNullable(params.getArgs()).orElse(ImmutableList.of()).size()));
            props.put("isLocalArtifact", String.valueOf(model.getIsLocalArtifact()));
            props.put("isDefaultArtifact",
                      model.getIsLocalArtifact()
                      ? "none"
                      : String.valueOf(Optional.ofNullable(model.getArtifactName())
                                               .orElse("")
                                               .toLowerCase()
                                               .endsWith("defaultartifact")));
            EventUtil.logEvent(EventType.info, operation, props);
        } catch (final Exception exp) {
            log().warn("Error sending telemetry when submit spark jobs. " + ExceptionUtils.getStackTrace(exp));
        }
    }

    @Nullable
    @Override
    protected RunContentDescriptor doExecute(@NotNull RunProfileState state, @NotNull ExecutionEnvironment environment)
            throws ExecutionException {
        final SparkBatchRemoteRunProfileState submissionState = (SparkBatchRemoteRunProfileState) state;

        final SparkSubmitModel submitModel = submissionState.getSubmitModel();
        final Project project = submitModel.getProject();

        // Prepare the run table console view UI
        final SparkJobLogConsoleView jobOutputView = new SparkJobLogConsoleView(project);

        final String artifactPath = submitModel.getArtifactPath().orElse(null);
        assert artifactPath != null
                : "artifactPath should be checked in LivySparkBatchJobRunConfiguration::checkSubmissionConfigurationBeforeRun";

        // To address issue https://github.com/microsoft/azure-tools-for-java/issues/4021.
        // In this issue, when user click rerun button, we are still using the legacy ctrlSubject which has already sent
        // "onComplete" message when the job is done in the previous time. To avoid this issue,  We clone a new Spark
        // batch job instance to re-initialize everything in the object.
        final ISparkBatchJob sparkBatch = submissionState.getSparkBatch().clone();
        final PublishSubject<SparkLogLine> ctrlSubject =
                (PublishSubject<SparkLogLine>) sparkBatch.getCtrlSubject();
        final SparkBatchJobRemoteProcess remoteProcess = new SparkBatchJobRemoteProcess(
                new IdeaSchedulers(project),
                sparkBatch,
                artifactPath,
                submitModel.getSubmissionParameter().getMainClassName(),
                ctrlSubject);
        final SparkBatchJobRunProcessHandler processHandler = new SparkBatchJobRunProcessHandler(remoteProcess,
                                                                                                 "Package and deploy the job to Spark cluster",
                                                                                                 null);

        // After attaching, the console view can read the process inputStreams and display them
        jobOutputView.attachToProcess(processHandler);

        remoteProcess.start();
        final Operation operation = environment.getUserData(TelemetryKeys.OPERATION);
        // After we define a new AnAction class, IntelliJ will construct a new AnAction instance for us.
        // Use one action instance can keep behaviours like isEnabled() consistent
        final SparkBatchJobDisconnectAction disconnectAction =
                (SparkBatchJobDisconnectAction) ActionManager.getInstance().getAction("Actions.SparkJobDisconnect");
        disconnectAction.init(remoteProcess, operation);

        sendTelemetryForParameters(submitModel, operation);

        final ExecutionResult result = new DefaultExecutionResult(jobOutputView,
                                                                  processHandler,
                                                                  Separator.getInstance(),
                                                                  disconnectAction);
        submissionState.setExecutionResult(result);
        final ConsoleView consoleView = jobOutputView.getSecondaryConsoleView();
        submissionState.setConsoleView(consoleView);

        addConsoleViewFilter(remoteProcess.getSparkJob(), consoleView);

        submissionState.setRemoteProcessCtrlLogHandler(processHandler);

        ctrlSubject.subscribe(
                messageWithType -> {
                },
                err -> disconnectAction.setEnabled(false),
                () -> disconnectAction.setEnabled(false));

        return super.doExecute(state, environment);
    }

    @Override
    public void setFocus(@NotNull RunConfiguration runConfiguration) {
        if (runConfiguration instanceof LivySparkBatchJobRunConfiguration) {
            final LivySparkBatchJobRunConfiguration livyRunConfig =
                    (LivySparkBatchJobRunConfiguration) runConfiguration;
            livyRunConfig.getModel().setFocusedTabIndex(1);
        }
    }
}
