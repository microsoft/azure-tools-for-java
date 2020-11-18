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

package com.microsoft.intellij.forms;

import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.openapi.ui.ValidationInfo;
import com.microsoft.intellij.helpers.LinkListener;
import com.microsoft.intellij.ui.components.AzureDialogWrapper;
import com.microsoft.tooling.msservices.model.storage.ClientStorageAccount;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

public class CreateQueueForm extends AzureDialogWrapper {
    private static final String NAME_REGEX = "^[a-z0-9](?!.*--)[a-z0-9-]+[a-z0-9]$";
    private static final int NAME_MAX = 63;
    private static final int NAME_MIN = 3;
    private JPanel contentPane;
    private JLabel namingGuidelinesLink;
    private JTextField nameTextField;
    private Runnable onCreate;
    private ClientStorageAccount storageAccount;
    private Project project;

    public CreateQueueForm(Project project) {
        super(project, true);

        this.project = project;
        setModal(true);

        setTitle("Create Queue");
        namingGuidelinesLink.addMouseListener(new LinkListener("http://go.microsoft.com/fwlink/?LinkId=255557"));

        nameTextField.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent documentEvent) {
                changedName();
            }

            @Override
            public void removeUpdate(DocumentEvent documentEvent) {
                changedName();
            }

            @Override
            public void changedUpdate(DocumentEvent documentEvent) {
                changedName();
            }
        });

        init();
    }

    private void changedName() {

        setOKActionEnabled(nameTextField.getText().length() > 0);
    }

    @Nullable
    @Override
    protected ValidationInfo doValidate() {
        final String name = nameTextField.getText();

        if (name.length() < NAME_MIN || name.length() > NAME_MAX || !name.matches(NAME_REGEX)) {
            return new ValidationInfo("Queue names must start with a letter or number, and can contain only letters, numbers, and the dash (-) character.\n" +
                    "Every dash (-) character must be immediately preceded and followed by a letter or number; consecutive dashes are not permitted in container names.\n" +
                    "All letters in a container name must be lowercase.\n" +
                    "Queue names must be from 3 through 63 characters long.", nameTextField);
        }

        return null;
    }

    @Nullable
    @Override
    protected JComponent createCenterPanel() {
        return contentPane;
    }

    @Override
    protected void doOKAction() {
        final String name = nameTextField.getText();
        sendTelemetry(OK_EXIT_CODE);
        close(DialogWrapper.OK_EXIT_CODE, true);
    }

    public void setOnCreate(Runnable onCreate) {
        this.onCreate = onCreate;
    }

    public void setStorageAccount(ClientStorageAccount storageAccount) {
        this.storageAccount = storageAccount;
    }

}
