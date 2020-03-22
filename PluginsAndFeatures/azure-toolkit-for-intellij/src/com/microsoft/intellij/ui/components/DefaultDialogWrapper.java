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

