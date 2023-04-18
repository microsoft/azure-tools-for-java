/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.containerregistry.pushimage;

import com.intellij.openapi.project.Project;
import com.microsoft.azure.toolkit.intellij.containerregistry.pushimage.ui.PushImageSettingPanel;
import com.microsoft.azure.toolkit.intellij.legacy.common.AzureSettingPanel;
import com.microsoft.azure.toolkit.intellij.legacy.common.AzureSettingsEditor;

import org.jetbrains.annotations.NotNull;

public class PushImageRunSettingsEditor extends AzureSettingsEditor<PushImageRunConfiguration> {
    private final PushImageSettingPanel settingPanel;
    private final PushImageRunConfiguration configuration;

    public PushImageRunSettingsEditor(Project project, PushImageRunConfiguration configuration) {
        super(project);
        this.settingPanel = new PushImageSettingPanel(project, configuration);
        this.configuration = configuration;
    }

    @Override
    @NotNull
    protected AzureSettingPanel getPanel() {
        return this.settingPanel;
    }
}
