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

package com.microsoft.intellij.docker.forms;

import com.intellij.openapi.fileChooser.FileChooserDescriptorFactory;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.openapi.ui.TextFieldWithBrowseButton;
import com.intellij.openapi.ui.ValidationInfo;
import com.microsoft.azure.docker.AzureDockerHostsManager;
import com.microsoft.azure.docker.model.AzureDockerCertVault;
import com.microsoft.azure.docker.model.EditableDockerHost;
import com.microsoft.azure.docker.ops.AzureDockerCertVaultOps;
import com.microsoft.azure.docker.ops.utils.AzureDockerValidationUtils;
import com.microsoft.intellij.docker.dialogs.AzureSelectKeyVault;
import com.microsoft.intellij.docker.utils.AzureDockerUIResources;
import com.microsoft.intellij.ui.util.UIUtils;
import com.microsoft.intellij.util.PluginUtil;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

public class AzureDockerHostUpdateLoginPanel {
  private JPanel contentPane;
  private JPanel mainPanel;
  private JButton copyFromAzureKeyButton;
  public JTextField dockerHostUsernameTextField;
  public JLabel dockerHostUsernameLabel;
  public JPasswordField dockerHostFirstPwdField;
  public JLabel dockerHostFirstPwdLabel;
  public JPasswordField dockerHostSecondPwdField;
  public JRadioButton dockerHostKeepSshRadioButton;
  public JRadioButton dockerHostAutoSshRadioButton;
  public JRadioButton dockerHostImportSshRadioButton;
  public TextFieldWithBrowseButton dockerHostImportSSHBrowseTextField;
  public JLabel dockerHostImportSSHBrowseLabel;
  public JLabel dockerHostSecondPwdLabel;
  private ButtonGroup authSelectionGroup;

  private Project project;
  private EditableDockerHost editableHost;
  private AzureDockerHostsManager dockerManager;
  private DialogWrapper dialogWrapperParent;

  public AzureDockerHostUpdateLoginPanel(Project project, EditableDockerHost editableHost, AzureDockerHostsManager dockerUIManager, DialogWrapper dialogWrapper) {
    this.project = project;
    this.editableHost = editableHost;
    this.dockerManager = dockerUIManager;
    this.dialogWrapperParent = dialogWrapper;

    authSelectionGroup = new ButtonGroup();
    authSelectionGroup.add(dockerHostKeepSshRadioButton);
    authSelectionGroup.add(dockerHostAutoSshRadioButton);
    authSelectionGroup.add(dockerHostImportSshRadioButton);

    initDefaultUI();

    copyFromAzureKeyButton.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent e) {
        AzureSelectKeyVault selectKeyvaultDialog = new AzureSelectKeyVault(project, dockerUIManager);
        selectKeyvaultDialog.show();

        if (selectKeyvaultDialog.getExitCode() == DialogWrapper.OK_EXIT_CODE && selectKeyvaultDialog.getSelectedKeyvault() != null) {
          updateUIWithKeyvault(selectKeyvaultDialog.getSelectedKeyvault());
        }
      }
    });

    dockerHostUsernameLabel.setVisible(editableHost.originalDockerHost.certVault == null || editableHost.originalDockerHost.certVault.vmUsername == null);
    dockerHostUsernameTextField.setText((editableHost.originalDockerHost.certVault != null && editableHost.originalDockerHost.certVault.vmUsername != null) ?
        editableHost.originalDockerHost.certVault.vmUsername : "");
    dockerHostUsernameTextField.setToolTipText(AzureDockerValidationUtils.getDockerHostUserNameTip());
    dockerHostUsernameTextField.setInputVerifier(new InputVerifier() {
      @Override
      public boolean verify(JComponent input) {
        String text = dockerHostUsernameTextField.getText();
        if (text == null || text.isEmpty() || (dockerHostSecondPwdField.isVisible() && !AzureDockerValidationUtils.validateDockerHostUserName(text))) {
          dockerHostUsernameLabel.setVisible(true);
          return false;
        } else {
          dockerHostUsernameLabel.setVisible(false);
          return true;
        }
      }
    });
//    dockerHostUsernameTextField.getDocument().addDocumentListener(resetDialogButtonsState(null));
    dockerHostFirstPwdField.setInputVerifier(new InputVerifier() {
      @Override
      public boolean verify(JComponent input) {
        String text = new String(dockerHostFirstPwdField.getPassword());
        if (dockerHostFirstPwdField.getPassword().length > 0 && !text.isEmpty() && dockerHostSecondPwdField.isVisible() && !AzureDockerValidationUtils.validateDockerHostPassword(text)) {
          dockerHostFirstPwdLabel.setVisible(true);
          return false;
        } else {
          dockerHostFirstPwdLabel.setVisible(false);
          if (!dockerHostSecondPwdField.isVisible()) {
            dockerHostSecondPwdField.setText(text);
          }
          return true;
        }
      }
    });
    dockerHostFirstPwdLabel.setVisible(false);
    dockerHostFirstPwdField.setToolTipText(AzureDockerValidationUtils.getDockerHostPasswordTip());
    dockerHostSecondPwdField.setInputVerifier(new InputVerifier() {
      @Override
      public boolean verify(JComponent input) {
        String pwd1 = new String(dockerHostFirstPwdField.getPassword());
        String pwd2 = new String(dockerHostSecondPwdField.getPassword());
        if (dockerHostSecondPwdField.getPassword().length > 0 && !pwd2.isEmpty() && !pwd2.equals(pwd1)) {
          dockerHostFirstPwdLabel.setVisible(true);
          return false;
        } else {
          dockerHostFirstPwdLabel.setVisible(false);
          return true;
        }
      }
    });
    dockerHostSecondPwdField.setToolTipText(AzureDockerValidationUtils.getDockerHostPasswordTip());

    dockerHostKeepSshRadioButton.setText(editableHost.originalDockerHost.hasSSHLogIn ? "Use current keys" : "None");
    dockerHostKeepSshRadioButton.setSelected(true);
    dockerHostKeepSshRadioButton.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent e) {
        dockerHostImportSSHBrowseTextField.setEnabled(false);
      }
    });
    dockerHostAutoSshRadioButton.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent e) {
        dockerHostImportSSHBrowseTextField.setEnabled(false);
      }
    });
    dockerHostImportSshRadioButton.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent e) {
        dockerHostImportSSHBrowseTextField.setEnabled(true);
      }
    });

    dockerHostImportSSHBrowseLabel.setVisible(false);
    dockerHostImportSSHBrowseTextField.addActionListener(UIUtils.createFileChooserListener(dockerHostImportSSHBrowseTextField, project,
        FileChooserDescriptorFactory.createSingleFolderDescriptor()));
    dockerHostImportSSHBrowseTextField.getTextField().setToolTipText(AzureDockerValidationUtils.getDockerHostSshDirectoryTip());
    dockerHostImportSSHBrowseTextField.getTextField().setInputVerifier(new InputVerifier() {
      @Override
      public boolean verify(JComponent input) {
        String text = dockerHostImportSSHBrowseTextField.getText();
        if (text == null || text.isEmpty() || !AzureDockerValidationUtils.validateDockerHostSshDirectory(text)) {
          dockerHostImportSSHBrowseLabel.setVisible(true);
          return false;
        } else {
          dockerHostImportSSHBrowseLabel.setVisible(false);
          return true;
        }
      }
    });


  }

  private void initDefaultUI() {
  }

  private void updateUIWithKeyvault(String keyvault) {
    AzureDockerCertVault certVault = dockerManager.getDockerVault(keyvault);
    if (certVault != null) {
      editableHost.updatedDockerHost.certVault = certVault;
      dockerHostUsernameTextField.setText((certVault.vmUsername != null) ? certVault.vmUsername : "");
      dockerHostFirstPwdField.setText((certVault.vmPwd != null) ? certVault.vmPwd : "");
      dockerHostSecondPwdField.setText((certVault.vmPwd != null) ? certVault.vmPwd : "");
    }
  }

  public JPanel getMainPanel() {
    return mainPanel;
  }

  public void DialogShaker(ValidationInfo info) {
    PluginUtil.dialogShaker(info, dialogWrapperParent);
  }

  public ValidationInfo doValidate(boolean shakeOnError) {
    // User name
    String vmUsername = dockerHostUsernameTextField.getText();
    if (vmUsername == null || vmUsername.isEmpty() ||
        (dockerHostSecondPwdField.isVisible() && !AzureDockerValidationUtils.validateDockerHostUserName(vmUsername)))
    {
      ValidationInfo info = AzureDockerUIResources.validateComponent("Missing username", mainPanel, dockerHostUsernameTextField, dockerHostUsernameLabel);
      if (shakeOnError) {
        DialogShaker(info);
      }
      return info;
    }
    editableHost.updatedDockerHost.certVault.vmUsername = vmUsername;

    // Password login
    String vmPwd1 = new String(dockerHostFirstPwdField.getPassword());
    String vmPwd2 = new String(dockerHostSecondPwdField.getPassword());
    if (((dockerHostKeepSshRadioButton.isSelected() && editableHost.originalDockerHost.hasSSHLogIn) ||
        dockerHostFirstPwdField.getPassword().length > 0 ||
        dockerHostSecondPwdField.getPassword().length > 0) &&
        (vmPwd1.isEmpty() || vmPwd2.isEmpty() || !vmPwd1.equals(vmPwd2) ||
            (dockerHostSecondPwdField.isVisible() && !AzureDockerValidationUtils.validateDockerHostPassword(vmPwd1))))
    {
      ValidationInfo info = AzureDockerUIResources.validateComponent("Incorrect password", mainPanel, dockerHostFirstPwdField, dockerHostFirstPwdLabel);
      if (shakeOnError) {
        DialogShaker(info);
      }
      return info;
    }
    if (dockerHostFirstPwdField.getPassword().length > 0) {
      editableHost.updatedDockerHost.certVault.vmPwd = new String(dockerHostFirstPwdField.getPassword());
      editableHost.updatedDockerHost.hasPwdLogIn = true;
    } else {
      editableHost.updatedDockerHost.certVault.vmPwd = null;
      editableHost.updatedDockerHost.hasPwdLogIn = false;
    }

    // Keep current SSH keys
    if (dockerHostKeepSshRadioButton.isSelected() && editableHost.originalDockerHost.hasSSHLogIn) {
      AzureDockerCertVaultOps.copyVaultSshKeys(editableHost.updatedDockerHost.certVault, editableHost.originalDockerHost.certVault);
      editableHost.updatedDockerHost.hasSSHLogIn = editableHost.originalDockerHost.hasSSHLogIn;
    }

    // SSH key auto generated
    if (dockerHostAutoSshRadioButton.isSelected()) {
      AzureDockerCertVault certVault = AzureDockerCertVaultOps.generateSSHKeys(null, "SSH keys for " + editableHost.updatedDockerHost.name);
      AzureDockerCertVaultOps.copyVaultSshKeys(editableHost.updatedDockerHost.certVault, certVault);
      editableHost.updatedDockerHost.hasSSHLogIn = true;
    }

    // SSH key imported from local file directory
    if (dockerHostImportSshRadioButton.isSelected()) {
      if (dockerHostImportSSHBrowseTextField.getText() == null || dockerHostImportSSHBrowseTextField.getText().isEmpty() ||
          !AzureDockerValidationUtils.validateDockerHostSshDirectory(dockerHostImportSSHBrowseTextField.getText())) {
        ValidationInfo info = AzureDockerUIResources.validateComponent("SSH key files were not found in the selected directory", mainPanel, dockerHostImportSSHBrowseTextField, dockerHostImportSSHBrowseLabel);
        if (shakeOnError) {
          DialogShaker(info);
        }
        return info;
      } else {
        AzureDockerCertVault certVault = AzureDockerCertVaultOps.getSSHKeysFromLocalFile(dockerHostImportSSHBrowseTextField.getText());
        AzureDockerCertVaultOps.copyVaultSshKeys(editableHost.updatedDockerHost.certVault, certVault);
        editableHost.updatedDockerHost.hasSSHLogIn = true;
      }
    }
    return null;
  }

}
