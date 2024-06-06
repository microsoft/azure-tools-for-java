package com.microsoft.azure.toolkit.intellij.connector.dotazure;

import com.intellij.execution.BeforeRunTask;
import com.intellij.execution.RunManagerEx;
import com.intellij.execution.RunManagerListener;
import com.intellij.execution.RunnerAndConfigurationSettings;
import com.intellij.execution.configurations.RunConfiguration;
import com.intellij.execution.impl.ConfigurationSettingsEditorWrapper;
import com.intellij.openapi.project.Project;
import com.microsoft.azure.toolkit.intellij.common.runconfig.IWebAppRunConfiguration;
import com.microsoft.azure.toolkit.intellij.connector.Connection;
import com.microsoft.azure.toolkit.intellij.connector.ConnectionTopics;
import com.microsoft.azure.toolkit.lib.common.messager.ExceptionNotification;
import com.microsoft.azure.toolkit.lib.common.operation.AzureOperation;
import com.microsoft.intellij.util.BuildArtifactBeforeRunTaskUtils;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

public class BeforeRunTaskAdder implements RunManagerListener, ConnectionTopics.ConnectionChanged, IWebAppRunConfiguration.ModuleChangedListener {
    @Override
    @ExceptionNotification
    public void runConfigurationAdded(@Nonnull RunnerAndConfigurationSettings settings) {
        final RunConfiguration config = settings.getConfiguration();
        final Profile profile = AzureModule.createIfSupport(config).map(AzureModule::getDefaultProfile).orElse(null);
        if (Objects.isNull(profile)) {
            return;
        }
        final List<Connection<?, ?>> connections = profile.getConnections();
        synchronized (config.getBeforeRunTasks()) {
            final List<BeforeRunTask<?>> tasks = new ArrayList<>(config.getBeforeRunTasks().stream()
                .filter(task -> !(task instanceof DotEnvBeforeRunTaskProvider.LoadDotEnvBeforeRunTask))
                .toList());
            if (connections.stream().anyMatch(c -> c.isApplicableFor(config))) {
                tasks.add(new DotEnvBeforeRunTaskProvider.LoadDotEnvBeforeRunTask(config));
            }
            config.setBeforeRunTasks(tasks);
        }
    }

    @Override
    @ExceptionNotification
    @AzureOperation(name = "boundary/connector.update_connection_task")
    public void connectionChanged(Project project, Connection<?, ?> connection, ConnectionTopics.Action change) {
        final RunManagerEx rm = RunManagerEx.getInstanceEx(project);
        final List<RunConfiguration> configurations = rm.getAllConfigurationsList();
        for (final RunConfiguration config : configurations) {
            final List<BeforeRunTask<?>> tasks = new ArrayList<>(config.getBeforeRunTasks());
            if (change == ConnectionTopics.Action.ADD) {
                if (connection.isApplicableFor(config) && tasks.stream().noneMatch(t -> t instanceof DotEnvBeforeRunTaskProvider.LoadDotEnvBeforeRunTask)) {
                    tasks.add(new DotEnvBeforeRunTaskProvider.LoadDotEnvBeforeRunTask(config));
                }
            } else {
                final List<Connection<?, ?>> connections = AzureModule.list(project).stream().map(AzureModule::getDefaultProfile).filter(Objects::nonNull)
                        .map(Profile::getConnectionManager)
                        .map(ConnectionManager::getConnections)
                        .flatMap(List::stream).toList();
                if (connections.stream().noneMatch(c -> c.isApplicableFor(config))) {
                    tasks.removeIf(t -> t instanceof DotEnvBeforeRunTaskProvider.LoadDotEnvBeforeRunTask);
                }
            }
            config.setBeforeRunTasks(tasks);
        }
    }

    @Override
    @ExceptionNotification
    public void artifactMayChanged(@Nonnull RunConfiguration config, @Nullable ConfigurationSettingsEditorWrapper editor) {
        final List<Connection<?, ?>> connections = AzureModule.createIfSupport(config).map(AzureModule::getDefaultProfile)
                .map(Profile::getConnectionManager)
                .map(ConnectionManager::getConnections)
                .orElse(Collections.emptyList());
        final List<BeforeRunTask<?>> tasks = config.getBeforeRunTasks();
        Optional.ofNullable(editor).ifPresent(e -> BuildArtifactBeforeRunTaskUtils.removeTasks(e, (t) -> t instanceof DotEnvBeforeRunTaskProvider.LoadDotEnvBeforeRunTask, config));
        tasks.removeIf(t -> t instanceof DotEnvBeforeRunTaskProvider.LoadDotEnvBeforeRunTask);
        if (connections.stream().anyMatch(c -> c.isApplicableFor(config))) {
            final List<BeforeRunTask<?>> newTasks = new ArrayList<>(tasks);
            final DotEnvBeforeRunTaskProvider.LoadDotEnvBeforeRunTask task = new DotEnvBeforeRunTaskProvider.LoadDotEnvBeforeRunTask(config);
            newTasks.add(task);
            config.setBeforeRunTasks(newTasks);
            Optional.ofNullable(editor).ifPresent(e -> e.addBeforeLaunchStep(task));
        }
    }
}
