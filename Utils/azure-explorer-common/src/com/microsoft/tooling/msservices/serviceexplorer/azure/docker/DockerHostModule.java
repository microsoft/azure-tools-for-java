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
package com.microsoft.tooling.msservices.serviceexplorer.azure.docker;

import com.microsoft.azure.docker.AzureDockerHostsManager;
import com.microsoft.azure.docker.model.DockerHost;
import com.microsoft.azuretools.authmanage.AuthMethodManager;
import com.microsoft.azuretools.sdkmanage.AzureManager;
import com.microsoft.azuretools.utils.AzureUIRefreshCore;
import com.microsoft.azuretools.utils.AzureUIRefreshEvent;
import com.microsoft.azuretools.utils.AzureUIRefreshListener;
import com.microsoft.tooling.msservices.components.DefaultLoader;
import com.microsoft.azuretools.azurecommons.helpers.AzureCmdException;
import com.microsoft.tooling.msservices.serviceexplorer.AzureRefreshableNode;
import com.microsoft.tooling.msservices.serviceexplorer.Node;

public class DockerHostModule extends AzureRefreshableNode {
  private static final String DOCKER_HOST_MODULE_ID = DockerHostModule.class.getName();
  private static final String DOCKER_HOST_ICON = "DockerContainer_16.png";
  private static final String BASE_MODULE_NAME = "Docker Hosts";

  private AzureDockerHostsManager dockerManager;

  public DockerHostModule(Node parent) {
    super(DOCKER_HOST_MODULE_ID, BASE_MODULE_NAME, parent, DOCKER_HOST_ICON);
    dockerManager = null;

    createListener();
  }

  private void createListener() {
    String id = "DockerHostModule";
    AzureUIRefreshListener listener = new AzureUIRefreshListener() {
      @Override
      public void run() {
        if (event.opsType == AzureUIRefreshEvent.EventType.SIGNIN || event.opsType == AzureUIRefreshEvent.EventType.SIGNOUT) {
          removeAllChildNodes();
        } else if (event.object == null &&
            (event.opsType == AzureUIRefreshEvent.EventType.UPDATE || event.opsType == AzureUIRefreshEvent.EventType.REMOVE)) {
          if (hasChildNodes()) {
            load(true);
          }
        } else if (event.object != null && event.object.getClass().toString().equals(DockerHost.class.toString())) {
          DockerHost dockerHost = (DockerHost) event.object;
          switch (event.opsType) {
            case ADD:
              DefaultLoader.getIdeHelper().invokeLater(new Runnable() {
                @Override
                public void run() {
                  try {
                      addChildNode(new DockerHostNode(DockerHostModule.this, dockerManager, dockerHost));
                  } catch (Exception ex) {
                    DefaultLoader.getUIHelper().logError("DockerHostModule::createListener ADD", ex);
                    ex.printStackTrace();
                  }
                }
              });
              break;
            case UPDATE:
              break;
            case REMOVE:
              break;
            default:
              break;
          }
        }
      }
    };
    AzureUIRefreshCore.addListener(id, listener);

  }

//  @Override
//  protected void onNodeClick(NodeActionEvent e) {
//    super.onNodeClick(e);
//  }

  @Override
  protected void refreshFromAzure() throws Exception {
    try {
      AzureManager azureAuthManager = AuthMethodManager.getInstance().getAzureManager();
      // not signed in
      if (azureAuthManager == null) {
        return;
      }

      dockerManager = AzureDockerHostsManager.getAzureDockerHostsManager(azureAuthManager);

      dockerManager.forceRefreshSubscriptions();
      dockerManager = AzureDockerHostsManager.getAzureDockerHostsManagerEmpty(null);

    } catch (Exception ex) {
      DefaultLoader.getUIHelper().showException("An error occurred while attempting to load the Docker virtual machines from Azure", ex,
          "Azure Services Explorer - Error Refreshing Docker Hosts", false, true);
    }
  }

  @Override
  protected void refreshItems() throws AzureCmdException {
    try {
      AzureManager azureAuthManager = AuthMethodManager.getInstance().getAzureManager();
      // not signed in
      if (azureAuthManager == null) {
        return;
      }

      dockerManager = AzureDockerHostsManager.getAzureDockerHostsManager(azureAuthManager);

      if (!dockerManager.isInitialized()) {
        dockerManager.forceRefreshSubscriptions();
        dockerManager = AzureDockerHostsManager.getAzureDockerHostsManagerEmpty(null);
      }

      for (DockerHost host : dockerManager.getDockerHostsList()) {
        addChildNode(new DockerHostNode(this, dockerManager, host));
      }
    } catch (Exception ex) {
      DefaultLoader.getUIHelper().showException("An error occurred while attempting to load the Docker virtual machines from Azure", ex,
          "Azure Services Explorer - Error Refreshing Docker Hosts", false, true);
    }
  }
}
