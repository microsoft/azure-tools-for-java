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

package com.microsoft.azure.docker.model;

public class EditableDockerHost {
  public DockerHost originalDockerHost;
  public DockerHost updatedDockerHost;
  public boolean isUpdated;
  public boolean hasNewLoginCreds;
  public boolean hasNewDockerdCreds;
  public boolean hasNewDockerdPort;
  public boolean hasNewKeyVaultSettings;
  public boolean hasNewVMState; // see DockerHostVMState

  public EditableDockerHost(DockerHost host) {
    originalDockerHost = host;
    isUpdated = false;
    updatedDockerHost = (host != null) ? new DockerHost(host) : null;
    if (updatedDockerHost != null && updatedDockerHost.certVault == null) {
      updatedDockerHost.certVault = new AzureDockerCertVault();
      updatedDockerHost.certVault.hostName = host.hostVM.name;
      updatedDockerHost.certVault.region = host.hostVM.region;
      updatedDockerHost.certVault.resourceGroupName = host.hostVM.resourceGroupName;
      updatedDockerHost.certVault.sid = host.sid;
    }
    this.hasNewLoginCreds = false;
    this.hasNewDockerdCreds = false;
    this.hasNewDockerdPort = false;
    this.hasNewKeyVaultSettings = false;
    this.hasNewVMState = false;
  }

  public void undoEdit() {
    updatedDockerHost = new DockerHost(originalDockerHost);
  }
}
