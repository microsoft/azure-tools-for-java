/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.intellij.ui;

import com.microsoft.applicationinsights.preference.ApplicationInsightsResource;
import com.microsoft.intellij.ui.components.AzureDialogWrapper;

import javax.swing.*;

import static com.microsoft.intellij.ui.messages.AzureBundle.message;

public class ApplicationInsightsDetailsDialog extends AzureDialogWrapper {
    private JTextField txtName;
    private JTextField txtKey;
    private JTextField txtSub;
    private JTextField txtGrp;
    private JTextField txtRegion;
    private JPanel contentPane;
    ApplicationInsightsResource resource;

    public ApplicationInsightsDetailsDialog(ApplicationInsightsResource resource) {
        super(true);
        this.resource = resource;
        setTitle(message("aiErrTtl"));
        init();
    }

    protected void init() {
        txtName.setEditable(false);
        txtKey.setEditable(false);
        txtSub.setEditable(false);
        txtGrp.setEditable(false);
        txtRegion.setEditable(false);

        txtName.setText(resource.getResourceName());
        txtKey.setText(resource.getInstrumentationKey());
        txtSub.setText(resource.getSubscriptionName());
        txtGrp.setText(resource.getResourceGroup());
        txtRegion.setText(resource.getLocation());

        super.init();
    }

    @Override
    protected JComponent createCenterPanel() {
        return contentPane;
    }
}
