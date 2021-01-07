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

package com.microsoft.tooling.msservices.serviceexplorer.azure.webapp.deploymentslot;

import com.microsoft.azure.management.appservice.DeploymentSlot;
import com.microsoft.azure.management.appservice.WebApp;
import com.microsoft.azure.toolkit.lib.common.operation.AzureOperation;
import com.microsoft.azuretools.azurecommons.helpers.Nullable;
import com.microsoft.azuretools.core.mvp.model.webapp.AzureWebAppMvpModel;
import com.microsoft.azuretools.telemetry.TelemetryParameter;
import com.microsoft.tooling.msservices.components.DefaultLoader;
import com.microsoft.tooling.msservices.serviceexplorer.AzureActionEnum;
import com.microsoft.tooling.msservices.serviceexplorer.AzureIconSymbol;
import com.microsoft.tooling.msservices.serviceexplorer.Node;
import com.microsoft.tooling.msservices.serviceexplorer.NodeAction;
import com.microsoft.tooling.msservices.serviceexplorer.NodeActionEvent;
import com.microsoft.tooling.msservices.serviceexplorer.NodeActionListener;
import com.microsoft.tooling.msservices.serviceexplorer.azure.webapp.base.WebAppBaseNode;
import com.microsoft.tooling.msservices.serviceexplorer.azure.webapp.base.WebAppBaseState;
import com.microsoft.tooling.msservices.serviceexplorer.listener.ActionBackgroundable;
import com.microsoft.tooling.msservices.serviceexplorer.listener.ActionPromptable;
import com.microsoft.tooling.msservices.serviceexplorer.listener.ActionTelemetrable;

import java.util.List;

public class DeploymentSlotNode extends WebAppBaseNode implements DeploymentSlotNodeView {
    private static final String ACTION_SWAP_WITH_PRODUCTION = "Swap with production";
    private static final String LABEL = "Slot";
    protected final String webAppId;
    protected final String webAppName;
    protected final String slotName;

    public DeploymentSlotNode(final String slotId, final String webAppId, final String webAppName,
                              final DeploymentSlotModule parent, final String name, final String state, final String os,
                              final String subscriptionId, final String hostName) {
        super(slotId, name, LABEL, parent, subscriptionId, hostName, os, state);
        this.webAppId = webAppId;
        this.webAppName = webAppName;
        this.slotName = name;
        loadActions();
    }

    @Override
    public @Nullable AzureIconSymbol getIconSymbol() {
        boolean isLinux = OS_LINUX.equalsIgnoreCase(os);
        boolean running = WebAppBaseState.RUNNING.equals(state);
        boolean updating = WebAppBaseState.UPDATING.equals(state);
        if (isLinux) {
            return running ? AzureIconSymbol.DeploymentSlot.RUNNING_ON_LINUX :
                    updating ? AzureIconSymbol.DeploymentSlot.UPDATING_ON_LINUX : AzureIconSymbol.DeploymentSlot.STOPPED_ON_LINUX;
        } else {
            return running ? AzureIconSymbol.DeploymentSlot.RUNNING :
                    updating ? AzureIconSymbol.DeploymentSlot.UPDATING : AzureIconSymbol.DeploymentSlot.STOPPED;
        }
    }

    public String getWebAppId() {
        return this.webAppId;
    }

    public String getWebAppName() {
        return this.webAppName;
    }

    @Override
    public List<NodeAction> getNodeActions() {
        getNodeActionByName(ACTION_SWAP_WITH_PRODUCTION).setEnabled(this.state == WebAppBaseState.RUNNING);
        return super.getNodeActions();
    }

    @Override
    protected void loadActions() {
        // todo: why only the stop action has icon?
        addAction(new StopAction().asGenericListener(AzureActionEnum.STOP));
        addAction(new StartAction().asGenericListener(AzureActionEnum.START));
        addAction(new RestartAction().asGenericListener(AzureActionEnum.RESTART));
        addAction(new OpenInBrowserAction().asGenericListener(AzureActionEnum.OPEN_IN_BROWSER));
        addAction(new DeleteAction().asGenericListener(AzureActionEnum.DELETE));
        addAction(new ShowPropertiesAction().asGenericListener(AzureActionEnum.SHOW_PROPERTIES));
        addAction(ACTION_SWAP_WITH_PRODUCTION, new SwapAction().asGenericListener());
        super.loadActions();
    }

    @Override
    protected void onNodeClick(NodeActionEvent e) {
        // RefreshableNode refresh itself when the first time being clicked.
        // The deployment slot node is just a single node for the time being.
        // Override the function to do noting to disable the auto refresh functionality.
    }

    @Override
    @AzureOperation(value = "refresh deployment slot", type = AzureOperation.Type.ACTION)
    protected void refreshItems() {
        final WebApp app = AzureWebAppMvpModel.getInstance().getWebAppById(subscriptionId, webAppId);
        final DeploymentSlot slot = app.deploymentSlots().getByName(slotName);
        this.renderNode(WebAppBaseState.fromString(slot.state()));
    }

    // Delete action class
    private class DeleteAction extends NodeActionListener implements ActionBackgroundable, ActionPromptable, ActionTelemetrable {

        @Override
        protected void actionPerformed(NodeActionEvent e) {
            DeploymentSlotNode.this.getParent()
                    .removeNode(DeploymentSlotNode.this.getSubscriptionId(), DeploymentSlotNode.this.getName(), DeploymentSlotNode.this);
        }

        @Override
        public String getPromptMessage() {
            return Node.getPromptMessage(AzureActionEnum.DELETE.getName().toLowerCase(), DeploymentSlotModule.MODULE_NAME, DeploymentSlotNode.this.name);
        }

        @Override
        public String getProgressMessage() {
            return Node.getProgressMessage(AzureActionEnum.DELETE.getDoingName(), DeploymentSlotModule.MODULE_NAME, DeploymentSlotNode.this.name);
        }

        @Override
        public TelemetryParameter getTelemetryParameter() {
            return TelemetryParameter.WebApp.DeploymentSlot.DELETE;
        }
    }

    // start action class
    private class StartAction extends NodeActionListener implements ActionBackgroundable, ActionTelemetrable {

        @AzureOperation(value = "start deployment slot", type = AzureOperation.Type.ACTION)
        @Override
        protected void actionPerformed(NodeActionEvent e) {
            AzureWebAppMvpModel.getInstance().startDeploymentSlot(subscriptionId, webAppId, slotName);
            DeploymentSlotNode.this.renderNode(WebAppBaseState.RUNNING);
        }

        @Override
        public String getProgressMessage() {
            return Node.getProgressMessage(AzureActionEnum.START.getDoingName(), DeploymentSlotModule.MODULE_NAME, DeploymentSlotNode.this.name);
        }

        @Override
        public TelemetryParameter getTelemetryParameter() {
            return TelemetryParameter.WebApp.DeploymentSlot.START;
        }
    }

    // stop action class
    private class StopAction extends NodeActionListener implements ActionBackgroundable, ActionTelemetrable {

        @AzureOperation(value = "stop deployment slot", type = AzureOperation.Type.ACTION)
        @Override
        protected void actionPerformed(NodeActionEvent e) {
            AzureWebAppMvpModel.getInstance().stopDeploymentSlot(subscriptionId, webAppId, slotName);
            DeploymentSlotNode.this.renderNode(WebAppBaseState.STOPPED);
        }

        @Override
        public String getProgressMessage() {
            return Node.getProgressMessage(AzureActionEnum.STOP.getDoingName(), DeploymentSlotModule.MODULE_NAME, DeploymentSlotNode.this.name);
        }

        @Override
        public TelemetryParameter getTelemetryParameter() {
            return TelemetryParameter.WebApp.DeploymentSlot.STOP;
        }

    }

    // restart action class
    private class RestartAction extends NodeActionListener implements ActionBackgroundable, ActionTelemetrable {

        @AzureOperation(value = "restart deployment slot", type = AzureOperation.Type.ACTION)
        @Override
        protected void actionPerformed(NodeActionEvent e) {
            AzureWebAppMvpModel.getInstance().restartDeploymentSlot(subscriptionId, webAppId, slotName);
            DeploymentSlotNode.this.renderNode(WebAppBaseState.RUNNING);
        }

        @Override
        public String getProgressMessage() {
            return Node.getProgressMessage(AzureActionEnum.RESTART.getDoingName(), DeploymentSlotModule.MODULE_NAME, DeploymentSlotNode.this.name);
        }

        @Override
        public TelemetryParameter getTelemetryParameter() {
            return TelemetryParameter.WebApp.DeploymentSlot.RESTART;
        }

    }

    // restart action class
    private class SwapAction extends NodeActionListener implements ActionBackgroundable, ActionTelemetrable {

        @AzureOperation(value = "swap deployment slot for production", type = AzureOperation.Type.ACTION)
        @Override
        protected void actionPerformed(NodeActionEvent e) {
            AzureWebAppMvpModel.getInstance().swapSlotWithProduction(subscriptionId, webAppId, slotName);
        }

        @Override
        public String getProgressMessage() {
            return Node.getProgressMessage("Swapping", DeploymentSlotModule.MODULE_NAME, DeploymentSlotNode.this.name);
        }

        @Override
        public TelemetryParameter getTelemetryParameter() {
            return TelemetryParameter.WebApp.DeploymentSlot.SWAP;
        }

    }

    // Open in browser action class
    private class OpenInBrowserAction extends NodeActionListener implements ActionBackgroundable, ActionTelemetrable {

        @AzureOperation(value = "open deployment slot in local browser", type = AzureOperation.Type.ACTION)
        @Override
        protected void actionPerformed(NodeActionEvent e) {
            DefaultLoader.getUIHelper().openInBrowser("http://" + DeploymentSlotNode.this.hostName);
        }

        @Override
        public String getProgressMessage() {
            return Node.getProgressMessage(AzureActionEnum.OPEN_IN_BROWSER.getDoingName(), DeploymentSlotModule.MODULE_NAME, DeploymentSlotNode.this.name);
        }

        @Override
        public TelemetryParameter getTelemetryParameter() {
            return TelemetryParameter.WebApp.DeploymentSlot.OPEN_IN_BROWSER;
        }
    }

    // Show properties action class
    private class ShowPropertiesAction extends NodeActionListener implements ActionTelemetrable {

        @AzureOperation(value = "show properties of deployment slot", type = AzureOperation.Type.ACTION)
        @Override
        protected void actionPerformed(NodeActionEvent e) {
            DefaultLoader.getUIHelper().openDeploymentSlotPropertyView(DeploymentSlotNode.this);
        }

        @Override
        public TelemetryParameter getTelemetryParameter() {
            return TelemetryParameter.WebApp.DeploymentSlot.SHOW_PROPERTIES;
        }
    }
}
