/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.containerregistry.dockerhost;

import com.intellij.openapi.project.Project;
import com.microsoft.azure.toolkit.intellij.containerregistry.dockerhost.ui.DockerHostRunConfigurationSettingPanel;
import com.microsoft.azure.toolkit.intellij.legacy.common.AzureSettingPanel;
import com.microsoft.azure.toolkit.intellij.legacy.common.AzureSettingsEditor;

public class DockerHostRunSettingsEditor extends AzureSettingsEditor<DockerHostRunConfiguration> {
    private DockerHostRunConfigurationSettingPanel settingPanel;
    private DockerHostRunConfiguration dockerHostRunConfiguration;

    public DockerHostRunSettingsEditor(Project project, DockerHostRunConfiguration dockerHostRunConfiguration) {
        super(project);
        this.dockerHostRunConfiguration = dockerHostRunConfiguration;
        this.settingPanel = new DockerHostRunConfigurationSettingPanel(project, dockerHostRunConfiguration);
    }

    @Override
    protected AzureSettingPanel getPanel() {
        return this.settingPanel;
    }
}
