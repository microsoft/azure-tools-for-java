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

package com.microsoft.tooling.msservices.serviceexplorer.azure.vmarm;

import static com.microsoft.azuretools.telemetry.TelemetryConstants.DELETE_VM;
import static com.microsoft.azuretools.telemetry.TelemetryConstants.RESTART_VM;
import static com.microsoft.azuretools.telemetry.TelemetryConstants.SHUTDOWN_VM;
import static com.microsoft.azuretools.telemetry.TelemetryConstants.START_VM;
import static com.microsoft.azuretools.telemetry.TelemetryConstants.VM;

import com.microsoft.azure.CloudException;
import com.microsoft.azure.CommonIcons;
import com.microsoft.azure.management.compute.InstanceViewStatus;
import com.microsoft.azure.management.compute.VirtualMachine;
import com.microsoft.azure.management.resources.fluentcore.arm.ResourceId;
import com.microsoft.azuretools.authmanage.AuthMethodManager;
import com.microsoft.azuretools.azurecommons.helpers.AzureCmdException;
import com.microsoft.azuretools.sdkmanage.AzureManager;
import com.microsoft.azuretools.telemetry.AppInsightsConstants;
import com.microsoft.azuretools.telemetry.TelemetryProperties;
import com.microsoft.tooling.msservices.components.DefaultLoader;
import com.microsoft.tooling.msservices.serviceexplorer.*;
import com.microsoft.tooling.msservices.serviceexplorer.azure.AzureNodeActionPromptListener;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class VMNode extends RefreshableNode implements TelemetryProperties {
    private static String RUNNING_STATUS = "PowerState/running";
    private static String STOPPED = "stopped";

    private NodeAction startAction;
    private NodeAction restartAction;
    private NodeAction stopAction;

    @Override
    public Map<String, String> toProperties() {
        final Map<String, String> properties = new HashMap<>();
        properties.put(AppInsightsConstants.SubscriptionId, ResourceId.fromString(this.virtualMachine.id()).subscriptionId());
        properties.put(AppInsightsConstants.Region, this.virtualMachine.regionName());
        return properties;
    }

    public class DeleteVMAction extends AzureNodeActionPromptListener {
        public DeleteVMAction() {
            super(VMNode.this,
                    String.format("This operation will delete virtual machine %s.\nThe associated disks will not be deleted " +
                            "from your storage account.\n\nAre you sure you want to continue?", virtualMachine.name()),
                    "Deleting VM");
        }

        @Override
        protected void azureNodeAction(NodeActionEvent e)
                throws AzureCmdException {
            try {
                AzureManager azureManager = AuthMethodManager.getInstance().getAzureManager();
                // not signed in
                if (azureManager == null) {
                    return;
                }
                azureManager.getAzure(subscriptionId).virtualMachines().
                    deleteByResourceGroup(virtualMachine.resourceGroupName(), virtualMachine.name());
            } catch (Exception ex) {
            }

            DefaultLoader.getIdeHelper().invokeLater(() -> {
                // instruct parent node to remove this node
                getParent().removeDirectChildNode(VMNode.this);
            });
        }

        @Override
        protected void onSubscriptionsChanged(NodeActionEvent e) throws AzureCmdException {
        }

        @Override
        protected String getServiceName(NodeActionEvent event) {
            return VM;
        }

        @Override
        protected String getOperationName(NodeActionEvent event) {
            return DELETE_VM;
        }
    }

    public class RestartVMAction extends AzureNodeActionPromptListener {
        public RestartVMAction() {
            super(VMNode.this,
                    String.format("Are you sure you want to restart the virtual machine %s?", virtualMachine.computerName()),
                    "Restarting VM");
        }

        @Override
        protected void azureNodeAction(NodeActionEvent e)
                throws AzureCmdException {
            virtualMachine.restart();
            refreshItems();
        }

        @Override
        protected void onSubscriptionsChanged(NodeActionEvent e) throws AzureCmdException {
        }

        @Override
        protected String getServiceName(NodeActionEvent event) {
            return VM;
        }

        @Override
        protected String getOperationName(NodeActionEvent event) {
            return RESTART_VM;
        }
    }

    public class StartVMAction extends AzureNodeActionPromptListener {
        public StartVMAction() {
            super(VMNode.this,
                    String.format("Are you sure you want to start the virtual machine %s?", virtualMachine.computerName()),
                    "Starting VM");
        }

        @Override
        protected void azureNodeAction(NodeActionEvent e)
                throws AzureCmdException {
            virtualMachine.start();
            refreshItems();
        }

        @Override
        protected void onSubscriptionsChanged(NodeActionEvent e) throws AzureCmdException {
        }

        @Override
        protected String getServiceName(NodeActionEvent event) {
            return VM;
        }

        @Override
        protected String getOperationName(NodeActionEvent event) {
            return START_VM;
        }
    }

    public class ShutdownVMAction extends AzureNodeActionPromptListener {
        public ShutdownVMAction() {
            super(VMNode.this, String.format(
                    "This operation will result in losing the virtual IP address\nthat was assigned to this virtual machine.\n\n" +
                            "Are you sure that you want to shut down virtual machine %s?", virtualMachine.name()),
                    "Shutting down VM");
        }

        @Override
        protected void azureNodeAction(NodeActionEvent e)
                throws AzureCmdException {
            virtualMachine.powerOff();
            refreshItems();
        }

        @Override
        protected void onSubscriptionsChanged(NodeActionEvent e) throws AzureCmdException {
        }

        @Override
        protected String getServiceName(NodeActionEvent event) {
            return VM;
        }

        @Override
        protected String getOperationName(NodeActionEvent event) {
            return SHUTDOWN_VM;
        }
    }

    private static final String WAIT_ICON_PATH = "VirtualMachineUpdating.svg";
    private static final String STOP_ICON_PATH = "VirtualMachineStopped.svg";
    private static final String RUN_ICON_PATH = "VirtualMachineRunning.svg";
    private static final String ACTION_DELETE = "Delete";
    public static final String ACTION_DOWNLOAD_RDP_FILE = "Connect Remote Desktop";
    private static final String ACTION_SHUTDOWN = "Shutdown";
    private static final String ACTION_START = "Start";
    private static final String ACTION_RESTART = "Restart";
    private static final String ACTION_SHUTDOWN_ICON = "AzureStop.svg";
    private static final String ACTION_START_ICON = "AzureStart.svg";
    private static final String ACTION_DELETE_ICON = "Discard.svg";
    public static final int REMOTE_DESKTOP_PORT = 3389;

    private VirtualMachine virtualMachine;
    private String subscriptionId;

    public VMNode(Node parent, String subscriptionId, VirtualMachine virtualMachine)
            throws AzureCmdException {
        super(virtualMachine.id(), virtualMachine.name(), parent, WAIT_ICON_PATH, true);
        this.virtualMachine = virtualMachine;
        this.subscriptionId = subscriptionId;
        loadActions();

        // update vm icon based on vm status
        refreshItemsInternal();
    }

    private String getVMIconPath() {
        try {
            for (InstanceViewStatus status : virtualMachine.instanceView().statuses()) {
                if (RUNNING_STATUS.equalsIgnoreCase(status.code())) {
                    return RUN_ICON_PATH;
                }
                if (status.code().toLowerCase().contains(STOPPED)) {
                    return STOP_ICON_PATH;
                }
            }
        } catch (CloudException e) {
            DefaultLoader.getUIHelper().logError(e.getMessage(), e);
        }
        return WAIT_ICON_PATH;
    }

    @Override
    protected void refreshItems() throws AzureCmdException {
        virtualMachine.refresh();

        refreshItemsInternal();
    }

    private void refreshItemsInternal() {
        // update vm name and status icon
        setName(virtualMachine.name());
        setIconPath(getVMIconPath());
    }

    @Override
    protected void loadActions() {
        super.loadActions();
        addAction(ACTION_START, CommonIcons.ACTION_START, new StartVMAction());
        addAction(ACTION_RESTART, CommonIcons.ACTION_RESTART, new RestartVMAction());
        addAction(ACTION_SHUTDOWN, CommonIcons.ACTION_STOP, new ShutdownVMAction());
        addAction(ACTION_DELETE, CommonIcons.ACTION_DISCARD, new DeleteVMAction(), NodeActionPosition.BOTTOM);
    }

    @Override
    public List<NodeAction> getNodeActions() {
        if (startAction == null)
            startAction = getNodeActionByName(ACTION_START);

        if (restartAction == null)
            restartAction = getNodeActionByName(ACTION_RESTART);

        if (stopAction == null)
            stopAction = getNodeActionByName(ACTION_SHUTDOWN);

        List<NodeAction> nodeActions = super.getNodeActions();

//        // enable/disable menu items according to VM status
        boolean started = isRunning();

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

//        boolean stopped = virtualMachine.getStatus().equals(VirtualMachine.Status.Stopped) ||
//                virtualMachine.getStatus().equals(VirtualMachine.Status.StoppedDeallocated);
//
//        getNodeActionByName(ACTION_DOWNLOAD_RDP_FILE).setEnabled(!stopped && hasRDPPort(virtualMachine));
        getNodeActionByName(ACTION_SHUTDOWN).setEnabled(started);
        getNodeActionByName(ACTION_START).setEnabled(!started);
        getNodeActionByName(ACTION_RESTART).setEnabled(started);

        return nodeActions;
    }

    private boolean hasRDPPort(VirtualMachine virtualMachine) {
//        for (Endpoint endpoint : virtualMachine.getEndpoints()) {
//            if (endpoint.getPrivatePort() == REMOTE_DESKTOP_PORT) {
//                return true;
//            }
//        }

        return false;
    }

    private boolean isRunning() {
        try {
            for (InstanceViewStatus status : virtualMachine.instanceView().statuses()) {
                if (RUNNING_STATUS.equalsIgnoreCase(status.code())) {
                    return true;
                }
                if (status.code().toLowerCase().contains(STOPPED)) {
                    return false;
                }
            }
        } catch (CloudException e) {
            DefaultLoader.getUIHelper().logError(e.getMessage(), e);
        }
        return false;
    }
}
