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

import com.google.common.collect.ImmutableMap;
import com.google.common.util.concurrent.UncheckedExecutionException;
import com.intellij.debugger.impl.GenericDebuggerRunner;
import com.intellij.debugger.impl.GenericDebuggerRunnerSettings;
import com.intellij.execution.DefaultExecutionResult;
import com.intellij.execution.ExecutionException;
import com.intellij.execution.ExecutionManager;
import com.intellij.execution.ExecutionResult;
import com.intellij.execution.configurations.*;
import com.intellij.execution.impl.ConsoleViewImpl;
import com.intellij.execution.process.ProcessHandler;
import com.intellij.execution.runners.ExecutionEnvironment;
import com.intellij.execution.runners.ExecutionEnvironmentBuilder;
import com.intellij.execution.ui.ConsoleViewContentType;
import com.intellij.execution.ui.RunContentDescriptor;
import com.intellij.openapi.application.ModalityState;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Key;
import com.intellij.openapi.wm.WindowManager;
import com.microsoft.azure.hdinsight.common.ClusterManagerEx;
import com.microsoft.azure.hdinsight.sdk.cluster.IClusterDetail;
import com.microsoft.azure.hdinsight.sdk.cluster.LivyCluster;
import com.microsoft.azure.hdinsight.spark.common.*;
import com.microsoft.azure.hdinsight.spark.common.log.SparkLogLine;
import com.microsoft.azure.hdinsight.spark.run.configuration.LivySparkBatchJobRunConfiguration;
import com.microsoft.azure.hdinsight.spark.ui.SparkJobLogConsoleView;
import com.microsoft.azure.hdinsight.spark.ui.SparkSubmissionAdvancedConfigPanel;
import com.microsoft.azuretools.azurecommons.helpers.NotNull;
import com.microsoft.azuretools.azurecommons.helpers.Nullable;
import com.microsoft.azuretools.telemetrywrapper.ErrorType;
import com.microsoft.azuretools.telemetrywrapper.EventUtil;
import com.microsoft.azuretools.telemetrywrapper.Operation;
import com.microsoft.intellij.rxjava.IdeaSchedulers;
import com.microsoft.intellij.telemetry.TelemetryKeys;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.jetbrains.concurrency.AsyncPromise;
import rx.Observable;
import rx.Subscription;
import rx.schedulers.Schedulers;
import rx.subjects.PublishSubject;

import javax.swing.*;
import java.io.IOException;
import java.net.URI;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.intellij.openapi.application.ModalityState.any;
import static com.intellij.openapi.application.ModalityState.stateForComponent;
import static com.microsoft.azure.hdinsight.spark.common.SparkBatchSubmission.getClusterSubmission;
import static com.microsoft.intellij.rxjava.RxJavaExtKt.toIdeaPromise;

public class SparkBatchJobDebuggerRunner extends GenericDebuggerRunner implements SparkSubmissionRunner {
    public static final Key<String> DEBUG_TARGET_KEY = new Key<>("debug-target");
    public static final String RUNNER_ID = "SparkBatchJobDebug";
    public static final String DEBUG_DRIVER = "driver";
    public static final String DEBUG_EXECUTOR = "executor";

    private static final Key<String> ProfileNameKey = new Key<>("profile-name");

    @Override
    public boolean canRun(@NotNull final String executorId, @NotNull final RunProfile profile) {
        if (!(profile instanceof LivySparkBatchJobRunConfiguration)) {
            return false;
        }

        final boolean isDebugEnabled = Optional.of((LivySparkBatchJobRunConfiguration) profile)
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
        return RUNNER_ID;
    }

    @Nullable
    @Override
    public GenericDebuggerRunnerSettings createConfigurationData(final ConfigurationInfoProvider settingsProvider) {
        return null;
    }

    private String getSparkJobUrl(@NotNull final SparkSubmitModel submitModel) throws ExecutionException, IOException {
        final String clusterName = submitModel.getSubmissionParameter().getClusterName();

        final IClusterDetail clusterDetail = ClusterManagerEx
                .getInstance()
                .getClusterDetailByName(clusterName)
                .orElseThrow(() -> new ExecutionException("No cluster name matched selection: " + clusterName));

        final String sparkJobUrl =
                clusterDetail instanceof LivyCluster ? ((LivyCluster) clusterDetail).getLivyBatchUrl() : null;
        if (sparkJobUrl == null) {
            throw new IOException("Can't get livy connection URL. Cluster: " + clusterName);
        }
        return sparkJobUrl;
    }

    /**
     * Execute Spark remote debugging action, refer to {@link GenericDebuggerRunner#execute(ExecutionEnvironment)}
     * implementations, some internal API leveraged.
     *
     * @param environment the execution environment
     * @throws ExecutionException the exception in execution
     */
    @Override
    public void execute(final ExecutionEnvironment environment) throws ExecutionException {
        final RunProfileState state = environment.getState();
        if (state == null) {
            return;
        }

        final Operation operation = environment.getUserData(TelemetryKeys.OPERATION);
        final AsyncPromise<ExecutionEnvironment> jobDriverEnvReady = new AsyncPromise<>();
        final SparkBatchRemoteDebugState submissionState = (SparkBatchRemoteDebugState) state;

        final SparkSubmitModel submitModel = submissionState.getSubmitModel();

        // Create SSH debug session firstly
        final SparkBatchDebugSession session;
        try {
            session = SparkBatchDebugSession
                    .factoryByAuth(getSparkJobUrl(submitModel), submitModel.getAdvancedConfigModel())
                    .open()
                    .verifyCertificate();
        } catch (final Exception e) {
            final ExecutionException exp = new ExecutionException("Failed to create SSH session for debugging. "
                                                                          + ExceptionUtils.getRootCauseMessage(e));
            EventUtil.logErrorClassNameOnlyWithComplete(operation, ErrorType.systemError, exp, null, null);
            throw exp;
        }

        final Project project = submitModel.getProject();
        final ExecutionManager executionManager = ExecutionManager.getInstance(project);
        final IdeaSchedulers schedulers = new IdeaSchedulers(project);
        final PublishSubject<SparkBatchJobSubmissionEvent> debugEventSubject = PublishSubject.create();
        final ISparkBatchDebugJob sparkDebugBatch = (ISparkBatchDebugJob) submissionState.getSparkBatch().clone();
        final PublishSubject<SparkLogLine> ctrlSubject =
                (PublishSubject<SparkLogLine>) sparkDebugBatch.getCtrlSubject();
        final SparkBatchJobRemoteDebugProcess driverDebugProcess = new SparkBatchJobRemoteDebugProcess(
                schedulers,
                session,
                sparkDebugBatch,
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
        ExecutionManager.getInstance(project).getContentManager().showRunContent(environment.getExecutor(),
                                                                                 submissionDesc);

        // Use the submission console to display the deployment ctrl message
        final Subscription jobSubscription = ctrlSubject.subscribe(typedMessage -> {
            final String line = typedMessage.getRawLog() + "\n";

            switch (typedMessage.getMessageInfoType()) {
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
        }, err -> {
            submissionConsole.print(ExceptionUtils.getRootCauseMessage(err), ConsoleViewContentType.ERROR_OUTPUT);
            final String errMsg = "The Spark job remote debug is cancelled due to "
                    + ExceptionUtils.getRootCauseMessage(err);
            jobDriverEnvReady.setError(errMsg);
            EventUtil.logErrorClassNameOnlyWithComplete(operation,
                                           ErrorType.systemError,
                                           new UncheckedExecutionException(errMsg, err),
                                           null,
                                           null);
        }, () -> {
            if (Optional.ofNullable(driverDebugHandler.getUserData(ProcessHandler.TERMINATION_REQUESTED))
                        .orElse(false)) {
                final String errMsg = "The Spark job remote debug is cancelled by user.";
                jobDriverEnvReady.setError(errMsg);

                final Map<String, String> props = ImmutableMap.of("isDebugCancelled", "true");
                EventUtil.logErrorClassNameOnlyWithComplete(
                        operation, ErrorType.userError, new ExecutionException(errMsg), props, null);
            }
        });

        // Call after completed or error
        debugEventSubject.subscribeOn(Schedulers.io()).doAfterTerminate(session::close).subscribe(debugEvent -> {
            try {
                if (debugEvent instanceof SparkBatchRemoteDebugHandlerReadyEvent) {
                    final SparkBatchRemoteDebugHandlerReadyEvent handlerReadyEvent =
                            (SparkBatchRemoteDebugHandlerReadyEvent) debugEvent;
                    final SparkBatchDebugJobJdbPortForwardedEvent jdbReadyEvent =
                            handlerReadyEvent.getJdbPortForwardedEvent();

                    if (!jdbReadyEvent.getLocalJdbForwardedPort().isPresent()) {
                        return;
                    }

                    final int localPort = jdbReadyEvent.getLocalJdbForwardedPort().get();

                    final ExecutionEnvironment forkEnv = forkEnvironment(
                            environment, jdbReadyEvent.getRemoteHost().orElse("unknown"), jdbReadyEvent.isDriver());

                    final RunProfile runProfile = forkEnv.getRunProfile();
                    if (!(runProfile instanceof LivySparkBatchJobRunConfiguration)) {
                        ctrlSubject.onError(new UnsupportedOperationException(
                                "Only supports LivySparkBatchJobRunConfiguration type, but got type"
                                        + runProfile.getClass().getCanonicalName()));

                        return;
                    }

                    // Reuse the driver's Spark batch job
                    ((LivySparkBatchJobRunConfiguration) runProfile).setSparkRemoteBatch(sparkDebugBatch);

                    final SparkBatchRemoteDebugState forkState = jdbReadyEvent.isDriver()
                                                                 ? submissionState
                                                                 : (SparkBatchRemoteDebugState) forkEnv.getState();

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
                            URI hostUri = URI.create(String.format("http://%s:%d",
                                                                   matcher.group(2),
                                                                   containerLogUrlPort));
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
                        // Start Executor debugging
                        executionManager.startRunProfile(forkEnv, () ->
                                toIdeaPromise(attachAndDebug(forkEnv, forkState)));
                    }
                } else if (debugEvent instanceof SparkBatchJobExecutorCreatedEvent) {
                    SparkBatchJobExecutorCreatedEvent executorCreatedEvent =
                            (SparkBatchJobExecutorCreatedEvent) debugEvent;

                    final String containerId = executorCreatedEvent.getContainerId();
                    final SparkBatchRemoteDebugJob debugJob =
                            (SparkBatchRemoteDebugJob) driverDebugProcess.getSparkJob();

                    URI internalHostUri = executorCreatedEvent.getHostUri();
                    URI executorLogUrl = debugJob.convertToPublicLogUri(internalHostUri)
                                                 .map(uri -> uri.resolve(String.format("node/containerlogs/%s/livy",
                                                                                       containerId)))
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
            } catch (final ExecutionException e) {
                EventUtil.logErrorClassNameOnlyWithComplete(
                        operation, ErrorType.systemError, new UncheckedExecutionException(e), null, null);
                throw new UncheckedExecutionException(e);
            }
        });

        driverDebugHandler.getRemoteDebugProcess().start();

        // Driver side execute, leverage Intellij Async Promise, to wait for the Spark app deployed
        executionManager.startRunProfile(environment, () -> jobDriverEnvReady.thenAsync(driverEnv ->
                toIdeaPromise(attachAndDebug(driverEnv, state))));
    }

    private Observable<RunContentDescriptor> attachAndDebug(final ExecutionEnvironment environment,
                                                            final RunProfileState state) {
        final Project project = environment.getProject();
        final JFrame ideFrame = WindowManager.getInstance().getFrame(project);
        final ModalityState ideModalityState = ideFrame != null ? stateForComponent(ideFrame) : any();

        return Observable.fromCallable(() -> {
            // Invoke GenericDebuggerRunner#doExecute to start real VM attach and debugging
            return doExecute(state, environment);
        }).subscribeOn(new IdeaSchedulers(project).dispatchUIThread(ideModalityState));
    }

    /*
     * Build a child environment with specified host and type
     */
    private ExecutionEnvironment forkEnvironment(@NotNull final ExecutionEnvironment parentEnv,
                                                 final String host,
                                                 final boolean isDriver) {
        final String savedProfileName = parentEnv.getUserData(ProfileNameKey);
        final String originProfileName =
                savedProfileName == null ? parentEnv.getRunProfile().getName() : savedProfileName;

        final RunConfiguration newRunConfiguration = ((RunConfiguration) parentEnv.getRunProfile()).clone();
        newRunConfiguration.setName(originProfileName + " [" + (isDriver ? "Driver " : "Executor ") + host + "]");

        final ExecutionEnvironment childEnv = new ExecutionEnvironmentBuilder(parentEnv).runProfile(newRunConfiguration)
                                                                                        .build();

        childEnv.putUserData(DEBUG_TARGET_KEY, isDriver ? DEBUG_DRIVER : DEBUG_EXECUTOR);
        childEnv.putUserData(ProfileNameKey, originProfileName);

        return childEnv;
    }

    @NotNull
    @Override
    public Observable<ISparkBatchJob> buildSparkBatchJob(@NotNull final SparkSubmitModel submitModel) {
        return Observable.fromCallable(() -> {
            SparkSubmissionAdvancedConfigPanel.Companion.checkSettings(submitModel.getAdvancedConfigModel());

            final SparkSubmissionParameter debugSubmissionParameter = SparkBatchRemoteDebugJob.convertToDebugParameter(
                    submitModel.getSubmissionParameter());
            final SparkSubmitModel debugModel = new SparkSubmitModel(submitModel.getProject(),
                                                                     debugSubmissionParameter,
                                                                     submitModel.getAdvancedConfigModel(),
                                                                     submitModel.getJobUploadStorageModel());

            final String clusterName = submitModel.getSubmissionParameter().getClusterName();
            final IClusterDetail clusterDetail = ClusterManagerEx
                    .getInstance()
                    .getClusterDetailByName(clusterName)
                    .orElseThrow(() -> new ExecutionException("Can't find cluster named " + clusterName));

            final Deployable jobDeploy = SparkBatchJobDeployFactory.getInstance().buildSparkBatchJobDeploy(
                    debugModel, clusterDetail);
            return new SparkBatchRemoteDebugJob(clusterDetail,
                                                debugModel.getSubmissionParameter(),
                                                getClusterSubmission(clusterDetail),
                                                jobDeploy);
        });
    }

    @Override
    public void setFocus(@NotNull final RunConfiguration runConfiguration) {
        if (runConfiguration instanceof LivySparkBatchJobRunConfiguration) {
            final LivySparkBatchJobRunConfiguration livyRunConfig =
                    (LivySparkBatchJobRunConfiguration) runConfiguration;

            livyRunConfig.getModel().setFocusedTabIndex(1);
            livyRunConfig.getModel().getSubmitModel().getAdvancedConfigModel().setUIExpanded(true);
        }
    }
}
