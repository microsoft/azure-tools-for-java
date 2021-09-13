/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.connector;

import com.intellij.execution.BeforeRunTask;
import com.intellij.execution.BeforeRunTaskProvider;
import com.intellij.execution.RunConfigurationExtension;
import com.intellij.execution.configurations.JavaParameters;
import com.intellij.execution.configurations.RunConfiguration;
import com.intellij.execution.configurations.RunConfigurationBase;
import com.intellij.execution.configurations.RunnerSettings;
import com.intellij.execution.runners.ExecutionEnvironment;
import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.openapi.util.Key;
import com.microsoft.azure.toolkit.intellij.common.AzureIcons;
import lombok.Getter;
import lombok.extern.java.Log;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.swing.*;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Log
public class ConnectionRunnerForRunConfiguration extends BeforeRunTaskProvider<ConnectionRunnerForRunConfiguration.MyBeforeRunTask> {
    @Getter
    public String name = MyBeforeRunTask.NAME;
    @Getter
    public Key<MyBeforeRunTask> id = MyBeforeRunTask.ID;
    @Getter
    public Icon icon = MyBeforeRunTask.ICON;

    @Override
    public @Nullable
    Icon getTaskIcon(MyBeforeRunTask task) {
        return MyBeforeRunTask.ICON;
    }

    @Override
    public String getDescription(MyBeforeRunTask task) {
        return MyBeforeRunTask.DESCRIPTION;
    }

    @Nullable
    @Override
    public ConnectionRunnerForRunConfiguration.MyBeforeRunTask createTask(@Nonnull RunConfiguration config) {
        return new MyBeforeRunTask();
    }

    @Override
    public boolean executeTask(@Nonnull DataContext dataContext, @Nonnull RunConfiguration configuration,
                               @Nonnull ExecutionEnvironment executionEnvironment, @Nonnull ConnectionRunnerForRunConfiguration.MyBeforeRunTask beforeRunTask) {
        return beforeRunTask.execute(dataContext, configuration);
    }

    public static class MyBeforeRunTask extends BeforeRunTask<MyBeforeRunTask> {
        private static final String NAME = "Connect Azure Resource";
        private static final String DESCRIPTION = "Connect Azure Resource";
        private static final Icon ICON = AzureIcons.getIcon("/icons/azure.svg");
        private static final Key<MyBeforeRunTask> ID = Key.create("ConnectionRunnerForConfigurationId");
        private List<Connection<?, ?>> connections;

        protected MyBeforeRunTask() {
            super(ID);
        }

        public boolean execute(@Nonnull DataContext dataContext, @Nonnull RunConfiguration configuration) {
            // find connections at runtime since connections may be created after before task added into RC.
            this.connections = configuration.getProject().getService(ConnectionManager.class).getConnections().stream()
                    .filter(c -> c.isApplicableFor(configuration)).collect(Collectors.toList());
            return this.connections.stream().allMatch(c -> c.prepareBeforeRun(configuration, dataContext));
        }
    }

    public static class MyRunConfigurationExtension extends RunConfigurationExtension {

        @Override
        public <T extends RunConfigurationBase<?>> void updateJavaParameters(@Nonnull T configuration, @Nonnull JavaParameters params,
                                                                             RunnerSettings settings) {
            final @Nonnull List<?> beforeTasks = configuration.getBeforeRunTasks();
            beforeTasks.stream().filter(t -> t instanceof MyBeforeRunTask).map(t -> (MyBeforeRunTask) t)
                    .flatMap(t -> t.connections.stream())
                    .forEach(c -> c.updateJavaParametersAtRun(configuration, params));
        }

        @Override
        public boolean isApplicableFor(@Nonnull RunConfigurationBase<?> configuration) {
            final boolean applicable = configuration.getProject().getService(ConnectionManager.class)
                    .getConnections().stream().anyMatch(c -> c.isApplicableFor(configuration));
            final List<BeforeRunTask<?>> tasks = configuration.getBeforeRunTasks();
            final List<BeforeRunTask<?>> myTasks = tasks.stream().filter(t -> t instanceof MyBeforeRunTask).collect(Collectors.toList());
            if (applicable && myTasks.isEmpty()) {
                final MyBeforeRunTask task = new MyBeforeRunTask();
                task.setEnabled(true);
                this.addTask(configuration, task);
            } else if (!applicable && !myTasks.isEmpty()) {
                tasks.removeAll(myTasks);
            }
            return applicable;
        }

        private void addTask(RunConfigurationBase<?> configuration, MyBeforeRunTask task) {
            try {
                configuration.getBeforeRunTasks().add(task);
            } catch (final UnsupportedOperationException e) { // EmptyList doesn't support `add`
                final ArrayList<BeforeRunTask<?>> newTasks = new ArrayList<>(configuration.getBeforeRunTasks());
                newTasks.add(task);
                configuration.setBeforeRunTasks(newTasks);
            }
        }
    }
}
