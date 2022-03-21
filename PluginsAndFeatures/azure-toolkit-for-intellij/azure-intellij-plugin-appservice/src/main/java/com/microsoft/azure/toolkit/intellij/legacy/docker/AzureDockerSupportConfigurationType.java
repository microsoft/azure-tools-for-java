/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.legacy.docker;

import com.intellij.execution.configurations.ConfigurationFactory;
import com.intellij.execution.configurations.ConfigurationType;
import com.intellij.execution.configurations.ConfigurationTypeUtil;
import com.microsoft.azure.toolkit.intellij.common.AzureIcons;
import com.microsoft.azure.toolkit.intellij.legacy.docker.dockerhost.DockerHostRunConfigurationFactory;
import com.microsoft.azure.toolkit.intellij.legacy.docker.pushimage.PushImageRunConfigurationFactory;
import com.microsoft.azure.toolkit.intellij.legacy.docker.webapponlinux.WebAppOnLinuxDeployConfigurationFactory;
import com.microsoft.tooling.msservices.serviceexplorer.AzureIconSymbol;
import org.jetbrains.annotations.NotNull;

import javax.swing.Icon;

public class AzureDockerSupportConfigurationType implements ConfigurationType {

    public static AzureDockerSupportConfigurationType getInstance() {
        return ConfigurationTypeUtil.findConfigurationType(AzureDockerSupportConfigurationType.class);
    }

    @Override
    public String getDisplayName() {
        return "Azure Docker Support";
    }

    @Override
    public String getConfigurationTypeDescription() {
        return "Azure Docker Support Configuration Type";
    }

    @Override
    public Icon getIcon() {
        return AzureIcons.getIcon(AzureIconSymbol.DockerSupport.MODULE.getPath());
    }

    @NotNull
    @Override
    public String getId() {
        return "AZURE_DOCKER_SUPPORT_CONFIGURATION";
    }

    @Override
    public ConfigurationFactory[] getConfigurationFactories() {
        // CAUTION: the order cannot be changed, referenced by index in other places.
        return new ConfigurationFactory[]{
            new WebAppOnLinuxDeployConfigurationFactory(this),
            new DockerHostRunConfigurationFactory(this),
            new PushImageRunConfigurationFactory(this),
        };
    }

    public WebAppOnLinuxDeployConfigurationFactory getWebAppOnLinuxDeployConfigurationFactory() {
        return new WebAppOnLinuxDeployConfigurationFactory(this);
    }

    public DockerHostRunConfigurationFactory getDockerHostRunConfigurationFactory() {
        return new DockerHostRunConfigurationFactory(this);
    }

    public PushImageRunConfigurationFactory getPushImageRunConfigurationFactory() {
        return new PushImageRunConfigurationFactory(this);
    }
}
