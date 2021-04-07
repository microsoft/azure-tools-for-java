/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.connector;

import com.intellij.execution.BeforeRunTaskProvider;
import com.intellij.execution.application.ApplicationConfiguration;
import com.intellij.execution.configurations.JavaParameters;
import com.intellij.execution.configurations.ModuleBasedConfiguration;
import com.intellij.execution.configurations.RunConfiguration;
import com.intellij.execution.configurations.RunConfigurationBase;
import com.intellij.execution.configurations.RunnerSettings;
import com.intellij.execution.runners.ExecutionEnvironment;
import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.IconLoader;
import com.intellij.openapi.util.Key;
import lombok.Getter;
import lombok.extern.java.Log;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.util.Map;
import java.util.Objects;

@Log
public class ConnectionBeforeRunTaskProvider extends BeforeRunTaskProvider<ConnectionBeforeRunTaskProvider.BeforeRunTask> {
    public static final Key<Map<String, String>> CONNECT_AZURE_RESOURCE_ENV_VARS = Key.create("ConnectAzureResourceEnvVars");

    @Getter
    public String name = BeforeRunTask.NAME;
    @Getter
    public Key<BeforeRunTask> id = BeforeRunTask.ID;
    @Getter
    public Icon icon = BeforeRunTask.ICON;

    @Override
    public @Nullable Icon getTaskIcon(BeforeRunTask task) {
        return BeforeRunTask.ICON;
    }

    @Override
    public String getDescription(BeforeRunTask task) {
        return BeforeRunTask.DESCRIPTION;
    }

    @Nullable
    @Override
    public ConnectionBeforeRunTaskProvider.BeforeRunTask createTask(@NotNull RunConfiguration configuration) {
        return new BeforeRunTask(getId(), configuration);
    }

    @Override
    public boolean executeTask(@NotNull DataContext dataContext, @NotNull RunConfiguration configuration,
                               @NotNull ExecutionEnvironment executionEnvironment, @NotNull ConnectionBeforeRunTaskProvider.BeforeRunTask beforeRunTask) {
        final Project project = configuration.getProject();
        final Module module = this.getTargetModule(configuration);
        if (Objects.nonNull(module)) {
            final String moduleId = new ModuleResource(module.getName()).getId();
            final ConnectionManager manager = project.getService(ConnectionManager.class);
            manager.getConnectionsByConsumerId(moduleId).forEach(connection -> connection.beforeRun(configuration, dataContext));
        }
        return true;
    }

    @Nullable
    private Module getTargetModule(@NotNull RunConfiguration configuration) {
        Module module = null;
        // TODO get module from artifact;
        // if (configuration instanceof WebAppConfiguration) {
        //    final WebAppConfiguration webAppConfiguration = (WebAppConfiguration) configuration;
        //    final AzureArtifact azureArtifact = AzureArtifactManager.getInstance(configuration.getProject())
        //            .getAzureArtifactById(webAppConfiguration.getAzureArtifactType(), webAppConfiguration.getArtifactIdentifier());
        //    module = AzureArtifactManager.getInstance(configuration.getProject()).getModuleFromAzureArtifact(azureArtifact);
        // }
        if (Objects.isNull(module) && configuration instanceof ModuleBasedConfiguration && BeforeRunTask.isApplicableFor(configuration)) {
            module = ((ModuleBasedConfiguration<?, ?>) configuration).getConfigurationModule().getModule();
        }
        return module;
    }

    public static class BeforeRunTask extends com.intellij.execution.BeforeRunTask<BeforeRunTask> {
        private static final String NAME = "Connect Azure Resource";
        private static final String DESCRIPTION = "Connect Azure Resource";
        private static final Icon ICON = IconLoader.getIcon("/icon/Common/Azure.svg");// AzureIconLoader.loadIcon(AzureIconSymbol.Common.AZURE);
        private static final String SPRING_BOOT_CONFIGURATION = "com.intellij.spring.boot.run.SpringBootApplicationRunConfiguration";
        private static final String AZURE_WEBAPP_CONFIGURATION = "com.microsoft.azure.toolkit.intellij.webapp.runner.webappconfig.WebAppConfiguration";
        private static final Key<BeforeRunTask> ID = Key.create("LinkAzureServiceBeforeRunProviderId");

        protected BeforeRunTask(@NotNull Key<BeforeRunTask> providerId, @NotNull RunConfiguration configuration) {
            super(providerId);
            setEnabled(isApplicableFor(configuration));
        }

        public static boolean isApplicableFor(RunConfiguration configuration) {
            final boolean springbootAppRunConfiguration = StringUtils.equals(configuration.getClass().getName(), SPRING_BOOT_CONFIGURATION);
            final boolean azureWebAppRunConfiguration = StringUtils.equals(configuration.getClass().getName(), AZURE_WEBAPP_CONFIGURATION);
            final boolean javaAppRunConfiguration = configuration instanceof ApplicationConfiguration;
            return springbootAppRunConfiguration || javaAppRunConfiguration || azureWebAppRunConfiguration;
        }
    }

    public static class RunConfigurationExtension extends com.intellij.execution.RunConfigurationExtension {

        @Override
        public <T extends RunConfigurationBase> void updateJavaParameters(@NotNull T configuration, @NotNull JavaParameters params, RunnerSettings settings) {
            final Map<String, String> envMap = configuration.getUserData(CONNECT_AZURE_RESOURCE_ENV_VARS);
            if (MapUtils.isNotEmpty(envMap)) {
                for (final Map.Entry<String, String> entry : envMap.entrySet()) {
                    params.addEnv(entry.getKey(), entry.getValue());
                }
            }
        }

        @Override
        public boolean isApplicableFor(@NotNull RunConfigurationBase<?> configuration) {
            return MapUtils.isNotEmpty(configuration.getUserData(CONNECT_AZURE_RESOURCE_ENV_VARS));
        }
    }
}
