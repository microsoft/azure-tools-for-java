package com.microsoft.intellij.runner.container.dockerhost;

import com.intellij.execution.configurations.ConfigurationFactory;
import com.intellij.execution.configurations.RunConfiguration;
import com.intellij.openapi.project.Project;
import com.microsoft.intellij.runner.container.AzureDockerSupportConfigurationType;

import org.jetbrains.annotations.NotNull;

public class DockerHostRunConfigurationFactory extends ConfigurationFactory {
    private static final String FACTORY_NAME = "Docker";

    public DockerHostRunConfigurationFactory(AzureDockerSupportConfigurationType dockerSupportConfigurationType) {
        super(dockerSupportConfigurationType);
    }

    @NotNull
    @Override
    public RunConfiguration createTemplateConfiguration(@NotNull Project project) {
        return new DockerHostRunConfiguration(project, this);
    }

    @Override
    public String getName() {
        return FACTORY_NAME;
    }

    @Override
    public RunConfiguration createConfiguration(String name, RunConfiguration template) {
        return new DockerHostRunConfiguration(template.getProject(), this);
    }
}
