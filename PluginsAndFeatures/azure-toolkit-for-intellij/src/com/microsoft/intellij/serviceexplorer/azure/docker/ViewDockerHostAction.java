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
import com.jcraft.jsch.Session;
import com.microsoft.azure.docker.AzureDockerHostsManager;
import com.microsoft.azure.docker.model.DockerHost;
import com.microsoft.azure.docker.model.EditableDockerHost;
import com.microsoft.azure.docker.ops.AzureDockerSSHOps;
import com.microsoft.azure.docker.ops.AzureDockerVMOps;
import com.microsoft.azure.docker.ops.utils.AzureDockerUtils;
import com.microsoft.intellij.docker.dialogs.AzureEditDockerLoginCredsDialog;
import com.microsoft.intellij.docker.dialogs.AzureInputDockerLoginCredsDialog;
import com.microsoft.intellij.docker.dialogs.AzureViewDockerDialog;
import com.microsoft.intellij.docker.utils.AzureDockerUIResources;
import com.microsoft.tooling.msservices.components.DefaultLoader;
import com.microsoft.tooling.msservices.helpers.Name;
import com.microsoft.tooling.msservices.serviceexplorer.NodeActionEvent;
import com.microsoft.tooling.msservices.serviceexplorer.NodeActionListener;
import com.microsoft.tooling.msservices.serviceexplorer.azure.docker.DockerHostNode;


@Name("Details")
public class ViewDockerHostAction extends NodeActionListener {
  private static final Logger LOGGER = Logger.getInstance(ViewDockerHostAction.class);
  DockerHost dockerHost;
  AzureDockerHostsManager dockerManager;
  Project project;
  DockerHostNode dockerHostNode;

  public ViewDockerHostAction(DockerHostNode dockerHostNode) {
    this.dockerManager = dockerHostNode.getDockerManager();
    this.dockerHost = dockerHostNode.getDockerHost();
    this.project = (Project) dockerHostNode.getProject();
    this.dockerHostNode = dockerHostNode;
  }

  @Override
  public void actionPerformed(NodeActionEvent e) {
    dockerHost = dockerManager.getDockerHostForURL(dockerHost.apiUrl);
    AzureViewDockerDialog viewDockerDialog = new AzureViewDockerDialog(project, dockerHost, dockerManager);
    viewDockerDialog.show();

    if (viewDockerDialog.getInternalExitCode() == AzureViewDockerDialog.UPDATE_EXIT_CODE) {
      AzureDockerUIResources.updateDockerHost(project, new EditableDockerHost(dockerHost), dockerManager, true);
    }
  }
}
