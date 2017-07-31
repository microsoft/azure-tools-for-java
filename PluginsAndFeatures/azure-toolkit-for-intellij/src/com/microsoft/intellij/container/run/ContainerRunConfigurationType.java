package com.microsoft.intellij.container.run;

import com.intellij.execution.configurations.ConfigurationFactory;
import com.intellij.execution.configurations.ConfigurationType;
import com.intellij.execution.configurations.ConfigurationTypeUtil;
import com.intellij.icons.AllIcons;
import com.microsoft.intellij.container.run.local.ContainerLocalRunConfigurationFactory;
import com.microsoft.intellij.container.run.remote.ContainerRemoteRunConfigurationFactory;

import org.jetbrains.annotations.NotNull;

import javax.swing.Icon;

public class ContainerRunConfigurationType implements ConfigurationType {
    public static ContainerRunConfigurationType getInstance() {
        return ConfigurationTypeUtil.findConfigurationType(ContainerRunConfigurationType.class);
    }

    @Override
    public String getDisplayName() {
        return "Azure Docker Support";
    }

    @Override
    public String getConfigurationTypeDescription() {
        return "Container Run Configuration Type";
    }

    @Override
    public Icon getIcon() {
        return AllIcons.General.Information;
    }

    @NotNull
    @Override
    public String getId() {
        return "CONTAINER_LOCAL_RUN_CONFIGURATION";
    }

    @Override
    public ConfigurationFactory[] getConfigurationFactories() {
        return new ConfigurationFactory[]{
                new ContainerLocalRunConfigurationFactory(this),
                new ContainerRemoteRunConfigurationFactory(this),
        };
    }

}
