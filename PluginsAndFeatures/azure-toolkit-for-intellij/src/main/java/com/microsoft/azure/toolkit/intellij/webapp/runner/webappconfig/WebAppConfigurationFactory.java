/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.webapp.runner.webappconfig;

import com.intellij.execution.configurations.ConfigurationFactory;
import com.intellij.execution.configurations.ConfigurationType;
import com.intellij.execution.configurations.RunConfiguration;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;

public class WebAppConfigurationFactory extends ConfigurationFactory {
    private static final String FACTORY_NAME = "Azure Web App";

    public WebAppConfigurationFactory(@NotNull ConfigurationType type) {
        super(type);
    }

    @NotNull
    @Override
    public RunConfiguration createTemplateConfiguration(@NotNull Project project) {
        return new WebAppConfiguration(project, this, project.getName());
    }

    @Override
    public RunConfiguration createConfiguration(String name, RunConfiguration template) {
        return new WebAppConfiguration(template.getProject(), this, name);
    }

    @Override
    public String getName() {
        return FACTORY_NAME;
    }

    @Override
    public @NotNull String getId() {
        return FACTORY_NAME;
    }
}
