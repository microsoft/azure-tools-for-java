/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.intellij.forms;

import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.openapi.ui.ValidationInfo;
import com.microsoft.azure.toolkit.lib.common.task.AzureTask;
import com.microsoft.azure.toolkit.lib.common.task.AzureTaskManager;
import com.microsoft.intellij.ui.components.AzureDialogWrapper;
import com.microsoft.tooling.msservices.model.storage.ClientStorageAccount;
import com.microsoft.tooling.msservices.model.storage.Queue;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import javax.swing.text.AbstractDocument;
import javax.swing.text.AttributeSet;
import javax.swing.text.BadLocationException;
import javax.swing.text.DocumentFilter;
import java.util.regex.Pattern;

import static java.util.regex.Pattern.compile;

public class QueueMessageForm extends AzureDialogWrapper {
    private JPanel contentPane;
    private JTextArea messageTextArea;
    private JComboBox unitComboBox;
    private JTextField expireTimeTextField;
    private ClientStorageAccount storageAccount;
    private Queue queue;
    private Project project;
    private Runnable onAddedMessage;

    public QueueMessageForm(Project project) {
        super(project, true);
        this.project = project;
        setModal(true);

        ((AbstractDocument) expireTimeTextField.getDocument()).setDocumentFilter(new DocumentFilter() {
            Pattern pat = compile("\\d+");

            @Override
            public void replace(FilterBypass filterBypass, int i, int i1, String s, AttributeSet attributeSet) throws BadLocationException {
                if (pat.matcher(s).matches()) {
                    super.replace(filterBypass, i, i1, s, attributeSet);
                }
            }
        });

        init();
    }

    private int getExpireSeconds() {
        int expireUnitFactor = 1;
        switch (unitComboBox.getSelectedIndex()) {
            case 0: //Days
                expireUnitFactor = 60 * 60 * 24;
                break;
            case 1: //Hours
                expireUnitFactor = 60 * 60;
                break;
            case 2: //Minutes
                expireUnitFactor = 60;
                break;
        }

        return expireUnitFactor * Integer.parseInt(expireTimeTextField.getText());

    }

    @Nullable
    @Override
    protected ValidationInfo doValidate() {
        int maxSeconds = 60 * 60 * 24 * 7;

        if (getExpireSeconds() > maxSeconds) {
            return new ValidationInfo(
                    "The specified message time span exceeds the maximum allowed by the storage client.",
                    expireTimeTextField);
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

        final String message = messageTextArea.getText();
        final int expireSeconds = getExpireSeconds();

        sendTelemetry(OK_EXIT_CODE);
        close(DialogWrapper.OK_EXIT_CODE, true);
    }

    public void setStorageAccount(ClientStorageAccount storageAccount) {
        this.storageAccount = storageAccount;
    }

    public void setQueue(Queue queue) {
        this.queue = queue;
    }

    public void setOnAddedMessage(Runnable onAddedMessage) {
        this.onAddedMessage = onAddedMessage;
    }
}
