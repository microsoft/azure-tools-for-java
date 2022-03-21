/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.legacy.docker.webapponlinux;

import com.intellij.execution.configurations.ConfigurationFactory;
import com.intellij.execution.configurations.RunConfiguration;
import com.intellij.openapi.project.Project;
import com.microsoft.azure.toolkit.intellij.common.AzureIcons;
import com.microsoft.azure.toolkit.intellij.legacy.docker.AzureDockerSupportConfigurationType;

import com.microsoft.tooling.msservices.serviceexplorer.AzureIconSymbol;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;

import javax.swing.Icon;

public class WebAppOnLinuxDeployConfigurationFactory extends ConfigurationFactory {
    private static final String FACTORY_NAME = "Web App for Containers";

    public WebAppOnLinuxDeployConfigurationFactory(AzureDockerSupportConfigurationType configurationType) {
        super(configurationType);
    }

    @NotNull
    @Override
    public RunConfiguration createTemplateConfiguration(@NotNull Project project) {
        return new WebAppOnLinuxDeployConfiguration(project, this, String.format("%s: %s", FACTORY_NAME, project
                .getName()));
    }

    @Override
    public String getName() {
        return FACTORY_NAME;
    }

    @Override
    public RunConfiguration createConfiguration(String name, RunConfiguration template) {
        return new WebAppOnLinuxDeployConfiguration(template.getProject(), this, name);
    }

    @Override
    public Icon getIcon() {
        return AzureIcons.getIcon(AzureIconSymbol.DockerSupport.RUN_ON_WEB_APP.getPath());
    }

    @Override
    public @NotNull @NonNls String getId() {
        return FACTORY_NAME;
    }
}
