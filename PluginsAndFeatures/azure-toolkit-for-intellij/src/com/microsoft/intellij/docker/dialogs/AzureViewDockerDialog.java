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
package com.microsoft.intellij.docker.dialogs;

import com.intellij.openapi.project.Project;
import com.microsoft.azure.docker.AzureDockerHostsManager;
import com.microsoft.azure.docker.model.DockerHost;
import com.microsoft.azure.docker.ops.AzureDockerCertVaultOps;
import com.microsoft.azuretools.authmanage.models.SubscriptionDetail;
import com.microsoft.intellij.ui.components.AzureDialogWrapper;
import com.microsoft.intellij.util.PluginUtil;
import org.jdesktop.swingx.JXHyperlink;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

public class AzureViewDockerDialog extends AzureDialogWrapper {
  private final String defaultTitle = "Viewing %s";
  public static final int OK_EXIT_CODE = 0;
  public static final int CANCEL_EXIT_CODE = 1;
  public static final int CLOSE_EXIT_CODE = 1;
  public static final int UPDATE_EXIT_CODE = 3;
  private JPanel mainPanel;
  private JTabbedPane tabbedPane1;
  private JXHyperlink dockerHostAuthUpdateHyperlink;
  private JXHyperlink dockerHostSshExportHyperlink;
  private JXHyperlink dockerHostTlsExportHyperlink;
  private JTextField dockerHostNameTextField;
  private JTextField dockerHostUrlTextField;
  private JTextField dockerHostLocationTextField;
  private JTextField dockerHostStatusTextField;
  private JTextField dockerHostUsernameTextField;
  private JTextField dockerHostKeyvaultTextField;
  private JTextField dockerHostOSTypeTextField;
  private JTextField dockerHostVMSizeTextField;
  private JTextField dockerHostRGNameTextField;
  private JTextField dockerHostVnetNameAddrTextField;
  private JTextField dockerHostSubnetNameAddrTextField;
  private JTextField dockerHostStorageNameTypeTextField;
  private JTextField dockerHostPortTextField;
  private JTextField dockerHostPwdLoginTextField;
  private JTextField dockerHostSshLoginTextField;
  private JTextField dockerHostTlsAuthTextField;
  private JTextPane dockerHostKeyvaultTextPane;
  private JTextField dockerHostSidTextField;
  private JTextField dockerHostPublicIpTextField;
  private JTextField dockerHostPrivateIpTextField;

  private Project project;
  private DockerHost dockerHost;
  private AzureDockerHostsManager dockerManager;
  private int exitCode;

  private void initDefaultUIValues(String updating) {
    // Docker VM info
    setTextField(dockerHostNameTextField, dockerHost.name);
    setTextField(dockerHostUrlTextField, dockerHost.apiUrl);
    setTextField(dockerHostSidTextField, dockerHost.sid);
    setSubscription(new SubscriptionDetail(dockerHost.sid, null, null, true));
    setTextField(dockerHostLocationTextField, dockerHost.hostVM.region);
    setTextField(dockerHostStatusTextField, (updating != null) ?
        dockerHost.state.toString() + updating :
        dockerHost.state.toString()
    );

    // Docker VM settings
    setTextField(dockerHostOSTypeTextField, dockerHost.hostOSType.toString());
    // TODO: enable resizing of the current VM -> see VirtualMachine::availableSizes() and update.withSize();
    setTextField(dockerHostVMSizeTextField, (updating != null) ?
        dockerHost.hostVM.vmSize + updating :
        dockerHost.hostVM.vmSize
    );
    setTextField(dockerHostRGNameTextField, dockerHost.hostVM.resourceGroupName);
    setTextField(dockerHostVnetNameAddrTextField, String.format("%s (%s)", dockerHost.hostVM.vnetName, dockerHost.hostVM.vnetAddressSpace));
    setTextField(dockerHostSubnetNameAddrTextField, String.format("%s (%s)", dockerHost.hostVM.subnetName, dockerHost.hostVM.subnetAddressRange));
    setTextField(dockerHostPublicIpTextField, String.format("%s (%s)", dockerHost.hostVM.publicIp, dockerHost.hostVM.publicIpName));
    setTextField(dockerHostPrivateIpTextField, dockerHost.hostVM.privateIp);
    setTextField(dockerHostStorageNameTypeTextField, String.format("%s (%s)", dockerHost.hostVM.storageAccountName, dockerHost.hostVM.storageAccountType));

    // Docker VM log in settings
    dockerHostAuthUpdateHyperlink.setEnabled(!dockerHost.isUpdating);
    String username = (dockerHost.certVault != null && dockerHost.certVault.vmUsername != null) ?
        dockerHost.certVault.vmUsername : "-unknown-";
    setTextField(dockerHostUsernameTextField, (updating != null) ?
        username + updating :
        username
    );
    setTextField(dockerHostPwdLoginTextField, (updating != null) ?
        (dockerHost.hasPwdLogIn ? "Yes" : (dockerHost.certVault == null) ? "-unknown- " : "No") + updating :
        (dockerHost.hasPwdLogIn ? "Yes" : (dockerHost.certVault == null) ? "-unknown- " : "No")
    );
    setTextField(dockerHostSshLoginTextField, (updating != null) ?
        (dockerHost.hasSSHLogIn ? "Yes" : (dockerHost.certVault == null) ? "-unknown- " : "No") + updating :
        (dockerHost.hasSSHLogIn ? "Yes" : (dockerHost.certVault == null) ? "-unknown- " : "No")
    );
    dockerHostSshExportHyperlink.setEnabled(!dockerHost.isUpdating && dockerHost.hasSSHLogIn);

    // Docker Daemon settings
    setTextField(dockerHostTlsAuthTextField, (updating != null) ?
        (dockerHost.isTLSSecured ? "Using TLS certificates" : (dockerHost.certVault == null) ? "-unknown- " : "Open/unsecured access") + updating :
        (dockerHost.isTLSSecured ? "Using TLS certificates" : (dockerHost.certVault == null) ? "-unknown- " : "Open/unsecured access")
    );
    dockerHostTlsExportHyperlink.setEnabled(!dockerHost.isUpdating && dockerHost.isTLSSecured);

    setTextField(dockerHostPortTextField, (updating != null) ?
            dockerHost.port + updating :
            dockerHost.port
    );

    // Docker Keyvault settings
    if (dockerHost.certVault != null &&
        dockerHost.hostVM.vaultName != null && !dockerHost.hostVM.vaultName.isEmpty() &&
        dockerHost.certVault.uri != null && !dockerHost.certVault.uri.isEmpty()) {
      setTextField(dockerHostKeyvaultTextField, (updating != null) ?
          dockerHost.certVault.uri  + updating :
          dockerHost.certVault.uri);
      dockerHostKeyvaultTextPane.setVisible(false);
    } else if (dockerHost.hostVM.vaultName != null && !dockerHost.hostVM.vaultName.isEmpty()) {
      setTextField(dockerHostKeyvaultTextField, String.format("Error reading https://%s.vault.azure.net", dockerHost.hostVM.vaultName));
      dockerHostKeyvaultTextPane.setVisible(true);
    } else {
      setTextField(dockerHostKeyvaultTextField, "Not using Key Vault");
      dockerHostKeyvaultTextPane.setVisible(false);
    }

    dockerHostKeyvaultTextPane.setFont(UIManager.getFont("Label.font"));

    exitCode = CLOSE_EXIT_CODE;

//    myClickApplyAction.setEnabled(!editableHost.originalDockerHost.equalsTo(dockerHost));
  }

  private void setTextField(JTextField textField, String text) {
    textField.setText(text != null ? text : "-unknown-");
    textField.setEditable(false);
    textField.setBackground(null);
    textField.setBorder(null);
  }

  public AzureViewDockerDialog(Project project, DockerHost host, AzureDockerHostsManager dockerManager) {
    super(project, true);

    this.project = project;
    this.dockerHost = host;
    this.dockerManager = dockerManager;
    setModal(true);

    init();
    dockerHostAuthUpdateHyperlink.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent e) {
        onUpdateLoginCreds();
      }
    });
    dockerHostSshExportHyperlink.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent e) {
        onExportSshKeys();
      }
    });
    dockerHostTlsExportHyperlink.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent e) {
        onExportTlsCerts();
      }
    });

    if (dockerHost.isUpdating) {
      initDefaultUIValues(" (updating...)");
    } else {
      initDefaultUIValues(null);
    }
    setTitle(String.format(defaultTitle, dockerHost.name));
  }

  private void onUpdateLoginCreds() {
    exitCode = UPDATE_EXIT_CODE;
    doOKAction();
  }

  private void onExportSshKeys() {
    if (dockerHost.hasSSHLogIn && dockerHost.certVault != null) {
      AzureExportDockerSshKeysDialog exportDockerSshKeysDialog = new AzureExportDockerSshKeysDialog(project);
      exportDockerSshKeysDialog.show();

      if (exportDockerSshKeysDialog.getExitCode() == 0) {
        try {
          AzureDockerCertVaultOps.saveSshKeysToLocalFiles(exportDockerSshKeysDialog.getPath(), dockerHost.certVault);
        }
        catch (Exception e){
          String msg = "An error occurred while attempting to export the SSh keys.\n" + e.getMessage();
          PluginUtil.displayErrorDialogAndLog("Error", msg, e);
        }
      }
    }
  }

  private void onExportTlsCerts() {
    if (dockerHost.isTLSSecured && dockerHost.certVault != null) {
      AzureExportDockerTlsKeysDialog exportDockerTlsKeysDialog = new AzureExportDockerTlsKeysDialog(project);
      exportDockerTlsKeysDialog.show();

      if (exportDockerTlsKeysDialog.getExitCode() == 0) {
        try {
          AzureDockerCertVaultOps.saveTlsCertsToLocalFiles(exportDockerTlsKeysDialog.getPath(), dockerHost.certVault);
        }
        catch (Exception e){
          String msg = "An error occurred while attempting to export the TLS keys.\n" + e.getMessage();
          PluginUtil.displayErrorDialogAndLog("Error", msg, e);
        }
      }
    }
  }

  public int getInternalExitCode() {
    return exitCode;
  }

  @Nullable
  @Override
  protected JComponent createCenterPanel() {
    return mainPanel;
  }

  @Nullable
  @Override
  protected String getHelpId() {
    return null;
  }

  @Override
  protected void doOKAction() {
    super.doOKAction();
  }

  @NotNull
  @Override
  protected Action[] createActions() {
    Action okAction = getOKAction();
    okAction.putValue(Action.NAME, "Close");
    return new Action[] {okAction};
  }


}
