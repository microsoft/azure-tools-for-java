/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.intellij.ui.components;


import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.TitlePanel;
import com.intellij.openapi.ui.ValidationInfo;
import com.microsoft.intellij.ui.AzureAbstractPanel;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;

public class DefaultDialogWrapper extends AzureDialogWrapper {
    private AzureAbstractPanel contentPanel;

    public DefaultDialogWrapper(Project project, AzureAbstractPanel panel) {
        super(project, true);
        this.contentPanel = panel;
        init();
    }

    @Override
    protected void init() {
        setTitle(contentPanel.getDisplayName());
        super.init();
    }

    @Override
    protected void doOKAction() {
        if (contentPanel.doOKAction()) {
            super.doOKAction();
        }
    }

    @Nullable
    @Override
    protected ValidationInfo doValidate() {
        return contentPanel.doValidate();
    }

    public String getSelectedValue() {
        return contentPanel.getSelectedValue();
    }

    @Override
    protected JComponent createTitlePane() {
        return new TitlePanel(contentPanel.getDisplayName(), "");
    }

    @Nullable
    @Override
    protected JComponent createCenterPanel() {
        return contentPanel.getPanel();
    }

    @Nullable
    @Override
    protected String getHelpId() {
        return contentPanel.getHelpTopic();
    }
}

