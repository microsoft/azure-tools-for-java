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
package com.microsoft.tooling.msservices.serviceexplorer.azure.docker;

import static com.microsoft.azuretools.telemetry.TelemetryConstants.DELETE_DOCKER_IMAGE;
import static com.microsoft.azuretools.telemetry.TelemetryConstants.DOCKER;

import com.microsoft.azure.docker.AzureDockerHostsManager;
import com.microsoft.azure.docker.model.DockerContainer;
import com.microsoft.azure.docker.model.DockerHost;
import com.microsoft.azure.docker.model.DockerImage;
import com.microsoft.azure.docker.ops.AzureDockerContainerOps;
import com.microsoft.azure.docker.ops.AzureDockerImageOps;
import com.microsoft.azuretools.azurecommons.helpers.AzureCmdException;
import com.microsoft.azuretools.telemetry.AppInsightsConstants;
import com.microsoft.azuretools.telemetry.TelemetryProperties;
import com.microsoft.tooling.msservices.components.DefaultLoader;
import com.microsoft.tooling.msservices.serviceexplorer.AzureRefreshableNode;
import com.microsoft.tooling.msservices.serviceexplorer.Node;
import com.microsoft.tooling.msservices.serviceexplorer.NodeActionEvent;
import com.microsoft.tooling.msservices.serviceexplorer.azure.AzureNodeActionPromptListener;

import java.util.HashMap;
import java.util.Map;

public class DockerImageNode extends AzureRefreshableNode implements TelemetryProperties{
  //TODO: Replace the icons with the real Docker host icons
  private static final String DOCKER_IMAGE_ICON_PATH = "DockerInstance_16.png";

  public static final String ACTION_DELETE = "Delete";
  public static final String ACTION_PUSH2REGISTRY = "Push";
  public static final String ACTION_NEW_CONTAINER = "Publish";

  DockerImage dockerImage;
  DockerHost dockerHost;
  AzureDockerHostsManager dockerManager;

  public DockerImageNode(Node parent, AzureDockerHostsManager dockerManager, DockerHost dockerHost, DockerImage dockerImage)
      throws AzureCmdException {
    super(dockerImage.id, AzureDockerImageOps.getDockerImageMapKey(dockerImage), parent, DOCKER_IMAGE_ICON_PATH, true);

    this.dockerManager = dockerManager;
    this.dockerHost = dockerHost;
    this.dockerImage = dockerImage;

    loadActions();

    refreshItems();
  }

  @Override
  protected void onNodeClick(NodeActionEvent e) {
//    super.onNodeClick(e);
  }


  @Override
  protected void refreshItems() {
    try {
      Map<String, DockerContainer> dockerContainers = AzureDockerContainerOps.getContainers(dockerHost);
      AzureDockerContainerOps.setContainersAndImages(dockerContainers, dockerHost.dockerImages);
      if (dockerHost.dockerImages != null) {
        dockerImage = dockerHost.dockerImages.get(AzureDockerImageOps.getDockerImageMapKey(dockerImage));
        if (dockerImage != null) {
          for (DockerContainer dockerContainer : dockerImage.containers.values()) {
            addChildNode(new DockerContainerNode(this, dockerManager, dockerHost, dockerContainer));
          }
        }
      }
    } catch (Exception e) {
      DefaultLoader.getUIHelper().logError(e.getMessage(), e);
    }
  }

  public DockerHost getDockerHost() {
    return dockerHost;
  }

  public AzureDockerHostsManager getDockerManager() {
    return dockerManager;
  }

  @Override
  protected void loadActions() {
    addAction(ACTION_DELETE, new DeleteDockerImageAction());
    super.loadActions();
  }

  @Override
  public Map<String, String> toProperties() {
    final Map<String, String> properties = new HashMap<>();
    properties.put(AppInsightsConstants.SubscriptionId, this.dockerHost.sid);
    properties.put(AppInsightsConstants.Region, this.dockerHost.hostVM.region);
    return properties;
  }

  public class DeleteDockerImageAction extends AzureNodeActionPromptListener {
    public DeleteDockerImageAction() {
      super(DockerImageNode.this,
          String.format("This operation will remove the Docker image %s from %s. Are you sure you want to continue?",
              dockerImage.name, dockerHost.name),
          "Deleting Docker Image");
    }

    @Override
    protected void azureNodeAction(NodeActionEvent e) {
      try {
        AzureDockerImageOps.delete(dockerImage, dockerHost.session);

        DefaultLoader.getIdeHelper().invokeLater(new Runnable() {
          @Override
          public void run() {
            // instruct parent node to remove this node
            getParent().removeDirectChildNode(DockerImageNode.this);
          }
        });
      } catch (Exception ex) {
        DefaultLoader.getUIHelper().logError(ex.getMessage(), ex);
      }
    }

    @Override
    protected void onSubscriptionsChanged(NodeActionEvent e) throws AzureCmdException {
    }

    @Override
    protected String getServiceName() {
      return DOCKER;
    }

    @Override
    protected String getOperationName(NodeActionEvent event) {
      return DELETE_DOCKER_IMAGE;
    }
  }
}
