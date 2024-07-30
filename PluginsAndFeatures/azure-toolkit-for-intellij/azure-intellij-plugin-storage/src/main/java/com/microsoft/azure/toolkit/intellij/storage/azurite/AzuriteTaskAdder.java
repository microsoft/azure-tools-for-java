/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */
package com.microsoft.azure.toolkit.intellij.storage.azurite;

import com.intellij.execution.BeforeRunTask;
import com.intellij.execution.RunManagerEx;
import com.intellij.execution.RunManagerListener;
import com.intellij.execution.RunnerAndConfigurationSettings;
import com.intellij.execution.configurations.RunConfiguration;
import com.intellij.execution.impl.ConfigurationSettingsEditorWrapper;
import com.intellij.openapi.project.Project;
import com.intellij.ui.CollectionListModel;
import com.microsoft.azure.toolkit.intellij.common.runconfig.IWebAppRunConfiguration;
import com.microsoft.azure.toolkit.intellij.connector.Connection;
import com.microsoft.azure.toolkit.intellij.connector.ConnectionTopics;
import com.microsoft.azure.toolkit.intellij.connector.dotazure.AzureModule;
import com.microsoft.azure.toolkit.intellij.connector.dotazure.ConnectionManager;
import com.microsoft.azure.toolkit.intellij.connector.dotazure.Profile;
import com.microsoft.azure.toolkit.intellij.storage.connection.BaseStorageAccountResourceDefinition;
import com.microsoft.azure.toolkit.lib.common.messager.ExceptionNotification;
import com.microsoft.azure.toolkit.lib.storage.AzuriteStorageAccount;
import lombok.SneakyThrows;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.reflect.FieldUtils;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.stream.Collectors;

// todo: Remove duplicates with com.microsoft.azure.toolkit.intellij.connector.BeforeRunTaskAdder
public class AzuriteTaskAdder implements RunManagerListener, ConnectionTopics.ConnectionChanged, IWebAppRunConfiguration.ModuleChangedListener {
    @Override
    @ExceptionNotification
    public void runConfigurationAdded(@Nonnull RunnerAndConfigurationSettings settings) {
        final RunConfiguration config = settings.getConfiguration();
        if (isConfigurationConnectedToAzurite(config) && !isConfigurationContainsAzuriteTask(config)
                && !isDeploymentTask(config)) {
            synchronized (config.getBeforeRunTasks()) {
                config.getBeforeRunTasks().add(new AzuriteTaskProvider.AzuriteBeforeRunTask());
            }
        }
    }

    @Override
    public void connectionChanged(Project project, Connection<?, ?> connection, ConnectionTopics.Action action) {
        final RunManagerEx rm = RunManagerEx.getInstanceEx(project);
        final List<RunConfiguration> configurations = rm.getAllConfigurationsList();
        if (action == ConnectionTopics.Action.ADD && isAzuriteResourceConnection(connection)) {
            configurations.stream()
                    .filter(config -> connection.isApplicableFor(config) && !isConfigurationContainsAzuriteTask(config)
                            && !isDeploymentTask(config))
                    .forEach(config -> config.getBeforeRunTasks().add(new AzuriteTaskProvider.AzuriteBeforeRunTask()));
        } else if (action == ConnectionTopics.Action.REMOVE) {
            // if user update connection from azurite to existing storage account, connection in remove event will not be azurite
            // so could not check isAzuriteResourceConnection here, but need to check all configurations
            configurations.stream()
                    .filter(config -> isConfigurationContainsAzuriteTask(config) && !isConfigurationConnectedToAzurite(config)
                            && !isDeploymentTask(config))
                    .forEach(config -> config.getBeforeRunTasks().removeIf(t -> t instanceof AzuriteTaskProvider.AzuriteBeforeRunTask));
        }
    }

    @Override
    public void artifactMayChanged(@Nonnull RunConfiguration config, @Nullable ConfigurationSettingsEditorWrapper editor) {
        if (isDeploymentTask(config)) {
            return;
        }
        final List<Connection<?, ?>> connections = AzureModule.createIfSupport(config).map(AzureModule::getDefaultProfile)
                .map(Profile::getConnectionManager)
                .map(ConnectionManager::getConnections)
                .orElse(Collections.emptyList());
        final List<BeforeRunTask<?>> tasks = config.getBeforeRunTasks();
        Optional.ofNullable(editor).ifPresent(e -> removeTasks(e, (t) -> t instanceof AzuriteTaskProvider.AzuriteBeforeRunTask));
        tasks.removeIf(t -> t instanceof AzuriteTaskProvider.AzuriteBeforeRunTask);
        if (isConfigurationConnectedToAzurite(config)) {
            final List<BeforeRunTask> newTasks = new ArrayList<>(tasks);
            final AzuriteTaskProvider.AzuriteBeforeRunTask task = new AzuriteTaskProvider.AzuriteBeforeRunTask();
            newTasks.add(task);
            RunManagerEx.getInstanceEx(config.getProject()).setBeforeRunTasks(config, newTasks);
            Optional.ofNullable(editor).ifPresent(e -> e.addBeforeLaunchStep(task));
        }
    }

    // workaround to filter out deployment tasks
    private static boolean isDeploymentTask(@Nonnull final RunConfiguration config) {
        final String configuration = config.getClass().getSimpleName();
        return StringUtils.equalsAnyIgnoreCase(configuration, "WebAppConfiguration", "FunctionDeployConfiguration", "SpringCloudDeploymentConfiguration");
    }

    private static boolean isConfigurationContainsAzuriteTask(@Nonnull final RunConfiguration config) {
        return config.getBeforeRunTasks().stream().anyMatch(t -> t instanceof AzuriteTaskProvider.AzuriteBeforeRunTask);
    }

    private static boolean isConfigurationConnectedToAzurite(@Nonnull final RunConfiguration config) {
        final List<Connection<?, ?>> connections = AzureModule.createIfSupport(config).map(AzureModule::getDefaultProfile).map(Profile::getConnections).orElse(Collections.emptyList());
        return connections.stream().anyMatch(c -> c.isApplicableFor(config) && isAzuriteResourceConnection(c));
    }

    public static boolean isAzuriteResourceConnection(@Nonnull final Connection<?, ?> connection) {
        return connection.getDefinition().getResourceDefinition() instanceof BaseStorageAccountResourceDefinition &&
            StringUtils.equalsIgnoreCase(connection.getResource().getDataId(), AzuriteStorageAccount.AZURITE_RESOURCE_ID);
    }

    @SneakyThrows
    public static synchronized <T extends BeforeRunTask<?>> void removeTasks(@Nonnull ConfigurationSettingsEditorWrapper editor, Predicate<T> cond) {
        // there is no way of removing tasks, use reflection
        final Object myBeforeRunStepsPanelField = FieldUtils.readField(editor, "myBeforeRunStepsPanel", true);
        final CollectionListModel<T> model = (CollectionListModel<T>) FieldUtils.readField(myBeforeRunStepsPanelField, "myModel", true);
        final List<T> tasks = model.getItems().stream().filter(cond).collect(Collectors.toList());
        for (final T t : tasks) {
            t.setEnabled(false);
            model.remove(t);
        }
    }
}
