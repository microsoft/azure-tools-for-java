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

        ProgressManager.getInstance().run(new Task.Backgroundable(project, "Adding queue message", false) {
            @Override
            public void run(@NotNull ProgressIndicator progressIndicator) {
                /*try {
                    QueueMessage queueMessage = new QueueMessage(
                            "",
                            queue.getName(),
                            message,
                            new GregorianCalendar(),
                            new GregorianCalendar(),
                            0);

                    StorageClientSDKManager.getManager().createQueueMessage(storageAccount, queueMessage, expireSeconds);

                    if (onAddedMessage != null) {
                        ApplicationManager.getApplication().invokeLater(onAddedMessage);
                    }
                } catch (AzureCmdException e) {
                    String msg = "An error occurred while attempting to add queue message." + "\n" + String.format(message("webappExpMsg"), e.getMessage());
                    PluginUtil.displayErrorDialogAndLog(message("errTtl"), msg, e);
                }*/
            }
        });

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