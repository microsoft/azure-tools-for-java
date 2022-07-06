/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.intellij;

import com.intellij.application.options.OptionsContainingConfigurable;
import com.intellij.openapi.options.Configurable;
import com.intellij.openapi.options.ConfigurationException;
import com.intellij.openapi.options.SearchableConfigurable;
import com.microsoft.intellij.ui.AzurePanel;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;

import static com.microsoft.azure.toolkit.intellij.common.AzureBundle.message;

public class AzureConfigurable implements SearchableConfigurable, Configurable.NoScroll, OptionsContainingConfigurable {
    public static final String AZURE_PLUGIN_NAME = "Microsoft Tools";
    public static final String AZURE_PLUGIN_ID = "com.microsoft.intellij";

    private java.util.List<Configurable> myPanels;
    private final AzurePanel azurePanel;

    public AzureConfigurable() {
        this.azurePanel = new AzurePanel();
    }

    @NotNull
    @Override
    public String getId() {
        return AZURE_PLUGIN_ID;
    }

    @Nls
    @Override
    public String getDisplayName() {
        return azurePanel.getDisplayName();
    }

    @Nullable
    @Override
    public String getHelpTopic() {
        return "windows_azure_project_properties";
    }

    @Override
    public JComponent createComponent() {
        azurePanel.init();
        return azurePanel.getPanel();
    }

    @Override
    public boolean isModified() {
        return azurePanel.isModified();
    }

    @Override
    public void apply() throws ConfigurationException {
        if (!azurePanel.doOKAction()) {
            throw new ConfigurationException(message("setPrefErMsg"), message("errTtl"));
        }
    }

    @Override
    public void reset() {
        azurePanel.reset();
    }
}
