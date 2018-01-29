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

import com.intellij.CommonBundle;
import com.intellij.ide.BrowserUtil;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.ModalityState;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.openapi.ui.popup.IconButton;
import com.microsoft.azure.hdinsight.common.ClusterManagerEx;
import com.microsoft.azure.hdinsight.sdk.cluster.HDInsightAdditionalClusterDetail;
import com.microsoft.azure.hdinsight.sdk.common.AuthenticationException;
import com.microsoft.azure.hdinsight.sdk.common.HDIException;
import com.microsoft.azure.hdinsight.sdk.storage.HDStorageAccount;
import com.microsoft.azure.hdinsight.serverexplore.AddHDInsightAdditionalClusterImpl;
import com.microsoft.azure.hdinsight.serverexplore.hdinsightnode.HDInsightRootModule;
import com.microsoft.azure.hdinsight.spark.jobs.JobUtils;
import com.microsoft.azuretools.azurecommons.helpers.AzureCmdException;
import com.microsoft.azuretools.azurecommons.helpers.StringHelper;
import com.microsoft.azuretools.telemetry.AppInsightsClient;
import com.microsoft.intellij.hdinsight.messages.HDInsightBundle;
import com.microsoft.tooling.msservices.helpers.azure.sdk.StorageClientSDKManager;
import com.microsoft.tooling.msservices.model.storage.BlobContainer;
import com.microsoft.tooling.msservices.model.storage.ClientStorageAccount;
import org.apache.commons.lang.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.util.Optional;
import java.util.stream.Stream;

public class AddNewClusterFrom extends DialogWrapper {

    private Project project;
    private String clusterName;
    private String userName;
    private String password;

    private String storageName;
    private String storageKey;
    private String storageContainer;

    private HDStorageAccount storageAccount;

    private String errorMessage;
    private boolean isCarryOnNextStep;

    private JPanel addNewClusterPanel;
    private JTextField clusterNameFiled;
    private JTextField userNameField;
    private JPasswordField passwordField;
    private JTextField errorMessageField;
    private JTextField storageNameField;
    private JTextArea storageKeyTextField;
    private JLabel clusterNameLabel;
    private JLabel storageNameLabel;
    private JLabel storageKeyLabel;
    private JLabel userNameLabel;
    private JLabel passwordLabel;
    private JComboBox<BlobContainer> containersComboBox;
    private JLabel storageContainerLabel;

    private HDInsightRootModule hdInsightModule;

    private static final String URL_PREFIX = "https://";
    private static final String HELP_URL = "https://go.microsoft.com/fwlink/?linkid=866472";

    public AddNewClusterFrom(final Project project, HDInsightRootModule hdInsightModule) {
        super(project, true);
        myHelpAction = new HelpAction();

        init();
        this.project = project;
        this.hdInsightModule = hdInsightModule;

        this.setTitle("Link A New HDInsight Cluster");

        errorMessageField.setBackground(this.addNewClusterPanel.getBackground());
        errorMessageField.setBorder(BorderFactory.createEmptyBorder());

        this.setModal(true);

        storageKeyTextField.addFocusListener(new FocusAdapter() {
            @Override
            public void focusLost(FocusEvent e) {
                super.focusLost(e);

                if (StringUtils.isNotBlank(storageNameField.getText()) && StringUtils.isNotBlank(storageKeyTextField.getText())) {
                    ClientStorageAccount storageAccount = new ClientStorageAccount(storageNameField.getText());
                    storageAccount.setPrimaryKey(storageKeyTextField.getText());

                    refreshContainers(storageAccount);
                }
            }
        });

        storageNameField.addFocusListener(new FocusAdapter() {
            @Override
            public void focusLost(FocusEvent e) {
                super.focusLost(e);

                if (StringUtils.isNotBlank(storageNameField.getText()) && StringUtils.isNotBlank(storageKeyTextField.getText())) {
                    ClientStorageAccount storageAccount = new ClientStorageAccount(storageNameField.getText());
                    storageAccount.setPrimaryKey(storageKeyTextField.getText());

                    refreshContainers(storageAccount);
                }
            }
        });
    }

    private void refreshContainers(@NotNull ClientStorageAccount storageAccount) {
        try {
            containersComboBox.removeAllItems();

            StorageClientSDKManager.getManager().getBlobContainers(storageAccount.getConnectionString())
                    .forEach(containersComboBox::addItem);

            containersComboBox.setMaximumRowCount(6);
        } catch (AzureCmdException e) {
            containersComboBox.removeAllItems();
        }
    }

    private class HelpAction extends AbstractAction {
        private HelpAction() {
            this.putValue("Name", CommonBundle.getHelpButtonText());
        }

        public void actionPerformed(ActionEvent e) {
            AddNewClusterFrom.this.doHelpAction();
        }
    }

    @Override
    protected void doHelpAction() {
        BrowserUtil.browse(HELP_URL);
    }

    @Override
    protected void doOKAction() {
        synchronized (AddNewClusterFrom.class) {
            isCarryOnNextStep = true;
            errorMessage = null;
            errorMessageField.setVisible(false);

            String clusterNameOrUrl = clusterNameFiled.getText().trim();
            userName = userNameField.getText().trim();
            storageName = storageNameField.getText().trim();

            storageKey = storageKeyTextField.getText().trim();

            password = String.valueOf(passwordField.getPassword());

            AppInsightsClient.create(HDInsightBundle.message("HDInsightAddNewClusterAction"), null);

            if (StringHelper.isNullOrWhiteSpace(clusterNameOrUrl) || StringHelper.isNullOrWhiteSpace(storageName) || StringHelper.isNullOrWhiteSpace(storageKey) || StringHelper.isNullOrWhiteSpace(userName) || StringHelper.isNullOrWhiteSpace(password)) {
                Stream<JLabel> highLightLabels = Stream.of(
                        clusterNameLabel,
                        storageNameLabel,
                        storageKeyLabel,
                        storageContainerLabel,
                        userNameLabel,
                        passwordLabel);

                String highlightPrefix = "* ";
                highLightLabels.filter(label -> !label.getText().startsWith(highlightPrefix))
                               .forEach(label -> label.setText(highlightPrefix + label.getText()));

                errorMessage = "All (*) fields are required.";
                isCarryOnNextStep = false;
            } else {
                clusterName = getClusterName(clusterNameOrUrl);

                if (clusterName == null) {
                    errorMessage = "Wrong cluster name or endpoint";
                    isCarryOnNextStep = false;
                } else {
                    int status = ClusterManagerEx.getInstance().isHDInsightAdditionalStorageExist(clusterName, storageName);
                    if(status == 1) {
                        errorMessage = "Cluster already exists in linked list";
                        isCarryOnNextStep = false;
                    } else if(status == 2) {
                        errorMessage = "Default storage account is required";
                        isCarryOnNextStep = false;
                    }
                }

                if (containersComboBox.getSelectedItem() == null) {
                    errorMessage = "The storage container isn't selected";
                    isCarryOnNextStep = false;
                } else {
                    storageContainer = ((BlobContainer) containersComboBox.getSelectedItem()).getName();
                }
            }

            if (isCarryOnNextStep) {
                getStorageAccount();

                if (storageAccount == null) {
                    isCarryOnNextStep = false;
                } else {
                    HDInsightAdditionalClusterDetail hdInsightAdditionalClusterDetail = new HDInsightAdditionalClusterDetail(clusterName, userName, password, storageAccount);
                    try {
                        JobUtils.authenticate(hdInsightAdditionalClusterDetail);

                        ClusterManagerEx.getInstance().addHDInsightAdditionalCluster(hdInsightAdditionalClusterDetail);
                        hdInsightModule.refreshWithoutAsync();
                    } catch (Exception ignore) {
                        isCarryOnNextStep = false;
                        errorMessage = "Wrong username/password to log in";
                    }
                }
            }

            if (isCarryOnNextStep) {
                super.doOKAction();
            } else {
                errorMessageField.setText(errorMessage);
                errorMessageField.setVisible(true);
            }
        }
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

        ApplicationManager.getApplication().invokeAndWait(() -> {
            storageAccount = new HDStorageAccount(
                    null, ClusterManagerEx.getInstance().getBlobFullName(storageName), storageKey, false, storageContainer);
            isCarryOnNextStep = true;
        }, ModalityState.NON_MODAL);

        addNewClusterPanel.setCursor(Cursor.getDefaultCursor());
    }

    @NotNull
    @Override
    protected Action[] createActions() {
        return new Action[] { getOKAction(), getCancelAction(), getHelpAction() };
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


