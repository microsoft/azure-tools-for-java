/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.function.runner.component;

import com.intellij.openapi.ui.ValidationInfo;
import com.microsoft.intellij.ui.components.AzureDialogWrapper;
import com.microsoft.intellij.util.ValidationUtils;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.util.ArrayList;
import java.util.List;

public class CreateApplicationInsightsDialog extends AzureDialogWrapper {
    private JPanel contentPane;
    private JTextField txtInsightsName;
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
        List<ValidationInfo> res = new ArrayList<>();
        final String insightsName = txtInsightsName.getText();
        try {
            ValidationUtils.validateApplicationInsightsName(insightsName);
        } catch (IllegalArgumentException iae) {
            res.add(new ValidationInfo(iae.getMessage(), txtInsightsName));
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

}
