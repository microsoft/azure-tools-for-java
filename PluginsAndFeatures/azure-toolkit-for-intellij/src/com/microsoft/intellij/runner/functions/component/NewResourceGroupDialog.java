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
package com.microsoft.intellij.runner.functions.component;

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
