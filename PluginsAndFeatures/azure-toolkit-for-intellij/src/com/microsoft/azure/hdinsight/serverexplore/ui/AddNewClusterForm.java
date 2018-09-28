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
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.net.URI;
import java.util.Arrays;

public class AddNewClusterForm extends DialogWrapper implements SettableControl<AddNewClusterModel> {
    private JPanel wholePanel;
    private JPanel clusterInfoPanel;
    private JPanel clusterPanel;
    private JPanel authenticationPanel;
    private JPanel comboBoxPanel;
    private JComboBox clusterComboBox;
    private JPanel clusterCardsPanel;
    private JPanel hdInsightClusterCard;
    private JTextField clusterNameOrUrlField;
    private JPanel livyServiceCard;
    private JTextField livyEndpointField;
    private JTextField errorMessageField;
    private JPanel authComboBoxPanel;
    private JComboBox authComboBox;
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

        this.setTitle("Link A New HDInsight Cluster");

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
        Arrays.asList(clusterComboBox, clusterNameOrUrlField, userNameField, passwordField, livyEndpointField,
                livyClusterNameField, yarnEndpointField).forEach(comp -> comp.addFocusListener(new FocusAdapter() {
            @Override
            public void focusLost(FocusEvent e) {
                if (isHDInsightClusterSelected()) {
                    if (StringUtils.isEmpty(clusterNameOrUrlField.getText()) ||
                            StringUtils.isEmpty(userNameField.getText()) ||
                            StringUtils.isEmpty(passwordField.getText())) {
                        getOKAction().setEnabled(false);
                    } else {
                        // cluster name or URL validation
                        String clusterName = ctrlProvider.getClusterName(clusterNameOrUrlField.getText());
                        if (clusterName == null) {
                            errorMessageField.setText("cluster URL is invalid");
                            getOKAction().setEnabled(false);
                        } else if (ctrlProvider.doesClusterNameExist(clusterName)) {
                            errorMessageField.setText("Cluster already exists in linked clusters");
                            getOKAction().setEnabled(false);
                        } else {
                            errorMessageField.setText(null);
                            getOKAction().setEnabled(true);
                        }
                    }
                } else {
                    if (StringUtils.isEmpty(livyEndpointField.getText()) ||
                            StringUtils.isEmpty(livyClusterNameField.getText())) {
                        getOKAction().setEnabled(false);
                    } else if (!ctrlProvider.isURLValid(livyEndpointField.getText())) {
                        errorMessageField.setText("Livy Endpoint is not a valid URL");
                        getOKAction().setEnabled(false);
                    } else if (ctrlProvider.doesClusterLivyEndpointExist(livyEndpointField.getText())) {
                        errorMessageField.setText("Livy Endpoint already exists in linked clusters");
                        getOKAction().setEnabled(false);
                    } else if (ctrlProvider.doesClusterNameExist(livyClusterNameField.getText())) {
                        errorMessageField.setText("Cluster Name already exists in linked clusters");
                        getOKAction().setEnabled(false);
                    } else if (!StringUtils.isEmpty(yarnEndpointField.getText()) &&
                            !ctrlProvider.isURLValid(yarnEndpointField.getText())) {
                        errorMessageField.setText("Yarn Endpoint is not a valid URL");
                        getOKAction().setEnabled(false);
                    } else {
                        errorMessageField.setText(null);
                        getOKAction().setEnabled(true);
                    }
                }
                super.focusLost(e);
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
                    .setClusterName(clusterNameOrUrlField.getText())
                    .setUserName(userNameField.getText())
                    .setPassword(passwordField.getText())
                    // TODO: these label title setting is no use other than to be compatible with legacy ctrlprovider code
                    .setClusterNameLabelTitle(clusterNameLabel.getText())
                    .setUserNameLabelTitle(userNameLabel.getText())
                    .setPasswordLabelTitle(passwordLabel.getText());
        } else {
            data.setHDInsightClusterSelected(false)
                    .setLivyEndpoint(URI.create(livyEndpointField.getText()))
                    .setYarnEndpoint(StringUtils.isEmpty(yarnEndpointField.getText()) ? null : URI.create(yarnEndpointField.getText()))
                    .setClusterName(livyClusterNameField.getText())
                    .setUserName(userNameField.getText())
                    .setPassword(passwordField.getText())
                    // TODO: these label title setting is no use other than to be compatible with legacy ctrlprovider code
                    .setClusterNameLabelTitle(livyClusterNameLabel.getText())
                    .setUserNameLabelTitle(userNameLabel.getText())
                    .setPasswordLabelTitle(passwordLabel.getText());
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
