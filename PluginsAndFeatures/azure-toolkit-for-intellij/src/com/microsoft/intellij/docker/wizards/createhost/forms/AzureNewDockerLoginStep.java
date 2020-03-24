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

package com.microsoft.intellij.docker.wizards.createhost.forms;

import com.intellij.openapi.fileChooser.FileChooserDescriptorFactory;
import com.intellij.openapi.ui.TextFieldWithBrowseButton;
import com.intellij.openapi.ui.ValidationInfo;
import com.intellij.ui.wizard.WizardNavigationState;
import com.intellij.ui.wizard.WizardStep;
import com.microsoft.azure.docker.AzureDockerHostsManager;
import com.microsoft.azure.docker.model.AzureDockerCertVault;
import com.microsoft.azure.docker.model.DockerHost;
import com.microsoft.azure.docker.ops.AzureDockerCertVaultOps;
import com.microsoft.azuretools.telemetry.TelemetryProperties;
import com.microsoft.intellij.docker.utils.AzureDockerUIResources;
import com.microsoft.azure.docker.ops.utils.AzureDockerValidationUtils;
import com.microsoft.intellij.docker.wizards.createhost.AzureNewDockerWizardModel;
import com.microsoft.intellij.docker.wizards.createhost.AzureNewDockerWizardStep;
import com.microsoft.intellij.ui.util.UIUtils;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Map;
import java.util.Vector;

public class AzureNewDockerLoginStep extends AzureNewDockerWizardStep implements TelemetryProperties {
  private JPanel rootConfigureContainerPanel;
  private ButtonGroup groupNewCredsType;
  private JRadioButton dockerHostImportKeyvaultCredsRadioButton;
  private JComboBox<AzureDockerCertVault> dockerHostImportKeyvaultComboBox;
  private JRadioButton dockerHostNewCredsRadioButton;
  private JTabbedPane credsTabbedPane;
  private JPanel vmCredsPanel;
  private JTextField dockerHostUsernameTextField;
  private JPasswordField dockerHostFirstPwdField;
  private JPasswordField dockerHostSecondPwdField;
  private ButtonGroup groupSSH;
  private JRadioButton dockerHostNoSshRadioButton;
  private JRadioButton dockerHostAutoSshRadioButton;
  private JRadioButton dockerHostImportSshRadioButton;
  private TextFieldWithBrowseButton dockerHostImportSSHBrowseTextField;
  private JPanel daemonCredsPanel;
  private JTextField dockerDaemonPortTextField;
  private ButtonGroup groupTLS;
  private JRadioButton dockerHostNoTlsRadioButton;
  private JRadioButton dockerHostAutoTlsRadioButton;
  private JRadioButton dockerHostImportTlsRadioButton;
  private TextFieldWithBrowseButton dockerHostImportTLSBrowseTextField;
  private JCheckBox dockerHostSaveCredsCheckBox;
  private JTextField dockerHostNewKeyvaultTextField;
  private JLabel dockerDaemonPortLabel;
  private JLabel dockerHostNewKeyvaultLabel;
  private JLabel dockerHostImportTLSBrowseLabel;
  private JLabel dockerHostUsernameLabel;
  private JLabel dockerHostFirstPwdLabel;
  private JLabel dockerHostImportSSHBrowseLabel;
  private JLabel dockerHostImportKeyvaultComboLabel;
  private JLabel dockerHostPwdLabel;

  private AzureNewDockerWizardModel model;
  private AzureDockerHostsManager dockerManager;
  private DockerHost newHost;

  public AzureNewDockerLoginStep(String title, AzureNewDockerWizardModel model) {
    // TODO: The message should go into the plugin property file that handles the various dialog titles
    super(title, "Configure log in credentials and port settings");
    this.model = model;
    this.dockerManager = model.getDockerManager();
    this.newHost = model.getDockerHost();

    initDialog();
    dockerHostImportKeyvaultCredsRadioButton.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent e) {
        dockerHostImportKeyvaultComboBox.setEnabled(true);

        dockerHostUsernameLabel.setVisible(false);
        dockerHostUsernameTextField.setEnabled(false);
        dockerHostFirstPwdLabel.setVisible(false);
        dockerHostFirstPwdField.setEnabled(false);
        dockerHostSecondPwdField.setEnabled(false);
        dockerHostNoSshRadioButton.setEnabled(false);
        dockerHostAutoSshRadioButton.setEnabled(false);
        dockerHostImportSshRadioButton.setEnabled(false);
        dockerHostImportSSHBrowseLabel.setVisible(false);
        dockerHostImportSSHBrowseTextField.setEnabled(false);
        dockerHostNoTlsRadioButton.setEnabled(false);
        dockerHostAutoTlsRadioButton.setEnabled(false);
        dockerHostImportTlsRadioButton.setEnabled(false);
        dockerHostImportTLSBrowseLabel.setVisible(false);
        dockerHostImportTLSBrowseTextField.setEnabled(false);
      }
    });
    dockerHostNewCredsRadioButton.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent e) {
        dockerHostImportKeyvaultComboBox.setEnabled(false);

        dockerHostUsernameTextField.setEnabled(true);
        dockerHostFirstPwdField.setEnabled(true);
        dockerHostSecondPwdField.setEnabled(true);
        dockerHostNoSshRadioButton.setEnabled(true);
        dockerHostAutoSshRadioButton.setEnabled(true);
        dockerHostImportSshRadioButton.setEnabled(true);
        if (dockerHostImportSshRadioButton.isSelected()) {
          dockerHostImportSSHBrowseTextField.setEnabled(true);
        }
        dockerDaemonPortTextField.setEnabled(true);
        dockerHostNoTlsRadioButton.setEnabled(true);
        dockerHostAutoTlsRadioButton.setEnabled(true);
        dockerHostImportTlsRadioButton.setEnabled(true);
        if (dockerHostImportTlsRadioButton.isSelected()) {
          dockerHostImportTLSBrowseTextField.setEnabled(true);
        }

      }
    });
    dockerHostNoSshRadioButton.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent e) {
        dockerHostPwdLabel.setText("(Required)");
        dockerHostImportSSHBrowseTextField.setEnabled(false);
        dockerHostImportSSHBrowseLabel.setVisible(false);
        setDialogButtonsState(doValidate(false) == null);
      }
    });
    dockerHostAutoSshRadioButton.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent e) {
        dockerHostPwdLabel.setText("(Optional)");
        dockerHostImportSSHBrowseTextField.setEnabled(false);
        dockerHostImportSSHBrowseLabel.setVisible(false);
        setDialogButtonsState(doValidate(false) == null);
      }
    });
    dockerHostImportSshRadioButton.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent e) {
        dockerHostPwdLabel.setText("(Optional)");
        dockerHostImportSSHBrowseTextField.setEnabled(true);
        setDialogButtonsState(doValidate(false) == null);
      }
    });
    dockerHostImportSSHBrowseTextField.addActionListener(UIUtils.createFileChooserListener(dockerHostImportSSHBrowseTextField, model.getProject(),
        FileChooserDescriptorFactory.createSingleFolderDescriptor()));

    dockerHostNoTlsRadioButton.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent e) {
        dockerHostImportTLSBrowseTextField.setEnabled(false);
        dockerHostImportTLSBrowseLabel.setVisible(false);
        setDialogButtonsState(doValidate(false) == null);
      }
    });
    dockerHostAutoTlsRadioButton.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent e) {
        dockerHostImportTLSBrowseTextField.setEnabled(false);
        dockerHostImportTLSBrowseLabel.setVisible(false);
        setDialogButtonsState(doValidate(false) == null);
      }
    });
    dockerHostImportTlsRadioButton.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent e) {
        dockerHostImportTLSBrowseTextField.setEnabled(true);
      }
    });
    dockerHostImportTLSBrowseTextField.addActionListener(UIUtils.createFileChooserListener(dockerHostImportTLSBrowseTextField, model.getProject(),
        FileChooserDescriptorFactory.createSingleFolderDescriptor()));
  }

  private void initDialog() {
    // New or import Key Vault credentials
    groupNewCredsType = new ButtonGroup();
    groupNewCredsType.add(dockerHostImportKeyvaultCredsRadioButton);
    groupNewCredsType.add(dockerHostNewCredsRadioButton);
    dockerHostNewCredsRadioButton.setSelected(true);
    dockerHostImportKeyvaultComboBox.setEnabled(false);
    dockerHostImportKeyvaultComboBox.setModel(new DefaultComboBoxModel<AzureDockerCertVault>(new Vector<AzureDockerCertVault>(dockerManager.getDockerKeyVaults())));
    dockerHostImportKeyvaultComboLabel.setVisible(false);

    groupSSH = new ButtonGroup();
    groupSSH.add(dockerHostNoSshRadioButton);
    groupSSH.add(dockerHostAutoSshRadioButton);
    groupSSH.add(dockerHostImportSshRadioButton);
    dockerHostAutoSshRadioButton.setSelected(true);
    dockerHostImportSSHBrowseLabel.setVisible(false);
    dockerHostImportSSHBrowseTextField.setEnabled(false);
    dockerHostImportSSHBrowseTextField.getTextField().setToolTipText(AzureDockerValidationUtils.getDockerHostSshDirectoryTip());
    dockerHostImportSSHBrowseTextField.getTextField().setInputVerifier(new InputVerifier() {
      @Override
      public boolean verify(JComponent input) {
        String text = dockerHostImportSSHBrowseTextField.getText();
        if (dockerHostImportSshRadioButton.isSelected() && (text == null || text.isEmpty() || !AzureDockerValidationUtils.validateDockerHostSshDirectory(text))) {
          dockerHostImportSSHBrowseLabel.setVisible(true);
          setDialogButtonsState(false);
          return false;
        } else {
          dockerHostImportSSHBrowseLabel.setVisible(false);
          setDialogButtonsState(doValidate(false) == null);
          return true;
        }
      }
    });
    dockerHostImportSSHBrowseTextField.getTextField().getDocument().addDocumentListener(resetDialogButtonsState(null));
    dockerHostUsernameLabel.setVisible(false);
    dockerHostUsernameTextField.setText(newHost.certVault.vmUsername);
    dockerHostUsernameTextField.setToolTipText(AzureDockerValidationUtils.getDockerHostUserNameTip());
    dockerHostUsernameTextField.setInputVerifier(new InputVerifier() {
      @Override
      public boolean verify(JComponent input) {
        String text = dockerHostUsernameTextField.getText();
        if (text == null || text.isEmpty() || !AzureDockerValidationUtils.validateDockerHostUserName(text)) {
          dockerHostUsernameLabel.setVisible(true);
          setDialogButtonsState(false);
          return false;
        } else {
          dockerHostUsernameLabel.setVisible(false);
          setDialogButtonsState(doValidate(false) == null);
          return true;
        }
      }
    });
    dockerHostUsernameTextField.getDocument().addDocumentListener(resetDialogButtonsState(null));
    dockerHostFirstPwdField.setInputVerifier(new InputVerifier() {
      @Override
      public boolean verify(JComponent input) {
        String text = new String(dockerHostFirstPwdField.getPassword());
        if (dockerHostFirstPwdField.getPassword().length > 0 && !text.isEmpty() && !AzureDockerValidationUtils.validateDockerHostPassword(text)) {
          dockerHostFirstPwdLabel.setVisible(true);
          setDialogButtonsState(false);
          return false;
        } else {
          dockerHostFirstPwdLabel.setVisible(false);
          setDialogButtonsState(doValidate(false) == null);
          return true;
        }
      }
    });
    dockerHostFirstPwdField.getDocument().addDocumentListener(resetDialogButtonsState(null));
    dockerHostFirstPwdLabel.setVisible(false);
    dockerHostFirstPwdField.setToolTipText(AzureDockerValidationUtils.getDockerHostPasswordTip());
    dockerHostSecondPwdField.setInputVerifier(new InputVerifier() {
      @Override
      public boolean verify(JComponent input) {
        String pwd1 = new String(dockerHostFirstPwdField.getPassword());
        String pwd2 = new String(dockerHostSecondPwdField.getPassword());
        if (dockerHostSecondPwdField.getPassword().length > 0 && !pwd2.isEmpty() && !pwd2.equals(pwd1)) {
          dockerHostFirstPwdLabel.setVisible(true);
          setDialogButtonsState(false);
          return false;
        } else {
          dockerHostFirstPwdLabel.setVisible(false);
          setDialogButtonsState(doValidate(false) == null);
          return true;
        }
      }
    });
    dockerHostSecondPwdField.getDocument().addDocumentListener(resetDialogButtonsState(null));
    dockerHostSecondPwdField.setToolTipText(AzureDockerValidationUtils.getDockerHostPasswordTip());

    dockerDaemonPortLabel.setVisible(false);
    dockerDaemonPortTextField.setText(newHost.port);
    dockerDaemonPortTextField.setToolTipText(AzureDockerValidationUtils.getDockerHostPortTip());
    dockerDaemonPortTextField.setInputVerifier(new InputVerifier() {
      @Override
      public boolean verify(JComponent input) {
        String text = dockerDaemonPortTextField.getText();
        if (text == null || text.isEmpty() || !AzureDockerValidationUtils.validateDockerHostPort(text)) {
          dockerDaemonPortLabel.setVisible(true);
          setDialogButtonsState(false);
          return false;
        } else {
          dockerDaemonPortLabel.setVisible(false);
          setDialogButtonsState(doValidate(false) == null);
          return true;
        }
      }
    });
    dockerDaemonPortTextField.getDocument().addDocumentListener(resetDialogButtonsState(null));
    groupTLS = new ButtonGroup();
    groupTLS.add(dockerHostNoTlsRadioButton);
    groupTLS.add(dockerHostAutoTlsRadioButton);
    groupTLS.add(dockerHostImportTlsRadioButton);
    dockerHostAutoTlsRadioButton.setSelected(true);
    dockerHostImportTLSBrowseLabel.setVisible(false);
    dockerHostImportTLSBrowseTextField.setEnabled(false);
    dockerHostImportTLSBrowseTextField.getTextField().setToolTipText(AzureDockerValidationUtils.getDockerHostTlsDirectoryTip());
    dockerHostImportTLSBrowseTextField.getTextField().setInputVerifier(new InputVerifier() {
      @Override
      public boolean verify(JComponent input) {
        String text = dockerHostImportTLSBrowseTextField.getText();
        if (dockerHostImportTlsRadioButton.isSelected() && (text == null || text.isEmpty() || !AzureDockerValidationUtils.validateDockerHostTlsDirectory(text))) {
          dockerHostImportTLSBrowseLabel.setVisible(true);
          setDialogButtonsState(false);
          return false;
        } else {
          dockerHostImportTLSBrowseLabel.setVisible(false);
          setDialogButtonsState(doValidate(false) == null);
          return true;
        }
      }
    });
    dockerHostImportTLSBrowseTextField.getTextField().getDocument().addDocumentListener(resetDialogButtonsState(null));

    dockerHostSaveCredsCheckBox.setSelected(true);
    dockerHostNewKeyvaultLabel.setVisible(false);
    dockerHostNewKeyvaultTextField.setText(newHost.certVault.name);
    dockerHostNewKeyvaultTextField.setToolTipText(AzureDockerValidationUtils.getDockerHostKeyvaultNameTip());
    dockerHostNewKeyvaultTextField.setInputVerifier(new InputVerifier() {
      @Override
      public boolean verify(JComponent input) {
        String text = dockerHostNewKeyvaultTextField.getText();
        if (text == null || text.isEmpty() || !AzureDockerValidationUtils.validateDockerHostKeyvaultName(text, dockerManager, false)) {
          dockerHostNewKeyvaultLabel.setVisible(true);
          setDialogButtonsState(false);
          return false;
        } else {
          dockerHostNewKeyvaultLabel.setVisible(false);
          setDialogButtonsState(doValidate(false) == null);
          return true;
        }
      }
    });
    dockerHostNewKeyvaultTextField.getDocument().addDocumentListener(resetDialogButtonsState(null));
  }

  public DockerHost getDockerHost() {
    return newHost;
  }

  @Override
  public ValidationInfo doValidate() {
    return doValidate(true);
  }

  private ValidationInfo doValidate(boolean shakeOnError) {
    if (dockerHostImportKeyvaultCredsRadioButton.isSelected()) {
      // read key vault secrets and set the credentials for the new host
      AzureDockerCertVault certVault = (AzureDockerCertVault) dockerHostImportKeyvaultComboBox.getSelectedItem();
      if (certVault == null) {
        ValidationInfo info = AzureDockerUIResources.validateComponent("Missing vault", rootConfigureContainerPanel, dockerHostImportKeyvaultComboBox, dockerHostImportKeyvaultComboLabel);
        setDialogButtonsState(false);
        if (shakeOnError) {
          model.DialogShaker(info);
        }
        return info;
      }

      dockerHostImportKeyvaultComboLabel.setVisible(false);

      newHost.certVault.name = certVault.name;
      newHost.certVault.resourceGroupName = certVault.resourceGroupName;
      newHost.certVault.region = certVault.region;
      newHost.certVault.uri = certVault.uri;
      AzureDockerCertVaultOps.copyVaultLoginCreds(newHost.certVault, certVault);
      AzureDockerCertVaultOps.copyVaultSshKeys(newHost.certVault, certVault);
      AzureDockerCertVaultOps.copyVaultTlsCerts(newHost.certVault, certVault);

      // create a weak link (resource tag) between the virtual machine and the key vault
      //  we will not create/update the key vault unless the user checks the specific option
      newHost.certVault.hostName = null;
      newHost.hasKeyVault = true;
    } else {
      // reset key vault info
      newHost.hasKeyVault = false;
      newHost.certVault.name = null;
      newHost.certVault.uri = null;

      dockerHostImportKeyvaultComboLabel.setVisible(false);
      // User name
      String vmUsername = dockerHostUsernameTextField.getText();
      if (vmUsername == null || vmUsername.isEmpty() ||
          !AzureDockerValidationUtils.validateDockerHostUserName(vmUsername))
      {
        ValidationInfo info = AzureDockerUIResources.validateComponent("Missing username", vmCredsPanel, dockerHostUsernameTextField, dockerHostUsernameLabel);
        credsTabbedPane.setSelectedComponent(vmCredsPanel);
        setDialogButtonsState(false);
        if (shakeOnError) {
          model.DialogShaker(info);
        }
        return info;
      }
      newHost.certVault.vmUsername = vmUsername;

      // Password login
      String vmPwd1 = new String(dockerHostFirstPwdField.getPassword());
      String vmPwd2 = new String(dockerHostSecondPwdField.getPassword());
      if ((dockerHostNoSshRadioButton.isSelected() || dockerHostFirstPwdField.getPassword().length > 0 ||
          dockerHostSecondPwdField.getPassword().length > 0) &&
          (vmPwd1.isEmpty() || vmPwd2.isEmpty() || !vmPwd1.equals(vmPwd2) ||
          !AzureDockerValidationUtils.validateDockerHostPassword(vmPwd1)))
      {
        ValidationInfo info = AzureDockerUIResources.validateComponent("Incorrect password", vmCredsPanel, dockerHostFirstPwdField, dockerHostFirstPwdLabel);
        credsTabbedPane.setSelectedComponent(vmCredsPanel);
        setDialogButtonsState(false);
        if (shakeOnError) {
          model.DialogShaker(info);
        }
        return info;
      }
      dockerHostFirstPwdLabel.setVisible(false);
      if (dockerHostFirstPwdField.getPassword().length > 0) {
        newHost.certVault.vmPwd = new String(dockerHostFirstPwdField.getPassword());
        newHost.hasPwdLogIn = true;
      } else {
        newHost.certVault.vmPwd = null;
        newHost.hasPwdLogIn = false;
      }

      // SSH key auto generated
      if (dockerHostAutoSshRadioButton.isSelected()) {
        AzureDockerCertVault certVault = AzureDockerCertVaultOps.generateSSHKeys(null, "SSH keys for " + newHost.name);
        AzureDockerCertVaultOps.copyVaultSshKeys(newHost.certVault, certVault);
        newHost.hasSSHLogIn = true;
      }

      if (dockerHostNoSshRadioButton.isSelected()) {
        newHost.hasSSHLogIn = false;
        newHost.certVault.sshKey = null;
        newHost.certVault.sshPubKey = null;
      }

      // SSH key imported from local file directory
      if (dockerHostImportSshRadioButton.isSelected()) {
        if (dockerHostImportSSHBrowseTextField.getText() == null || dockerHostImportSSHBrowseTextField.getText().isEmpty() ||
            !AzureDockerValidationUtils.validateDockerHostSshDirectory(dockerHostImportSSHBrowseTextField.getText())) {
          ValidationInfo info = AzureDockerUIResources.validateComponent("SSH key files were not found in the selected directory", vmCredsPanel, dockerHostImportSSHBrowseTextField, dockerHostImportSSHBrowseLabel);
          credsTabbedPane.setSelectedComponent(vmCredsPanel);
          setDialogButtonsState(false);
          if (shakeOnError) {
            model.DialogShaker(info);
          }
          return info;
        } else {
          AzureDockerCertVault certVault = AzureDockerCertVaultOps.getSSHKeysFromLocalFile(dockerHostImportSSHBrowseTextField.getText());
          AzureDockerCertVaultOps.copyVaultSshKeys(newHost.certVault, certVault);
          newHost.hasSSHLogIn = true;
        }
      }

      // No Docker daemon security
      if (dockerHostNoTlsRadioButton.isSelected()) {
        newHost.isTLSSecured = false;
      }

      // TLS certs auto generated
      if (dockerHostAutoTlsRadioButton.isSelected()) {
        AzureDockerCertVault certVault = AzureDockerCertVaultOps.generateTLSCerts("TLS certs for " + newHost.name);
        AzureDockerCertVaultOps.copyVaultTlsCerts(newHost.certVault, certVault);
        newHost.isTLSSecured = true;
      }

      // TLS certs imported from local file directory
      if (dockerHostImportTlsRadioButton.isSelected()) {
        if (dockerHostImportTLSBrowseTextField.getText() == null || dockerHostImportTLSBrowseTextField.getText().isEmpty() ||
            !AzureDockerValidationUtils.validateDockerHostTlsDirectory(dockerHostImportTLSBrowseTextField.getText())) {
          ValidationInfo info = AzureDockerUIResources.validateComponent("TLS certificates files were not found in the selected directory", vmCredsPanel, dockerHostImportTLSBrowseTextField, dockerHostImportTLSBrowseLabel);
          credsTabbedPane.setSelectedComponent(vmCredsPanel);
          setDialogButtonsState(false);
          if (shakeOnError) {
            model.DialogShaker(info);
          }
          return info;
        } else {
          AzureDockerCertVault certVault = AzureDockerCertVaultOps.getTLSCertsFromLocalFile(dockerHostImportTLSBrowseTextField.getText());
          AzureDockerCertVaultOps.copyVaultTlsCerts(newHost.certVault, certVault);
          newHost.isTLSSecured = true;
        }
      }
    }

    // Docker daemon port settings
    if (dockerDaemonPortTextField.getText() == null || dockerDaemonPortTextField.getText().isEmpty() ||
        !AzureDockerValidationUtils.validateDockerHostPort(dockerDaemonPortTextField.getText()))
    {
      ValidationInfo info = AzureDockerUIResources.validateComponent("Invalid Docker daemon port settings", daemonCredsPanel, dockerDaemonPortTextField, dockerDaemonPortLabel);
      credsTabbedPane.setSelectedComponent(daemonCredsPanel);
      setDialogButtonsState(false);
      if (shakeOnError) {
        model.DialogShaker(info);
      }
      return info;
    }
    newHost.port = dockerDaemonPortTextField.getText();

    // create new key vault for storing the credentials
    if (dockerHostSaveCredsCheckBox.isSelected()) {
      if (dockerHostNewKeyvaultTextField.getText() == null || dockerHostNewKeyvaultTextField.getText().isEmpty() ||
          !AzureDockerValidationUtils.validateDockerHostKeyvaultName(dockerHostNewKeyvaultTextField.getText(), dockerManager, true)) {
        ValidationInfo info = AzureDockerUIResources.validateComponent("Incorrect Azure Key Vault", rootConfigureContainerPanel, dockerHostNewKeyvaultTextField, dockerHostNewKeyvaultLabel);
        setDialogButtonsState(false);
        return info;
      } else {
        newHost.hasKeyVault = true;
        newHost.certVault.name = dockerHostNewKeyvaultTextField.getText();
        newHost.certVault.hostName = (newHost.name != null) ? newHost.name : null;
        newHost.certVault.region = (newHost.hostVM.region != null) ? newHost.hostVM.region : null;
        newHost.certVault.resourceGroupName = (newHost.hostVM.resourceGroupName != null) ? newHost.hostVM.resourceGroupName : null;
        newHost.certVault.uri = (newHost.hostVM.region != null && newHost.hostVM.resourceGroupName != null) ?
            "https://" + newHost.certVault.name + ".vault.azure.net" :
            null;
      }
    } else {
      newHost.certVault.hostName = null;
    }

    setDialogButtonsState(true);

    return null;
  }

  private void setFinishButtonState(boolean finishButtonState) {
    model.getCurrentNavigationState().FINISH.setEnabled(finishButtonState);
  }

  private void setPreviousButtonState(boolean previousButtonState) {
    model.getCurrentNavigationState().PREVIOUS.setEnabled(previousButtonState);
  }

  @Override
  protected void setDialogButtonsState(boolean buttonsState) {
    setFinishButtonState(buttonsState);
    setPreviousButtonState(buttonsState);
  }

  @Override
  public JComponent prepare(final WizardNavigationState state) {
    rootConfigureContainerPanel.revalidate();
    setDialogButtonsState(true);

    return rootConfigureContainerPanel;
  }

  @Override
  public WizardStep onNext(final AzureNewDockerWizardModel model) {
    if (doValidate() == null) {
      return super.onNext(model);
    } else {
      return this;
    }
  }

  @Override
  public boolean onFinish() {
    return model.doValidate() == null && super.onFinish();
  }

  @Override
  public boolean onCancel() {
    model.finishedOK = true;

    return super.onCancel();
  }

  @Override
  public Map<String, String> toProperties() {
    return model.toProperties();
  }

}
