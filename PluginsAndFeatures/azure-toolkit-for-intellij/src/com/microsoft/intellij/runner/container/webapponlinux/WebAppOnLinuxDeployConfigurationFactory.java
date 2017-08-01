package com.microsoft.intellij.runner.container.webapponlinux;

import com.intellij.execution.configurations.ConfigurationFactory;
import com.intellij.execution.configurations.RunConfiguration;
import com.intellij.openapi.project.Project;
import com.microsoft.intellij.runner.container.AzureDockerSupportConfigurationType;
import org.jetbrains.annotations.NotNull;

public class WebAppOnLinuxDeployConfigurationFactory extends ConfigurationFactory {
    private static final String FACTORY_NAME = "Web App on Linux";

    @NotNull
    @Override
    public RunConfiguration createTemplateConfiguration(@NotNull Project project) {
        return new WebAppOnLinuxDeployConfiguration(project, this,
                "createTemplateConfiguration@WebAppOnLinuxDeployConfigurationFactory");
    }

    @Override
    public String getName() {
        return FACTORY_NAME;
    }

    public WebAppOnLinuxDeployConfigurationFactory(AzureDockerSupportConfigurationType azureDockerSupportConfigurationType) {
        super(azureDockerSupportConfigurationType);
    }
}
