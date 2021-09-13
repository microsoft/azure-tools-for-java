/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.connector;

import com.intellij.execution.BeforeRunTask;
import com.intellij.execution.BeforeRunTaskProvider;
import com.intellij.execution.Location;
import com.intellij.execution.RunConfigurationExtension;
import com.intellij.execution.RunManagerEx;
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
            setEnabled(true);
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
        @SuppressWarnings("rawtypes")
        protected void extendCreatedConfiguration(@Nonnull RunConfigurationBase<?> configuration, @Nonnull Location location) {
            System.out.println(this + ".extendCreatedConfiguration");
            final boolean applicable = this.isApplicableFor(configuration);
            final RunManagerEx manager = RunManagerEx.getInstanceEx(configuration.getProject());
            final List<BeforeRunTask> tasks = new ArrayList<>(manager.getBeforeRunTasks(configuration));
            final List<BeforeRunTask> myTasks = tasks.stream().filter(t -> t instanceof MyBeforeRunTask).collect(Collectors.toList());
            if (applicable && myTasks.isEmpty()) {
                tasks.add(new MyBeforeRunTask());
                manager.setBeforeRunTasks(configuration, tasks);
            } else if (!applicable && !myTasks.isEmpty()) {
                tasks.removeAll(myTasks);
                manager.setBeforeRunTasks(configuration, tasks);
            }
        }

        @Override
        public <T extends RunConfigurationBase<?>> void updateJavaParameters(@Nonnull T config, @Nonnull JavaParameters params, RunnerSettings s) {
            config.getBeforeRunTasks().stream().filter(t -> t instanceof MyBeforeRunTask).map(t -> (MyBeforeRunTask) t)
                    .flatMap(t -> t.connections.stream())
                    .forEach(c -> c.updateJavaParametersAtRun(config, params));
        }

        @Override
        public boolean isApplicableFor(@Nonnull RunConfigurationBase<?> configuration) {
            return configuration.getProject().getService(ConnectionManager.class)
                    .getConnections().stream().anyMatch(c -> c.isApplicableFor(configuration));
        }
    }
}
