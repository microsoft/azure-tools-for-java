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
import com.microsoft.azuretools.azureexplorer.Activator;
import com.microsoft.azuretools.core.components.AzureDialogWrapper;
import com.microsoft.tooling.msservices.helpers.azure.sdk.StorageClientSDKManager;
import com.microsoft.tooling.msservices.model.storage.ClientStorageAccount;
import org.apache.commons.lang3.StringUtils;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.*;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.*;
import org.eclipse.ui.PlatformUI;

import java.net.URL;
import java.util.ArrayList;

public class ExternalStorageAccountForm extends AzureDialogWrapper {
    public static final String HTTP = "http";
    public static final String HTTPS = "https";

    private Link privacyLink;
    private Label headerNote;
    private Label accountNameLabel;
    private Text accountNameTextField;
    private Label accountKeyLabel;
    private Text accountKeyTextField;
    private Button rememberAccountKeyCheckBox;
    private Label connectionLabel;
    private Button useHTTPSRecommendedRadioButton;
    private Button useHTTPRadioButton;
    private Button specifyCustomEndpointsRadioButton;
    private Label blobURLLabel;
    private Text blobURLTextField;
    private Label tableURLLabel;
    private Text tableURLTextField;
    private Label queueURLLabel;
    private Text queueURLTextField;
    private Composite customEndpointsPanel;
//    private ScrolledComposite sc;
//    private Label connectionStringLabel;
//    private Label connectionStringTextPane;

    private static final String PRIVACY_LINK = "<a href=\"http://go.microsoft.com/fwlink/?LinkID=286720\">Online privacy statement</a>";
    private Runnable onFinish;
    private String title;

    private ClientStorageAccount storageAccount;
    private ClientStorageAccount fullStorageAccount;

    public ExternalStorageAccountForm(Shell parentShell, String title) {
        super(parentShell);
        this.title = title;
    }

    @Override
    protected void configureShell(Shell shell) {
        super.configureShell(shell);
        shell.setText(title);
    }

    @Override
    protected Control createContents(Composite parent) {
        Composite container = new Composite(parent, SWT.NONE);
        GridLayout gridLayout = new GridLayout();
        container.setLayout(gridLayout);
        GridData gridData = new GridData();
        gridData.widthHint = 450;
        container.setLayoutData(gridData);

        headerNote = new Label(container, SWT.LEFT);
        headerNote.setText("Enter information to connect to the Microsoft Azure storage account.");

        accountNameLabel = new Label(container, SWT.LEFT);
        accountNameLabel.setText("Account name:");
        accountNameTextField = new Text(container, SWT.LEFT | SWT.BORDER);
        gridData = new GridData(SWT.FILL, SWT.CENTER, true, true);
        accountNameTextField.setLayoutData(gridData);

        accountKeyLabel = new Label(container, SWT.LEFT);
        accountKeyLabel.setText("Account key:");
        gridData = new GridData(SWT.FILL, SWT.CENTER, true, true);
        accountKeyTextField = new Text(container, SWT.LEFT | SWT.BORDER);
        accountKeyTextField.setLayoutData(gridData);

        rememberAccountKeyCheckBox = new Button(container, SWT.CHECK);
        rememberAccountKeyCheckBox.setText("Remember account key");
        rememberAccountKeyCheckBox.setSelection(true);

        connectionLabel = new Label(container, SWT.LEFT);
        connectionLabel.setText("Connection:");
        useHTTPSRecommendedRadioButton = new Button(container, SWT.RADIO);
        useHTTPSRecommendedRadioButton.setText("Use HTTPS (Recommended)");
        useHTTPSRecommendedRadioButton.setSelection(true);
        useHTTPRadioButton = new Button(container, SWT.RADIO);
        useHTTPRadioButton.setText("Use HTTP");
        specifyCustomEndpointsRadioButton = new Button(container, SWT.RADIO);
        specifyCustomEndpointsRadioButton.setText("Specify custom endpoints");

        customEndpointsPanel = createEndpointsPanel(container);

        privacyLink = new Link(container, SWT.LEFT);
        privacyLink.setText(PRIVACY_LINK);
        privacyLink.setLayoutData(gridData);
        privacyLink.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent event) {
                try {
                    PlatformUI.getWorkbench().getBrowserSupport().getExternalBrowser().openURL(new URL(event.text));
                } catch (Exception ex) {
                    /*
                     * only logging the error in log file
                     * not showing anything to end user
                     */
                    Activator.getDefault().log("Error occurred while opening link in default browser.", ex);
                }
            }
        });

//        connectionStringLabel = new Label(container, SWT.LEFT);
//        connectionLabel.setText("Preview connection string:");
//        sc = new ScrolledComposite(container, SWT.H_SCROLL | SWT.V_SCROLL | SWT.BORDER);
//        gridData = new GridData();
//        gridData.horizontalAlignment = SWT.CENTER;
//        sc.setLayoutData(gridData);
//        connectionStringTextPane = new Label(sc, SWT.LEFT | SWT.BORDER);
//        gridData = new GridData(SWT.FILL, SWT.FILL, true, true);
//        connectionStringTextPane.setLayoutData(gridData);
//        sc.setContent(connectionStringTextPane);
//        sc.setMinSize(400, 200);
//
//        sc.setExpandHorizontal(true);
//        sc.setExpandVertical(true);

        SelectionListener connectionClick = new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                updateConnectionString();
                customEndpointsPanel.setVisible(specifyCustomEndpointsRadioButton.getSelection());
            }
        };

        useHTTPRadioButton.addSelectionListener(connectionClick);
        useHTTPSRecommendedRadioButton.addSelectionListener(connectionClick);
        specifyCustomEndpointsRadioButton.addSelectionListener(connectionClick);

        FocusListener focusListener = new FocusListener() {
            @Override
            public void focusGained(FocusEvent focusEvent) {
            }

            @Override
            public void focusLost(FocusEvent focusEvent) {
                updateConnectionString();
            }
        };

        accountNameTextField.addFocusListener(focusListener);
        accountKeyTextField.addFocusListener(focusListener);
        rememberAccountKeyCheckBox.addFocusListener(focusListener);
        blobURLTextField.addFocusListener(focusListener);
        tableURLTextField.addFocusListener(focusListener);
        queueURLTextField.addFocusListener(focusListener);

        return super.createContents(parent);
    }

    private Composite createEndpointsPanel(Composite parent) {
        Composite container = new Composite(parent, SWT.NONE);
        GridLayout gridLayout = new GridLayout(2, false);
        container.setLayout(gridLayout);
        GridData gridData = new GridData(SWT.FILL, SWT.CENTER, true, false);
        gridData.horizontalIndent = 10;
        container.setLayoutData(gridData);

        blobURLLabel = new Label(container, SWT.LEFT);
        blobURLLabel.setText("Blob URL:");
        gridData = new GridData(SWT.FILL, SWT.CENTER, true, false);
        blobURLTextField = new Text(container, SWT.BORDER);
        blobURLTextField.setLayoutData(gridData);

        tableURLLabel = new Label(container, SWT.LEFT);
        tableURLLabel.setText("Table URL:");
        gridData = new GridData(SWT.FILL, SWT.CENTER, true, false);
        tableURLTextField = new Text(container, SWT.BORDER);
        tableURLTextField.setLayoutData(gridData);

        queueURLLabel = new Label(container, SWT.LEFT);
        queueURLLabel.setText("Queue URL:");
        gridData = new GridData(SWT.FILL, SWT.CENTER, true, false);
        queueURLTextField = new Text(container, SWT.BORDER);
        queueURLTextField.setLayoutData(gridData);

        container.setVisible(false);
        return container;
    }

    private void updateConnectionString() {
        ArrayList<String> connStr = new ArrayList<String>();

        if (specifyCustomEndpointsRadioButton.getSelection()) {
            connStr.add("BlobEndpoint=" + blobURLTextField.getText());
            connStr.add("QueueEndpoint=" + queueURLTextField.getText());
            connStr.add("TableEndpoint=" + tableURLTextField.getText());
        } else {
            connStr.add("DefaultEndpointsProtocol=" + (useHTTPRadioButton.getSelection() ? HTTP : HTTPS));
        }

        connStr.add("AccountName=" + accountNameTextField.getText());
        connStr.add("AccountKey=" + accountKeyTextField.getText());

        String connectionString = StringUtils.join(connStr, ";");
//        connectionStringTextPane.setText(connectionString);
//        sc.setRedraw(true);
    }

    @Override
    protected void okPressed() {
        String errors = "";

        if (accountNameTextField.getText().isEmpty()) {
            errors = errors + " - Missing account name.\n";
        }

        if (accountKeyTextField.getText().isEmpty()) {
            errors = errors + " - Missing account key.\n";
        }

        if (specifyCustomEndpointsRadioButton.getSelection()) {
            if (blobURLTextField.getText().isEmpty()
                    || queueURLTextField.getText().isEmpty()
                    || tableURLTextField.getText().isEmpty()) {
                errors = errors + " - The connection string requires Blob, Table, and Queue endpoints.\n";
            }
        }

        if (!errors.isEmpty()) {
            DefaultLoader.getUIHelper().showError(errors, "Azure Explorer");
            return;
        }
        populateStorageAccount();
        populateFullStorageAccount();
//        try {
            //Validate querystring by making a request
            // TODO
//            StorageClientSDKManager.getManager().getgetTables(StorageClientSDKManager.getManager().getStorageAccount(getFullStorageAccount().getConnectionString()));

//        } catch (AzureCmdException e) {
//            DefaultLoader.getUIHelper().showError(
//                    "The storage account contains invalid values. More information:\n" + e.getCause().getMessage(), "Azure Explorer");
//            return;
//        }
        if (onFinish != null) {
            onFinish.run();
        }

        super.okPressed();
    }

    public void setStorageAccount(ClientStorageAccount storageAccount) {
        accountNameTextField.setText(storageAccount.getName());
        accountKeyTextField.setText(storageAccount.getPrimaryKey());
        specifyCustomEndpointsRadioButton.setSelection(storageAccount.isUseCustomEndpoints());

        if (storageAccount.isUseCustomEndpoints()) {
            blobURLTextField.setText(storageAccount.getBlobsUri());
            tableURLTextField.setText(storageAccount.getTablesUri());
            queueURLTextField.setText(storageAccount.getQueuesUri());

            customEndpointsPanel.setVisible(true);
        } else {
            useHTTPRadioButton.setSelection(storageAccount.getProtocol().equals(HTTP));
            useHTTPSRecommendedRadioButton.setSelection(storageAccount.getProtocol().equals(HTTPS));
        }
        rememberAccountKeyCheckBox.setSelection(!storageAccount.getPrimaryKey().isEmpty());
        accountNameTextField.setEnabled(false);

        updateConnectionString();
    }

    public void populateStorageAccount() {
        ClientStorageAccount clientStorageAccount = new ClientStorageAccount(accountNameTextField.getText());
        clientStorageAccount.setUseCustomEndpoints(specifyCustomEndpointsRadioButton.getSelection());

        if (rememberAccountKeyCheckBox.getSelection()) {
            clientStorageAccount.setPrimaryKey(accountKeyTextField.getText());
        }

        if (specifyCustomEndpointsRadioButton.getSelection()) {
            clientStorageAccount.setBlobsUri(blobURLTextField.getText());
            clientStorageAccount.setQueuesUri(queueURLTextField.getText());
            clientStorageAccount.setTablesUri(tableURLTextField.getText());
        } else {
            clientStorageAccount.setProtocol(useHTTPRadioButton.getSelection() ? HTTP : HTTPS);
        }

        storageAccount = clientStorageAccount;
    }

    public void populateFullStorageAccount() {
        ClientStorageAccount clientStorageAccount = new ClientStorageAccount(accountNameTextField.getText());
        clientStorageAccount.setPrimaryKey(accountKeyTextField.getText());
        clientStorageAccount.setUseCustomEndpoints(specifyCustomEndpointsRadioButton.getSelection());

        if (specifyCustomEndpointsRadioButton.getSelection()) {
            clientStorageAccount.setBlobsUri(blobURLTextField.getText());
            clientStorageAccount.setQueuesUri(queueURLTextField.getText());
            clientStorageAccount.setTablesUri(tableURLTextField.getText());
        } else {
            clientStorageAccount.setProtocol(useHTTPRadioButton.getSelection() ? HTTP : HTTPS);
        }

        fullStorageAccount = clientStorageAccount;
    }

    public void setOnFinish(Runnable onFinish) {
        this.onFinish = onFinish;
    }

    public String getPrimaryKey() {
        return accountKeyTextField.getText();
    }

    public ClientStorageAccount getStorageAccount() {
        return storageAccount;
    }

    public ClientStorageAccount getFullStorageAccount() {
        return fullStorageAccount;
    }
}
