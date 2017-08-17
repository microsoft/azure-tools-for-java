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

package com.microsoft.intellij.runner.container.webapponlinux;

import com.intellij.openapi.options.ConfigurationException;
import com.intellij.openapi.options.SettingsEditor;
import com.intellij.openapi.project.Project;
import com.intellij.packaging.artifacts.Artifact;
import com.intellij.packaging.impl.run.BuildArtifactsBeforeRunTaskProvider;
import com.microsoft.intellij.runner.container.webapponlinux.ui.SettingPanel;
import com.microsoft.intellij.util.MavenRunTaskUtil;

import org.jetbrains.annotations.NotNull;

import javax.swing.JComponent;
import java.util.List;

public class WebAppOnLinuxDeploySettingsEditor extends SettingsEditor<WebAppOnLinuxDeployConfiguration> {
    private final SettingPanel settingPanel;
    private final Project project;

    public WebAppOnLinuxDeploySettingsEditor(Project project) {
        this.project = project;
        settingPanel = new SettingPanel(project);
    }

    @Override
    protected void resetEditorFrom(@NotNull WebAppOnLinuxDeployConfiguration webAppOnLinuxDeployConfiguration) {
        if (webAppOnLinuxDeployConfiguration.isFirstTimeCreated()) {
            if (MavenRunTaskUtil.isMavenProject(project)) {
                MavenRunTaskUtil.addMavenPackageBeforeRunTask(webAppOnLinuxDeployConfiguration);
            } else {
                List<Artifact> artifacts = MavenRunTaskUtil.collectProjectArtifact(project);
                if(null != artifacts && artifacts.size() > 0) {
                    BuildArtifactsBeforeRunTaskProvider.setBuildArtifactBeforeRun(project, webAppOnLinuxDeployConfiguration, artifacts.get(0));
                }
            }
        }
        webAppOnLinuxDeployConfiguration.setFirstTimeCreated(false);
        settingPanel.reset(webAppOnLinuxDeployConfiguration);
    }

    @Override
    protected void applyEditorTo(@NotNull WebAppOnLinuxDeployConfiguration webAppOnLinuxDeployConfiguration) throws
            ConfigurationException {
        settingPanel.apply(webAppOnLinuxDeployConfiguration);
        webAppOnLinuxDeployConfiguration.validate();
    }

    @Override
    protected void disposeEditor() {
        settingPanel.disposeEditor();
        super.disposeEditor();
    }

    @NotNull
    @Override
    protected JComponent createEditor() {
        return settingPanel.getRootPanel();
    }
}
