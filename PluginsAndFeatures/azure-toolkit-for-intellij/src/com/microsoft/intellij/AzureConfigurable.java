/*
 * Copyright (c) Microsoft Corporation
 *
 * All rights reserved.
 *
 * MIT License
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated
 * documentation files (the "Software"), to deal in the Software without restriction, including without limitation
 * the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and
 * to permit persons to whom the Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of
 * the Software.
 *
 * THE SOFTWARE IS PROVIDED *AS IS*, WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO
 * THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT,
 * TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
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
