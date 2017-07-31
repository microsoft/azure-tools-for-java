package com.microsoft.intellij.container.run.remote;

import com.intellij.execution.configurations.ConfigurationFactory;
import com.intellij.execution.configurations.RunConfiguration;
import com.intellij.openapi.project.Project;
import com.microsoft.intellij.container.run.ContainerRunConfigurationType;

import org.jetbrains.annotations.NotNull;

public class ContainerRemoteRunConfigurationFactory extends ConfigurationFactory {
    private static final String FACTORY_NAME = "Web App on Linux";

    @NotNull
    @Override
    public RunConfiguration createTemplateConfiguration(@NotNull Project project) {
        return new ContainerRemoteRunConfiguration(project, this,
                "createTemplateConfiguration@ContainerRemoteRunConfigurationFactory");
    }

    @Override
    public String getName() {
        return FACTORY_NAME;
    }

    public ContainerRemoteRunConfigurationFactory(ContainerRunConfigurationType containerRunConfigurationType) {
        super(containerRunConfigurationType);
    }
}
