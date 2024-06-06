/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.containerapps.deployimage;

import com.intellij.execution.configurations.ConfigurationFactory;
import com.intellij.execution.configurations.RunConfiguration;
import com.intellij.openapi.project.Project;
import com.microsoft.azure.toolkit.ide.common.icon.AzureIcons;
import com.microsoft.azure.toolkit.intellij.common.IntelliJAzureIcons;
import com.microsoft.azure.toolkit.intellij.containerapps.AzureContainerAppConfigurationType;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;

public class DeployImageRunConfigurationFactory extends ConfigurationFactory {
    public static final String FACTORY_NAME = "Deploy to Azure Container App";
    public static final String FACTORY_ID = "Deploy Image to Azure Container App";

    public DeployImageRunConfigurationFactory(AzureContainerAppConfigurationType configurationType) {
        super(configurationType);
    }

    @NotNull
    @Override
    public RunConfiguration createTemplateConfiguration(@NotNull Project project) {
        return new DeployImageRunConfiguration(project, this, String.format("%s: %s", FACTORY_NAME, project.getName()));
    }

    @Override
    public String getName() {
        return FACTORY_NAME;
    }

    @Override
    public RunConfiguration createConfiguration(String name, RunConfiguration template) {
        return new DeployImageRunConfiguration(template.getProject(), this, name);
    }

    @Override
    public Icon getIcon() {
        return IntelliJAzureIcons.getIcon(AzureIcons.ContainerApps.MODULE);
    }

    @Override
    public @NotNull @NonNls String getId() {
        return FACTORY_ID;
    }
}
