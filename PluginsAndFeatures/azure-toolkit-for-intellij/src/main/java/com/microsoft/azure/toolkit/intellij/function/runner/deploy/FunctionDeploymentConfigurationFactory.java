/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.function.runner.deploy;

import com.intellij.execution.configurations.ConfigurationFactory;
import com.intellij.execution.configurations.ConfigurationType;
import com.intellij.execution.configurations.RunConfiguration;
import com.intellij.openapi.project.Project;
import com.microsoft.intellij.helpers.AzureIconLoader;
import com.microsoft.tooling.msservices.serviceexplorer.AzureIconSymbol;
import org.jetbrains.annotations.NotNull;

import javax.swing.Icon;

import static com.microsoft.intellij.ui.messages.AzureBundle.message;

public class FunctionDeploymentConfigurationFactory extends ConfigurationFactory {

    public FunctionDeploymentConfigurationFactory(@NotNull ConfigurationType type) {
        super(type);
    }

    @NotNull
    @Override
    public RunConfiguration createTemplateConfiguration(@NotNull Project project) {
        return new FunctionDeployConfiguration(project, this, project.getName());
    }

    @Override
    public RunConfiguration createConfiguration(String name, RunConfiguration template) {
        return new FunctionDeployConfiguration(template.getProject(), this, name);
    }

    @Override
    public String getName() {
        return message("function.deploy.factory.name");
    }

    @Override
    public Icon getIcon() {
        return AzureIconLoader.loadIcon(AzureIconSymbol.FunctionApp.DEPLOY);
    }
}
