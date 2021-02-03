/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.intellij;

import com.intellij.application.options.OptionsContainingConfigurable;
import com.intellij.openapi.options.Configurable;
import com.intellij.openapi.options.ConfigurationException;
import com.intellij.openapi.options.SearchableConfigurable;
import com.intellij.openapi.project.Project;
import com.microsoft.intellij.ui.*;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

import static com.microsoft.intellij.ui.messages.AzureBundle.message;

public class AzureConfigurable extends SearchableConfigurable.Parent.Abstract implements OptionsContainingConfigurable {
    public static final String AZURE_PLUGIN_NAME = "Microsoft Tools";
    public static final String AZURE_PLUGIN_ID = "com.microsoft.intellij";

    private java.util.List<Configurable> myPanels;
    private final Project myProject;

    public AzureConfigurable(Project project) {
        myProject = project;
    }

    @Override
    protected Configurable[] buildConfigurables() {
        myPanels = new ArrayList<Configurable>();
        if (!AzurePlugin.IS_ANDROID_STUDIO) {
            myPanels.add(new AzureAbstractConfigurable(new AzurePanel()));
            myPanels.add(new AzureAbstractConfigurable(new AppInsightsMngmtPanel(myProject)));
        }
        return myPanels.toArray(new Configurable[myPanels.size()]);
    }

    @NotNull
    @Override
    public String getId() {
        return AZURE_PLUGIN_ID;
    }

    @Nls
    @Override
    public String getDisplayName() {
        return AZURE_PLUGIN_NAME;
    }

    @Nullable
    @Override
    public String getHelpTopic() {
        return "windows_azure_project_properties";
    }

    @Override
    public JComponent createComponent() {
        JLabel label = new JLabel(message("winAzMsg"), SwingConstants.LEFT);
        label.setVerticalAlignment(SwingConstants.TOP);
        return label;
    }

    @Override
    public boolean hasOwnContent() {
        return true;
    }

    @Override
    public Set<String> processListOptions() {
        return new HashSet<String>();
    }

    public class AzureAbstractConfigurable implements SearchableConfigurable, Configurable.NoScroll, OptionsContainingConfigurable {
        private AzureAbstractConfigurablePanel myPanel;

        public AzureAbstractConfigurable(AzureAbstractConfigurablePanel myPanel) {
            this.myPanel = myPanel;
        }

        @Nls
        @Override
        public String getDisplayName() {
            return myPanel.getDisplayName();
        }

        @Nullable
        @Override
        public String getHelpTopic() {
            return null;
        }

        @Override
        public Set<String> processListOptions() {
            return null;
        }

        @Nullable
        @Override
        public JComponent createComponent() {
            myPanel.init();
            return myPanel.getPanel();
        }

        @Override
        public boolean isModified() {
            return myPanel.isModified();
        }

        @Override
        public void apply() throws ConfigurationException {
            if (!myPanel.doOKAction()) {
                throw new ConfigurationException(message("setPrefErMsg"), message("errTtl"));
            }
        }

        @Override
        public void reset() {
            myPanel.reset();
        }

        @Override
        public void disposeUIResources() {

        }

        @NotNull
        @Override
        public String getId() {
            return "preferences.sourceCode." + getDisplayName();
        }

        @Nullable
        @Override
        public Runnable enableSearch(String option) {
            return null;
        }
    }
}
