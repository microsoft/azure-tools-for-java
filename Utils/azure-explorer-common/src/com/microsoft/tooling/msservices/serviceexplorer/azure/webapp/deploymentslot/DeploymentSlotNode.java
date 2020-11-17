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

import com.microsoft.azure.toolkit.lib.common.operation.AzureOperation;
import com.microsoft.tooling.msservices.components.DefaultLoader;
import com.microsoft.tooling.msservices.serviceexplorer.NodeAction;
import com.microsoft.tooling.msservices.serviceexplorer.NodeActionEvent;
import com.microsoft.tooling.msservices.serviceexplorer.NodeActionListener;
import com.microsoft.tooling.msservices.serviceexplorer.WrappedTelemetryNodeActionListener;
import com.microsoft.tooling.msservices.serviceexplorer.azure.AzureNodeActionPromptListener;
import com.microsoft.tooling.msservices.serviceexplorer.azure.webapp.base.WebAppBaseNode;
import com.microsoft.tooling.msservices.serviceexplorer.azure.webapp.base.WebAppBaseState;

import java.util.List;

import static com.microsoft.azuretools.telemetry.TelemetryConstants.*;

public class DeploymentSlotNode extends WebAppBaseNode implements DeploymentSlotNodeView {
    private static final String ACTION_SWAP_WITH_PRODUCTION = "Swap with production";
    private static final String LABEL = "Slot";
    private static final String DELETE_SLOT_PROMPT_MESSAGE = "This operation will delete the Deployment Slot: %s.\n"
        + "Are you sure you want to continue?";
    private static final String DELETE_SLOT_PROGRESS_MESSAGE = "Deleting Deployment Slot";
    private final DeploymentSlotNodePresenter presenter;
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
        this.presenter = new DeploymentSlotNodePresenter();
        this.presenter.onAttachView(this);
        loadActions();
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
        addAction(ACTION_STOP, getIcon(this.os, this.label, WebAppBaseState.STOPPED),
            new WrappedTelemetryNodeActionListener(WEBAPP, STOP_WEBAPP_SLOT,
                createBackgroundActionListener("Stopping Deployment Slot", () -> stop())));
        addAction(ACTION_START, new WrappedTelemetryNodeActionListener(WEBAPP, START_WEBAPP_SLOT,
            createBackgroundActionListener("Starting Deployment Slot", () -> start())));
        addAction(ACTION_RESTART, new WrappedTelemetryNodeActionListener(WEBAPP, RESTART_WEBAPP_SLOT,
            createBackgroundActionListener("Restarting Deployment Slot", () -> restart())));
        addAction(ACTION_SWAP_WITH_PRODUCTION, new WrappedTelemetryNodeActionListener(WEBAPP, SWAP_WEBAPP_SLOT,
            createBackgroundActionListener("Swapping with Production", () -> swapWithProduction())));
        addAction(ACTION_OPEN_IN_BROWSER, new WrappedTelemetryNodeActionListener(WEBAPP, OPERN_WEBAPP_SLOT_BROWSER, new NodeActionListener() {
            @Override
            protected void actionPerformed(NodeActionEvent e) {
                openInBrowser();
            }
        }));
        addAction(ACTION_DELETE, new DeleteDeploymentSlotAction());
        addAction(ACTION_SHOW_PROPERTY, new WrappedTelemetryNodeActionListener(WEBAPP, SHOW_WEBAPP_SLOT_PROP, new NodeActionListener() {
            @Override
            protected void actionPerformed(NodeActionEvent e) {
                showProperties();
            }
        }));
        super.loadActions();
    }

    @AzureOperation(
        value = "show properties of deployment slot[%s] of app[%s]",
        params = {"@slotName", "@webAppName"},
        type = AzureOperation.Type.ACTION
    )
    private void showProperties() {
        DefaultLoader.getUIHelper().openDeploymentSlotPropertyView(
            DeploymentSlotNode.this);
    }

    @AzureOperation(
        value = "open deployment slot[%s] of app[%s] in local browser",
        params = {"@slotName", "@webAppName"},
        type = AzureOperation.Type.ACTION
    )
    private void openInBrowser() {
        DefaultLoader.getUIHelper().openInBrowser("http://" + hostName);
    }

    @Override
    protected void onNodeClick(NodeActionEvent e) {
        // RefreshableNode refresh itself when the first time being clicked.
        // The deployment slot node is just a single node for the time being.
        // Override the function to do noting to disable the auto refresh functionality.
    }

    @AzureOperation(
        value = "start deployment slot[%s] of app[%s]",
        params = {"@slotName", "@webAppName"},
        type = AzureOperation.Type.ACTION
    )
    private void start() {
        presenter.onStartDeploymentSlot(this.subscriptionId, this.webAppId, this.slotName);
    }

    @AzureOperation(
        value = "stop deployment slot[%s] of app[%s]",
        params = {"@slotName", "@webAppName"},
        type = AzureOperation.Type.ACTION
    )
    private void stop() {
        presenter.onStopDeploymentSlot(this.subscriptionId, this.webAppId, this.slotName);
    }

    @AzureOperation(
        value = "restart deployment slot[%s] of app[%s]",
        params = {"@slotName", "@webAppName"},
        type = AzureOperation.Type.ACTION
    )
    private void restart() {
        presenter.onRestartDeploymentSlot(this.subscriptionId, this.webAppId, this.slotName);
    }

    @AzureOperation(
        value = "swap deployment slot[%s] of app[%s] for production",
        params = {"@slotName", "@webAppName"},
        type = AzureOperation.Type.ACTION
    )
    private void swapWithProduction() {
        presenter.onSwapWithProduction(this.subscriptionId, this.webAppId, this.slotName);
    }

    @Override
    @AzureOperation(
        value = "refresh deployment slot[%s] of app[%s]",
        params = {"@slotName", "@webAppName"},
        type = AzureOperation.Type.ACTION
    )
    protected void refreshItems() {
        presenter.onRefreshNode(this.subscriptionId, this.webAppId, this.slotName);
    }

    private class DeleteDeploymentSlotAction extends AzureNodeActionPromptListener {
        DeleteDeploymentSlotAction() {
            super(DeploymentSlotNode.this, String.format(DELETE_SLOT_PROMPT_MESSAGE, getName()),
                DELETE_SLOT_PROGRESS_MESSAGE);
        }

        @Override
        protected void azureNodeAction(NodeActionEvent e) {
            getParent().removeNode(getSubscriptionId(), getName(), DeploymentSlotNode.this);
        }

        @Override
        protected void onSubscriptionsChanged(NodeActionEvent e) {
        }

        @Override
        protected String getServiceName(NodeActionEvent event) {
            return WEBAPP;
        }

        @Override
        protected String getOperationName(NodeActionEvent event) {
            return DELETE_WEBAPP_SLOT;
        }
    }
}
