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

package com.microsoft.intellij.runner.container.pushimage;

import com.intellij.openapi.options.ConfigurationException;
import com.intellij.openapi.options.SettingsEditor;
import com.intellij.openapi.project.Project;
import com.intellij.packaging.artifacts.Artifact;
import com.intellij.packaging.impl.run.BuildArtifactsBeforeRunTaskProvider;
import com.microsoft.intellij.runner.container.pushimage.ui.SettingPanel;
import com.microsoft.intellij.util.MavenRunTaskUtil;

import org.jetbrains.annotations.NotNull;

import java.util.List;

import javax.swing.JComponent;

public class PushImageRunSettingsEditor extends SettingsEditor<PushImageRunConfiguration> {
    private final Project project;
    private SettingPanel settingPanel;

    public PushImageRunSettingsEditor(Project project) {
        this.project = project;
        this.settingPanel = new SettingPanel(project);
    }

    @SuppressWarnings("Duplicates")
    @Override
    protected void resetEditorFrom(@NotNull PushImageRunConfiguration containerLocalRunConfiguration) {
        if (containerLocalRunConfiguration.isFirstTimeCreated()) {
            if (MavenRunTaskUtil.isMavenProject(project)) {
                MavenRunTaskUtil.addMavenPackageBeforeRunTask(containerLocalRunConfiguration);
            } else {
                List<Artifact> artifacts = MavenRunTaskUtil.collectProjectArtifact(project); // is NotNull
                if (artifacts.size() > 0) {
                    BuildArtifactsBeforeRunTaskProvider.setBuildArtifactBeforeRun(project,
                            containerLocalRunConfiguration, artifacts.get(0));
                }
            }
        }
        containerLocalRunConfiguration.setFirstTimeCreated(false);
        settingPanel.reset(containerLocalRunConfiguration);
    }


    @Override
    protected void applyEditorTo(@NotNull PushImageRunConfiguration containerLocalRunConfiguration) throws
            ConfigurationException {
        settingPanel.apply(containerLocalRunConfiguration);
        containerLocalRunConfiguration.validate();
    }


    @NotNull
    @Override
    protected JComponent createEditor() {
        return settingPanel.getRootPanel();
    }


}
