/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.legacy.webapp.runner.webapponlinux;

import com.intellij.openapi.project.Project;
import com.microsoft.azure.toolkit.intellij.legacy.common.AzureSettingPanel;
import com.microsoft.azure.toolkit.intellij.legacy.common.AzureSettingsEditor;
import com.microsoft.azure.toolkit.intellij.legacy.webapp.runner.webapponlinux.ui.DockerWebAppSettingPanel;
import org.jetbrains.annotations.NotNull;

public class WebAppOnLinuxDeploySettingsEditor extends AzureSettingsEditor<WebAppOnLinuxDeployConfiguration> {
    private final DockerWebAppSettingPanel settingPanel;
    private final WebAppOnLinuxDeployConfiguration configuration;

    public WebAppOnLinuxDeploySettingsEditor(Project project, WebAppOnLinuxDeployConfiguration configuration) {
        super(project);
        this.configuration = configuration;
        this.settingPanel = new DockerWebAppSettingPanel(project, configuration);
    }
    @Override
    @NotNull
    protected AzureSettingPanel getPanel() {
        return this.settingPanel;
    }
}
