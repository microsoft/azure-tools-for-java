/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.function.runner.deploy;

import com.intellij.openapi.project.Project;
import com.microsoft.azure.toolkit.intellij.common.AzureSettingPanel;
import com.microsoft.azure.toolkit.intellij.common.AzureSettingsEditor;
import com.microsoft.azure.toolkit.intellij.function.runner.deploy.ui.FunctionDeploymentPanel;
import org.jetbrains.annotations.NotNull;

public class FunctionDeploymentSettingEditor extends AzureSettingsEditor<FunctionDeployConfiguration> {

    private final AzureSettingPanel mainPanel;
    private final FunctionDeployConfiguration functionDeployConfiguration;

    public FunctionDeploymentSettingEditor(Project project, @NotNull FunctionDeployConfiguration functionDeployConfiguration) {
        super(project);
        this.mainPanel = new FunctionDeploymentPanel(project, functionDeployConfiguration);
        this.functionDeployConfiguration = functionDeployConfiguration;
    }

    @Override
    @NotNull
    protected AzureSettingPanel getPanel() {
        return this.mainPanel;
    }
}
