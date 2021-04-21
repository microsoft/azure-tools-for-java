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
import com.intellij.openapi.util.IconLoader;
import com.intellij.openapi.util.Key;
import lombok.Getter;
import lombok.extern.java.Log;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
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
    public @Nullable Icon getTaskIcon(MyBeforeRunTask task) {
        return MyBeforeRunTask.ICON;
    }

    @Override
    public String getDescription(MyBeforeRunTask task) {
        return MyBeforeRunTask.DESCRIPTION;
    }

    @Nullable
    @Override
    public ConnectionRunnerForRunConfiguration.MyBeforeRunTask createTask(@NotNull RunConfiguration config) {
        return new MyBeforeRunTask();
    }

    @Override
    public boolean executeTask(@NotNull DataContext dataContext, @NotNull RunConfiguration configuration,
                               @NotNull ExecutionEnvironment executionEnvironment, @NotNull ConnectionRunnerForRunConfiguration.MyBeforeRunTask beforeRunTask) {
        return beforeRunTask.execute(dataContext, configuration);
    }

    public static class MyBeforeRunTask extends BeforeRunTask<MyBeforeRunTask> {
        private static final String NAME = "Connect Azure Resource";
        private static final String DESCRIPTION = "Connect Azure Resource";
        private static final Icon ICON = IconLoader.getIcon("/icons/Common/Azure.svg");// AzureIconLoader.loadIcon(AzureIconSymbol.Common.AZURE);
        private static final Key<MyBeforeRunTask> ID = Key.create("ConnectionRunnerForConfigurationId");
        private List<Connection<? extends Resource, ? extends Resource>> connections;

        protected MyBeforeRunTask() {
            super(ID);
        }

        public boolean execute(@NotNull DataContext dataContext, @NotNull RunConfiguration configuration) {
            // find connections at runtime since connections may be created after before task added into RC.
            this.connections = configuration.getProject().getService(ConnectionManager.class).getConnections().stream()
                    .filter(c -> c.isApplicableFor(configuration)).collect(Collectors.toList());
            return this.connections.stream().allMatch(c -> c.prepareBeforeRun(configuration, dataContext));
        }
    }

    public static class MyRunConfigurationExtension extends RunConfigurationExtension {

        @Override
        public <T extends RunConfigurationBase> void updateJavaParameters(@NotNull T configuration, @NotNull JavaParameters params, RunnerSettings settings) {
            final @NotNull List<?> beforeTasks = configuration.getBeforeRunTasks();
            beforeTasks.stream().filter(t -> t instanceof MyBeforeRunTask).map(t -> (MyBeforeRunTask) t)
                    .flatMap(t -> t.connections.stream())
                    .forEach(c -> c.updateJavaParametersAtRun(configuration, params));
        }

        @Override
        public boolean isApplicableFor(@NotNull RunConfigurationBase<?> configuration) {
            final boolean applicable = configuration.getProject().getService(ConnectionManager.class)
                    .getConnections().stream().anyMatch(c -> c.isApplicableFor(configuration));
            if (applicable) {
                if (configuration.getBeforeRunTasks().stream().noneMatch(t -> t instanceof MyBeforeRunTask)) {
                    final MyBeforeRunTask task = new MyBeforeRunTask();
                    task.setEnabled(true);
                    configuration.getBeforeRunTasks().add(task);
                }
            } else {
                final List<BeforeRunTask<?>> tasks = configuration.getBeforeRunTasks().stream()
                        .filter(t -> t instanceof MyBeforeRunTask).collect(Collectors.toList());
                if (tasks.size() > 0) {
                    configuration.getBeforeRunTasks().removeAll(tasks);
                }
            }
            return applicable;
        }
    }
}
