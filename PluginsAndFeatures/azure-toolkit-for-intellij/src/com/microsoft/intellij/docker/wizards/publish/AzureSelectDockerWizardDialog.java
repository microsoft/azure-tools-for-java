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
package com.microsoft.intellij.docker.wizards.publish;

import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.openapi.ui.ValidationInfo;
import com.intellij.ui.wizard.WizardDialog;
import com.jcraft.jsch.Session;
import com.microsoft.azure.docker.model.AzureDockerImageInstance;
import com.microsoft.azure.docker.model.AzureDockerPreferredSettings;
import com.microsoft.azure.docker.model.DockerHost;
import com.microsoft.azure.docker.model.EditableDockerHost;
import com.microsoft.azure.docker.ops.AzureDockerSSHOps;
import com.microsoft.azure.docker.ops.utils.AzureDockerUtils;
import com.microsoft.azure.management.Azure;
import com.microsoft.intellij.docker.dialogs.AzureInputDockerLoginCredsDialog;
import com.microsoft.intellij.util.PluginUtil;
import com.microsoft.tasks.DockerContainerDeployTask;
import com.microsoft.tooling.msservices.components.DefaultLoader;
import org.jetbrains.annotations.Nullable;

import java.awt.*;

public class AzureSelectDockerWizardDialog extends WizardDialog<AzureSelectDockerWizardModel> {
  private AzureSelectDockerWizardModel model;
  private Runnable onCreate;

  public AzureSelectDockerWizardDialog(AzureSelectDockerWizardModel model) {
    super(model.getProject(), true, model);
    this.model = model;
    model.setSelectDockerWizardDialog(this);
    onCreate = null;

    setTitle("Deploy Docker Container on Azure");
  }

  @Override
  public void onWizardGoalAchieved() {
    if (model.finishedOK) {
      super.onWizardGoalAchieved();
    }
  }

  @Override
  public void onWizardGoalDropped() {
    if (model.finishedOK) {
      super.onWizardGoalDropped();
    }
  }

  @Override
  protected Dimension getWindowPreferredSize() {
    return new Dimension(600, 400);
  }

  @Nullable
  @Override
  protected ValidationInfo doValidate() {
    return model.doValidate();
  }

  @Override
  protected void doOKAction() {
//        validateInput();
    if (isOKActionEnabled()) {
      super.doOKAction();
    }
  }

//  @Override
//  public void doCancelAction() {
//    model.finishedOK = true;
//    super.doCancelAction();
//  }

  public void DialogShaker(ValidationInfo info) {
    PluginUtil.dialogShaker(info, this);
  }

  public String deploy() {
    AzureDockerImageInstance dockerImageInstance = model.getDockerImageDescription();
    AzureDockerPreferredSettings dockerPreferredSettings = model.getDockerHostsManager().getDockerPreferredSettings();

    if (dockerPreferredSettings == null) {
      dockerPreferredSettings = new AzureDockerPreferredSettings();
    }
    dockerPreferredSettings.dockerApiName = dockerImageInstance.host.apiUrl;
    dockerPreferredSettings.dockerfileOption = dockerImageInstance.predefinedDockerfile;
    dockerPreferredSettings.region = dockerImageInstance.host.hostVM.region;
    dockerPreferredSettings.vmSize = dockerImageInstance.host.hostVM.vmSize;
    dockerPreferredSettings.vmOS = dockerImageInstance.host.hostOSType.name();
    model.getDockerHostsManager().setDockerPreferredSettings(dockerPreferredSettings);

    DefaultLoader.getIdeHelper().runInBackground(model.getProject(), "Deploying Docker Container on Azure", false, true, "Deploying Web app to a Docker host on Azure...", new Runnable() {
      @Override
      public void run() {
        try {
          DefaultLoader.getIdeHelper().invokeLater(new Runnable() {
            @Override
            public void run() {
              AzureDockerImageInstance dockerImageInstance = model.getDockerImageDescription();
              if (!dockerImageInstance.hasNewDockerHost) {
                Session session = null;

                do {
                  try {
                    // check if the Docker host is accessible
                    session = AzureDockerSSHOps.createLoginInstance(dockerImageInstance.host);
                  } catch (Exception e) {
                    session = null;
                  }

                  if (session == null) {
                    EditableDockerHost editableDockerHost = new EditableDockerHost(dockerImageInstance.host);
                    AzureInputDockerLoginCredsDialog loginCredsDialog = new AzureInputDockerLoginCredsDialog(model.getProject(), editableDockerHost, model.getDockerHostsManager(), false);
                    loginCredsDialog.show();

                    if (loginCredsDialog.getExitCode() == DialogWrapper.OK_EXIT_CODE) {
                      // Update Docker host log in credentials
                      DockerHost dockerHost = model.getDockerHostsManager().getDockerHostForURL(dockerImageInstance.host.apiUrl);
                      dockerHost.certVault = editableDockerHost.updatedDockerHost.certVault;
                      dockerHost.hasPwdLogIn = editableDockerHost.updatedDockerHost.hasPwdLogIn;
                      dockerHost.hasSSHLogIn = editableDockerHost.updatedDockerHost.hasSSHLogIn;
                      dockerImageInstance.host = dockerHost;
//                    AzureDockerVMOps.updateDockerHostVM(model.getDockerHostsManager().getSubscriptionsMap().get(model.getDockerImageDescription().sid).azureClient, editableDockerHost.updatedDockerHost);
                    } else {
                      return;
                    }
                  }
                } while (session == null);
              }

              Azure azureClient = model.getDockerHostsManager().getSubscriptionsMap().get(model.getDockerImageDescription().sid).azureClient;

              DockerContainerDeployTask task = new DockerContainerDeployTask(model.getProject(), azureClient, model.getDockerImageDescription());
              task.queue();

              // Update caches here
              if (onCreate != null) {
                onCreate.run();
              }
            }
          });
        } catch (Exception e) {
          String msg = "An error occurred while attempting to deploy to the selected Docker host." + "\n" + e.getMessage();
          PluginUtil.displayErrorDialogInAWTAndLog("Failed to Deploy Web App as Docker Container", msg, e);
        }
      }
    });

    return AzureDockerUtils.getUrl(dockerImageInstance);
  }

  public AzureDockerImageInstance getDockerImageInstance() {
    return model.getDockerImageDescription();
  }

}
