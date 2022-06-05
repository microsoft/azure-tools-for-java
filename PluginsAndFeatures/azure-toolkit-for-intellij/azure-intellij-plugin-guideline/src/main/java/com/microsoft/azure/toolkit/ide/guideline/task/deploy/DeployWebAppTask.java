package com.microsoft.azure.toolkit.ide.guideline.task.deploy;

import com.intellij.execution.BeforeRunTask;
import com.intellij.execution.ProgramRunnerUtil;
import com.intellij.execution.RunManagerEx;
import com.intellij.execution.RunnerAndConfigurationSettings;
import com.intellij.execution.configurations.ConfigurationFactory;
import com.intellij.execution.configurations.RunConfiguration;
import com.intellij.execution.executors.DefaultRunExecutor;
import com.intellij.execution.process.ProcessAdapter;
import com.intellij.execution.process.ProcessEvent;
import com.intellij.execution.runners.ExecutionEnvironment;
import com.intellij.execution.runners.ExecutionEnvironmentBuilder;
import com.intellij.openapi.project.Project;
import com.microsoft.azure.toolkit.ide.guideline.Context;
import com.microsoft.azure.toolkit.ide.guideline.InputComponent;
import com.microsoft.azure.toolkit.ide.guideline.Process;
import com.microsoft.azure.toolkit.ide.guideline.Task;
import com.microsoft.azure.toolkit.ide.guideline.task.create.webapp.CreateWebAppTask;
import com.microsoft.azure.toolkit.intellij.common.AzureArtifact;
import com.microsoft.azure.toolkit.intellij.common.AzureArtifactManager;
import com.microsoft.azure.toolkit.intellij.legacy.webapp.runner.WebAppConfigurationType;
import com.microsoft.azure.toolkit.intellij.legacy.webapp.runner.webappconfig.WebAppConfiguration;
import com.microsoft.azure.toolkit.lib.Azure;
import com.microsoft.azure.toolkit.lib.appservice.webapp.AzureWebApp;
import com.microsoft.azure.toolkit.lib.common.bundle.AzureString;
import com.microsoft.azure.toolkit.lib.common.messager.AzureMessager;
import com.microsoft.azure.toolkit.lib.common.utils.Utils;
import com.microsoft.intellij.util.BuildArtifactBeforeRunTaskUtils;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nonnull;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CountDownLatch;

public class DeployWebAppTask implements Task {
    private Project project;
    private Process process;

    public DeployWebAppTask(@Nonnull Process process) {
        this.process = process;
        this.project = process.getProject();
    }

    @Override
    public InputComponent getInputComponent() {
        return null;
    }

    @Override
    public void execute(Context context) throws Exception {
        AzureMessager.getMessager().info("Setting up run configuration for web app deployment...");
        final RunManagerEx manager = RunManagerEx.getInstanceEx(project);
        final RunnerAndConfigurationSettings settings = getRunConfigurationSettings((String) context.getProperty(CreateWebAppTask.WEBAPP_ID), manager);
        manager.addConfiguration(settings);
        manager.setSelectedConfiguration(settings);
        CountDownLatch countDownLatch = new CountDownLatch(1);
        ExecutionEnvironmentBuilder executionEnvironmentBuilder = ExecutionEnvironmentBuilder.create(DefaultRunExecutor.getRunExecutorInstance(), settings);
        AzureMessager.getMessager().info(AzureString.format("Executing run configuration %s...", settings.getName()));
        ExecutionEnvironment build = executionEnvironmentBuilder.contentToReuse(null).dataContext(null).activeTarget().build();
        ProgramRunnerUtil.executeConfigurationAsync(build, true, true, runContentDescriptor -> {
            runContentDescriptor.getProcessHandler().addProcessListener(new ProcessAdapter() {
                @Override
                public void processTerminated(@NotNull ProcessEvent event) {
                    countDownLatch.countDown();
                }
            });
        });
        countDownLatch.await();
    }

    private RunnerAndConfigurationSettings getRunConfigurationSettings(String appId, RunManagerEx manager) {
        final ConfigurationFactory factory = WebAppConfigurationType.getInstance().getWebAppConfigurationFactory();
        final String runConfigurationName = String.format("Azure Sample: %s-%s", process.getName(), Utils.getTimestamp());
        RunnerAndConfigurationSettings settings = manager.createConfiguration(runConfigurationName, factory);
        final RunConfiguration runConfiguration = settings.getConfiguration();
        if (runConfiguration instanceof WebAppConfiguration) {
            ((WebAppConfiguration) runConfiguration).setWebApp(Azure.az(AzureWebApp.class).webApp(appId));
            List<AzureArtifact> allSupportedAzureArtifacts = AzureArtifactManager.getInstance(project).getAllSupportedAzureArtifacts();
            // todo: change to use artifact build by maven in last step if not exist
            final AzureArtifact azureArtifact = allSupportedAzureArtifacts.get(0);
            ((WebAppConfiguration) runConfiguration).saveArtifact(azureArtifact);
            final List<BeforeRunTask> beforeRunTasks = Arrays.asList(BuildArtifactBeforeRunTaskUtils.createBuildTask(azureArtifact, runConfiguration));
            beforeRunTasks.addAll(runConfiguration.getBeforeRunTasks());
            manager.setBeforeRunTasks(runConfiguration, beforeRunTasks);
            ((WebAppConfiguration) runConfiguration).setOpenBrowserAfterDeployment(false);
        }
        return settings;
    }
}
