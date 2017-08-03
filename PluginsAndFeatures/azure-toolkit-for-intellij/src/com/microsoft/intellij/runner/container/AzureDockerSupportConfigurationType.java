package com.microsoft.intellij.runner.container;

import com.intellij.execution.configurations.ConfigurationFactory;
import com.intellij.execution.configurations.ConfigurationType;
import com.intellij.execution.configurations.ConfigurationTypeUtil;
import com.intellij.icons.AllIcons;
import com.microsoft.intellij.runner.container.dockerhost.DockerHostRunConfigurationFactory;
import com.microsoft.intellij.runner.container.webapponlinux.WebAppOnLinuxDeployConfigurationFactory;

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
        return AllIcons.General.Information;
    }

    @NotNull
    @Override
    public String getId() {
        return "AZURE_DOCKER_SUPPORT_CONFIGURATION";
    }

    @Override
    public ConfigurationFactory[] getConfigurationFactories() {
        return new ConfigurationFactory[]{
                new DockerHostRunConfigurationFactory(this),
                new WebAppOnLinuxDeployConfigurationFactory(this),
        };
    }

}
