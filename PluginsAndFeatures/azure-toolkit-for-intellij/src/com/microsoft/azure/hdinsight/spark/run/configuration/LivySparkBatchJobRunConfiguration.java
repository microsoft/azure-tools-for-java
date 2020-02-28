/**
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
 */
package com.microsoft.azure.hdinsight.spark.run.configuration;

import com.intellij.compiler.options.CompileStepBeforeRun;
import com.intellij.execution.ExecutionException;
import com.intellij.execution.Executor;
import com.intellij.execution.JavaExecutionUtil;
import com.intellij.execution.configuration.AbstractRunConfiguration;
import com.intellij.execution.configurations.*;
import com.intellij.execution.executors.DefaultDebugExecutor;
import com.intellij.execution.executors.DefaultRunExecutor;
import com.intellij.execution.runners.ExecutionEnvironment;
import com.intellij.execution.runners.ProgramRunner;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.options.SettingsEditor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.InvalidDataException;
import com.intellij.openapi.util.WriteExternalException;
import com.intellij.packaging.artifacts.Artifact;
import com.intellij.packaging.impl.artifacts.ArtifactUtil;
import com.intellij.packaging.impl.run.BuildArtifactsBeforeRunTask;
import com.intellij.packaging.impl.run.BuildArtifactsBeforeRunTaskProvider;

import com.microsoft.azure.hdinsight.common.logger.ILogger;
import com.microsoft.azure.hdinsight.spark.common.SparkBatchJobConfigurableModel;
import com.microsoft.azure.hdinsight.spark.common.SparkSubmissionParameter;
import com.microsoft.azure.hdinsight.spark.common.SparkSubmitAdvancedConfigModel;
import com.microsoft.azure.hdinsight.spark.common.SparkSubmitJobUploadStorageModel;
import com.microsoft.azure.hdinsight.spark.common.SparkSubmitModel;
import com.microsoft.azure.hdinsight.spark.run.*;
import com.microsoft.azure.hdinsight.spark.run.action.SparkApplicationType;
import com.microsoft.azure.hdinsight.spark.ui.SparkBatchJobConfigurable;
import com.microsoft.azuretools.azurecommons.helpers.NotNull;
import com.microsoft.azuretools.azurecommons.helpers.Nullable;
import com.microsoft.azuretools.securestore.SecureStore;
import com.microsoft.azuretools.service.ServiceManager;
import com.microsoft.azuretools.telemetry.TelemetryConstants;
import com.microsoft.azuretools.telemetrywrapper.EventType;
import com.microsoft.azuretools.telemetrywrapper.EventUtil;
import com.microsoft.azuretools.telemetrywrapper.Operation;
import com.microsoft.azuretools.telemetrywrapper.TelemetryManager;
import com.microsoft.intellij.telemetry.TelemetryKeys;
import org.apache.commons.lang3.StringUtils;
import org.jdom.Element;
import rx.subjects.PublishSubject;

import java.io.File;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.microsoft.azure.hdinsight.spark.common.SparkBatchRemoteDebugJobSshAuth.SSHAuthType.UsePassword;
import static com.microsoft.azure.hdinsight.spark.common.SparkSubmitJobUploadStorageModelKt.getSecureStoreServiceOf;
import static com.microsoft.azure.hdinsight.spark.common.SparkSubmitStorageType.ADLS_GEN2;
import static com.microsoft.azure.hdinsight.spark.common.SparkSubmitStorageType.BLOB;

public class LivySparkBatchJobRunConfiguration extends ModuleBasedConfiguration<JavaRunConfigurationModule, Element>
        implements ILogger
{
    enum RunMode {
        LOCAL,
        REMOTE,
        REMOTE_DEBUG_EXECUTOR
    }

    public SparkApplicationType getSparkApplicationType() {
        return SparkApplicationType.HDInsight;
    }

    @NotNull
    private RunMode mode = RunMode.LOCAL;

    // The prop to store the action trigger source if it can be got, such as Run Context
    final public static String ACTION_TRIGGER_PROP = "ActionTrigger";

    private @Nullable SecureStore secureStore = ServiceManager.getServiceProvider(SecureStore.class);

    @NotNull
    private SparkBatchJobConfigurableModel jobModel;
    @NotNull
    final private Properties actionProperties = new Properties();

    public LivySparkBatchJobRunConfiguration(@NotNull Project project,
                                             @NotNull SparkBatchJobConfigurableModel jobModel,
                                             @NotNull ConfigurationFactory factory,
                                             @NotNull String name) {
        super(name, new JavaRunConfigurationModule(project, true), factory);

        this.jobModel = jobModel;
        // FIXME: Too many telemetries will be sent if we uncomment the following code
        // EventUtil.logEvent(EventType.info, getSparkApplicationType().getValue(), TelemetryConstants.CREATE_NEW_RUN_CONFIG, null, null);
    }

    @Override
    public void readExternal(@NotNull Element rootElement) throws InvalidDataException {
        super.readExternal(rootElement);

        jobModel.applyFromElement(rootElement);

        // Load certificates from Secure Store
        loadFromSecureStore(getSubmitModel());
    }

    @Override
    public void writeExternal(@NotNull Element rootElement) throws WriteExternalException {
        super.writeExternal(rootElement);

        Element jobConfigElement = jobModel.exportToElement();
        rootElement.addContent(jobConfigElement);
    }

    public void loadFromSecureStore(final SparkSubmitModel toSubmitModel) {
        if (secureStore == null) {
            return;
        }

        // SSH password
        final SparkSubmitAdvancedConfigModel advModel = toSubmitModel.getAdvancedConfigModel();

        if (advModel.enableRemoteDebug && advModel.getSshAuthType() == UsePassword) {
            // Load password for no password input
            try {
                advModel.setClusterName(toSubmitModel.getClusterName());
                advModel.setSshPassword(
                        secureStore.loadPassword(advModel.getCredentialStoreAccount(), advModel.getSshUserName()));
            } catch (Throwable ex) {
                log().warn("Can't load SSH ${advModel.sshUserName}'s password from Secure Store", ex);
            }
        }

        // Artifacts uploading storage credentials
        final SparkSubmitJobUploadStorageModel storeModel = toSubmitModel.getJobUploadStorageModel();

        // Blob access key
        final String blobSecAccount = storeModel.getStorageAccount();
        if (blobSecAccount != null && StringUtils.isNoneBlank(blobSecAccount)) {
            final String blobSecService = getSecureStoreServiceOf(BLOB, blobSecAccount);

            if (blobSecService != null) {
                try {
                    storeModel.setStorageKey(secureStore.loadPassword(blobSecService, blobSecAccount));
                } catch (Throwable ex) {
                    log().warn("Can't load Blob access key $storageAccount from Secure Store", ex);
                }
            }
        }

        // ADLS Gen2 access key
        final String gen2SecAccount = storeModel.getGen2Account();
        if (gen2SecAccount != null && StringUtils.isNoneBlank(gen2SecAccount)) {
            final String gen2SecService = getSecureStoreServiceOf(ADLS_GEN2, gen2SecAccount);

            if (gen2SecService != null) {
                try {
                    storeModel.setAccessKey(secureStore.loadPassword(gen2SecService, gen2SecAccount));
                } catch (Throwable ex){
                    log().warn("Can't load ADLS Gen2 access key $gen2Account from Secure Store", ex);
                }
            }
        }
    }

    public void saveToSecureStore(final SparkSubmitModel fromSubmitModel) {
        if (secureStore == null) {
            return;
        }

        // SSH password
        final SparkSubmitAdvancedConfigModel advModel = fromSubmitModel.getAdvancedConfigModel();

        if (advModel.enableRemoteDebug
                && advModel.getSshAuthType() == UsePassword && StringUtils.isNoneBlank(advModel.getSshPassword())) {
            secureStore.savePassword(
                    advModel.getCredentialStoreAccount(), advModel.getSshUserName(), advModel.getSshPassword());
        }

        // Artifacts uploading storage credentials
        final SparkSubmitJobUploadStorageModel storeModel = fromSubmitModel.getJobUploadStorageModel();

        // Blob access key
        final String blobSecAccount = storeModel.getStorageAccount();
        if (blobSecAccount != null && StringUtils.isNoneBlank(blobSecAccount)) {
            final String blobSecService = getSecureStoreServiceOf(BLOB, blobSecAccount);

            if (blobSecService != null) {
                secureStore.savePassword(blobSecService, blobSecAccount, storeModel.getStorageKey());
                log().info("The Blob access key for " + blobSecAccount + " has been saved into Secure Store.");
            }
        }

        // ADLS Gen2 access key
        final String gen2SecAccount = storeModel.getGen2Account();
        if (gen2SecAccount != null && StringUtils.isNoneBlank(gen2SecAccount)) {
            final String gen2SecService = getSecureStoreServiceOf(ADLS_GEN2, gen2SecAccount);

            if (gen2SecService != null) {
                secureStore.savePassword(gen2SecService, gen2SecAccount, storeModel.getAccessKey());
                log().info("The ADLS Gen2 access key for " + gen2SecAccount + " has been saved into Secure Store.");
            }
        }
    }

    @NotNull
    public SparkBatchJobConfigurableModel getModel() {
        return jobModel;
    }

    @NotNull
    public SparkSubmitModel getSubmitModel() {
        return getModel().getSubmitModel();
    }

    @NotNull
    public Properties getActionProperties() {
        return actionProperties;
    }

    @NotNull
    @Override
    public SettingsEditor<? extends RunConfiguration> getConfigurationEditor() {
        return new LivySparkRunConfigurationSettingsEditor(new SparkBatchJobConfigurable(getProject()));
    }

    @Override
    public void checkConfiguration() throws RuntimeConfigurationException {

    }

    @Override
    public void checkRunnerSettings(@NotNull ProgramRunner runner, @Nullable RunnerSettings runnerSettings, @Nullable ConfigurationPerRunnerSettings configurationPerRunnerSettings) throws RuntimeConfigurationException {
        if (runner instanceof SparkSubmissionRunner) {
            // Focus on the submission tab
            SparkSubmissionRunner submissionRunner = (SparkSubmissionRunner) runner;
            submissionRunner.setFocus(this);

            // Check remote submission
            checkSubmissionConfigurationBeforeRun(submissionRunner);
        } else {
            // Focus on the local run tab
            getModel().setFocusedTabIndex(0);

            checkLocalRunConfigurationBeforeRun();
        }

        super.checkRunnerSettings(runner, runnerSettings, configurationPerRunnerSettings);
    }

    protected void checkBuildSparkJobBeforeRun(@NotNull SparkSubmissionRunner runner,
                                               @NotNull SparkSubmitModel submitModel) throws RuntimeConfigurationError {
        try {
            runner.buildSparkBatchJob(submitModel, PublishSubject.create());
        } catch (Exception err) {
            throw new RuntimeConfigurationError(err.getMessage());
        }
    }

    protected String getErrorMessageClusterNull() {
        return "The cluster should be selected as the target for Spark application submission";
    }

    protected void checkSubmissionConfigurationBeforeRun(@NotNull SparkSubmissionRunner runner) throws RuntimeConfigurationException {
        SparkSubmissionParameter parameter = getSubmitModel().getSubmissionParameter();
        if (StringUtils.isBlank(parameter.getClusterName())) {
            throw new RuntimeConfigurationError(this.getErrorMessageClusterNull());
        }

        if (!parameter.isLocalArtifact()) {
            if (StringUtils.isBlank(parameter.getArtifactName())) {
                throw new RuntimeConfigurationError("Couldn't find the artifact to submit, please create one and select it, or select a local artifact");
            }

            if (!getSubmitModel().getArtifactPath().isPresent()) {
                throw new RuntimeConfigurationError(String.format(
                        "No artifact selected or selected artifact %s is gone.", getSubmitModel().getArtifactName()));
            }
        } else {
            if (StringUtils.isBlank(parameter.getLocalArtifactPath())) {
                throw new RuntimeConfigurationError("The specified local artifact path is empty");
            }

            if (!new File(parameter.getLocalArtifactPath()).exists()) {
                throw new RuntimeConfigurationError(String.format(
                        "The specified local artifact path '%s' doesn't exist", parameter.getLocalArtifactPath()));
            }
        }

        if (StringUtils.isBlank(parameter.getMainClassName())) {
            throw new RuntimeConfigurationError("The main class name should not be empty");
        }

        if (getSubmitModel().getTableModel().getFirstCheckResults() != null) {
            throw new RuntimeConfigurationError("There are Spark job configuration issues, fix it before continue, please: " +
                    getSubmitModel().getTableModel().getFirstCheckResults().getMessaqge());
        }

        String modelError = getSubmitModel().getErrors().stream().filter(StringUtils::isNotBlank).findFirst().orElse(null);
        if (StringUtils.isNotBlank(modelError)) {
            throw new RuntimeConfigurationError("There are errors in submit model: " + modelError);
        }

        checkBuildSparkJobBeforeRun(runner, getSubmitModel());
    }

    private void checkLocalRunConfigurationBeforeRun() throws RuntimeConfigurationException {
        if (StringUtils.isBlank(getModel().getLocalRunConfigurableModel().getRunClass())) {
            throw new RuntimeConfigurationError("The main class name should not be empty");
        }
    }

    public void setRunMode(@NotNull RunMode mode) {
        this.mode = mode;
    }

    @NotNull
    @Override
    public List getBeforeRunTasks() {
        Stream compileTask = super.getBeforeRunTasks().stream()
                .filter(task -> task instanceof CompileStepBeforeRun.MakeBeforeRunTask);
        Stream buildArtifactTask = super.getBeforeRunTasks().stream()
                .filter(task -> task instanceof BuildArtifactsBeforeRunTask);

        switch (mode) {
        case LOCAL:
            compileTask.forEach(task -> ((CompileStepBeforeRun.MakeBeforeRunTask) task).setEnabled(true));
            buildArtifactTask.forEach(task -> ((BuildArtifactsBeforeRunTask) task).setEnabled(false));
            break;
        case REMOTE:
            compileTask.forEach(task -> ((CompileStepBeforeRun.MakeBeforeRunTask) task).setEnabled(false));
            buildArtifactTask.forEach(task -> ((BuildArtifactsBeforeRunTask) task).setEnabled(true));
            break;
        case REMOTE_DEBUG_EXECUTOR:
            compileTask.forEach(task -> ((CompileStepBeforeRun.MakeBeforeRunTask) task).setEnabled(false));
            buildArtifactTask.forEach(task -> ((BuildArtifactsBeforeRunTask) task).setEnabled(false));
            break;
        }

        return super.getBeforeRunTasks();
    }

    @Nullable
    @Override
    public RunProfileState getState(@NotNull Executor executor, @NotNull ExecutionEnvironment executionEnvironment) throws ExecutionException {
        Operation operation = executionEnvironment.getUserData(TelemetryKeys.OPERATION);
        final String debugTarget = executionEnvironment.getUserData(SparkBatchJobDebuggerRunner.DebugTargetKey);
        final boolean isExecutor = StringUtils.equals(debugTarget, SparkBatchJobDebuggerRunner.DebugExecutor);
        RunProfileStateWithAppInsightsEvent state = null;
        final Artifact selectedArtifact = ArtifactUtil.getArtifactWithOutputPaths(getProject()).stream()
                .filter(artifact -> artifact.getName().equals(getSubmitModel().getArtifactName()))
                .findFirst().orElse(null);

        if (executor instanceof SparkBatchJobDebugExecutor) {
            if (isExecutor) {
                setRunMode(RunMode.REMOTE_DEBUG_EXECUTOR);
                state = new SparkBatchRemoteDebugExecutorState(getModel().getSubmitModel(), operation);
            } else {
                if (selectedArtifact != null) {
                    BuildArtifactsBeforeRunTaskProvider.setBuildArtifactBeforeRun(getProject(), this, selectedArtifact);
                }

                setRunMode(RunMode.REMOTE);
                state = new SparkBatchRemoteDebugState(getModel().getSubmitModel(), operation);
            }
        } else if (executor instanceof SparkBatchJobRunExecutor) {
            if (selectedArtifact != null) {
                BuildArtifactsBeforeRunTaskProvider.setBuildArtifactBeforeRun(getProject(), this, selectedArtifact);
            }

            setRunMode(RunMode.REMOTE);
            state = new SparkBatchRemoteRunState(getModel().getSubmitModel(), operation);
        } else if (executor instanceof DefaultDebugExecutor) {
            setRunMode(RunMode.LOCAL);
            if (operation == null) {
                operation = TelemetryManager.createOperation(TelemetryConstants.HDINSIGHT, TelemetryConstants.DEBUG_LOCAL_SPARK_JOB);
                operation.start();
            }
            state = new SparkBatchLocalDebugState(getProject(), getModel().getLocalRunConfigurableModel(), operation);
        } else if (executor instanceof DefaultRunExecutor) {
            setRunMode(RunMode.LOCAL);
            if (operation == null) {
                operation = TelemetryManager.createOperation(TelemetryConstants.HDINSIGHT, TelemetryConstants.RUN_LOCAL_SPARK_JOB);
                operation.start();
            }
            state = new SparkBatchLocalRunState(getProject(), getModel().getLocalRunConfigurableModel(), operation);
        }

        if (state != null) {
            Map<String, String> props = getActionProperties().entrySet().stream().collect(Collectors.toMap(
                    (Map.Entry<Object, Object> entry) -> entry.getKey() == null ? null : entry.getKey().toString(),
                    (Map.Entry<Object, Object> entry) -> entry.getValue() == null ? "" : entry.getValue().toString()));
            String configurationId =
                    Optional.ofNullable(executionEnvironment.getRunnerAndConfigurationSettings())
                            .map(settings -> settings.getType().getId())
                            .orElse("");
            props.put("configurationId", configurationId);

            state.createAppInsightEvent(executor, props);
            EventUtil.logEvent(EventType.info, operation, props);

            // Clear the action properties
            getActionProperties().clear();
        }

        return state;
    }

    public void setActionProperty(@NotNull final String key, @NotNull final String value) {
        this.actionProperties.put(key, value);
    }

    @Override
    public Collection<Module> getValidModules() {
        return new ArrayList<>();
    }

    @Nullable
    @Override
    public String suggestedName() {
        return Optional.ofNullable(getModel().getLocalRunConfigurableModel().getRunClass())
                .map(JavaExecutionUtil::getPresentableClassName)
                .map(className -> getSuggestedNamePrefix() + " " + className)
                .orElse(null);
    }

    public String getSuggestedNamePrefix() {
        return "[Spark on HDInsight]";
    }

    @Nullable
    @Override
    public String getActionName() {
        // To avoid to be shorten
        return this.getName() + this.getPresentableType();
    }
}

