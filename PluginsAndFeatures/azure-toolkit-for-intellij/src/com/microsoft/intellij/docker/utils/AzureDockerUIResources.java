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

package com.microsoft.intellij.docker.utils;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.openapi.ui.ValidationInfo;
import com.jcraft.jsch.Session;
import com.microsoft.azure.docker.AzureDockerHostsManager;
import com.microsoft.azure.docker.model.AzureDockerCertVault;
import com.microsoft.azure.docker.model.AzureDockerImageInstance;
import com.microsoft.azure.docker.model.DockerHost;
import com.microsoft.azure.docker.model.EditableDockerHost;
import com.microsoft.azure.docker.ops.AzureDockerCertVaultOps;
import com.microsoft.azure.docker.ops.AzureDockerSSHOps;
import com.microsoft.azure.docker.ops.AzureDockerVMOps;
import com.microsoft.azure.docker.ops.utils.AzureDockerUtils;
import com.microsoft.azure.keyvault.KeyVaultClient;
import com.microsoft.azure.management.Azure;
import com.microsoft.azuretools.authmanage.AuthMethodManager;
import com.microsoft.azuretools.sdkmanage.AzureManager;
import com.microsoft.azuretools.telemetry.AppInsightsClient;
import com.microsoft.intellij.docker.dialogs.AzureInputDockerLoginCredsDialog;
import com.microsoft.intellij.docker.wizards.publish.AzureSelectDockerWizardDialog;
import com.microsoft.intellij.docker.wizards.publish.AzureSelectDockerWizardModel;
import com.microsoft.intellij.util.PluginUtil;
import com.microsoft.tooling.msservices.components.DefaultLoader;

import javax.swing.*;
import java.util.Date;

import static com.microsoft.intellij.ui.messages.AzureBundle.message;

public class AzureDockerUIResources {
  private static final Logger LOGGER = Logger.getInstance(AzureDockerUIResources.class);
  public static boolean CANCELED = false;

  public static void updateAzureResourcesWithProgressDialog(Project project) {
    ProgressManager.getInstance().run(new Task.Modal(project, "Loading Azure Resources", true) {
      @Override
      public void run(ProgressIndicator progressIndicator) {
        try {
          progressIndicator.setFraction(.05);
          AzureManager azureAuthManager = AuthMethodManager.getInstance().getAzureManager();

          if (progressIndicator.isCanceled()) {
            return;
          }

          // not signed in
          if (azureAuthManager == null) {
            return;
          }

          progressIndicator.setFraction(.1);
          AzureDockerHostsManager dockerManager = AzureDockerHostsManager.getAzureDockerHostsManagerEmpty(azureAuthManager);
          if (progressIndicator.isCanceled()) {
            return;
          }

          progressIndicator.setFraction(.2);
          progressIndicator.setText2("Retrieving the subscription details...");
          dockerManager.refreshDockerSubscriptions();
          if (progressIndicator.isCanceled()) {
            return;
          }

          progressIndicator.setFraction(.3);
          progressIndicator.setText2("Retrieving the key vault...");
          dockerManager.refreshDockerVaults();
          if (progressIndicator.isCanceled()) {
            return;
          }

          progressIndicator.setFraction(.45);
          progressIndicator.setText2("Retrieving the key vault details...");
          dockerManager.refreshDockerVaultDetails();
          if (progressIndicator.isCanceled()) {
            return;
          }

          progressIndicator.setFraction(.7);
          progressIndicator.setText2("Retrieving the network details...");
          dockerManager.refreshDockerVnetDetails();
          if (progressIndicator.isCanceled()) {
            return;
          }

          progressIndicator.setFraction(.8);
          progressIndicator.setText2("Retrieving the storage account details...");
          dockerManager.refreshDockerStorageAccountDetails();
          if (progressIndicator.isCanceled()) {
            return;
          }

          progressIndicator.setIndeterminate(true);
          progressIndicator.setText2("Retrieving the Docker virtual machines details...");
          dockerManager.refreshDockerHostDetails();
          if (progressIndicator.isCanceled()) {
            return;
          }

          CANCELED = false;
          progressIndicator.setIndeterminate(true);

        } catch (Exception ex) {
          ex.printStackTrace();
          LOGGER.error("updateAzureResourcesWithProgressDialog", ex);
          CANCELED = true;
        }
      }

      @Override
      public void onCancel() {
        CANCELED = true;
        super.onCancel();
      }
    });
  }

  /*
   * Opens a confirmation dialog box for the user to chose the delete action
   * @return integer representing the user's choise
   *  -1 - action was canceled
   *   0 - action was canceled
   *   1 - delete VM only
   *   2 - delete VM and associated resources (vnet, publicIp, nic, nsg)
   */

  public static int deleteAzureDockerHostConfirmationDialog(Azure azureClient, DockerHost dockerHost) {
    String promptMessageDeleteAll = String.format("This operation will delete virtual machine %s and its resources:\n" +
            "\t - network interface: %s\n" +
            "\t - public IP: %s\n" +
            "\t - virtual network: %s\n" +
            "The associated disks and storage account will not be deleted\n",
        dockerHost.hostVM.name,
        dockerHost.hostVM.nicName,
        dockerHost.hostVM.publicIpName,
        dockerHost.hostVM.vnetName);

    String promptMessageDelete = String.format("This operation will delete virtual machine %s.\n" +
            "The associated disks and storage account will not be deleted\n\n" +
            "Are you sure you want to continue?\n",
        dockerHost.hostVM.name);

    String[] options;
    String promptMessage;

    if (AzureDockerVMOps.isDeletingDockerHostAllSafe(
        azureClient,
        dockerHost.hostVM.resourceGroupName,
        dockerHost.hostVM.name)) {
      promptMessage = promptMessageDeleteAll;
      options = new String[]{"Cancel", "Delete VM Only", "Delete All"};
    } else {
      promptMessage = promptMessageDelete;
      options = new String[]{"Cancel", "Delete"};
    }

    int dialogReturn = JOptionPane.showOptionDialog(null,
        promptMessage,
        "Delete Docker Host",
        JOptionPane.YES_NO_OPTION,
        JOptionPane.QUESTION_MESSAGE,
        PluginUtil.getIcon("/icons/logwarn.png"),
        options,
        null);

    switch (dialogReturn) {
      case 0:
        if (AzureDockerUtils.DEBUG) System.out.format("Delete Docker Host op was canceled %s\n", dialogReturn);
        break;
      case 1:
        if (AzureDockerUtils.DEBUG) System.out.println("Delete VM only: " + dockerHost.name);
        break;
      case 2:
        if (AzureDockerUtils.DEBUG) System.out.println("Delete VM and resources: " + dockerHost.name);
        break;
      default:
        if (AzureDockerUtils.DEBUG) System.out.format("Delete Docker Host op was canceled %s\n", dialogReturn);
    }

    return dialogReturn;
  }

  public static void deleteDockerHost(Project project, Azure azureClient, DockerHost dockerHost, int option, Runnable runnable) {
    String progressMsg = (option == 2) ? String.format("Deleting Virtual Machine %s and Its Resources...", dockerHost.name) :
        String.format("Deleting Docker Host %s...", dockerHost.name);

    DefaultLoader.getIdeHelper().runInBackground(project, "Deleting Docker Host", false, true, progressMsg, new Runnable() {
      @Override
      public void run() {
        try {
          if (option == 2) {
            AzureDockerVMOps.deleteDockerHostAll(azureClient, dockerHost.hostVM.resourceGroupName, dockerHost.hostVM.name);
          } else {
            AzureDockerVMOps.deleteDockerHost(azureClient, dockerHost.hostVM.resourceGroupName, dockerHost.hostVM.name);
          }
          DefaultLoader.getIdeHelper().runInBackground(project, "Updating Docker Hosts Details ", false, true, "Updating Docker hosts details...", new Runnable() {
            @Override
            public void run() {
              try {
                AzureDockerHostsManager dockerManager = AzureDockerHostsManager.getAzureDockerHostsManagerEmpty(null);
                dockerManager.refreshDockerHostDetails();

                if (runnable != null) {
                  runnable.run();
                }
              } catch (Exception ee) {
                if (AzureDockerUtils.DEBUG) ee.printStackTrace();
                LOGGER.error("onRemoveDockerHostAction", ee);
              }
            }
          });
        } catch (Exception e) {
          ApplicationManager.getApplication().invokeLater(new Runnable() {
            @Override
            public void run() {
              if (AzureDockerUtils.DEBUG) e.printStackTrace();
              LOGGER.error("onRemoveDockerHostAction", e);
              PluginUtil.displayErrorDialog("Delete Docker Host Error", String.format("Unexpected error detected while deleting Docker host %s:\n\n%s", dockerHost.name, e.getMessage()));
            }
          });
        }
      }
    });
  }

  public static void publish2DockerHostContainer(Project project) {
    try {
      AzureDockerUIResources.CANCELED = false;

      AzureManager azureAuthManager = AuthMethodManager.getInstance().getAzureManager();
      // not signed in
      if (azureAuthManager == null) {
        System.out.println("ERROR! Not signed in!");
        return;
      }

      AzureDockerHostsManager dockerManager = AzureDockerHostsManager.getAzureDockerHostsManagerEmpty(azureAuthManager);

      if (!dockerManager.isInitialized()) {
        AzureDockerUIResources.updateAzureResourcesWithProgressDialog(project);
        if (AzureDockerUIResources.CANCELED) {
          return;
        }
      }

      DockerHost dockerHost = (dockerManager.getDockerPreferredSettings() != null) ? dockerManager.getDockerHostForURL(dockerManager.getDockerPreferredSettings().dockerApiName) : null;
      AzureDockerImageInstance dockerImageDescription = dockerManager.getDefaultDockerImageDescription(project.getName(), dockerHost);

      AzureSelectDockerWizardModel model = new AzureSelectDockerWizardModel(project, dockerManager, dockerImageDescription);
      AzureSelectDockerWizardDialog wizard = new AzureSelectDockerWizardDialog(model);
      if (dockerHost != null) {
        model.selectDefaultDockerHost(dockerHost, true);
      }
      wizard.show();

      if (wizard.getExitCode() == DialogWrapper.OK_EXIT_CODE) {
        try {
          String url = wizard.deploy();
          if (AzureDockerUtils.DEBUG) System.out.println("Web app published at: " + url);
        } catch (Exception ex) {
          PluginUtil.displayErrorDialogAndLog(message("webAppDplyErr"), ex.getMessage(), ex);
        }
      }
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  public static ValidationInfo validateComponent(String msgErr, JPanel panel, JComponent component, JComponent componentLabel) {
    panel.requestFocus();
    component.requestFocus();
    if (componentLabel != null) {
      componentLabel.setVisible(true);
    }
    panel.repaint();
    ValidationInfo info = new ValidationInfo(msgErr, component);
    return info;
  }

  public static void createDockerKeyVault(Project project, DockerHost dockerHost, AzureDockerHostsManager dockerManager) {
    if (dockerHost.certVault.hostName != null) {
      ProgressManager.getInstance().run(new Task.Backgroundable(project, String.format("Creating Key Vault for %s...", dockerHost.name), true) {
        @Override
        public void run(ProgressIndicator progressIndicator) {
          try {
            progressIndicator.setFraction(.05);
            progressIndicator.setText2(String.format("Reading subscription details for Docker host %s ...", dockerHost.apiUrl));
            Azure azureClient = dockerManager.getSubscriptionsMap().get(dockerHost.sid).azureClient;
            KeyVaultClient keyVaultClient = dockerManager.getSubscriptionsMap().get(dockerHost.sid).keyVaultClient;
            if (progressIndicator.isCanceled()) {
              if (displayWarningOnCreateKeyVaultCancelAction() == 1) {
                return;
              }
            }

            String retryMsg = "Create";
            int retries = 0;
            AzureDockerCertVault certVault = null;
            do {
              progressIndicator.setFraction(.15 + .15 * retries);
              progressIndicator.setText2(String.format("%s new key vault %s ...", retryMsg, dockerHost.certVault.name));
              if (AzureDockerUtils.DEBUG) System.out.println(retryMsg + " new Docker key vault: " + new Date().toString());
              AzureDockerCertVaultOps.createOrUpdateVault(azureClient, dockerHost.certVault, keyVaultClient);
              if (AzureDockerUtils.DEBUG) System.out.println("Done creating new key vault: " + new Date().toString());
              if (progressIndicator.isCanceled()) {
                if (displayWarningOnCreateKeyVaultCancelAction() == 1) {
                  return;
                }
              }
              certVault = AzureDockerCertVaultOps.getVault(azureClient, dockerHost.certVault.name, dockerHost.certVault.resourceGroupName, keyVaultClient);
              retries++;
              retryMsg = "Retry creating";
            } while (retries < 5 && (certVault == null || certVault.vmUsername == null)); // Retry couple times

            progressIndicator.setFraction(.90);
            progressIndicator.setText2("Updating key vaults ...");
            if (AzureDockerUtils.DEBUG) System.out.println("Refreshing key vaults: " + new Date().toString());
            dockerManager.refreshDockerVaults();
            dockerManager.refreshDockerVaultDetails();

            if (AzureDockerUtils.DEBUG) System.out.println("Done refreshing key vaults: " + new Date().toString());
            if (progressIndicator.isCanceled()) {
              if (displayWarningOnCreateKeyVaultCancelAction() == 1) {
                return;
              }
            }

            progressIndicator.setFraction(.90);
            progressIndicator.setIndeterminate(true);

          } catch (Exception e) {
            String msg = "An error occurred while attempting to create Azure Key Vault for Docker host." + "\n" + e.getMessage() + "\n Try logging in using the automated path (create and use a service principal).\n";
            LOGGER.error("Failed to Create Azure Key Vault", e);
            PluginUtil.displayErrorDialogInAWTAndLog("Failed to Create Azure Key Vault", msg, e);
          }
        }
      });

    }
  }

  private static int displayWarningOnCreateKeyVaultCancelAction(){
    return JOptionPane.showOptionDialog(null,
        "This action can leave the Docker virtual machine host in an partial setup state and which can cause publishing to a Docker container to fail!\n\n Are you sure you want this?",
        "Stop Create Azure Key Vault",
        JOptionPane.YES_NO_OPTION,
        JOptionPane.QUESTION_MESSAGE,
        PluginUtil.getIcon("/icons/logwarn.png"),
        new String[]{"Cancel", "OK"},
        null);
  }

  public static void updateDockerHost(Project project, EditableDockerHost editableDockerHost, AzureDockerHostsManager dockerManager, boolean doReset) {
    AppInsightsClient.createByType(AppInsightsClient.EventType.DockerHost, "", "Update");
    AzureInputDockerLoginCredsDialog loginCredsDialog = new AzureInputDockerLoginCredsDialog(project, editableDockerHost, dockerManager, doReset);
    loginCredsDialog.show();

    if (loginCredsDialog.getExitCode() == DialogWrapper.OK_EXIT_CODE) {
      // Update Docker host log in credentials
      DockerHost updateHost = editableDockerHost.originalDockerHost;
      updateHost.isUpdating = true;
      DefaultLoader.getIdeHelper().runInBackground(project, String.format("Updating %s Log In Credentials", updateHost.name), false, true, String.format("Updating log in credentials for %s...", updateHost.name), new Runnable() {
        @Override
        public void run() {
          try {
            AzureDockerVMOps.updateDockerHostVM(dockerManager.getSubscriptionsMap().get(updateHost.sid).azureClient, editableDockerHost.updatedDockerHost);
            updateHost.certVault = editableDockerHost.updatedDockerHost.certVault;
            updateHost.hasPwdLogIn = editableDockerHost.updatedDockerHost.hasPwdLogIn;
            updateHost.hasSSHLogIn = editableDockerHost.updatedDockerHost.hasSSHLogIn;
            updateHost.hasKeyVault = false;
            updateHost.certVault.uri = "";
            updateHost.certVault.name = "";
            Session session = AzureDockerSSHOps.createLoginInstance(updateHost);
            AzureDockerVMOps.UpdateCurrentDockerUser(session);
            updateHost.session = session;
          } catch (Exception ee) {
            if (AzureDockerUtils.DEBUG) ee.printStackTrace();
            LOGGER.error("onEditDockerHostAction", ee);
          }
          updateHost.isUpdating = false;
        }
      });
    }

  }
}
