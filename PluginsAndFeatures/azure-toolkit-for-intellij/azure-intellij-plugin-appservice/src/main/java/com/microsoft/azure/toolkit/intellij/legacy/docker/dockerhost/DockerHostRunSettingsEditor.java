/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.legacy.docker.dockerhost;

import com.intellij.openapi.project.Project;
import com.microsoft.azure.toolkit.intellij.legacy.docker.dockerhost.ui.SettingPanel;
import com.microsoft.azure.toolkit.intellij.legacy.common.AzureSettingPanel;
import com.microsoft.azure.toolkit.intellij.legacy.common.AzureSettingsEditor;

public class DockerHostRunSettingsEditor extends AzureSettingsEditor<DockerHostRunConfiguration> {
    private SettingPanel settingPanel;

    public DockerHostRunSettingsEditor(Project project) {
        super(project);
        this.settingPanel = new SettingPanel(project);
    }

    @Override
    protected AzureSettingPanel getPanel() {
        return this.settingPanel;
    }
}
