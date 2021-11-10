/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.connector;

import com.intellij.execution.BeforeRunTask;
import com.intellij.execution.BeforeRunTaskProvider;
import com.intellij.execution.RunConfigurationExtension;
import com.intellij.execution.RunManagerEx;
import com.intellij.execution.RunManagerListener;
import com.intellij.execution.RunnerAndConfigurationSettings;
import com.intellij.execution.configurations.JavaParameters;
import com.intellij.execution.configurations.RunConfiguration;
import com.intellij.execution.configurations.RunConfigurationBase;
import com.intellij.execution.configurations.RunnerSettings;
import com.intellij.execution.runners.ExecutionEnvironment;
import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Key;
import com.microsoft.azure.toolkit.intellij.common.AzureIcons;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.java.Log;
import org.apache.commons.collections4.CollectionUtils;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.swing.*;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * @see "org.jetbrains.idea.maven.tasks.MavenBeforeRunTasksProvider"
 */
@Log
public class ConnectionRunnerForRunConfiguration extends BeforeRunTaskProvider<ConnectionRunnerForRunConfiguration.MyBeforeRunTask> {
    private static final String NAME = "Connect Azure Resource";
    private static final String DESCRIPTION = "Connect Azure Resource";
    private static final Icon ICON = AzureIcons.getIcon("/icons/Common/Azure.svg");
    private static final Key<MyBeforeRunTask> ID = Key.create("ConnectionRunnerForConfigurationId");
    @Getter
    public String name = NAME;
    @Getter
    public Key<MyBeforeRunTask> id = ID;
    @Getter
    public Icon icon = ICON;

    @Override
    public @Nullable
    Icon getTaskIcon(MyBeforeRunTask task) {
        return ICON;
    }

    @Override
    public String getDescription(MyBeforeRunTask task) {
        final List<Connection<?, ?>> connections = task.getConnections();
        if (CollectionUtils.isEmpty(connections)) {
            return "No Azure resource is connected.";
        }
        if (connections.size() == 1) {
            return String.format("Connect \"%s\"", connections.get(0).getResource().toString());
        } else {
            return String.format("Connect \"%s\" and %d other resources", connections.get(0).getResource().toString(), (connections.size() - 1));
        }
    }

    @Nullable
    @Override
    public ConnectionRunnerForRunConfiguration.MyBeforeRunTask createTask(@Nonnull RunConfiguration config) {
        return new MyBeforeRunTask(config);
    }

    @Nullable
    private static MyBeforeRunTask createTaskFor(@Nonnull RunConfiguration config) {
        final List<Connection<?, ?>> connections = config.getProject().getService(ConnectionManager.class).getConnections();
        final List<BeforeRunTask<?>> tasks = config.getBeforeRunTasks();
        if (connections.stream().anyMatch(c -> c.isApplicableFor(config)) && tasks.stream().noneMatch(t -> t instanceof MyBeforeRunTask)) {
            return new MyBeforeRunTask(config);
        }
        return null;
    }

    @Override
    public boolean executeTask(
        @Nonnull DataContext dataContext,
        @Nonnull RunConfiguration configuration,
        @Nonnull ExecutionEnvironment executionEnvironment,
        @Nonnull ConnectionRunnerForRunConfiguration.MyBeforeRunTask task) {
        return task.getConnections().stream().allMatch(c -> c.prepareBeforeRun(configuration, dataContext));
    }

    @Getter
    @Setter
    public static class MyBeforeRunTask extends BeforeRunTask<MyBeforeRunTask> {
        private final RunConfiguration config;

        protected MyBeforeRunTask(RunConfiguration config) {
            super(ID);
            this.config = config;
        }

        public List<Connection<?, ?>> getConnections() {
            final List<Connection<?, ?>> connections = this.config.getProject().getService(ConnectionManager.class).getConnections();
            return connections.stream().filter(c -> c.isApplicableFor(config)).collect(Collectors.toList());
        }
    }

    public static class MyRunConfigurationExtension extends RunConfigurationExtension {

        @Override
        public <T extends RunConfigurationBase<?>> void updateJavaParameters(@Nonnull T config, @Nonnull JavaParameters params, RunnerSettings s) {
            config.getBeforeRunTasks().stream().filter(t -> t instanceof MyBeforeRunTask).map(t -> (MyBeforeRunTask) t)
                .flatMap(t -> t.getConnections().stream())
                .forEach(c -> c.updateJavaParametersAtRun(config, params));
        }

        @Override
        public boolean isApplicableFor(@Nonnull RunConfigurationBase<?> configuration) {
            return configuration.getBeforeRunTasks().stream().anyMatch(c -> c instanceof MyBeforeRunTask);
        }
    }

    public static class BeforeRunTaskAdder implements RunManagerListener, ConnectionTopics.ConnectionChanged {
        public void runConfigurationAdded(@Nonnull RunnerAndConfigurationSettings settings) {
            final RunConfiguration config = settings.getConfiguration();
            final MyBeforeRunTask task = createTaskFor(config);
            if (Objects.nonNull(task)) {
                config.getBeforeRunTasks().add(task);
            }
        }

        @Override
        public void connectionChanged(Project project, Connection<?, ?> connection, ConnectionTopics.Action change) {
            final RunManagerEx rm = RunManagerEx.getInstanceEx(project);
            final List<RunConfiguration> configurations = rm.getAllConfigurationsList();
            for (final RunConfiguration config : configurations) {
                final List<BeforeRunTask<?>> tasks = config.getBeforeRunTasks();
                if (change == ConnectionTopics.Action.ADD) {
                    if (connection.isApplicableFor(config) && tasks.stream().noneMatch(t -> t instanceof MyBeforeRunTask)) {
                        tasks.add(new MyBeforeRunTask(config));
                    }
                } else {
                    final List<Connection<?, ?>> connections = config.getProject().getService(ConnectionManager.class).getConnections();
                    if (connections.stream().noneMatch(c -> c.isApplicableFor(config))) {
                        tasks.removeIf(t -> t instanceof MyBeforeRunTask);
                    }
                }
            }
        }
    }
}
