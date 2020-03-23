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
