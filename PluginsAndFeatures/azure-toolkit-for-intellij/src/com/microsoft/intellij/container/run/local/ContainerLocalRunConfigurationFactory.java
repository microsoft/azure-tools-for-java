package com.microsoft.intellij.container.run.local;

import com.intellij.execution.configurations.ConfigurationFactory;
import com.intellij.execution.configurations.RunConfiguration;
import com.intellij.openapi.project.Project;
import com.microsoft.intellij.container.run.ContainerRunConfigurationType;
import org.jetbrains.annotations.NotNull;

public class  ContainerLocalRunConfigurationFactory extends ConfigurationFactory {
    private static final String FACTORY_NAME = "Docker Host";
    public ContainerLocalRunConfigurationFactory(ContainerRunConfigurationType containerLocalRunConfigurationType) {
        super(containerLocalRunConfigurationType);
    }

    @NotNull
    @Override
    public RunConfiguration createTemplateConfiguration(@NotNull Project project) {
        return new ContainerLocalRunConfiguration(project, this, " createTemplateConfiguration@ContainerLocalRunConfigurationFactory");
    }

    @Override
    public String getName() {
        return FACTORY_NAME;
    }
}
