/**
 * Copyright (c) Microsoft Corporation
 * <p/>
 * All rights reserved.
 * <p/>
 * MIT License
 * <p/>
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated
 * documentation files (the "Software"), to deal in the Software without restriction, including without limitation
 * the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and
 * to permit persons to whom the Software is furnished to do so, subject to the following conditions:
 * <p/>
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of
 * the Software.
 * <p/>
 * THE SOFTWARE IS PROVIDED *AS IS*, WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO
 * THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT,
 * TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package com.microsoft.intellij.runner.webapp.webappconfig;

import com.intellij.openapi.options.ConfigurationException;
import com.intellij.openapi.options.SettingsEditor;
import com.intellij.openapi.project.Project;
import com.microsoft.intellij.ui.webapp.deploysetting.WebAppSettingPanel;
import com.microsoft.intellij.util.MavenRunTaskUtil;

import org.jetbrains.annotations.NotNull;

import javax.swing.JComponent;

public class WebAppSettingEditor extends SettingsEditor<WebAppConfiguration> {

    private final WebAppSettingPanel mainPanel;
    private final Project project;

    public WebAppSettingEditor(Project project) {
        this.project = project;
        mainPanel = new WebAppSettingPanel(project);
    }

    @Override
    protected void resetEditorFrom(@NotNull WebAppConfiguration webAppConfiguration) {
        if (webAppConfiguration.isFirstTimeCreated()) {
            MavenRunTaskUtil.addMavenPackageBeforeRunTask(webAppConfiguration);
        }
        webAppConfiguration.setFirstTimeCreated(false);
        mainPanel.resetEditorForm(webAppConfiguration.getWebAppSettingModel());
    }

    @Override
    protected void applyEditorTo(@NotNull WebAppConfiguration webAppConfiguration) throws ConfigurationException {
        validateConfiguration();
        WebAppSettingModel model = webAppConfiguration.getWebAppSettingModel();
        model.setWebAppId(mainPanel.getSelectedWebAppId());
        model.setSubscriptionId(mainPanel.getSubscriptionIdOfSelectedWebApp());
        model.setWebAppUrl(mainPanel.getWebAppUrl());
        model.setDeployToRoot(mainPanel.isDeployToRoot());
        model.setCreatingNew(mainPanel.isCreatingNew());
        model.setTargetPath(mainPanel.getTargetPath());
        model.setTargetName(mainPanel.getTargetName());
    }

    @NotNull
    @Override
    protected JComponent createEditor() {
        return mainPanel.getMainPanel();
    }

    private void validateConfiguration() throws ConfigurationException {
        String webAppId = mainPanel.getSelectedWebAppId();
        if (webAppId == null || webAppId.length() == 0) {
            throw new ConfigurationException("Choose a web app to deploy.");
        }
        if (!MavenRunTaskUtil.isMavenProject(project)) {
            throw new ConfigurationException("Current project is not a Maven project.");
        }
    }
}
