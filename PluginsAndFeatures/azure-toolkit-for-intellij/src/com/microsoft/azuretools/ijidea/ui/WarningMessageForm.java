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

package com.microsoft.azuretools.ijidea.ui;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.DialogWrapper;
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
