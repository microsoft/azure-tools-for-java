/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.springcloud.streaminglog;

import com.intellij.openapi.project.Project;
import com.intellij.ui.ListCellRendererWithRightAlignedComponent;
import com.microsoft.azure.management.appplatform.v2020_07_01.DeploymentInstance;
import com.microsoft.intellij.ui.components.AzureDialogWrapper;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.util.List;

public class SpringCloudStreamingLogDialog extends AzureDialogWrapper {

    private JPanel pnlRoot;
    private JComboBox<DeploymentInstance> cbInstances;

    private DeploymentInstance instance;

    public SpringCloudStreamingLogDialog(@Nullable final Project project, List<DeploymentInstance> instances) {
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
