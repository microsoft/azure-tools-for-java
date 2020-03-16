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

package com.microsoft.intellij.runner.springcloud.deploy;

import com.intellij.openapi.project.Project;
import com.intellij.packaging.artifacts.Artifact;
import com.microsoft.intellij.runner.AzureRunConfigurationBase;
import com.microsoft.intellij.runner.AzureSettingPanel;
import com.microsoft.intellij.runner.AzureSettingsEditor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.idea.maven.project.MavenProject;

import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;

public class SpringCloudDeploymentSettingEditor extends AzureSettingsEditor<SpringCloudDeployConfiguration> {

    private Project project;

    public SpringCloudDeploymentSettingEditor(Project project, @NotNull SpringCloudDeployConfiguration springCloudDeployConfiguration) {
        super(project);
        this.project = project;
    }

    @Override
    @NotNull
    protected AzureSettingPanel getPanel() {
        return new AzureSettingPanel(project) {
            @NotNull
            @Override
            public String getPanelName() {
                return "Deploy to Azure Spring Cloud";
            }

            @Override
            public void disposeEditor() {

            }

            @Override
            protected void resetFromConfig(@NotNull AzureRunConfigurationBase configuration) {

            }

            @Override
            protected void apply(@NotNull AzureRunConfigurationBase configuration) {

            }

            @NotNull
            @Override
            public JPanel getMainPanel() {
                return new JPanel();
            }

            @NotNull
            @Override
            protected JComboBox<Artifact> getCbArtifact() {
                return new JComboBox<>();
            }

            @NotNull
            @Override
            protected JLabel getLblArtifact() {
                return new JLabel();
            }

            @NotNull
            @Override
            protected JComboBox<MavenProject> getCbMavenProject() {
                return new JComboBox<>();
            }

            @NotNull
            @Override
            protected JLabel getLblMavenProject() {
                return new JLabel();
            }
        };
    }
}
