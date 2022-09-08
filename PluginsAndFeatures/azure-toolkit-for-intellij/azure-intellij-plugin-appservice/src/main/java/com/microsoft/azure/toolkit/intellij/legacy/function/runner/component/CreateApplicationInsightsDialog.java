/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.legacy.function.runner.component;

import com.intellij.openapi.ui.ValidationInfo;
import com.microsoft.azure.toolkit.intellij.common.AzureFormInputComponent;
import com.microsoft.azure.toolkit.lib.common.form.AzureValidationInfo;
import com.microsoft.intellij.ui.components.AzureDialogWrapper;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.util.ArrayList;
import java.util.List;

public class CreateApplicationInsightsDialog extends AzureDialogWrapper {

    private JPanel contentPane;
    private ApplicationInsightsNameTextField txtInsightsName;
    private JButton buttonOK;
    private String applicationInsightsName;

    public CreateApplicationInsightsDialog() {
        super(false);
        setModal(true);
        setTitle("Create new Application Insights");
        getRootPane().setDefaultButton(buttonOK);
        init();
    }

    @Nullable
    @Override
    protected JComponent createCenterPanel() {
        return contentPane;
    }

    @Override
    protected List<ValidationInfo> doValidateAll() {
        final AzureValidationInfo info = this.txtInsightsName.doValidateValue();
        final List<ValidationInfo> res = new ArrayList<>();
        if (info.getType() != AzureValidationInfo.Type.SUCCESS) {
            res.add(AzureFormInputComponent.toIntellijValidationInfo(info));
        }
        return res;
    }

    @Override
    protected void doOKAction() {
        applicationInsightsName = txtInsightsName.getText();
        super.doOKAction();
    }

    @Override
    public void doCancelAction() {
        applicationInsightsName = null;
        super.doCancelAction();
    }

    public String getApplicationInsightsName() {
        return applicationInsightsName;
    }

    @Override
    public @Nullable JComponent getPreferredFocusedComponent() {
        return this.txtInsightsName;
    }
}
