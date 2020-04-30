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

package com.microsoft.azuretools.azureexplorer.forms;

import com.microsoft.tooling.msservices.components.DefaultLoader;
import com.microsoft.azuretools.azurecommons.helpers.AzureCmdException;
import com.microsoft.azuretools.core.components.AzureDialogWrapper;
import com.microsoft.tooling.msservices.helpers.azure.sdk.StorageClientSDKManager;
import com.microsoft.tooling.msservices.model.storage.ClientStorageAccount;
import com.microsoft.tooling.msservices.model.storage.Queue;
import com.microsoft.tooling.msservices.model.storage.QueueMessage;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.*;

import java.util.GregorianCalendar;

public class QueueMessageForm extends AzureDialogWrapper {
    private static String[] PERIODS = {"Days", "Hours", "Minutes", "Seconds"};
    private Button buttonOK;
    private Label messageLabel;
    private Text messageTextArea;
    private Combo unitComboBox;
    private Label expireLabel;
    private Text expireTimeTextField;
    private ClientStorageAccount storageAccount;
    private Queue queue;
    private Runnable onAddedMessage;

    public QueueMessageForm(Shell parentShell, ClientStorageAccount storageAccount, Queue queue) {
        super(parentShell);
        this.storageAccount = storageAccount;
        this.queue = queue;
    }

    @Override
    protected Control createContents(Composite parent) {
        Composite container = new Composite(parent, SWT.NONE);
        GridLayout gridLayout = new GridLayout();
        container.setLayout(gridLayout);
        GridData gridData = new GridData();
        gridData.widthHint = 350;
        gridData.heightHint = 300;
        container.setLayoutData(gridData);

        messageLabel = new Label(container, SWT.LEFT);
        messageLabel.setText("Message Text:");
        messageTextArea = new Text(container, SWT.LEFT | SWT.BORDER | SWT.MULTI);
        gridData = new GridData(SWT.FILL, SWT.FILL, true, true);
        gridData.heightHint = 200;
        messageTextArea.setLayoutData(gridData);

        expireLabel = new Label(container, SWT.LEFT);
        Composite expireContainer = new Composite(parent, SWT.NONE);
        expireContainer.setLayout(new GridLayout(2, true));
        expireTimeTextField = new Text(expireContainer, SWT.LEFT | SWT.BORDER);
        expireTimeTextField.setText("7");
        gridData = new GridData();
        gridData.widthHint = 100;
        expireTimeTextField.setLayoutData(gridData);
        unitComboBox = new Combo(expireContainer, SWT.READ_ONLY);
        unitComboBox.setItems(PERIODS);
        unitComboBox.select(0);

        return super.createContents(parent);
    }

    @Override
    protected void okPressed() {
        int expireUnitFactor = 1;
        int maxSeconds = 60 * 60 * 24 * 7;

        switch (unitComboBox.getSelectionIndex()) {
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

        final int expireSeconds = expireUnitFactor * Integer.parseInt(expireTimeTextField.getText());
        final String message = messageTextArea.getText();

        if (expireSeconds > maxSeconds) {
            MessageDialog.openInformation(null,
                    "The specified message time span exceeds the maximum allowed by the storage client.",
                    "Service Explorer");
            return;
        }
        DefaultLoader.getIdeHelper().runInBackground(null, "Adding queue message", false, true, "Adding queue message", new Runnable() {
            public void run() {
        /*        try {
                    QueueMessage queueMessage = new QueueMessage(
                            "",
                            queue.getName(),
                            message,
                            new GregorianCalendar(),
                            new GregorianCalendar(),
                            0);

                    StorageClientSDKManager.getManager().createQueueMessage(storageAccount, queueMessage, expireSeconds);

                    if (onAddedMessage != null) {
                        DefaultLoader.getIdeHelper().invokeLater(onAddedMessage);
                    }
                } catch (AzureCmdException e) {
                    DefaultLoader.getUIHelper().showException("Error adding queue message", e, "Service Explorer", false, true);
                }*/
            }
        });
        super.okPressed();
    }

    public void setOnAddedMessage(Runnable onAddedMessage) {
        this.onAddedMessage = onAddedMessage;
    }
}
