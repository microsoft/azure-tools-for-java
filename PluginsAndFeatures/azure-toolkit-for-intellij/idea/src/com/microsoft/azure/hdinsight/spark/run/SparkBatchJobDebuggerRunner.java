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

import com.google.common.collect.ImmutableMap;
import com.google.common.util.concurrent.UncheckedExecutionException;
import com.intellij.debugger.impl.GenericDebuggerRunner;
import com.intellij.debugger.impl.GenericDebuggerRunnerSettings;
import com.intellij.execution.*;
import com.intellij.execution.configurations.*;
import com.intellij.execution.impl.ConsoleViewImpl;
import com.intellij.execution.process.ProcessHandler;
import com.intellij.execution.runners.ExecutionEnvironment;
import com.intellij.execution.runners.ExecutionEnvironmentBuilder;
import com.intellij.execution.runners.ProgramRunner;
import com.intellij.execution.ui.ConsoleViewContentType;
import com.intellij.execution.ui.RunContentDescriptor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Key;
import com.microsoft.azure.hdinsight.common.ClusterManagerEx;
import com.microsoft.azure.hdinsight.common.MessageInfoType;
import com.microsoft.azure.hdinsight.sdk.cluster.IClusterDetail;
import com.microsoft.azure.hdinsight.sdk.cluster.LivyCluster;
import com.microsoft.azure.hdinsight.spark.common.*;
import com.microsoft.azure.hdinsight.spark.run.configuration.LivySparkBatchJobRunConfiguration;
import com.microsoft.azure.hdinsight.spark.ui.SparkJobLogConsoleView;
import com.microsoft.azure.hdinsight.spark.ui.SparkSubmissionAdvancedConfigPanel;
import com.microsoft.azuretools.azurecommons.helpers.NotNull;
import com.microsoft.azuretools.telemetrywrapper.ErrorType;
import com.microsoft.azuretools.telemetrywrapper.EventUtil;
import com.microsoft.azuretools.telemetrywrapper.Operation;
import com.microsoft.intellij.rxjava.IdeaSchedulers;
import com.microsoft.intellij.telemetry.TelemetryKeys;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.jetbrains.concurrency.AsyncPromise;
import org.jetbrains.concurrency.Promise;
import rx.Observable;
import rx.Observer;
import rx.Subscription;
import rx.schedulers.Schedulers;
import rx.subjects.PublishSubject;

import java.io.IOException;
import java.net.URI;
import java.util.AbstractMap.SimpleImmutableEntry;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class SparkBatchJobDebuggerRunner extends GenericDebuggerRunner implements SparkSubmissionRunner {
    public static final Key<String> DebugTargetKey = new Key<>("debug-target");
    private static final Key<String> ProfileNameKey = new Key<>("profile-name");
    public static final String DebugDriver = "driver";
    public static final String DebugExecutor = "executor";

    @Override
    public boolean canRun(@NotNull String executorId, @NotNull RunProfile profile) {
        if (!(profile instanceof LivySparkBatchJobRunConfiguration)) {
            return false;
        }

        boolean isDebugEnabled = Optional.of((LivySparkBatchJobRunConfiguration) profile)
                .map(LivySparkBatchJobRunConfiguration::getSubmitModel)
                .map(SparkSubmitModel::getAdvancedConfigModel)
                .map(advModel -> advModel.enableRemoteDebug && advModel.isValid())
                .orElse(false);

        // Only support debug now, will enable run in future
        return SparkBatchJobDebugExecutor.EXECUTOR_ID.equals(executorId) &&
                isDebugEnabled;
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

    private String getSparkJobUrl(@NotNull SparkSubmitModel submitModel) throws ExecutionException, IOException {
        String clusterName = submitModel.getSubmissionParameter().getClusterName();

        IClusterDetail clusterDetail = ClusterManagerEx.getInstance()
                .getClusterDetailByName(clusterName)
                .orElseThrow(() -> new ExecutionException("No cluster name matched selection: " + clusterName));

        String sparkJobUrl = clusterDetail instanceof LivyCluster ? ((LivyCluster) clusterDetail).getLivyBatchUrl() : null;
        if (sparkJobUrl == null) {
            throw new IOException("Can't get livy connection URL. Cluster: " + clusterName);
        }
        return sparkJobUrl;
    }
    /**
     * Running in Event dispatch thread
     */
    @Override
    protected RunContentDescriptor doExecute(RunProfileState state, ExecutionEnvironment environment) throws ExecutionException {
        super.doExecute(state, environment);

        final Operation operation = environment.getUserData(TelemetryKeys.OPERATION);
        final AsyncPromise<ExecutionEnvironment> jobDriverEnvReady = new AsyncPromise<> ();
        final SparkBatchRemoteDebugState submissionState = (SparkBatchRemoteDebugState) state;

        final SparkSubmitModel submitModel = submissionState.getSubmitModel();
        // Create SSH debug session firstly
        SparkBatchDebugSession session;
        try {
            session = SparkBatchDebugSession.factoryByAuth(getSparkJobUrl(submitModel), submitModel.getAdvancedConfigModel())
                    .open()
                    .verifyCertificate();
        } catch (Exception e) {
            ExecutionException exp = new ExecutionException("Failed to create SSH session for debugging. " +
                    ExceptionUtils.getRootCauseMessage(e));
            EventUtil.logErrorWithComplete(operation, ErrorType.systemError, exp, null, null);
            throw exp;
        }

        final Project project = submitModel.getProject();
        final IdeaSchedulers schedulers = new IdeaSchedulers(project);
        final PublishSubject<SimpleImmutableEntry<MessageInfoType, String>> ctrlSubject = PublishSubject.create();
        final PublishSubject<SparkBatchJobSubmissionEvent> debugEventSubject = PublishSubject.create();
        final SparkBatchJobRemoteDebugProcess driverDebugProcess = new SparkBatchJobRemoteDebugProcess(
                schedulers,
                session,
                (ISparkBatchDebugJob) buildSparkBatchJob(submitModel, ctrlSubject),
                submitModel.getArtifactPath().orElseThrow(() -> new ExecutionException("No artifact selected")),
                submitModel.getSubmissionParameter().getMainClassName(),
                submitModel.getAdvancedConfigModel(),
                ctrlSubject);

        final SparkBatchJobDebugProcessHandler driverDebugHandler =
                new SparkBatchJobDebugProcessHandler(project, driverDebugProcess, debugEventSubject);

        // Prepare an independent submission console
        final ConsoleViewImpl submissionConsole = new ConsoleViewImpl(project, true);
        final RunContentDescriptor submissionDesc = new RunContentDescriptor(
                submissionConsole,
                driverDebugHandler,
                submissionConsole.getComponent(),
                String.format("Submit %s to cluster %s",
                              submitModel.getSubmissionParameter().getMainClassName(),
                              submitModel.getSubmissionParameter().getClusterName()));

        // Show the submission console view
        ExecutionManager.getInstance(project).getContentManager().showRunContent(environment.getExecutor(), submissionDesc);

        // Use the submission console to display the deployment ctrl message
        final Subscription jobSubscription = ctrlSubject.subscribe(typedMessage -> {
                    String line = typedMessage.getValue() + "\n";

                    switch (typedMessage.getKey()) {
                        case Error:
                            submissionConsole.print(line, ConsoleViewContentType.ERROR_OUTPUT);
                            break;
                        case Info:
                            submissionConsole.print(line, ConsoleViewContentType.NORMAL_OUTPUT);
                            break;
                        case Log:
                            submissionConsole.print(line, ConsoleViewContentType.SYSTEM_OUTPUT);
                            break;
                        case Warning:
                            submissionConsole.print(line, ConsoleViewContentType.LOG_WARNING_OUTPUT);
                            break;
                    }
                },
                err -> {
                    submissionConsole.print(ExceptionUtils.getRootCauseMessage(err), ConsoleViewContentType.ERROR_OUTPUT);
                    String errMsg = "The Spark job remote debug is cancelled due to " + ExceptionUtils.getRootCauseMessage(err);
                    jobDriverEnvReady.setError(errMsg);
                    EventUtil.logErrorWithComplete(operation, ErrorType.systemError, new UncheckedExecutionException(errMsg, err), null, null);
                },
                () -> {
                    if (Optional.ofNullable(driverDebugHandler.getUserData(ProcessHandler.TERMINATION_REQUESTED))
                                .orElse(false)) {
                        String errMsg = "The Spark job remote debug is cancelled by user.";
                        jobDriverEnvReady.setError(errMsg);
                        Map<String, String> props = ImmutableMap.of("isDebugCancelled", "true");
                        EventUtil.logErrorWithComplete(operation, ErrorType.userError, new ExecutionException(errMsg), props, null);
                    }
                });

        debugEventSubject
                .subscribeOn(Schedulers.io())
                .doAfterTerminate(() -> {
                    // Call after completed or error
                    session.close();
                })
                .subscribe(debugEvent -> {
                    try {
                        if (debugEvent instanceof SparkBatchRemoteDebugHandlerReadyEvent) {
                            SparkBatchRemoteDebugHandlerReadyEvent handlerReadyEvent =
                                    (SparkBatchRemoteDebugHandlerReadyEvent) debugEvent;
                            SparkBatchDebugJobJdbPortForwardedEvent jdbReadyEvent =
                                    handlerReadyEvent.getJdbPortForwardedEvent();

                            if (!jdbReadyEvent.getLocalJdbForwardedPort().isPresent()) {
                                return;
                            }

                            int localPort = jdbReadyEvent.getLocalJdbForwardedPort().get();

                            ExecutionEnvironment forkEnv = forkEnvironment(
                                    environment,
                                    jdbReadyEvent.getRemoteHost().orElse("unknown"),
                                    jdbReadyEvent.isDriver());

                            SparkBatchRemoteDebugState forkState = jdbReadyEvent.isDriver() ?
                                    submissionState :
                                    (SparkBatchRemoteDebugState) forkEnv.getState();

                            if (forkState == null) {
                                return;
                            }

                            // Set the debug connection to localhost and local forwarded port to the state
                            forkState.setRemoteConnection(
                                    new RemoteConnection(true, "localhost", Integer.toString(localPort), false));

                            // Prepare the debug tab console view UI
                            SparkJobLogConsoleView jobOutputView = new SparkJobLogConsoleView(project);
                            // Get YARN container log URL port
                            int containerLogUrlPort =
                                    ((SparkBatchRemoteDebugJob) driverDebugProcess.getSparkJob())
                                            .getYarnContainerLogUrlPort()
                                            .toBlocking()
                                            .single();
                            // Parse container ID and host URL from driver console view
                            jobOutputView.getSecondaryConsoleView().addMessageFilter((line, entireLength) -> {
                                Matcher matcher = Pattern.compile(
                                        "Launching container (\\w+).* on host ([a-zA-Z_0-9-.]+)",
                                        Pattern.CASE_INSENSITIVE)
                                        .matcher(line);
                                while (matcher.find()) {
                                    String containerId = matcher.group(1);
                                    // TODO: get port from somewhere else rather than hard code here
                                    URI hostUri = URI.create(String.format("http://%s:%d", matcher.group(2), containerLogUrlPort));
                                    debugEventSubject.onNext(new SparkBatchJobExecutorCreatedEvent(hostUri, containerId));
                                }
                                return null;
                            });
                            jobOutputView.attachToProcess(handlerReadyEvent.getDebugProcessHandler());

                            ExecutionResult result = new DefaultExecutionResult(
                                    jobOutputView, handlerReadyEvent.getDebugProcessHandler());
                            forkState.setExecutionResult(result);
                            forkState.setConsoleView(jobOutputView.getSecondaryConsoleView());
                            forkState.setRemoteProcessCtrlLogHandler(handlerReadyEvent.getDebugProcessHandler());

                            if (jdbReadyEvent.isDriver()) {
                                // Let the debug console view to handle the control log
                                jobSubscription.unsubscribe();

                                // Resolve job driver promise, handle the driver VM attaching separately
                                jobDriverEnvReady.setResult(forkEnv);
                            } else {
                                // Call supper class method to attach Java virtual machine
                                super.execute(forkEnv);
                            }
                        } else if (debugEvent instanceof SparkBatchJobExecutorCreatedEvent) {
                            SparkBatchJobExecutorCreatedEvent executorCreatedEvent =
                                    (SparkBatchJobExecutorCreatedEvent) debugEvent;

                            final String containerId = executorCreatedEvent.getContainerId();
                            final SparkBatchRemoteDebugJob debugJob =
                                    (SparkBatchRemoteDebugJob) driverDebugProcess.getSparkJob();

                            URI internalHostUri = executorCreatedEvent.getHostUri();
                            URI executorLogUrl = debugJob.convertToPublicLogUri(internalHostUri)
                                    .map(uri -> uri.resolve(String.format("node/containerlogs/%s/livy", containerId)))
                                    .toBlocking().singleOrDefault(internalHostUri);

                            // Create an Executor Debug Process
                            SparkBatchJobRemoteDebugExecutorProcess executorDebugProcess =
                                    new SparkBatchJobRemoteDebugExecutorProcess(
                                            schedulers,
                                            debugJob,
                                            internalHostUri.getHost(),
                                            driverDebugProcess.getDebugSession(),
                                            executorLogUrl.toString());

                            SparkBatchJobDebugProcessHandler executorDebugHandler =
                                    new SparkBatchJobDebugProcessHandler(project, executorDebugProcess, debugEventSubject);

                            executorDebugHandler.getRemoteDebugProcess().start();
                        }
                    } catch (ExecutionException e) {
                        EventUtil.logErrorWithComplete(operation, ErrorType.systemError, new UncheckedExecutionException(e), null, null);
                        throw new UncheckedExecutionException(e);
                    }
                });

        // Driver side execute, leverage Intellij Async Promise, to wait for the Spark app deployed
        ExecutionManager.getInstance(project).startRunProfile(new RunProfileStarter() {
            @Override
            public Promise<RunContentDescriptor> executeAsync(@NotNull ExecutionEnvironment env) throws ExecutionException {
                driverDebugHandler.getRemoteDebugProcess().start();

                RunProfileState state = environment.getState();

                return jobDriverEnvReady
                        .then(forkEnv -> Observable.fromCallable(() -> doExecute(state, forkEnv))
                                .subscribeOn(schedulers.dispatchUIThread()).toBlocking().singleOrDefault(null))
                        .then(descriptor -> {
                            // Borrow BaseProgramRunner.postProcess() codes since it's only package public accessible.
                            if (descriptor != null) {
                                descriptor.setExecutionId(env.getExecutionId());
                                descriptor.setContentToolWindowId(ExecutionManager.getInstance(env.getProject()).getContentManager()
                                        .getContentDescriptorToolWindowId(env.getRunnerAndConfigurationSettings()));
                                RunnerAndConfigurationSettings settings = env.getRunnerAndConfigurationSettings();

                                if (settings != null) {
                                    descriptor.setActivateToolWindowWhenAdded(settings.isActivateToolWindowBeforeRun());
                                }
                            }

                            return descriptor;
                        });
            }
        }, submissionState, environment);

        return submissionDesc;
    }

    /*
     * Build a child environment with specified host and type
     */
    private ExecutionEnvironment forkEnvironment(@NotNull ExecutionEnvironment parentEnv, String host, boolean isDriver) {
        String savedProfileName = parentEnv.getUserData(ProfileNameKey);
        String originProfileName = savedProfileName == null ? parentEnv.getRunProfile().getName() : savedProfileName;

        RunConfiguration newRunConfiguration = ((RunConfiguration) parentEnv.getRunProfile()).clone();
        newRunConfiguration.setName(originProfileName + " [" + (isDriver ? "Driver " : "Executor ") + host + "]");

        ExecutionEnvironment childEnv = new ExecutionEnvironmentBuilder(parentEnv).runProfile(newRunConfiguration)
                .build();

        childEnv.putUserData(DebugTargetKey, isDriver ? DebugDriver : DebugExecutor);
        childEnv.putUserData(ProfileNameKey, originProfileName);

        return childEnv;
    }

    @NotNull
    @Override
    public ISparkBatchJob buildSparkBatchJob(@NotNull SparkSubmitModel submitModel, @NotNull Observer<SimpleImmutableEntry<MessageInfoType, String>> ctrlSubject) throws ExecutionException {
        try {
            SparkSubmissionAdvancedConfigPanel.Companion.checkSettings(submitModel.getAdvancedConfigModel());
            SparkSubmissionParameter debugSubmissionParameter = SparkBatchRemoteDebugJob.convertToDebugParameter(submitModel.getSubmissionParameter());
            SparkSubmitModel debugModel = new SparkSubmitModel(submitModel.getProject(), debugSubmissionParameter,
                    submitModel.getAdvancedConfigModel(), submitModel.getJobUploadStorageModel());

            String clusterName = submitModel.getSubmissionParameter().getClusterName();
            IClusterDetail clusterDetail = ClusterManagerEx.getInstance().getClusterDetailByName(clusterName)
                    .orElseThrow(() -> new ExecutionException("Can't find cluster named " + clusterName));

            Deployable jobDeploy = SparkBatchJobDeployFactory.getInstance().buildSparkBatchJobDeploy(debugModel, ctrlSubject);
            return new SparkBatchRemoteDebugJob(clusterDetail, debugModel.getSubmissionParameter(), SparkBatchSubmission.getInstance(), ctrlSubject, jobDeploy);
        } catch (DebugParameterDefinedException e) {
            throw new ExecutionException(e);
        }
    }

    @Override
    public void setFocus(@NotNull RunConfiguration runConfiguration) {
        if (runConfiguration instanceof LivySparkBatchJobRunConfiguration) {
            LivySparkBatchJobRunConfiguration livyRunConfig = (LivySparkBatchJobRunConfiguration) runConfiguration;
            livyRunConfig.getModel().setFocusedTabIndex(1);
            livyRunConfig.getModel().getSubmitModel().getAdvancedConfigModel().setUIExpanded(true);
        }
    }
}
