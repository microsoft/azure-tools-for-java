/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.webapp.runner.webappconfig;

import com.intellij.openapi.project.Project;
import com.microsoft.azure.toolkit.intellij.common.AzureSettingPanel;
import com.microsoft.azure.toolkit.intellij.common.AzureSettingsEditor;
import com.microsoft.azure.toolkit.intellij.webapp.runner.webappconfig.slimui.WebAppSlimSettingPanel;
import org.jetbrains.annotations.NotNull;

public class WebAppSettingEditor extends AzureSettingsEditor<WebAppConfiguration> {

    private final AzureSettingPanel mainPanel;
    private final WebAppConfiguration webAppConfiguration;

    public WebAppSettingEditor(Project project, @NotNull WebAppConfiguration webAppConfiguration) {
        super(project);
        mainPanel = new WebAppSlimSettingPanel(project, webAppConfiguration);
        this.webAppConfiguration = webAppConfiguration;
    }

    @Override
    @NotNull
    protected AzureSettingPanel getPanel() {
        return this.mainPanel;
    }
}
