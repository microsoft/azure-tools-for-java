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

package com.microsoft.azure.hdinsight.serverexplore.ui;

import com.intellij.CommonBundle;
import com.intellij.ide.BrowserUtil;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.ui.DocumentAdapter;
import com.microsoft.azure.hdinsight.common.mvc.SettableControl;
import com.microsoft.azure.hdinsight.serverexplore.AddNewClusterCtrlProvider;
import com.microsoft.azure.hdinsight.serverexplore.AddNewClusterModel;
import com.microsoft.azure.hdinsight.serverexplore.hdinsightnode.HDInsightRootModule;
import com.microsoft.azuretools.azurecommons.helpers.NotNull;
import com.microsoft.azuretools.azurecommons.helpers.Nullable;
import com.microsoft.azuretools.ijidea.ui.HintTextField;
import com.microsoft.azuretools.telemetry.AppInsightsClient;
import com.microsoft.intellij.hdinsight.messages.HDInsightBundle;
import com.microsoft.intellij.rxjava.IdeaSchedulers;
import org.apache.commons.lang3.StringUtils;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.net.URI;
import java.util.Arrays;

public class AddNewClusterForm extends DialogWrapper implements SettableControl<AddNewClusterModel> {
    private JPanel wholePanel;
    private JPanel clusterInfoPanel;
    private JPanel clusterPanel;
    private JPanel authenticationPanel;
    private JPanel comboBoxPanel;
    protected JComboBox clusterComboBox;
    protected JPanel clusterCardsPanel;
    private JPanel hdInsightClusterCard;
    private JTextField clusterNameOrUrlField;
    private JPanel livyServiceCard;
    private JTextField livyEndpointField;
    private JTextField errorMessageField;
    private JPanel authComboBoxPanel;
    protected JComboBox authComboBox;
    private JPanel authCardsPanel;
    private JPanel basicAuthCard;
    private JTextField userNameField;
    private JTextField passwordField;
    private JPanel noAuthCard;
    private JTextField livyClusterNameField;
    private HintTextField yarnEndpointField;
    private JLabel clusterNameLabel;
    private JLabel userNameLabel;
    private JLabel passwordLabel;
    private JLabel livyClusterNameLabel;
    @NotNull
    private HDInsightRootModule hdInsightModule;
    @NotNull
    private AddNewClusterCtrlProvider ctrlProvider;

    private static final String HELP_URL = "https://go.microsoft.com/fwlink/?linkid=866472";

    public AddNewClusterForm(@Nullable final Project project, @NotNull HDInsightRootModule hdInsightModule) {
        super(project, true);
        this.ctrlProvider = new AddNewClusterCtrlProvider(this, new IdeaSchedulers(project));

        myHelpAction = new AddNewClusterForm.HelpAction();

        init();
        this.hdInsightModule = hdInsightModule;

        this.setTitle("Link A Cluster");

        errorMessageField.setBackground(this.wholePanel.getBackground());
        errorMessageField.setBorder(BorderFactory.createEmptyBorder());

        this.setModal(true);

        clusterComboBox.addItemListener(e -> {
            CardLayout layout = (CardLayout) (clusterCardsPanel.getLayout());
            layout.show(clusterCardsPanel, (String) e.getItem());

            // if "HDInsight Cluster" is chose, "Basic Authentication" should be the only one authentication method
            if (isHDInsightClusterSelected()) {
                authComboBox.setSelectedItem(authComboBox.getModel().getElementAt(0));
                authComboBox.setEnabled(false);
            } else {
                authComboBox.setEnabled(true);
            }

        });
        authComboBox.addItemListener(e -> {
            CardLayout layout = (CardLayout) (authCardsPanel.getLayout());
            layout.show(authCardsPanel, (String)e.getItem());
        });

        // field validation check
        Arrays.asList(clusterComboBox, authComboBox).forEach(comp -> comp.addActionListener(event -> basicValidate()));

        Arrays.asList(clusterNameOrUrlField, userNameField, passwordField, livyEndpointField, livyClusterNameField,
                yarnEndpointField).forEach(comp -> comp.getDocument().addDocumentListener(new DocumentAdapter() {
            @Override
            protected void textChanged(DocumentEvent e) {
                basicValidate();
            }
        }));
        getOKAction().setEnabled(false);
    }

    // Data -> Components
    @Override
    public void setData(@NotNull AddNewClusterModel data) {
        errorMessageField.setText(data.getErrorMessage());
    }

    // Components -> Data
    @Override
    public void getData(@NotNull AddNewClusterModel data) {
        if (isHDInsightClusterSelected()) {
            data.setHDInsightClusterSelected(true)
                    .setClusterName(clusterNameOrUrlField.getText().trim())
                    .setUserName(userNameField.getText().trim())
                    .setPassword(passwordField.getText().trim())
                    // TODO: these label title setting is no use other than to be compatible with legacy ctrlprovider code
                    .setClusterNameLabelTitle(clusterNameLabel.getText())
                    .setUserNameLabelTitle(userNameLabel.getText())
                    .setPasswordLabelTitle(passwordLabel.getText());
        } else {
            data.setHDInsightClusterSelected(false)
                    .setLivyEndpoint(URI.create(livyEndpointField.getText().trim()))
                    .setYarnEndpoint(StringUtils.isBlank(yarnEndpointField.getText()) ? null : URI.create(yarnEndpointField.getText().trim()))
                    .setClusterName(livyClusterNameField.getText().trim())
                    // TODO: these label title setting is no use other than to be compatible with legacy ctrlprovider code
                    .setClusterNameLabelTitle(livyClusterNameLabel.getText())
                    .setUserNameLabelTitle(userNameLabel.getText())
                    .setPasswordLabelTitle(passwordLabel.getText());
            if (isBasicAuthSelected()) {
                data.setUserName(userNameField.getText().trim())
                        .setPassword(passwordField.getText().trim());
            }
        }
    }

    @Override
    protected void doOKAction() {
        if (!getOKAction().isEnabled()) {
            return;
        }

        getOKAction().setEnabled(false);

        ctrlProvider
                .validateAndAdd()
                .doOnEach(notification -> getOKAction().setEnabled(true))
                .subscribe(toUpdate -> {
                    hdInsightModule.refreshWithoutAsync();
                    AppInsightsClient.create(HDInsightBundle.message("HDInsightAddNewClusterAction"), null);

                    super.doOKAction();
                });
    }

    private void createUIComponents() {
        clusterNameOrUrlField = new HintTextField("Example: spk22 or https://spk22.azurehdinsight.net");
        livyEndpointField = new HintTextField("Example: http://headnodehost:8998");
        yarnEndpointField = new HintTextField("(Optional)Example: http://hn0-spark2:8088");
    }

    private boolean isHDInsightClusterSelected() {
        return ((String)clusterComboBox.getSelectedItem()).equalsIgnoreCase("HDInsight Cluster");
    }

    private boolean isBasicAuthSelected() {
        return ((String) authComboBox.getSelectedItem()).equalsIgnoreCase("Basic Authentication");
    }

    private void basicValidate() {
        String errorMessage = null;
        if (isHDInsightClusterSelected()) {
            if (StringUtils.isBlank(clusterNameOrUrlField.getText())) {
                errorMessage = "Cluster name can't be empty";
            } else {
                String clusterName = ctrlProvider.getClusterName(clusterNameOrUrlField.getText());
                if (clusterName == null) {
                    errorMessage = "Cluster URL is not a valid URL";
                } else if (ctrlProvider.doesClusterNameExistInLinkedClusters(clusterName)) {
                    errorMessage = "Cluster already exists in linked clusters";
                }
            }
        } else {
            if (StringUtils.isBlank(livyEndpointField.getText()) ||
                    StringUtils.isBlank(livyClusterNameField.getText())) {
                errorMessage = "Livy Endpoint and cluster name can't be empty";
            } else if (!ctrlProvider.isURLValid(livyEndpointField.getText())) {
                errorMessage = "Livy Endpoint is not a valid URL";
            } else if (ctrlProvider.doesClusterLivyEndpointExistInAllClusters(livyEndpointField.getText())) {
                errorMessage = "The same name Livy Endpoint already exists in clusters";
            } else if (ctrlProvider.doesClusterNameExistInAllClusters(livyClusterNameField.getText())) {
                errorMessage = "Cluster Name already exists in clusters";
            } else if (!StringUtils.isEmpty(yarnEndpointField.getText()) &&
                    !ctrlProvider.isURLValid(yarnEndpointField.getText())) {
                errorMessage = "Yarn Endpoint is not a valid URL";
            }
        }

        if (errorMessage == null && isBasicAuthSelected()) {
            if (StringUtils.isBlank(userNameField.getText()) || StringUtils.isBlank(passwordField.getText())) {
                errorMessage = "Username and password can't be empty in Basic Authentication";
            }
        }

        errorMessageField.setText(errorMessage);
        getOKAction().setEnabled(StringUtils.isEmpty(errorMessageField.getText()));
    }

    private class HelpAction extends AbstractAction {
        private HelpAction() {
            this.putValue("Name", CommonBundle.getHelpButtonText());
        }

        public void actionPerformed(ActionEvent e) {
            AddNewClusterForm.this.doHelpAction();
        }
    }

    @Override
    protected void doHelpAction() {
        BrowserUtil.browse(HELP_URL);
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
        return wholePanel;
    }
}
