/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.webapp.docker.pushimage;

import com.intellij.openapi.project.Project;
import com.microsoft.azure.toolkit.intellij.common.AzureSettingPanel;
import com.microsoft.azure.toolkit.intellij.common.AzureSettingsEditor;
import com.microsoft.azure.toolkit.intellij.webapp.docker.pushimage.ui.SettingPanel;

import org.jetbrains.annotations.NotNull;

public class PushImageRunSettingsEditor extends AzureSettingsEditor<PushImageRunConfiguration> {
    private final SettingPanel settingPanel;

    public PushImageRunSettingsEditor(Project project) {
        super(project);
        this.settingPanel = new SettingPanel(project);
    }

    @Override
    @NotNull
    protected AzureSettingPanel getPanel() {
        return this.settingPanel;
    }
}
