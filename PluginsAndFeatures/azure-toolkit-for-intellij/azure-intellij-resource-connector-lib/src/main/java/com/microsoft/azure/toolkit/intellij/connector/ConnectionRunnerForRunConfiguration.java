/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.connector;

import com.intellij.execution.BeforeRunTask;
import com.intellij.execution.BeforeRunTaskProvider;
import com.intellij.execution.configurations.RunConfiguration;
import com.intellij.execution.runners.ExecutionEnvironment;
import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.openapi.util.Key;
import com.microsoft.azure.toolkit.ide.common.icon.AzureIcons;
import com.microsoft.azure.toolkit.intellij.common.IntelliJAzureIcons;
import com.microsoft.azure.toolkit.lib.common.messager.ExceptionNotification;
import com.microsoft.azure.toolkit.lib.common.operation.AzureOperation;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.java.Log;
import org.apache.commons.collections4.CollectionUtils;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.swing.*;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @see "org.jetbrains.idea.maven.tasks.MavenBeforeRunTasksProvider"
 */
@Log
public class ConnectionRunnerForRunConfiguration extends BeforeRunTaskProvider<ConnectionRunnerForRunConfiguration.MyBeforeRunTask> {

    private static final String NAME = "Connect Azure Resource";

    private static final String DESCRIPTION = "Connect Azure Resource";

    private static final Icon ICON = IntelliJAzureIcons.getIcon(AzureIcons.Common.AZURE);

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
    @ExceptionNotification
    @AzureOperation(name = "user/connector.get_task_description")
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

    @Override
    @ExceptionNotification
    @AzureOperation(name = "platform/connector.setup_connection_for_configuration")
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
        private RunConfiguration config;

        public MyBeforeRunTask(RunConfiguration config) {
            super(ID);
            this.config = config;
        }

        public List<Connection<?, ?>> getConnections() {
            final List<Connection<?, ?>> connections = this.config.getProject().getService(ConnectionManager.class).getConnections();
            return connections.stream().filter(c -> c.isApplicableFor(config)).collect(Collectors.toList());
        }
    }
}
