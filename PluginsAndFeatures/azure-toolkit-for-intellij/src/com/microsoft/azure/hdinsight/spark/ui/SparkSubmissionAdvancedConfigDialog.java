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

import com.intellij.openapi.fileChooser.FileChooser;
import com.intellij.openapi.fileChooser.FileChooserDescriptor;
import com.intellij.openapi.ui.TextFieldWithBrowseButton;
import com.intellij.openapi.vfs.VirtualFile;
import com.microsoft.azure.hdinsight.common.CallBack;
import com.microsoft.azure.hdinsight.spark.common.SparkSubmitAdvancedConfigModel;

import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;

public class SparkSubmissionAdvancedConfigDialog extends JDialog{
    public SparkSubmissionAdvancedConfigDialog(SparkSubmitAdvancedConfigModel advancedConfigModel, CallBack updateCallBack) {
        this.advancedConfigModel = advancedConfigModel;

        this.updateCallBack = updateCallBack;

        setTitle("Spark Submission Advanced Configuration");

        setLayout(new GridBagLayout());

        addRemoteDebugLineItem();

        addOperationPanel();

        loadParameters();

        pack();
    }

    public SparkSubmitAdvancedConfigModel getAdvancedConfigModel() {
        return advancedConfigModel;
    }

    private SparkSubmitAdvancedConfigModel advancedConfigModel;

    private final int margin = 12;

    private int displayLayoutCurrentRow = 0;
    JCheckBox enableRemoteDebugCheckBox;
    JTextField sshUserNameTextField;
    ButtonGroup sshPasswordButtonGroup;
    JRadioButton sshPasswordUseArtifactRadioButtion;
    JRadioButton sshPasswordUsePasswordRadioButton;
    JRadioButton sshPasswordUseKeyFileRadioButton;
    JPasswordField sshPasswordUsePasswordField;
    TextFieldWithBrowseButton sshPasswordUseKeyFileTextField;

    private CallBack updateCallBack;

    private void addRemoteDebugLineItem() {
        enableRemoteDebugCheckBox = new JCheckBox("Enable Spark remote debug", true);
        enableRemoteDebugCheckBox.setToolTipText("Enable Spark remote debug, use with caution since this might override data previously generated");
        add(enableRemoteDebugCheckBox,
                new GridBagConstraints(0, ++displayLayoutCurrentRow,
                        1, 1,
                        1, 0,
                        GridBagConstraints.NORTHWEST, GridBagConstraints.NONE,
                        new Insets(margin / 6, 0, 0, 0), 0, 0));

        String sshUserNameToolTipText = "Secure shell (SSH) user name used in Spark remote debugging, by default using sshuser";
        JLabel sshUserNameLabel = new JLabel("Secure Shell (SSH) User Name:");
        sshUserNameLabel.setToolTipText(sshUserNameToolTipText);
        add(sshUserNameLabel,
                new GridBagConstraints(0, ++displayLayoutCurrentRow,
                        1, 1,
                        1, 0,
                        GridBagConstraints.NORTHWEST, GridBagConstraints.NONE,
                        new Insets(margin, margin, 0, 0), 0, 0));

        sshUserNameTextField = new JTextField("sshuser");
        sshUserNameTextField.setToolTipText(sshUserNameToolTipText);
        add(sshUserNameTextField,
                new GridBagConstraints(1, displayLayoutCurrentRow,
                        0, 1,
                        1, 0,
                        GridBagConstraints.NORTHWEST, GridBagConstraints.HORIZONTAL,
                        new Insets(margin, margin, 0, margin), 0, 0));

        JLabel sshPasswordKeyLabel = new JLabel("Secure Shell (SSH) Password or Key");
        sshPasswordKeyLabel.setToolTipText("Secure shell (SSH) password or key used in Spark remote debugging, by default use the artifact from IntelliJ project");

        sshPasswordUseArtifactRadioButtion = new JRadioButton("Use the artifact from IntelliJ project", false);
        sshPasswordUseArtifactRadioButtion.setToolTipText("For secure shell (SSH) password, use the artifact stored in IntelliJ project");

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
        sshPasswordUseKeyFileTextField.getButton().addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                FileChooserDescriptor fileChooserDescriptor = new FileChooserDescriptor(true,
                        false,
                        false,
                        false,
                        false,
                        false);

                fileChooserDescriptor.setTitle("Select SSH key file");
                VirtualFile chooseFile = FileChooser.chooseFile(fileChooserDescriptor, null, null);
                if (chooseFile != null) {
                    String path = chooseFile.getPath();
                    sshPasswordUseKeyFileTextField.setText(path);
                }
            }
        });

        sshPasswordButtonGroup = new ButtonGroup();
        sshPasswordButtonGroup.add(sshPasswordUseArtifactRadioButtion);
        sshPasswordButtonGroup.add(sshPasswordUsePasswordRadioButton);
        sshPasswordButtonGroup.add(sshPasswordUseKeyFileRadioButton);

        sshPasswordUseArtifactRadioButtion.addItemListener(new ItemListener() {
            @Override
            public void itemStateChanged(ItemEvent e) {
                if (e.getStateChange() == ItemEvent.SELECTED){
                    sshPasswordUsePasswordField.setEnabled(false);
                    sshPasswordUseKeyFileTextField.setEnabled(false);
                }
            }
        });

        sshPasswordUsePasswordRadioButton.addItemListener(new ItemListener() {
            @Override
            public void itemStateChanged(ItemEvent e) {
                if (e.getStateChange() == ItemEvent.SELECTED){
                    sshPasswordUsePasswordField.setEnabled(true);
                    sshPasswordUseKeyFileTextField.setEnabled(false);
                }
            }
        });

        sshPasswordUseKeyFileRadioButton.addItemListener(new ItemListener() {
            @Override
            public void itemStateChanged(ItemEvent e) {
                if (e.getStateChange() == ItemEvent.SELECTED){
                    sshPasswordUsePasswordField.setEnabled(false);
                    sshPasswordUseKeyFileTextField.setEnabled(true);
                }
            }
        });

        enableRemoteDebugCheckBox.addItemListener(new ItemListener() {
            @Override
            public void itemStateChanged(ItemEvent e) {
                if (enableRemoteDebugCheckBox.isSelected()){
                    sshUserNameTextField.setEnabled(true);
                    sshUserNameLabel.setEnabled(true);
                    sshPasswordKeyLabel.setEnabled(true);

                    sshPasswordUseArtifactRadioButtion.setEnabled(true);
                    sshPasswordUsePasswordRadioButton.setEnabled(true);
                    sshPasswordUseKeyFileRadioButton.setEnabled(true);

                    sshPasswordUsePasswordField.setEnabled(true);
                    sshPasswordUseKeyFileTextField.setEnabled(true);

                    ButtonModel currentSelection = sshPasswordButtonGroup.getSelection();
                    sshPasswordUsePasswordRadioButton.setSelected(true);
                    sshPasswordUseArtifactRadioButtion.setSelected(true);
                    currentSelection.setSelected(true);
                }
                else
                {
                    sshUserNameTextField.setEnabled(false);
                    sshUserNameLabel.setEnabled(false);
                    sshPasswordKeyLabel.setEnabled(false);

                    sshPasswordUseArtifactRadioButtion.setEnabled(false);
                    sshPasswordUsePasswordRadioButton.setEnabled(false);
                    sshPasswordUseKeyFileRadioButton.setEnabled(false);

                    sshPasswordUsePasswordField.setEnabled(false);
                    sshPasswordUseKeyFileTextField.setEnabled(false);
                }
            }
        });

        add(sshPasswordKeyLabel,
                new GridBagConstraints(0, ++displayLayoutCurrentRow,
                        0, 1,
                        0, 0,
                        GridBagConstraints.WEST, GridBagConstraints.NONE,
                        new Insets(margin, margin, 0, margin), 0, 0));

        add(sshPasswordUseArtifactRadioButtion,
                new GridBagConstraints(0, ++displayLayoutCurrentRow,
                        0, 1,
                        0, 0,
                        GridBagConstraints.WEST, GridBagConstraints.NONE,
                        new Insets(margin / 3, margin * 3, 0, margin), 0, 0));

        add(sshPasswordUsePasswordRadioButton,
                new GridBagConstraints(0, ++displayLayoutCurrentRow,
                        1, 1,
                        0, 0,
                        GridBagConstraints.WEST, GridBagConstraints.NONE,
                        new Insets(margin / 3, margin * 3, 0, margin), 0, 0));

        add(sshPasswordUsePasswordField,
                new GridBagConstraints(1, displayLayoutCurrentRow,
                        0, 1,
                        1, 0,
                        GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL,
                        new Insets(margin / 3, margin, 0, margin), 0, 0));

        add(sshPasswordUseKeyFileRadioButton,
                new GridBagConstraints(0, ++displayLayoutCurrentRow,
                        1, 1,
                        0, 0,
                        GridBagConstraints.WEST, GridBagConstraints.NONE,
                        new Insets(margin / 3, margin * 3, 0, margin), 0, 0));

        add(sshPasswordUseKeyFileTextField,
                new GridBagConstraints(1, displayLayoutCurrentRow,
                        0, 1,
                        1, 0,
                        GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL,
                        new Insets(margin / 3, margin, 0, margin), 0, 0));

        sshPasswordUseArtifactRadioButtion.setSelected(true);
        enableRemoteDebugCheckBox.setSelected(false);
    }

    private void addOperationPanel() {
        JPanel operationPanel = new JPanel();
        operationPanel.setLayout(new FlowLayout());

        JButton okButton = new JButton("Ok");
        JButton cancelButton = new JButton("Cancel");
        operationPanel.add(okButton);
        operationPanel.add(cancelButton);

        okButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                onOk();
            }
        });

        cancelButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                onCancel();
            }
        });

        getRootPane().setDefaultButton(cancelButton);

        add(operationPanel,
                new GridBagConstraints(1, ++displayLayoutCurrentRow,
                        0, 1,
                        1, 0,
                        GridBagConstraints.SOUTHEAST, GridBagConstraints.NONE,
                        new Insets(0, margin, 0, 0), 0, 0));
    }

    private void loadParameters() {
        if (advancedConfigModel != null) {
            if (advancedConfigModel.sshPassword != null && advancedConfigModel.sshPassword.length() > 0) {
                sshPasswordUsePasswordField.setText(advancedConfigModel.sshPassword);
            }

            if (advancedConfigModel.sshKyeFile != null && advancedConfigModel.sshKyeFile.length() > 0) {
                sshPasswordUseKeyFileTextField.setText(advancedConfigModel.sshKyeFile);
            }

            switch (advancedConfigModel.sshPasswordKeyOrLabel)
            {
                case UseArtifact:
                    sshPasswordUseArtifactRadioButtion.setSelected(true);
                    break;
                case UsePassword:
                    sshPasswordUsePasswordRadioButton.setSelected(true);
                    break;
                case UseKeyFile:
                    sshPasswordUseKeyFileRadioButton.setSelected(true);
                    break;

            }

            if (advancedConfigModel.sshUserName != null && advancedConfigModel.sshUserName.length() > 0){
                sshUserNameTextField.setText(advancedConfigModel.sshUserName);
            }

            enableRemoteDebugCheckBox.setSelected(advancedConfigModel.enableRemoteDebug);
        }
    }

    private void saveParameters() {
        if (advancedConfigModel == null){
            advancedConfigModel = new SparkSubmitAdvancedConfigModel();
        }

        if (sshPasswordUsePasswordField.getPassword() != null && sshPasswordUsePasswordField.getPassword().length > 0) {
            advancedConfigModel.sshPassword = new String(sshPasswordUsePasswordField.getPassword());
        }

        if (sshPasswordUseKeyFileTextField.getText() != null && sshPasswordUseKeyFileTextField.getText().length() > 0) {
            advancedConfigModel.sshKyeFile = sshPasswordUseKeyFileTextField.getText();
        }

        if (sshPasswordUseArtifactRadioButtion.isSelected())
        {
            advancedConfigModel.sshPasswordKeyOrLabel = SparkSubmitAdvancedConfigModel.SSHPasswordKeyOrLabel.UseArtifact;
        }
        else if (sshPasswordUsePasswordRadioButton.isSelected())
        {
            advancedConfigModel.sshPasswordKeyOrLabel = SparkSubmitAdvancedConfigModel.SSHPasswordKeyOrLabel.UsePassword;
        }
        else if (sshPasswordUseKeyFileRadioButton.isSelected())
        {
            advancedConfigModel.sshPasswordKeyOrLabel = SparkSubmitAdvancedConfigModel.SSHPasswordKeyOrLabel.UseKeyFile;
        }

        if (sshUserNameTextField.getText() != null && sshUserNameTextField.getText().length() > 0){
            advancedConfigModel.sshUserName = sshUserNameTextField.getText();
        }

        advancedConfigModel.enableRemoteDebug = enableRemoteDebugCheckBox.isSelected();
    }

    private void onOk() {
        saveParameters();

        if (updateCallBack != null) {
            updateCallBack.run();
        }

        dispose();
    }

    private void onCancel() {
        dispose();
    }
}
