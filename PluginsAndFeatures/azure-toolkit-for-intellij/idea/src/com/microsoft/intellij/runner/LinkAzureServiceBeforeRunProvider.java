/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.intellij.runner;

import com.intellij.execution.BeforeRunTaskProvider;
import com.intellij.execution.application.ApplicationConfiguration;
import com.intellij.execution.configuration.AbstractRunConfiguration;
import com.intellij.execution.configurations.ModuleBasedConfiguration;
import com.intellij.execution.configurations.RunConfiguration;
import com.intellij.execution.configurations.RunConfigurationModule;
import com.intellij.execution.runners.ExecutionEnvironment;
import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.util.Key;
import com.microsoft.azure.toolkit.intellij.common.AzureArtifact;
import com.microsoft.azure.toolkit.intellij.common.AzureArtifactManager;
import com.microsoft.azure.toolkit.intellij.webapp.runner.webappconfig.WebAppConfiguration;
import com.microsoft.azure.toolkit.lib.link.AzureLinkService;
import com.microsoft.intellij.actions.SelectSubscriptionsAction;
import com.microsoft.intellij.helpers.AzureIconLoader;
import com.microsoft.tooling.msservices.serviceexplorer.AzureIconSymbol;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.util.Map;
import java.util.Objects;

public class LinkAzureServiceBeforeRunProvider extends BeforeRunTaskProvider<LinkAzureServiceBeforeRunTask> {
    private static final Logger LOGGER = Logger.getInstance(SelectSubscriptionsAction.class);
    private static final String NAME = "Connect Azure Resource";
    private static final String DESCRIPTION = "Connect Azure Resource";
    private static final String SPRING_BOOT_CONFIGURATION_REF = "com.intellij.spring.boot.run.SpringBootApplicationRunConfiguration";
    public static final Key<LinkAzureServiceBeforeRunTask> ID = Key.create("LinkAzureServiceBeforeRunProviderId");
    public static final Key<Boolean> LINK_AZURE_SERVICE = Key.create("LinkAzureService");
    public static final Key<Map<String, String>> LINK_AZURE_SERVICE_ENVS = Key.create("LinkAzureServiceEnvs");

    @Override
    public Key<LinkAzureServiceBeforeRunTask> getId() {
        return ID;
    }

    @Override
    public String getName() {
        return NAME;
    }

    @Override
    public String getDescription(LinkAzureServiceBeforeRunTask task) {
        return DESCRIPTION;
    }

    @Nullable
    @Override
    public Icon getIcon() {
        return AzureIconLoader.loadIcon(AzureIconSymbol.Common.AZURE);
    }

    @Nullable
    @Override
    public LinkAzureServiceBeforeRunTask createTask(@NotNull RunConfiguration runConfiguration) {
        boolean enable = false;
        if (StringUtils.equals(runConfiguration.getClass().getName(), SPRING_BOOT_CONFIGURATION_REF)
                || runConfiguration instanceof ApplicationConfiguration
                || runConfiguration instanceof WebAppConfiguration) {
            enable = true;
        }
        return new LinkAzureServiceBeforeRunTask(getId(), enable);
    }

    @Override
    public boolean executeTask(@NotNull DataContext dataContext, @NotNull RunConfiguration runConfiguration,
                               @NotNull ExecutionEnvironment executionEnvironment, @NotNull LinkAzureServiceBeforeRunTask linkAzureServiceBeforeRunTask) {
        String moduleName = this.getModuleName(runConfiguration);
        if (StringUtils.isBlank(moduleName)) {
            return true;
        }
        Map<String, String> linkedEnvMap = AzureLinkService.getInstance().retrieveLinkEnvsByModuleName(runConfiguration.getProject(), moduleName);
        if (MapUtils.isNotEmpty(linkedEnvMap)) {
            // set envs for remote deploy
            if (runConfiguration instanceof WebAppConfiguration) {
                WebAppConfiguration webAppConfiguration = (WebAppConfiguration) runConfiguration;
                webAppConfiguration.setApplicationSettings(linkedEnvMap);
            }
            // set envs for local run
            if (runConfiguration instanceof AbstractRunConfiguration
                    || StringUtils.equals(runConfiguration.getClass().getName(), SPRING_BOOT_CONFIGURATION_REF)
                    || runConfiguration instanceof ApplicationConfiguration) {
                ((ModuleBasedConfiguration<?, ?>) runConfiguration).putUserData(LINK_AZURE_SERVICE, true);
                ((ModuleBasedConfiguration<?, ?>) runConfiguration).putUserData(LINK_AZURE_SERVICE_ENVS, linkedEnvMap);
            }
        }
        return true;
    }

    private String getModuleName(@NotNull RunConfiguration runConfiguration) {
        if (runConfiguration instanceof WebAppConfiguration) {
            WebAppConfiguration webAppConfiguration = (WebAppConfiguration) runConfiguration;
            final AzureArtifact azureArtifact = AzureArtifactManager.getInstance(runConfiguration.getProject())
                    .getAzureArtifactById(webAppConfiguration.getAzureArtifactType(), webAppConfiguration.getArtifactIdentifier());
            final Module module = AzureArtifactManager.getInstance(runConfiguration.getProject()).getModuleFromAzureArtifact(azureArtifact);
            return Objects.nonNull(module) ? module.getName() : StringUtils.EMPTY;
        }
        if (runConfiguration instanceof AbstractRunConfiguration
                || StringUtils.equals(runConfiguration.getClass().getName(), SPRING_BOOT_CONFIGURATION_REF)) {
            RunConfigurationModule module = ((ModuleBasedConfiguration<?, ?>) runConfiguration).getConfigurationModule();
            return module.getModule().getName();
        }
        if (runConfiguration instanceof ApplicationConfiguration) {
            return ((ApplicationConfiguration) runConfiguration).getConfigurationModule().getModule().getName();
        }
        return null;
    }

}
