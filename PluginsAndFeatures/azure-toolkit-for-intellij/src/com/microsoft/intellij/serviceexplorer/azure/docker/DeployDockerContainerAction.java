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
package com.microsoft.intellij.serviceexplorer.azure.docker;

import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.DialogWrapper;
import com.microsoft.azure.docker.AzureDockerHostsManager;
import com.microsoft.azure.docker.model.AzureDockerImageInstance;
import com.microsoft.azure.docker.model.DockerHost;
import com.microsoft.azuretools.authmanage.AuthMethodManager;
import com.microsoft.azuretools.ijidea.actions.AzureSignInAction;
import com.microsoft.intellij.docker.wizards.publish.AzureSelectDockerWizardDialog;
import com.microsoft.intellij.docker.wizards.publish.AzureSelectDockerWizardModel;
import com.microsoft.intellij.util.PluginUtil;
import com.microsoft.tooling.msservices.helpers.Name;
import com.microsoft.tooling.msservices.serviceexplorer.NodeActionEvent;
import com.microsoft.tooling.msservices.serviceexplorer.NodeActionListener;
import com.microsoft.tooling.msservices.serviceexplorer.azure.docker.DockerHostNode;

import static com.microsoft.intellij.ui.messages.AzureBundle.message;


@Name("Publish")
public class DeployDockerContainerAction extends NodeActionListener {
  private static final Logger LOGGER = Logger.getInstance(DeployDockerContainerAction.class);
  DockerHost dockerHost;
  AzureDockerHostsManager dockerManager;
  Project project;
  DockerHostNode dockerHostNode;

  public DeployDockerContainerAction(DockerHostNode dockerHostNode) {
    this.dockerManager = dockerHostNode.getDockerManager();
    this.dockerHost = dockerHostNode.getDockerHost();
    this.project = (Project) dockerHostNode.getProject();
    this.dockerHostNode = dockerHostNode;
  }

  @Override
  public void actionPerformed(NodeActionEvent e) {
    try {
      if (!AzureSignInAction.doSignIn(AuthMethodManager.getInstance(), project)) return;
      if (dockerManager.getSubscriptionsMap().isEmpty()) {
        PluginUtil.displayErrorDialog("Publish Docker Container", "Must select an Azure subscription first");
        return;
      }

      AzureDockerImageInstance dockerImageDescription = dockerManager.getDefaultDockerImageDescription(project.getName(), dockerHost);

      AzureSelectDockerWizardModel model = new AzureSelectDockerWizardModel(project, dockerManager, dockerImageDescription);
      AzureSelectDockerWizardDialog wizard = new AzureSelectDockerWizardDialog(model);
      model.selectDefaultDockerHost(dockerHost, false);
      wizard.show();

      if (wizard.getExitCode() == DialogWrapper.OK_EXIT_CODE) {
        try {
          String url = wizard.deploy();
          System.out.println("Container published at: " + url);
        } catch (Exception ex) {
          PluginUtil.displayErrorDialogAndLog(message("webAppDplyErr"), ex.getMessage(), ex);
        }
      }
    } catch(Exception ex1) {
      LOGGER.error("actionPerformed", ex1);
      ex1.printStackTrace();
    }
  }
}
