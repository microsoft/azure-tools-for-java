/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.intellij.forms;

import com.intellij.openapi.project.Project;
import com.microsoft.intellij.ui.components.AzureDialogWrapper;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;

public class ViewMessageForm extends AzureDialogWrapper {
    private JPanel contentPane;
    private JTextArea messageTextArea;

    public ViewMessageForm(Project project) {
        super(project, true);

        setModal(true);
        setTitle("View Message");
        init();

    }

    public void setMessage(String message) {
        messageTextArea.setText(message);
    }

    @NotNull
    @Override
    protected Action[] createActions() {
        return new Action[]{getOKAction()};
    }

    @Nullable
    @Override
    protected JComponent createCenterPanel() {
        return contentPane;
    }
}
