/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.containerapps;

import com.intellij.execution.configurations.ConfigurationFactory;
import com.intellij.execution.configurations.ConfigurationType;
import com.intellij.execution.configurations.ConfigurationTypeUtil;
import com.microsoft.azure.toolkit.ide.common.icon.AzureIcons;
import com.microsoft.azure.toolkit.intellij.common.IntelliJAzureIcons;
import com.microsoft.azure.toolkit.intellij.containerapps.deployimage.DeployImageRunConfigurationFactory;

import javax.annotation.Nonnull;
import javax.swing.*;

public class AzureContainerAppConfigurationType implements ConfigurationType {
    @Override
    @Nonnull
    public String getDisplayName() {
        return "Azure Container App";
    }

    @Override
    @Nonnull
    public String getConfigurationTypeDescription() {
        return "Azure Container App Support";
    }

    public static AzureContainerAppConfigurationType getInstance() {
        return ConfigurationTypeUtil.findConfigurationType(AzureContainerAppConfigurationType.class);
    }

    @Override
    public Icon getIcon() {
        return IntelliJAzureIcons.getIcon(AzureIcons.ContainerApps.MODULE);
    }

    @Override
    @Nonnull
    public String getId() {
        return "AZURE_CONTAINER_APP_SUPPORT_CONFIGURATION";
    }

    @Override
    @Nonnull
    public ConfigurationFactory[] getConfigurationFactories() {
        return new ConfigurationFactory[]{new DeployImageRunConfigurationFactory(this)};
    }

    public DeployImageRunConfigurationFactory getDeployImageRunConfigurationFactory() {
        return new DeployImageRunConfigurationFactory(this);
    }
}
