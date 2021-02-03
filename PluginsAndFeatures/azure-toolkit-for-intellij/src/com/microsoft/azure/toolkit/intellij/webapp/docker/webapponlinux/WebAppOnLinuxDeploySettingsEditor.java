/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.webapp.docker.webapponlinux;

import com.intellij.openapi.project.Project;
import com.microsoft.azure.toolkit.intellij.common.AzureSettingPanel;
import com.microsoft.azure.toolkit.intellij.common.AzureSettingsEditor;
import com.microsoft.azure.toolkit.intellij.webapp.docker.webapponlinux.ui.SettingPanel;

import org.jetbrains.annotations.NotNull;

public class WebAppOnLinuxDeploySettingsEditor extends AzureSettingsEditor<WebAppOnLinuxDeployConfiguration> {
    private final SettingPanel settingPanel;

    public WebAppOnLinuxDeploySettingsEditor(Project project) {
        super(project);
        settingPanel = new SettingPanel(project);
    }
    @Override
    @NotNull
    protected AzureSettingPanel getPanel() {
        return this.settingPanel;
    }
}
