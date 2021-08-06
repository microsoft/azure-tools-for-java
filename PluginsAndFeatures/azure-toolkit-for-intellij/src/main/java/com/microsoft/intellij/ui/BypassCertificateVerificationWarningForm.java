/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.intellij.ui;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.util.ui.UIUtil;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;

public class BypassCertificateVerificationWarningForm extends DialogWrapper {
    private JPanel contentPanel;
    private JPanel iconPanel;
    private JLabel warningIconLabel;
    private JLabel warningMessageLabel;

    protected BypassCertificateVerificationWarningForm(@Nullable Project project) {
        super(project);
        init();

        setModal(true);
        setTitle("Disable SSL Certificate Verification");
        setOKButtonText("Proceed");
    }

    protected void createUIComponents() {
        warningIconLabel = new JLabel(UIUtil.getWarningIcon());
    }

    @Nullable
    @Override
    protected JComponent createCenterPanel() {
        return contentPanel;
    }

    @Nullable
    @Override
    public JComponent getPreferredFocusedComponent() {
        return warningMessageLabel;
    }
}
