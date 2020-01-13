/**
 * Copyright (c) Microsoft Corporation
 * <p/>
 * All rights reserved.
 * <p/>
 * MIT License
 * <p/>
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated
 * documentation files (the "Software"), to deal in the Software without restriction, including without limitation
 * the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and
 * to permit persons to whom the Software is furnished to do so, subject to the following conditions:
 * <p/>
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of
 * the Software.
 * <p/>
 * THE SOFTWARE IS PROVIDED *AS IS*, WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO
 * THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT,
 * TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package com.microsoft.intellij.forms;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.openapi.ui.ValidationInfo;
import com.microsoft.intellij.helpers.LinkListener;
import com.microsoft.intellij.ui.components.AzureDialogWrapper;
import com.microsoft.tooling.msservices.model.storage.ClientStorageAccount;
//import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.util.ArrayList;

public class ExternalStorageAccountForm extends AzureDialogWrapper {
    public static final String HTTP = "http";
    public static final String HTTPS = "https";
    private JPanel contentPane;
    private JLabel privacyLink;
    private JTextPane connectionStringTextPane;
    private JTextField accountNameTextField;
    private JTextField accountKeyTextField;
    private JCheckBox rememberAccountKeyCheckBox;
    private JRadioButton useHTTPSRecommendedRadioButton;
    private JRadioButton useHTTPRadioButton;
    private JRadioButton specifyCustomEndpointsRadioButton;
    private JTextField blobURLTextField;
    private JTextField tableURLTextField;
    private JTextField queueURLTextField;
    private JPanel customEndpointsPanel;

    private static final String PRIVACY_LINK = "https://go.microsoft.com/fwlink/?LinkID=286720";
    private Runnable onFinish;

    public ExternalStorageAccountForm(Project project) {
        super(project, true);

        setModal(true);
        privacyLink.addMouseListener(new LinkListener(PRIVACY_LINK));

        ActionListener connectionClick = new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent actionEvent) {
                updateConnectionString();
                customEndpointsPanel.setVisible(specifyCustomEndpointsRadioButton.isSelected());
            }
        };

        useHTTPRadioButton.addActionListener(connectionClick);
        useHTTPSRecommendedRadioButton.addActionListener(connectionClick);
        specifyCustomEndpointsRadioButton.addActionListener(connectionClick);

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

        init();
    }

    private void updateConnectionString() {
        ArrayList<String> connStr = new ArrayList<String>();

        if (specifyCustomEndpointsRadioButton.isSelected()) {
            connStr.add("BlobEndpoint=" + blobURLTextField.getText());
            connStr.add("QueueEndpoint=" + queueURLTextField.getText());
            connStr.add("TableEndpoint=" + tableURLTextField.getText());
        } else {
            connStr.add("DefaultEndpointsProtocol=" + (useHTTPRadioButton.isSelected() ? HTTP : HTTPS));
        }

        connStr.add("AccountName=" + accountNameTextField.getText());
        connStr.add("AccountKey=" + accountKeyTextField.getText());

//        String connectionString = StringUtils.join(connStr, ";");
        String connectionString = connStr + ";";
        connectionStringTextPane.setText(connectionString);
    }

    @Nullable
    @Override
    protected ValidationInfo doValidate() {
        if (accountNameTextField.getText().isEmpty()) {
            return new ValidationInfo("Missing account name.", accountNameTextField);
        }

        if (accountKeyTextField.getText().isEmpty()) {
            return new ValidationInfo("Missing account key.", accountKeyTextField);
        }

        if (specifyCustomEndpointsRadioButton.isSelected()) {
            if (blobURLTextField.getText().isEmpty()
                    || queueURLTextField.getText().isEmpty()
                    || tableURLTextField.getText().isEmpty()) {
                return new ValidationInfo("The connection string requires Blob, Table, and Queue endpoints");
            }
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

        /*try {
            //Validate querystring by making a request
            StorageClientSDKManager.getManager().getTables(StorageClientSDKManager.getManager().getStorageAccount(
                            getFullStorageAccount().getConnectionString()));

        } catch (AzureCmdException e) {
            JOptionPane.showMessageDialog(contentPane,
                    "The storage account contains invalid values. More information:\n" + e.getCause().getMessage(), "Azure Explorer", JOptionPane.ERROR_MESSAGE);
            return;
        }*/

        if (onFinish != null) {
            onFinish.run();
        }

        sendTelemetry(OK_EXIT_CODE);
        close(DialogWrapper.OK_EXIT_CODE, true);
    }

    public void setStorageAccount(ClientStorageAccount storageAccount) {
        accountNameTextField.setText(storageAccount.getName());
        accountKeyTextField.setText(storageAccount.getPrimaryKey());
        specifyCustomEndpointsRadioButton.setSelected(storageAccount.isUseCustomEndpoints());

        if (storageAccount.isUseCustomEndpoints()) {
            blobURLTextField.setText(storageAccount.getBlobsUri());
            tableURLTextField.setText(storageAccount.getTablesUri());
            queueURLTextField.setText(storageAccount.getQueuesUri());

            customEndpointsPanel.setVisible(true);
        } else {
            useHTTPRadioButton.setSelected(storageAccount.getProtocol().equals(HTTP));
            useHTTPSRecommendedRadioButton.setSelected(storageAccount.getProtocol().equals(HTTPS));
        }
        rememberAccountKeyCheckBox.setSelected(!storageAccount.getPrimaryKey().isEmpty());
        accountNameTextField.setEnabled(false);

        updateConnectionString();
    }

    public ClientStorageAccount getStorageAccount() {
        ClientStorageAccount clientStorageAccount = new ClientStorageAccount(accountNameTextField.getText());
        clientStorageAccount.setUseCustomEndpoints(specifyCustomEndpointsRadioButton.isSelected());

        if (rememberAccountKeyCheckBox.isSelected()) {
            clientStorageAccount.setPrimaryKey(accountKeyTextField.getText());
        }

        if (specifyCustomEndpointsRadioButton.isSelected()) {
            clientStorageAccount.setBlobsUri(blobURLTextField.getText());
            clientStorageAccount.setQueuesUri(queueURLTextField.getText());
            clientStorageAccount.setTablesUri(tableURLTextField.getText());
        } else {
            clientStorageAccount.setProtocol(useHTTPRadioButton.isSelected() ? HTTP : HTTPS);
        }

        return clientStorageAccount;
    }

    public ClientStorageAccount getFullStorageAccount() {
        ClientStorageAccount clientStorageAccount = new ClientStorageAccount(accountNameTextField.getText());
        clientStorageAccount.setPrimaryKey(accountKeyTextField.getText());
        clientStorageAccount.setUseCustomEndpoints(specifyCustomEndpointsRadioButton.isSelected());

        if (specifyCustomEndpointsRadioButton.isSelected()) {
            clientStorageAccount.setBlobsUri(blobURLTextField.getText());
            clientStorageAccount.setQueuesUri(queueURLTextField.getText());
            clientStorageAccount.setTablesUri(tableURLTextField.getText());
        } else {
            clientStorageAccount.setProtocol(useHTTPRadioButton.isSelected() ? HTTP : HTTPS);
        }

        return clientStorageAccount;
    }

    public void setOnFinish(Runnable onFinish) {
        this.onFinish = onFinish;
    }

    public String getPrimaryKey() {
        return accountKeyTextField.getText();
    }
}