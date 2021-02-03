/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.common;

import com.intellij.openapi.options.ConfigurationException;
import com.intellij.openapi.options.SettingsEditor;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;

import javax.swing.JComponent;

public abstract class AzureSettingsEditor<T extends AzureRunConfigurationBase> extends SettingsEditor<T> {
    private final Project project;

    public AzureSettingsEditor(@NotNull Project project) {
        this.project = project;
    }

    @Override
    protected void applyEditorTo(@NotNull T conf) throws ConfigurationException {
        this.getPanel().apply(conf);
        conf.validate();
    }

    @Override
    protected void resetEditorFrom(@NotNull T conf) {
        this.getPanel().reset(conf);
    }

    @NotNull
    @Override
    protected JComponent createEditor() {
        return getPanel().getMainPanel();
    }

    @Override
    protected void disposeEditor() {
        getPanel().disposeEditor();
        super.disposeEditor();
    }

    @NotNull
    protected abstract AzureSettingPanel getPanel();
}
