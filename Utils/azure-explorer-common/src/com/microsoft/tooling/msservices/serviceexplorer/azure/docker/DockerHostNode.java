/*
 * Copyright (c) Microsoft Corporation
 * Copyright (c) 2018-2020 JetBrains s.r.o.
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

import com.microsoft.azure.CommonIcons;
import com.microsoft.azure.docker.AzureDockerHostsManager;
import com.microsoft.azure.docker.model.DockerContainer;
import com.microsoft.azure.docker.model.DockerHost;
import com.microsoft.azure.docker.model.DockerImage;
import com.microsoft.azure.docker.ops.AzureDockerContainerOps;
import com.microsoft.azure.docker.ops.AzureDockerImageOps;
import com.microsoft.azure.docker.ops.AzureDockerVMOps;
import com.microsoft.azure.management.Azure;
import com.microsoft.azure.management.compute.VirtualMachine;
import com.microsoft.azuretools.azurecommons.helpers.AzureCmdException;
import com.microsoft.azuretools.telemetry.AppInsightsConstants;
import com.microsoft.azuretools.telemetry.TelemetryProperties;
import com.microsoft.tooling.msservices.components.DefaultLoader;
import com.microsoft.tooling.msservices.serviceexplorer.*;
import com.microsoft.tooling.msservices.serviceexplorer.azure.AzureNodeActionPromptListener;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.microsoft.azure.docker.model.DockerHost.DockerHostVMState.RUNNING;
import static com.microsoft.azuretools.telemetry.TelemetryConstants.DOCKER;
import static com.microsoft.azuretools.telemetry.TelemetryConstants.RESTART_DOCKER_HOST;
import static com.microsoft.azuretools.telemetry.TelemetryConstants.SHUTDOWN_DOCKER_HOST;
import static com.microsoft.azuretools.telemetry.TelemetryConstants.START_DOCKER_HOST;

public class DockerHostNode extends AzureRefreshableNode implements TelemetryProperties {
  //TODO: Replace the icons with the real Docker host icons
  private static final String DOCKERHOST_WAIT_ICON_PATH = "DockerContainerUpdating.svg";
  private static final String DOCKERHOST_STOP_ICON_PATH = "DockerContainerStopped.svg";
  private static final String DOCKERHOST_RUN_ICON_PATH = "DockerContainerRunning.svg";

  public static final String ACTION_START = "Start";
  public static final String ACTION_RESTART = "Restart";
  public static final String ACTION_DELETE = "Delete";
  public static final String ACTION_SSH_CONNECT = "Connect";
  public static final String ACTION_SHUTDOWN = "Shutdown";
  public static final String ACTION_VIEW = "Details";
  public static final String ACTION_DEPLOY = "Publish";
  private static final String ACTION_SHUTDOWN_ICON = "Stop.png";
  private static final String ACTION_START_ICON = "Start.png";

  private NodeAction startAction;
  private NodeAction restartAction;
  private NodeAction stopAction;

  DockerHost dockerHost;
  AzureDockerHostsManager dockerManager;

  public DockerHostNode(Node parent, AzureDockerHostsManager dockerManager, DockerHost dockerHost)
      throws AzureCmdException {
    super(dockerHost.apiUrl, dockerHost.name, parent, DOCKERHOST_WAIT_ICON_PATH, true);

    this.dockerManager = dockerManager;
    this.dockerHost = dockerHost;

    loadActions();

    // update vm icon based on vm status
    //refreshItemsInternal();
    setIconPath(getDockerHostIcon());
  }

  @Override
  protected void onNodeClick(NodeActionEvent e) {
    super.onNodeClick(e);
  }


  @Override
  protected void refreshItems() throws AzureCmdException {
    try {
      Azure azureClient = dockerManager.getSubscriptionsMap().get(dockerHost.sid).azureClient;
      VirtualMachine vm = azureClient.virtualMachines().getByResourceGroup(dockerHost.hostVM.resourceGroupName, dockerHost.hostVM.name);
      if (vm != null) {
        refreshDockerHostInstance(vm);
      }
    } catch (Exception e) {
      DefaultLoader.getUIHelper().logError(e.getMessage(), e);
    }
  }

  private void refreshDockerHostInstance(VirtualMachine vm) {
    if (vm != null) {
      DockerHost updatedDockerHost = AzureDockerVMOps.getDockerHost(vm, dockerManager.getDockerVaultsMap());
      if (updatedDockerHost != null) {
        updatedDockerHost.sid = dockerHost.sid;
        updatedDockerHost.hostVM.sid = dockerHost.hostVM.sid;
        if (updatedDockerHost.certVault == null) {
          updatedDockerHost.certVault = dockerHost.certVault;
          updatedDockerHost.hasPwdLogIn = dockerHost.hasPwdLogIn;
          updatedDockerHost.hasSSHLogIn = dockerHost.hasSSHLogIn;
          updatedDockerHost.isTLSSecured = dockerHost.isTLSSecured;
        }
        dockerManager.updateDockerHost(updatedDockerHost);
        dockerHost = updatedDockerHost;

        if (dockerHost.certVault != null) {
          try { // it might throw here if the credentials are invalid
            Map<String, DockerImage> dockerImages = AzureDockerImageOps.getImages(dockerHost);
            Map<String, DockerContainer> dockerContainers = AzureDockerContainerOps.getContainers(dockerHost);
            AzureDockerContainerOps.setContainersAndImages(dockerContainers, dockerImages);
            dockerHost.dockerImages = dockerImages;
          } catch (Exception e) {
            DefaultLoader.getUIHelper().logError(e.getMessage(), e);
          }
        }
        setIconPath(getDockerHostIcon());

        for (DockerImage dockerImage : updatedDockerHost.dockerImages.values()) {
          try {
            addChildNode(new DockerImageNode(this, dockerManager, updatedDockerHost, dockerImage));
          } catch (Exception ignored) {}
        }
      }
    }
  }

  private String getDockerHostIcon() {
    switch (dockerHost.state) {
      case RUNNING:
        return DOCKERHOST_RUN_ICON_PATH;
      case STOPPED:
      case UNKNOWN:
        return DOCKERHOST_STOP_ICON_PATH;
      case DEALLOCATING:
      case DEALLOCATED:
      case STARTING:
      case UPDATING:
      default:
        return DOCKERHOST_WAIT_ICON_PATH;
    }
  }

  private boolean isRunning() {
    return dockerHost.state == RUNNING;
  }

  public DockerHost getDockerHost() {
    return dockerHost;
  }

  public AzureDockerHostsManager getDockerManager() {
    return dockerManager;
  }

  @Override
  protected void loadActions() {
    addAction(ACTION_START, CommonIcons.ACTION_START, new WrappedTelemetryNodeActionListener(DOCKER, START_DOCKER_HOST,
        new NodeActionListener() {
      @Override
      public void actionPerformed(NodeActionEvent e) {
        DefaultLoader.getIdeHelper().runInBackground(null, "Starting Docker Host", false, true, "Starting Docker Host...", new Runnable() {
          @Override
          public void run() {
            removeAllChildNodes();
            setIconPath(DOCKERHOST_WAIT_ICON_PATH);
            try {
              Azure azureClient = dockerManager.getSubscriptionsMap().get(dockerHost.sid).azureClient;
              VirtualMachine vm = azureClient.virtualMachines().getByResourceGroup(dockerHost.hostVM.resourceGroupName, dockerHost.hostVM.name);
              if (vm != null) {
                vm.start();
                setIconPath(DOCKERHOST_RUN_ICON_PATH);
                refreshDockerHostInstance(vm);
              }
            } catch (Exception e) {
              DefaultLoader.getUIHelper().logError(e.getMessage(), e);
            }
          }
        });
      }
    }));
    addAction(ACTION_RESTART, CommonIcons.ACTION_RESTART, new RestartDockerHostAction());
    addAction(ACTION_SHUTDOWN, CommonIcons.ACTION_STOP, new ShutdownDockerHostAction());
    super.loadActions();
  }

  @Override
  public List<NodeAction> getNodeActions() {
    if (startAction == null)
      startAction = getNodeActionByName(ACTION_START);

    if (restartAction == null)
      restartAction = getNodeActionByName(ACTION_RESTART);

    if (stopAction == null)
      stopAction = getNodeActionByName(ACTION_SHUTDOWN);

//  enable/disable menu items according to VM status
    boolean started = isRunning();

    List<NodeAction> nodeActions = super.getNodeActions();

    if (started) {
      nodeActions.remove(startAction);
      if (!nodeActions.contains(stopAction))
        nodeActions.add(stopAction);
    } else {
      nodeActions.remove(stopAction);
      if (!nodeActions.contains(startAction))
        nodeActions.add(startAction);
    }

    restartAction.setEnabled(started);

    getNodeActionByName(ACTION_SHUTDOWN).setEnabled(started);
    getNodeActionByName(ACTION_START).setEnabled(!started);
    getNodeActionByName(ACTION_RESTART).setEnabled(started);

    return nodeActions;
  }

  @Override
  public Map<String, String> toProperties() {
    final Map<String, String> properties = new HashMap<>();
    properties.put(AppInsightsConstants.SubscriptionId, this.dockerHost.sid);
    properties.put(AppInsightsConstants.Region, this.dockerHost.hostVM.region);
    return properties;
  }

  public class RestartDockerHostAction extends AzureNodeActionPromptListener {
    public RestartDockerHostAction() {
      super(DockerHostNode.this,
          String.format("Are you sure you want to restart the virtual machine %s?", dockerHost.name),
          "Restarting Docker Host");
    }

    @Override
    protected void azureNodeAction(NodeActionEvent e)
        throws AzureCmdException {
      try {
        removeAllChildNodes();
        setIconPath(DOCKERHOST_WAIT_ICON_PATH);
        Azure azureClient = dockerManager.getSubscriptionsMap().get(dockerHost.sid).azureClient;
        VirtualMachine vm = azureClient.virtualMachines().getByResourceGroup(dockerHost.hostVM.resourceGroupName, dockerHost.hostVM.name);
        if (vm != null) {
          vm.restart();
          setIconPath(DOCKERHOST_RUN_ICON_PATH);
          refreshDockerHostInstance(vm);
        }
      } catch (Exception ee) {
        DefaultLoader.getUIHelper().logError(ee.getMessage(), ee);
      }
    }

    @Override
    protected void onSubscriptionsChanged(NodeActionEvent e) throws AzureCmdException {
    }

    @Override
    protected String getServiceName(NodeActionEvent event) {
      return DOCKER;
    }

    @Override
    protected String getOperationName(NodeActionEvent event) {
      return RESTART_DOCKER_HOST;
    }
  }

  public class ShutdownDockerHostAction extends AzureNodeActionPromptListener {
    public ShutdownDockerHostAction() {
      super(DockerHostNode.this, String.format(
          "This operation will result in losing the virtual IP address that was assigned to this virtual machine.\n" +
              "Are you sure that you want to shut down virtual machine %s?", dockerHost.name),
          "Shutting Down Docker Host");
    }

    @Override
    protected void azureNodeAction(NodeActionEvent e)
        throws AzureCmdException {
      try {
        removeAllChildNodes();
        setIconPath(DOCKERHOST_STOP_ICON_PATH);
        for (NodeAction nodeAction : getNodeActions()) {
          nodeAction.setEnabled(false);
        }
        getNodeActionByName(ACTION_START).setEnabled(true);
        getNodeActionByName(ACTION_RESTART).setEnabled(true);

        Azure azureClient = dockerManager.getSubscriptionsMap().get(dockerHost.sid).azureClient;
        VirtualMachine vm = azureClient.virtualMachines().getByResourceGroup(dockerHost.hostVM.resourceGroupName, dockerHost.hostVM.name);
        if (vm != null) {
          vm.powerOff();
          refreshDockerHostInstance(vm);
        }
        for (NodeAction nodeAction : getNodeActions()) {
          nodeAction.setEnabled(true);
        }
      } catch (Exception ee) {
        DefaultLoader.getUIHelper().logError(ee.getMessage(), ee);
      }
    }

    @Override
    protected void onSubscriptionsChanged(NodeActionEvent e) throws AzureCmdException {
    }

    @Override
    protected String getServiceName(NodeActionEvent event) {
      return DOCKER;
    }

    @Override
    protected String getOperationName(NodeActionEvent event) {
      return SHUTDOWN_DOCKER_HOST;
    }
  }

}
