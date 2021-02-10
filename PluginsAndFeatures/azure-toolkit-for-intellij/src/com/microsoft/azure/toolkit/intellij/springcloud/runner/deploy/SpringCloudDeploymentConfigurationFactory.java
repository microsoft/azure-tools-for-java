/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.springcloud.runner.deploy;

import com.intellij.execution.configurations.ConfigurationFactory;
import com.intellij.execution.configurations.ConfigurationType;
import com.intellij.execution.configurations.RunConfiguration;
import com.intellij.openapi.project.Project;
import com.microsoft.azure.toolkit.intellij.springcloud.runner.SpringCloudConstants;
import org.jetbrains.annotations.NotNull;

public class SpringCloudDeploymentConfigurationFactory extends ConfigurationFactory {
    public SpringCloudDeploymentConfigurationFactory(@NotNull ConfigurationType type) {
        super(type);
    }

    @NotNull
    @Override
    public RunConfiguration createTemplateConfiguration(@NotNull Project project) {
        return new SpringCloudDeployConfiguration(project, this, project.getName());
    }

    @Override
    public RunConfiguration createConfiguration(String name, RunConfiguration template) {
        return new SpringCloudDeployConfiguration(template.getProject(), this, name);
    }

    @Override
    public String getName() {
        return SpringCloudConstants.FACTORY_NAME;
    }
}
