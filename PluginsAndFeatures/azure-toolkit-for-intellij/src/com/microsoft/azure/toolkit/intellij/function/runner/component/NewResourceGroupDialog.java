/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.function.runner.component;

import com.intellij.openapi.ui.ValidationInfo;
import com.microsoft.intellij.ui.components.AzureDialogWrapper;
import com.microsoft.intellij.util.ValidationUtils;
import org.jetbrains.annotations.Nullable;

import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import java.util.ArrayList;
import java.util.List;

public class NewResourceGroupDialog extends AzureDialogWrapper {
    private JPanel contentPane;
    private JTextField txtResourceGroup;
    private JLabel lblResourceGroup;
    private JPanel pnlResourceGroup;

    private String subscriptionId;
    private ResourceGroupPanel.ResourceGroupWrapper resourceGroupWrapper;

    public NewResourceGroupDialog(String subscriptionId) {
        super(false);
        setModal(true);
        setTitle("Create Resource Group");
        this.subscriptionId = subscriptionId;

        init();
    }

    public ResourceGroupPanel.ResourceGroupWrapper getResourceGroup() {
        return resourceGroupWrapper;
    }

    @Nullable
    @Override
    protected JComponent createCenterPanel() {
        return contentPane;
    }

    @Override
    protected List<ValidationInfo> doValidateAll() {
        List<ValidationInfo> res = new ArrayList<>();
        final String resourceGroupName = txtResourceGroup.getText();
        try {
            ValidationUtils.validateResourceGroupName(subscriptionId, resourceGroupName);
        } catch (IllegalArgumentException iae) {
            res.add(new ValidationInfo(iae.getMessage(), txtResourceGroup));
        }
        return res;
    }

    @Override
    protected void doOKAction() {
        resourceGroupWrapper = new ResourceGroupPanel.ResourceGroupWrapper(txtResourceGroup.getText());
        super.doOKAction();
    }

    @Override
    public void doCancelAction() {
        resourceGroupWrapper = null;
        super.doCancelAction();
    }
}
