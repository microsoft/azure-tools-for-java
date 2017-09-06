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

package com.microsoft.azure.hdinsight.spark.ui;

import com.intellij.icons.AllIcons;
import com.intellij.ide.BrowserUtil;
import com.intellij.openapi.fileChooser.FileChooser;
import com.intellij.openapi.fileChooser.FileChooserDescriptor;
import com.intellij.openapi.ui.TextFieldWithBrowseButton;
import com.intellij.openapi.ui.popup.IconButton;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.ui.DocumentAdapter;
import com.intellij.ui.InplaceButton;
import com.intellij.util.ui.JBUI;
import com.microsoft.azure.hdinsight.common.CallBack;
import com.microsoft.azure.hdinsight.common.ClusterManagerEx;
import com.microsoft.azure.hdinsight.common.Docs;
import com.microsoft.azure.hdinsight.sdk.cluster.IClusterDetail;
import com.microsoft.azure.hdinsight.sdk.common.HDIException;
import com.microsoft.azure.hdinsight.spark.common.SparkBatchDebugSession;
import com.microsoft.azure.hdinsight.spark.common.SparkSubmitAdvancedConfigModel;
import com.microsoft.azure.hdinsight.spark.common.SparkSubmitModel;
import com.microsoft.azure.hdinsight.spark.run.SparkBatchJobDebuggerRunner;
import com.microsoft.azuretools.utils.Pair;
import rx.Subscription;
import rx.subjects.PublishSubject;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.*;
import java.io.File;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

import static com.microsoft.azure.hdinsight.spark.common.SparkSubmitAdvancedConfigModel.SSHAuthType.UseKeyFile;
import static com.microsoft.azure.hdinsight.spark.common.SparkSubmitAdvancedConfigModel.SSHAuthType.UsePassword;
import static java.awt.event.ItemEvent.DESELECTED;
import static java.awt.event.ItemEvent.SELECTED;
import static java.awt.event.KeyEvent.VK_ESCAPE;
import static javax.swing.JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT;

public class SparkSubmissionAdvancedConfigDialog extends JDialog{
    SparkSubmissionAdvancedConfigDialog(SparkSubmitModel submitModel,
                                        SparkSubmitAdvancedConfigModel advancedConfigModel,
                                        CallBack updateCallBack) {
        this.submitModel = submitModel;
        this.advancedConfigModel = advancedConfigModel;

        this.updateCallBack = updateCallBack;

        // FIXME!!! Since the Intellij has no locale setting, just set en-us here.
        this.helpUrl = new Docs(Locale.US).getDocUrlByTopic(Docs.TOPIC_CONNECT_HADOOP_LINUX_USING_SSH);

        this.sshCheckSubject = PublishSubject.create();
        this.inputListener = new DocumentAdapter() {
            @Override
            protected void textChanged(DocumentEvent ev) {
                SparkSubmissionAdvancedConfigDialog.this.onChanged(ev.toString());
            }
        };

        setTitle("Spark Submission Advanced Configuration");

        setLayout(new GridBagLayout());

        // Main panel view
        addMainPanel();

        // Main panel actions

        // To enable/disable all options
        enableRemoteDebugCheckBox.addItemListener(e -> setSshAuthenticationUIEnabled(e.getStateChange() == SELECTED));
        // To trigger SSH authentication background check
        sshUserNameTextField.getDocument().addDocumentListener(inputListener);
        sshPasswordUsePasswordField.getDocument().addDocumentListener(inputListener);
        sshPasswordUseKeyFileTextField.getTextField().getDocument().addDocumentListener(inputListener);
        sshPasswordUsePasswordRadioButton.addActionListener(e -> onChanged(e.toString()));
        sshPasswordUseKeyFileRadioButton.addActionListener(e -> onChanged(e.toString()));
        // To change SSH authentication type
        sshPasswordUsePasswordRadioButton.addItemListener(e -> setSshPasswordInputEnabled(e.getStateChange() == SELECTED));
        sshPasswordUseKeyFileRadioButton.addItemListener(e -> setSshPasswordInputEnabled(e.getStateChange() == DESELECTED));
        // To popup the key file chooser dialog
        sshPasswordUseKeyFileTextField.getButton().addActionListener(e -> showSshKeyFileChooser());

        // Operation panel view: OK, Cancel buttons
        addOperationPanel();

        // Operation panel actions
        okButton.addActionListener(e -> onOk());
        cancelButton.addActionListener(e -> onCancel());
        // ESC key to cancel the dialog
        getRootPane().registerKeyboardAction(
                e -> onCancel(), KeyStroke.getKeyStroke(VK_ESCAPE, 0), WHEN_ANCESTOR_OF_FOCUSED_COMPONENT);

        enableRemoteDebugCheckBox.setSelected(false);

        this.sshCheckSubscription = registerAsyncSshAuthCheck(sshCheckSubject);

        loadParameters(advancedConfigModel);

        pack();
    }

    public SparkSubmitAdvancedConfigModel getAdvancedConfigModel() {
        return advancedConfigModel;
    }

    private SparkSubmitModel submitModel;
    private SparkSubmitAdvancedConfigModel advancedConfigModel;
    private String helpUrl;

    private final int margin = 12;

    private int displayLayoutCurrentRow = 0;
    JCheckBox enableRemoteDebugCheckBox;
    JTextField sshUserNameTextField;
    JLabel sshUserNameLabel;
    JLabel sshAuthTypeLabel;
    ButtonGroup sshPasswordButtonGroup;
    JRadioButton sshPasswordUsePasswordRadioButton;
    JRadioButton sshPasswordUseKeyFileRadioButton;
    JPasswordField sshPasswordUsePasswordField;
    TextFieldWithBrowseButton sshPasswordUseKeyFileTextField;
    JButton okButton;
    JButton cancelButton;
    BackgroundTaskIndicator checkSshCertIndicator;
    DocumentListener inputListener;
    IconButton helpButton;
    JPanel helpPanel;
    JPanel operationPanel;

    private PublishSubject<String> sshCheckSubject;
    private Subscription sshCheckSubscription;

    private CallBack updateCallBack;

    private void addMainPanel() {
        enableRemoteDebugCheckBox = new JCheckBox("Enable Spark remote debug", true);
        enableRemoteDebugCheckBox.setToolTipText("Enable Spark remote debug, use with caution since this might override data previously generated");
        add(enableRemoteDebugCheckBox,
                new GridBagConstraints(0, ++displayLayoutCurrentRow,
                        1, 1,
                        1, 0,
                        GridBagConstraints.NORTHWEST, GridBagConstraints.NONE,
                        JBUI.insetsTop(margin / 6), 0, 0));

        String sshUserNameToolTipText = "Secure shell (SSH) user name used in Spark remote debugging, by default using sshuser";
        sshUserNameLabel = new JLabel("Secure Shell (SSH) User Name:");
        sshUserNameLabel.setToolTipText(sshUserNameToolTipText);
        add(sshUserNameLabel,
                new GridBagConstraints(0, ++displayLayoutCurrentRow,
                        1, 1,
                        0, 0,
                        GridBagConstraints.NORTHWEST, GridBagConstraints.NONE,
                        JBUI.insets(margin, margin, 0, 0), 0, 0));

        sshUserNameTextField = new JTextField("sshuser");
        sshUserNameTextField.setToolTipText(sshUserNameToolTipText);
        add(sshUserNameTextField,
                new GridBagConstraints(1, displayLayoutCurrentRow,
                        0, 1,
                        1, 0,
                        GridBagConstraints.NORTHWEST, GridBagConstraints.HORIZONTAL,
                        JBUI.insets(margin, margin, 0, margin), 0, 0));

        sshAuthTypeLabel = new JLabel("Secure Shell (SSH) Auth Type");
        sshAuthTypeLabel.setToolTipText("Secure shell (SSH) authentication type used in Spark remote debugging, by default using the password");

        sshPasswordUsePasswordRadioButton = new JRadioButton("Use secure shell (SSH) password:", false);
        String sshPasswordUsePasswordToolTip = "For secure shell (SSH) password, use the password specified here";
        sshPasswordUsePasswordRadioButton.setToolTipText(sshPasswordUsePasswordToolTip);
        sshPasswordUsePasswordField = new JPasswordField();
        sshPasswordUsePasswordField.setToolTipText(sshPasswordUsePasswordToolTip);

        sshPasswordUseKeyFileRadioButton = new JRadioButton("Use private key file:", false);
        String sshPasswordUseKeyFileToolTip = "For secure shell (SSH) password, use the key file specified here";
        sshPasswordUseKeyFileRadioButton.setToolTipText(sshPasswordUseKeyFileToolTip);
        sshPasswordUseKeyFileTextField = new TextFieldWithBrowseButton();
        sshPasswordUseKeyFileTextField.setToolTipText(sshPasswordUseKeyFileToolTip);

        sshPasswordButtonGroup = new ButtonGroup();
        sshPasswordButtonGroup.add(sshPasswordUsePasswordRadioButton);
        sshPasswordButtonGroup.add(sshPasswordUseKeyFileRadioButton);

        add(sshAuthTypeLabel,
                new GridBagConstraints(0, ++displayLayoutCurrentRow,
                        0, 1,
                        0, 0,
                        GridBagConstraints.WEST, GridBagConstraints.NONE,
                        JBUI.insets(margin, margin, 0, margin), 0, 0));

        add(sshPasswordUsePasswordRadioButton,
                new GridBagConstraints(0, ++displayLayoutCurrentRow,
                        1, 1,
                        0, 0,
                        GridBagConstraints.WEST, GridBagConstraints.NONE,
                        JBUI.insets(margin / 3, margin * 3, 0, margin), 0, 0));

        add(sshPasswordUsePasswordField,
                new GridBagConstraints(1, displayLayoutCurrentRow,
                        0, 1,
                        1, 0,
                        GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL,
                        JBUI.insets(margin / 3, margin, 0, margin), 0, 0));

        add(sshPasswordUseKeyFileRadioButton,
                new GridBagConstraints(0, ++displayLayoutCurrentRow,
                        1, 1,
                        0, 0,
                        GridBagConstraints.WEST, GridBagConstraints.NONE,
                        JBUI.insets(margin / 3, margin * 3, 0, margin), 0, 0));

        add(sshPasswordUseKeyFileTextField,
                new GridBagConstraints(1, displayLayoutCurrentRow,
                        0, 1,
                        1, 0,
                        GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL,
                        JBUI.insets(margin / 3, margin, 0, margin), 0, 0));

        sshPasswordUsePasswordRadioButton.setSelected(true);
    }

    private void addOperationPanel() {
        operationPanel = new JPanel();
        operationPanel.setLayout(new FlowLayout());

        okButton = new JButton("Ok");
        cancelButton = new JButton("Cancel");

        operationPanel.add(okButton);
        operationPanel.add(cancelButton);

        getRootPane().setDefaultButton(cancelButton);

        helpPanel = new JPanel();
        helpPanel.setLayout(new BoxLayout(helpPanel, BoxLayout.LINE_AXIS));

        helpButton = new IconButton("Help about connection to HDInsight using SSH", AllIcons.Actions.Help);
        checkSshCertIndicator = new BackgroundTaskIndicator("Verify SSH Authentication...");

        helpPanel.add(new InplaceButton(helpButton, e -> BrowserUtil.browse(this.helpUrl)));
        helpPanel.add(checkSshCertIndicator);

        add(helpPanel,
                new GridBagConstraints(0, ++displayLayoutCurrentRow,
                        0, 1,
                        1, 0,
                        GridBagConstraints.WEST, GridBagConstraints.NONE,
                        JBUI.insets(0, margin), 0, 0));

        add(operationPanel,
                new GridBagConstraints(1, displayLayoutCurrentRow,
                        0, 1,
                        1, 0,
                        GridBagConstraints.SOUTHEAST, GridBagConstraints.NONE,
                        JBUI.insetsLeft(margin), 0, 0));
    }

    private void loadParameters(SparkSubmitAdvancedConfigModel newAdvConfigModel) {
        if (newAdvConfigModel != null) {
            if (newAdvConfigModel.sshPassword != null && newAdvConfigModel.sshPassword.length() > 0) {
                sshPasswordUsePasswordField.setText(newAdvConfigModel.sshPassword);
            }

            if (newAdvConfigModel.sshKeyFile != null && newAdvConfigModel.sshKeyFile.exists()) {
                sshPasswordUseKeyFileTextField.setText(newAdvConfigModel.sshKeyFile.getAbsolutePath());
            }

            sshPasswordUsePasswordRadioButton.setSelected(true);
            switch (newAdvConfigModel.sshAuthType)
            {
                case UsePassword:
                    sshPasswordUsePasswordRadioButton.setSelected(true);
                    break;
                case UseKeyFile:
                    sshPasswordUseKeyFileRadioButton.setSelected(true);
                    break;

            }

            if (newAdvConfigModel.sshUserName != null && newAdvConfigModel.sshUserName.length() > 0){
                sshUserNameTextField.setText(newAdvConfigModel.sshUserName);
            }

            enableRemoteDebugCheckBox.setSelected(newAdvConfigModel.enableRemoteDebug);
        }
    }

    private SparkSubmitAdvancedConfigModel saveParameters() {
        SparkSubmitAdvancedConfigModel currentConfigModel = new SparkSubmitAdvancedConfigModel();

        if (sshPasswordUsePasswordField.getPassword() != null && sshPasswordUsePasswordField.getPassword().length > 0) {
            currentConfigModel.sshPassword = new String(sshPasswordUsePasswordField.getPassword());
        }

        if (!sshPasswordUseKeyFileTextField.getText().trim().isEmpty()) {
            File f = new File(sshPasswordUseKeyFileTextField.getText());
            if (f.exists() && !f.isDirectory()){
                currentConfigModel.sshKeyFile = f;
            }
        }

        if (sshPasswordUsePasswordRadioButton.isSelected())
        {
            currentConfigModel.sshAuthType = UsePassword;
        }
        else if (sshPasswordUseKeyFileRadioButton.isSelected())
        {
            currentConfigModel.sshAuthType = SparkSubmitAdvancedConfigModel.SSHAuthType.UseKeyFile;
        }

        if (sshUserNameTextField.getText() != null && sshUserNameTextField.getText().length() > 0){
            currentConfigModel.sshUserName = sshUserNameTextField.getText();
        }

        currentConfigModel.enableRemoteDebug = enableRemoteDebugCheckBox.isSelected();

        return currentConfigModel;
    }

    private void setSshAuthenticationUIEnabled(Boolean isEnabled) {
        if (isEnabled) {
            sshUserNameTextField.setEnabled(true);
            sshUserNameLabel.setEnabled(true);
            sshAuthTypeLabel.setEnabled(true);

            sshPasswordUsePasswordRadioButton.setEnabled(true);
            sshPasswordUseKeyFileRadioButton.setEnabled(true);

            sshPasswordUsePasswordField.setEnabled(true);
            sshPasswordUseKeyFileTextField.setEnabled(true);

            ButtonModel currentSelection = sshPasswordButtonGroup.getSelection();
            sshPasswordUsePasswordRadioButton.setSelected(true);
            sshPasswordUseKeyFileRadioButton.setSelected(true);
            currentSelection.setSelected(true);

            // enable it after the ssh certification verified
            okButton.setEnabled(false);

            sshCheckSubject.onNext("Enable settings");
        } else {
            sshUserNameTextField.setEnabled(false);
            sshUserNameLabel.setEnabled(false);
            sshAuthTypeLabel.setEnabled(false);

            sshPasswordUsePasswordRadioButton.setEnabled(false);
            sshPasswordUseKeyFileRadioButton.setEnabled(false);

            sshPasswordUsePasswordField.setEnabled(false);
            sshPasswordUseKeyFileTextField.setEnabled(false);

            checkSshCertIndicator.stop("");
            okButton.setEnabled(true);
        }
    }

    private void setSshPasswordInputEnabled(Boolean isEnabled) {
        sshPasswordUsePasswordField.setEnabled(isEnabled);
        sshPasswordUseKeyFileTextField.setEnabled(!isEnabled);
    }

    private void showSshKeyFileChooser() {
        FileChooserDescriptor fileChooserDescriptor = new FileChooserDescriptor(true,
                false,
                false,
                false,
                false,
                false);

        fileChooserDescriptor.setTitle("Select SSH Key File");
        VirtualFile chooseFile = FileChooser.chooseFile(fileChooserDescriptor, null, null);

        if (chooseFile != null) {
            String path = chooseFile.getPath();
            sshPasswordUseKeyFileTextField.setText(path);
        }
    }

    private Subscription registerAsyncSshAuthCheck(PublishSubject<String> checkEventSubject) {
        return checkEventSubject.throttleWithTimeout(500, TimeUnit.MILLISECONDS)
            .map(message -> saveParameters())
            .filter(model -> !model.sshUserName.isEmpty() &&
                             enableRemoteDebugCheckBox.isSelected() &&
                             (model.sshAuthType == UsePassword ? !model.sshPassword.isEmpty() : model.sshKeyFile != null))
            .map(advConfModelToProbe -> {
                checkSshCertIndicator.start();

                String selectedClusterName = submitModel.getSubmissionParameter().getClusterName();

                try {
                    IClusterDetail clusterDetail = ClusterManagerEx.getInstance()
                            .getClusterDetailByName(selectedClusterName)
                            .orElseThrow(() -> new HDIException(
                                    "No cluster name matched selection: " + selectedClusterName));

                    SparkBatchJobDebuggerRunner debuggerRunner = new SparkBatchJobDebuggerRunner();

                    SparkBatchDebugSession debugSession = debuggerRunner
                            .createSparkBatchDebugSession(clusterDetail.getConnectionUrl(), advConfModelToProbe)
                            .open();

                    debugSession.close();

                    return new Pair<>(advConfModelToProbe, true);
                } catch (Exception ex) {
                    return new Pair<>(advConfModelToProbe, false);
                }
            })
            .subscribe(pair -> {
                SparkSubmitAdvancedConfigModel probedAdvModel = pair.first();
                Boolean isPass = pair.second();

                if (enableRemoteDebugCheckBox.isSelected() &&
                        ((probedAdvModel.sshAuthType == UsePassword &&
                                sshPasswordUsePasswordRadioButton.isSelected() &&
                                probedAdvModel.sshPassword.equals(new String(sshPasswordUsePasswordField.getPassword()))) ||
                         (probedAdvModel.sshAuthType == UseKeyFile &&
                                sshPasswordUseKeyFileRadioButton.isSelected() &&
                                probedAdvModel.sshKeyFile != null &&
                                probedAdvModel.sshKeyFile.toString().equals(sshPasswordUseKeyFileTextField.getText())))
                        ){
                    // Checked parameter is matched with current content
                    checkSshCertIndicator.stop("SSH Authentication is " + (isPass ? "passed" : "failed"));
                    okButton.setEnabled(isPass);
                    SparkSubmissionAdvancedConfigDialog.this.getRootPane()
                            .setDefaultButton(isPass ? okButton : cancelButton);
                } else {
                    checkSshCertIndicator.stop("");
                }
            });
    }

    private void onOk() {
        this.advancedConfigModel = saveParameters();

        if (updateCallBack != null) {
            updateCallBack.run();
        }

        dispose();
    }

    private void onCancel() {
        dispose();
    }

    private void onChanged(String message) {
        okButton.setEnabled(false);
        checkSshCertIndicator.stop("");
        sshCheckSubject.onNext(message);
    }

    @Override
    public void dispose() {
        sshCheckSubscription.unsubscribe();
        super.dispose();
    }
}
