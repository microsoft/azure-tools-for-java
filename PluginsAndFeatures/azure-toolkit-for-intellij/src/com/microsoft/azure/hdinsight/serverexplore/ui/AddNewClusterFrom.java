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
package com.microsoft.azure.hdinsight.serverexplore.ui;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.ModalityState;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.DialogWrapper;
import com.microsoft.azure.hdinsight.common.ClusterManagerEx;
import com.microsoft.azure.hdinsight.sdk.cluster.HDInsightAdditionalClusterDetail;
import com.microsoft.azure.hdinsight.sdk.common.HDIException;
import com.microsoft.azure.hdinsight.sdk.storage.HDStorageAccount;
import com.microsoft.azure.hdinsight.serverexplore.AddHDInsightAdditionalClusterImpl;
import com.microsoft.azure.hdinsight.serverexplore.hdinsightnode.HDInsightRootModule;
import com.microsoft.azuretools.azurecommons.helpers.AzureCmdException;
import com.microsoft.azuretools.azurecommons.helpers.StringHelper;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

public class AddNewClusterFrom extends DialogWrapper {

    private Project project;
    private String clusterName;
    private String userName;
    private String password;

    private String storageName;
    private String storageKey;

    private HDStorageAccount storageAccount;

    private String errorMessage;
    private boolean isCarryOnNextStep;

    private JPanel addNewClusterPanel;
    private JTextField clusterNameFiled;
    private JTextField userNameField;
    private JPasswordField passwordField;
    private JTextField errorMessageField;
    private JPanel buttonPanel;
    private JButton Okbutton;
    private JButton cancelButton;
    private JTextField storageNameField;
    private JTextField storageKeyTextField;

    private HDInsightRootModule hdInsightModule;

    private static final String URL_PREFIX = "https://";

    public AddNewClusterFrom(final Project project, HDInsightRootModule hdInsightModule) {
        super(project, true);
        init();
        this.project = project;
        this.hdInsightModule = hdInsightModule;

        this.setTitle("Link A New HDInsight Cluster");

        errorMessageField.setBackground(this.addNewClusterPanel.getBackground());
        errorMessageField.setBorder(BorderFactory.createEmptyBorder());

        this.setModal(true);

        addActionListener();
    }

    private void addActionListener() {
        Okbutton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                synchronized (AddNewClusterFrom.class) {
                    isCarryOnNextStep = true;
                    errorMessage = null;
                    errorMessageField.setVisible(false);

                    String clusterNameOrUrl = clusterNameFiled.getText().trim();
                    userName = userNameField.getText().trim();
                    storageName = storageNameField.getText().trim();

                    storageKey = storageKeyTextField.getText().trim();

                    password = String.valueOf(passwordField.getPassword());

                    if (StringHelper.isNullOrWhiteSpace(clusterNameOrUrl) || StringHelper.isNullOrWhiteSpace(storageName) || StringHelper.isNullOrWhiteSpace(storageKey) || StringHelper.isNullOrWhiteSpace(userName) || StringHelper.isNullOrWhiteSpace(password)) {
                        errorMessage = "Cluster Name, Storage Key, User Name, or Password shouldn't be empty";
                        isCarryOnNextStep = false;
                    } else {
                        clusterName = getClusterName(clusterNameOrUrl);

                        if (clusterName == null) {
                            errorMessage = "Wrong cluster name or endpoint";
                            isCarryOnNextStep = false;
                        } else {
                            int status = ClusterManagerEx.getInstance().isHDInsightAdditionalStorageExist(clusterName, storageName);
                            if(status == 1) {
                                errorMessage = "Cluster already exist in current list";
                                isCarryOnNextStep = false;
                            } else if(status == 2) {
                                errorMessage = "Default storage account is required";
                                isCarryOnNextStep = false;
                            }
                        }
                    }

                    if (isCarryOnNextStep) {
                        getStorageAccount();
                    }

                    if (isCarryOnNextStep) {
                        if (storageAccount != null) {
                            HDInsightAdditionalClusterDetail hdInsightAdditionalClusterDetail = new HDInsightAdditionalClusterDetail(clusterName, userName, password, storageAccount);
                            ClusterManagerEx.getInstance().addHDInsightAdditionalCluster(hdInsightAdditionalClusterDetail);
                            hdInsightModule.refreshWithoutAsync();
                        }
                        close(DialogWrapper.OK_EXIT_CODE, true);
                    } else {
                        errorMessageField.setText(errorMessage);
                        errorMessageField.setVisible(true);
                    }
                }
            }
        });

        cancelButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                close(DialogWrapper.CANCEL_EXIT_CODE, true);
            }
        });
    }

    //format input string
    private static String getClusterName(String userNameOrUrl) {
        if (userNameOrUrl.startsWith(URL_PREFIX)) {
            return StringHelper.getClusterNameFromEndPoint(userNameOrUrl);
        } else {
            return userNameOrUrl;
        }
    }

    private void getStorageAccount() {
        addNewClusterPanel.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));

        ApplicationManager.getApplication().invokeAndWait(new Runnable() {
            @Override
            public void run() {
                try {
                    storageAccount = AddHDInsightAdditionalClusterImpl.getStorageAccount(clusterName, storageName, storageKey, userName, password);
                    isCarryOnNextStep = true;
                } catch (AzureCmdException | HDIException e) {
                    isCarryOnNextStep = false;
                    errorMessage = e.getMessage();
                }
            }
        }, ModalityState.NON_MODAL);

        addNewClusterPanel.setCursor(Cursor.getDefaultCursor());
    }

    @NotNull
    @Override
    protected Action[] createActions() {
        return new Action[0];
    }

    @NotNull
    @Override
    protected Action[] createLeftSideActions() {
        return new Action[0];
    }

    @Nullable
    @Override
    protected JComponent createCenterPanel() {
        return addNewClusterPanel;
    }
}


