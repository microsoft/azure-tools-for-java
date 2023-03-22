package com.microsoft.azure.toolkit.ide.guidance.task;

import com.google.common.util.concurrent.SettableFuture;
import com.intellij.execution.ProgramRunnerUtil;
import com.intellij.execution.RunManagerEx;
import com.intellij.execution.RunnerAndConfigurationSettings;
import com.intellij.execution.executors.DefaultRunExecutor;
import com.intellij.execution.process.ProcessAdapter;
import com.intellij.execution.process.ProcessEvent;
import com.intellij.execution.runners.ExecutionEnvironment;
import com.intellij.execution.runners.ExecutionEnvironmentBuilder;
import com.intellij.openapi.project.Project;
import com.microsoft.azure.toolkit.ide.guidance.ComponentContext;
import com.microsoft.azure.toolkit.ide.guidance.Course;
import com.microsoft.azure.toolkit.ide.guidance.Task;
import com.microsoft.azure.toolkit.lib.common.bundle.AzureString;
import com.microsoft.azure.toolkit.lib.common.exception.AzureToolkitRuntimeException;
import com.microsoft.azure.toolkit.lib.common.messager.AzureMessager;
import com.microsoft.azure.toolkit.lib.common.task.AzureTaskManager;

import javax.annotation.Nonnull;
import java.util.Objects;

import static com.microsoft.azure.toolkit.intellij.common.runconfig.RunConfigurationUtils.AZURE_RUN_STATE_EXCEPTION;
import static com.microsoft.azure.toolkit.intellij.common.runconfig.RunConfigurationUtils.AZURE_RUN_STATE_RESULT;

public abstract class BaseDeployTask implements Task {
    protected final Project project;
    protected final Course guidance;
    protected final ComponentContext context;

    public BaseDeployTask(@Nonnull ComponentContext context) {
        this.context = context;
        this.project = context.getProject();
        this.guidance = context.getCourse();
    }

    @Override
    public void execute() throws Exception {
        AzureMessager.getMessager().info("Setting up run configuration for deployment...");
        final RunManagerEx manager = RunManagerEx.getInstanceEx(project);
        final RunnerAndConfigurationSettings settings = getRunConfigurationSettings(context, manager);
        manager.addConfiguration(settings);
        manager.setSelectedConfiguration(settings);
        final ExecutionEnvironmentBuilder executionEnvironmentBuilder = ExecutionEnvironmentBuilder.create(DefaultRunExecutor.getRunExecutorInstance(), settings);
        AzureMessager.getMessager().info(AzureString.format("Executing run configuration %s...", settings.getName()));
        final ExecutionEnvironment build = executionEnvironmentBuilder.contentToReuse(null).dataContext(null).activeTarget().build();
        final SettableFuture<Void> future = SettableFuture.create();
        AzureTaskManager.getInstance().runLater(() -> ProgramRunnerUtil.executeConfigurationAsync(build, true, true, runContentDescriptor -> Objects.requireNonNull(runContentDescriptor.getProcessHandler()).addProcessListener(new ProcessAdapter() {
            @Override
            public void processTerminated(@Nonnull ProcessEvent event) {
                final Boolean result = event.getProcessHandler().getUserData(AZURE_RUN_STATE_RESULT);
                if (Boolean.TRUE.equals(result)) {
                    future.set(null);
                } else {
                    final Throwable throwable = event.getProcessHandler().getUserData(AZURE_RUN_STATE_EXCEPTION);
                    future.setException(Objects.requireNonNullElseGet(throwable, () -> new AzureToolkitRuntimeException("Execution was terminated, please see output below")));
                }
            }
        })));
        future.get();
    }

    protected abstract RunnerAndConfigurationSettings getRunConfigurationSettings(@Nonnull ComponentContext context, RunManagerEx manager);
}
