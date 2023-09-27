/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.intellij.ui;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.util.ui.UIUtil;
import com.microsoft.azuretools.azurecommons.helpers.NotNull;
import com.microsoft.azuretools.azurecommons.helpers.Nullable;

import javax.swing.*;

public class WarningMessageForm extends DialogWrapper {
    private JPanel contentPanel;
    private JPanel iconPanel;
    private JLabel warningIconLabel;
    protected JLabel warningMsgLabel;
    @Nullable
    private Project project;

    public WarningMessageForm(
            @Nullable Project project,
            @NotNull String title,
            @NotNull String warningMessage,
            @Nullable String okButtonText) {
        super(project);
        this.project = project;

        init();
        setModal(true);

        this.setTitle(title);
        this.warningMsgLabel.setText(warningMessage);

        if (okButtonText != null) {
            this.setOKButtonText(okButtonText);
        }
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
        return warningMsgLabel;
    }

    @Nullable
    public Project getProject() {
        return project;
    }
}
