/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.settings;

import com.intellij.application.options.OptionsContainingConfigurable;
import com.intellij.openapi.options.Configurable;
import com.intellij.openapi.options.ConfigurationException;
import com.intellij.openapi.options.SearchableConfigurable;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;

import static com.microsoft.azure.toolkit.intellij.common.AzureBundle.message;

public class AzureSettingsConfigurable implements SearchableConfigurable, Configurable.NoScroll, OptionsContainingConfigurable {
    public static final String AZURE_CONFIGURABLE_ID = "com.microsoft.azure.toolkit.intellij.settings.AzureSettingsConfigurable";

    private java.util.List<Configurable> myPanels;
    private final AzureSettingsPanel azureSettingsPanel;

    public AzureSettingsConfigurable() {
        this.azureSettingsPanel = new AzureSettingsPanel();
    }

    @NotNull
    @Override
    public String getId() {
        return AZURE_CONFIGURABLE_ID;
    }

    @Nls
    @Override
    public String getDisplayName() {
        return azureSettingsPanel.getDisplayName();
    }

    @Nullable
    @Override
    public String getHelpTopic() {
        return "windows_azure_project_properties";
    }

    @Override
    public JComponent createComponent() {
        azureSettingsPanel.init();
        return azureSettingsPanel.getPanel();
    }

    @Override
    public boolean isModified() {
        return azureSettingsPanel.isModified();
    }

    @Override
    public void apply() throws ConfigurationException {
        if (!azureSettingsPanel.doOKAction()) {
            throw new ConfigurationException(message("setPrefErMsg"), message("errTtl"));
        }
    }

    @Override
    public void reset() {
        azureSettingsPanel.reset();
    }
}
