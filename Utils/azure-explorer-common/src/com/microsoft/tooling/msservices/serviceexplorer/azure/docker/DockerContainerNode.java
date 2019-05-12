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
import com.microsoft.tooling.msservices.serviceexplorer.*;
import com.microsoft.tooling.msservices.serviceexplorer.azure.AzureNodeActionPromptListener;

import java.util.HashMap;
import java.util.Map;

import static com.microsoft.azure.docker.ops.utils.AzureDockerUtils.checkDockerContainerUrlAvailability;
import static com.microsoft.azuretools.telemetry.TelemetryConstants.BROWSE_DOCKER_CONTAINER;
import static com.microsoft.azuretools.telemetry.TelemetryConstants.DELETE_DOCKER_CONTAINER;
import static com.microsoft.azuretools.telemetry.TelemetryConstants.DOCKER;
import static com.microsoft.azuretools.telemetry.TelemetryConstants.RESTART_DOCKER_CONTAINER;
import static com.microsoft.azuretools.telemetry.TelemetryConstants.START_DOCKER_CONTAINER;
import static com.microsoft.azuretools.telemetry.TelemetryConstants.STOP_DOCKER_CONTAINER;

public class DockerContainerNode extends AzureRefreshableNode implements TelemetryProperties {
  //TODO: Replace the icons with the real Docker host icons
  private static final String DOCKER_CONTAINER_ICON_PATH = "DockerInstance2_16.png";
  private static final String DOCKER_CONTAINER_WEB_RUN_ICON = "DockerInstanceRunning2_16.png";
  private static final String DOCKER_CONTAINER_WEB_STOP_ICON = "DockerInstanceStopped2_16.png";

  private static final String ACTION_STOP_ICON = "Stop.png";
  private static final String ACTION_START_ICON = "Start.png";
  private static final String ACTION_DELETE_ICON = "Delete.png";

  public static final String ACTION_START = "Start";
  public static final String ACTION_DELETE = "Delete";
  public static final String ACTION_DOCKER_CONNECT = "Connect";
  public static final String ACTION_STOP = "Stop";
  public static final String ACTION_RESTART = "Restart";
  public static final String ACTION_OPEN_WEB_APP = "Browse";

  DockerContainer dockerContainer;
  DockerHost dockerHost;
  AzureDockerHostsManager dockerManager;

  public DockerContainerNode(Node parent, AzureDockerHostsManager dockerManager, DockerHost dockerHost, DockerContainer dockerContainer)
      throws AzureCmdException {
    super(dockerContainer.id, dockerContainer.name, parent, DOCKER_CONTAINER_ICON_PATH, true);

    this.dockerManager = dockerManager;
    this.dockerHost = dockerHost;
    this.dockerContainer = dockerContainer;
    if (dockerContainer.isRunning) {
      setIconPath(DOCKER_CONTAINER_WEB_RUN_ICON);
    }

    loadActions();
  }

  @Override
  protected void onNodeClick(NodeActionEvent e) {
//    super.onNodeClick(e);
  }

  @Override
  protected void refreshItems() throws AzureCmdException {
    try {
      Map<String, DockerImage> dockerImages = AzureDockerImageOps.getImages(dockerHost);
      Map<String, DockerContainer> dockerContainers = AzureDockerContainerOps.getContainers(dockerHost);
      AzureDockerContainerOps.setContainersAndImages(dockerContainers, dockerImages);
      dockerHost.dockerImages = dockerImages;
      if (dockerContainers != null) {
        DockerContainer updatedDockerContainer = dockerContainers.get(dockerContainer.name);
        if (updatedDockerContainer != null) {
          dockerContainer = updatedDockerContainer;
          setDockerContainerIconPath();
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

  private void setDockerContainerIconPath() {
    if (dockerHost.dockerImages.get(dockerContainer.image).isPluginImage) {
      if (dockerContainer.isRunning) {
        setIconPath(DOCKER_CONTAINER_WEB_RUN_ICON);
      } else {
        setIconPath(DOCKER_CONTAINER_WEB_STOP_ICON);
      }
    } else {
      setIconPath(DOCKER_CONTAINER_ICON_PATH);
    }
  }

  @Override
  public java.util.List<NodeAction> getNodeActions() {
//  enable/disable menu items according to VM status
    boolean started = dockerContainer != null && dockerContainer.isRunning;
    getNodeActionByName(ACTION_OPEN_WEB_APP).setEnabled(started);
    getNodeActionByName(ACTION_STOP).setEnabled(started);
    getNodeActionByName(ACTION_START).setEnabled(!started);
    getNodeActionByName(ACTION_RESTART).setEnabled(started);

    return super.getNodeActions();
  }



  @Override
  protected void loadActions() {
    addAction(ACTION_OPEN_WEB_APP, DOCKER_CONTAINER_WEB_RUN_ICON, new WrappedTelemetryNodeActionListener(DOCKER,
        BROWSE_DOCKER_CONTAINER, new NodeActionListener() {
      @Override
      public void actionPerformed(NodeActionEvent e) {
        DefaultLoader.getIdeHelper().runInBackground(null, "Open Web Link", false, true, "Opening Web Link...", new Runnable() {
          @Override
          public void run() {
            if(dockerContainer.url != null && dockerContainer.isRunning && dockerHost.dockerImages.get(dockerContainer.image).isPluginImage) {
              DefaultLoader.getIdeHelper().openLinkInBrowser(dockerContainer.url);
            }
          }
        });
      }
    }));
    addAction(ACTION_STOP, ACTION_STOP_ICON, new WrappedTelemetryNodeActionListener(DOCKER, STOP_DOCKER_CONTAINER,
        new NodeActionListener() {
      @Override
      public void actionPerformed(NodeActionEvent e) {
        DefaultLoader.getIdeHelper().runInBackground(null, "Stopping Docker Container", false, true, "Stopping Docker Container...", new Runnable() {
          @Override
          public void run() {
            AzureDockerContainerOps.stop(dockerContainer, dockerHost.session);
            dockerContainer.isRunning = false;
            setDockerContainerIconPath();
          }
        });
      }
    }));
    addAction(ACTION_START, ACTION_START_ICON, new WrappedTelemetryNodeActionListener(DOCKER, START_DOCKER_CONTAINER,
        new NodeActionListener() {
      @Override
      public void actionPerformed(NodeActionEvent e) {
        DefaultLoader.getIdeHelper().runInBackground(null, "Starting Docker Container", false, true, "Starting Docker Container...", new Runnable() {
          @Override
          public void run() {
            AzureDockerContainerOps.start(dockerContainer, dockerHost.session);
            dockerContainer.isRunning = true;
            DockerImage dockerImage = dockerHost.dockerImages.get(dockerContainer.image);
            if (dockerImage != null) {
              if (dockerImage.artifactFile != null && !dockerImage.artifactFile.isEmpty()) {
                // adjust the Url path to capture the artifact name
                String url = dockerImage.artifactFile.toLowerCase().matches(".*\\.war") ?
                    dockerContainer.url + dockerImage.artifactFile.substring(0, dockerImage.artifactFile.lastIndexOf(".")) :
                    dockerContainer.url;
                try {
                  // Give some time for the container to start
                  Thread.sleep(5000);
                } catch (Exception ignored) {}
                if (dockerContainer.isRunning && checkDockerContainerUrlAvailability(url)) {
                  dockerContainer.url = url;
                }
              }
            }
            setDockerContainerIconPath();
          }
        });
      }
    }));
    addAction(ACTION_RESTART, ACTION_START_ICON, new WrappedTelemetryNodeActionListener(DOCKER, RESTART_DOCKER_CONTAINER,
        new NodeActionListener() {
      @Override
      public void actionPerformed(NodeActionEvent e) {
        DefaultLoader.getIdeHelper().runInBackground(null, "Restarting Docker Container", false, true, "Restarting Docker Container...", new Runnable() {
          @Override
          public void run() {
            AzureDockerContainerOps.stop(dockerContainer, dockerHost.session);
            AzureDockerContainerOps.start(dockerContainer, dockerHost.session);
            dockerContainer.isRunning = true;
            setDockerContainerIconPath();
          }
        });
      }
    }));
    addAction(ACTION_DELETE, ACTION_DELETE_ICON, new DeleteDockerContainerAction());
    super.loadActions();
  }

  @Override
  public Map<String, String> toProperties() {
    final Map<String, String> properties = new HashMap<>();
    properties.put(AppInsightsConstants.SubscriptionId, this.dockerHost.sid);
    properties.put(AppInsightsConstants.Region, this.dockerHost.hostVM.region);
    return properties;
  }

  public class DeleteDockerContainerAction extends AzureNodeActionPromptListener {
    public DeleteDockerContainerAction() {
      super(DockerContainerNode.this,
          String.format("This operation will permanently remove the Docker container %s from %s. Are you sure you want to continue?",
              dockerContainer.name, dockerHost.name),
          "Deleting Docker Container");
    }

    @Override
    protected void azureNodeAction(NodeActionEvent e)
        throws AzureCmdException {
      try {
        AzureDockerContainerOps.delete(dockerContainer, dockerHost.session);

        DefaultLoader.getIdeHelper().invokeLater(new Runnable() {
          @Override
          public void run() {
            // instruct parent node to remove this node
            getParent().removeDirectChildNode(DockerContainerNode.this);
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
      return DELETE_DOCKER_CONTAINER;
    }
  }

}
