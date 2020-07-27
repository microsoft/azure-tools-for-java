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

package com.microsoft.intellij.runner.springcloud.deploy;

import com.intellij.openapi.project.Project;
import com.microsoft.intellij.runner.AzureSettingPanel;
import com.microsoft.intellij.runner.AzureSettingsEditor;
import com.microsoft.intellij.runner.springcloud.ui.SpringCloudAppSettingPanel;
import com.microsoft.intellij.util.MavenRunTaskUtil;
import org.jetbrains.annotations.NotNull;

public class SpringCloudDeploymentSettingEditor extends AzureSettingsEditor<SpringCloudDeployConfiguration> {
    private final Project project;
    private final AzureSettingPanel mainPanel;

    public SpringCloudDeploymentSettingEditor(Project project, @NotNull SpringCloudDeployConfiguration springCloudDeployConfiguration) {
        super(project);
        this.project = project;
        this.mainPanel = new SpringCloudAppSettingPanel(project, springCloudDeployConfiguration);
    }

    protected void disposeEditor() {
        this.mainPanel.disposeEditor();
    }

    @Override
    @NotNull
    protected AzureSettingPanel getPanel() {
        return this.mainPanel;
    }

    @Override
    protected void resetEditorFrom(@NotNull SpringCloudDeployConfiguration conf) {
        if (conf.isFirstTimeCreated()) {
            if (MavenRunTaskUtil.isMavenProject(project)) {
                MavenRunTaskUtil.addMavenPackageBeforeRunTask(conf);
            }
        }
        conf.setFirstTimeCreated(false);
        this.getPanel().reset(conf);
    }
}
