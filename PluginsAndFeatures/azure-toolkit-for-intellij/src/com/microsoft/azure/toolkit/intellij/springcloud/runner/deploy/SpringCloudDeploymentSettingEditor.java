/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.springcloud.runner.deploy;

import com.intellij.openapi.project.Project;
import com.microsoft.azure.toolkit.intellij.common.AzureSettingPanel;
import com.microsoft.azure.toolkit.intellij.common.AzureSettingsEditor;
import com.microsoft.azure.toolkit.intellij.springcloud.runner.ui.SpringCloudAppSettingPanel;
import org.jetbrains.annotations.NotNull;

public class SpringCloudDeploymentSettingEditor extends AzureSettingsEditor<SpringCloudDeployConfiguration> {
    private final Project project;
    private final AzureSettingPanel mainPanel;

    public SpringCloudDeploymentSettingEditor(Project project, @NotNull SpringCloudDeployConfiguration springCloudDeployConfiguration) {
        super(project);
        this.project = project;
        this.mainPanel = new SpringCloudAppSettingPanel(project, springCloudDeployConfiguration);
    }

    protected void disposeEditor() {
        this.mainPanel.disposeEditor();
    }

    @Override
    @NotNull
    protected AzureSettingPanel getPanel() {
        return this.mainPanel;
    }

    @Override
    protected void resetEditorFrom(@NotNull SpringCloudDeployConfiguration conf) {
        this.getPanel().reset(conf);
    }
}
