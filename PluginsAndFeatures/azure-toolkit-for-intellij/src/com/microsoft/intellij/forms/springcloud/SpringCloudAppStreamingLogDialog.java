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

package com.microsoft.intellij.forms.springcloud;

import com.intellij.openapi.project.Project;
import com.intellij.ui.ListCellRendererWithRightAlignedComponent;
import com.microsoft.azure.management.appplatform.v2019_05_01_preview.DeploymentInstance;
import com.microsoft.intellij.ui.components.AzureDialogWrapper;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.util.List;

public class SpringCloudAppStreamingLogDialog extends AzureDialogWrapper {

    private JPanel pnlRoot;
    private JComboBox<DeploymentInstance> cbInstances;

    private DeploymentInstance instance;

    public SpringCloudAppStreamingLogDialog(@Nullable final Project project, List<DeploymentInstance> instances) {
        super(project, false);
        setTitle("Select instance");
        instances.forEach(instance -> cbInstances.addItem(instance));
        cbInstances.setRenderer(new ListCellRendererWithRightAlignedComponent<DeploymentInstance>() {
            @Override
            protected void customize(final DeploymentInstance deploymentInstance) {
                setLeftText(deploymentInstance.name());
            }
        });

        init();
    }

    public DeploymentInstance getInstance() {
        return instance;
    }

    @Override
    protected void doOKAction() {
        instance = (DeploymentInstance) cbInstances.getSelectedItem();
        super.doOKAction();
    }

    @Override
    public void doCancelAction() {
        instance = null;
        super.doCancelAction();
    }

    @Nullable
    @Override
    protected JComponent createCenterPanel() {
        return pnlRoot;
    }
}
