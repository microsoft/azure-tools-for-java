/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.legacy.docker.webapponlinux;

import com.intellij.openapi.project.Project;
import com.microsoft.azure.toolkit.intellij.legacy.docker.webapponlinux.ui.SettingPanel;
import com.microsoft.azure.toolkit.intellij.legacy.common.AzureSettingPanel;
import com.microsoft.azure.toolkit.intellij.legacy.common.AzureSettingsEditor;

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
