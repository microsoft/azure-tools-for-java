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

package com.microsoft.azure.toolkit.intellij.webapp;

import com.intellij.openapi.project.Project;
import com.intellij.ui.TitledSeparator;
import com.microsoft.azure.toolkit.intellij.appservice.AppNameInput;
import com.microsoft.azure.toolkit.intellij.appservice.platform.PlatformComboBox;
import com.microsoft.azure.toolkit.intellij.common.AzureArtifactComboBox;
import com.microsoft.azure.toolkit.intellij.common.AzureFormPanel;
import com.microsoft.azure.toolkit.lib.appservice.Platform;
import com.microsoft.azure.toolkit.lib.common.form.AzureFormInput;
import com.microsoft.azure.toolkit.lib.webapp.WebAppConfig;

import javax.swing.*;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;

public class WebAppConfigFormPanelBasic extends JPanel implements AzureFormPanel<WebAppConfig> {
    private Project project;
    private JPanel contentPanel;

    private AppNameInput textName;
    private PlatformComboBox selectorPlatform;
    private AzureArtifactComboBox selectorApplication;
    private TitledSeparator deploymentTitle;
    private JLabel deploymentLabel;

    public WebAppConfigFormPanelBasic(final Project project) {
        super();
        this.project = project;
    }

    @Override
    public WebAppConfig getData() {
        final String name = this.textName.getValue();
        final Platform platform = this.selectorPlatform.getValue();
        final Path path = Paths.get(this.selectorApplication.getValue().getTargetPath());
        return WebAppConfig.builder()
                           .name(name)
                           .platform(platform)
                           .application(path)
                           .build();
    }

    @Override
    public List<AzureFormInput<?>> getInputs() {
        final AzureFormInput<?>[] inputs = {
            this.textName,
            this.selectorPlatform,
            this.selectorApplication
        };
        return Arrays.asList(inputs);
    }

    @Override
    public void setVisible(final boolean visible) {
        this.contentPanel.setVisible(visible);
        super.setVisible(visible);
    }

    private void createUIComponents() {
        // TODO: place custom component creation code here
        this.selectorApplication = new AzureArtifactComboBox(project);
    }

    public void setDeploymentVisible(boolean visible) {
        this.deploymentTitle.setVisible(visible);
        this.deploymentLabel.setVisible(visible);
        this.selectorApplication.setVisible(visible);
    }
}
