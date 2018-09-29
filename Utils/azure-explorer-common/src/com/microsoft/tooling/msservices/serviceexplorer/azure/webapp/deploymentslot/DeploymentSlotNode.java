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

import java.io.IOException;
import java.util.List;

import com.microsoft.tooling.msservices.components.DefaultLoader;
import com.microsoft.tooling.msservices.serviceexplorer.NodeAction;
import com.microsoft.tooling.msservices.serviceexplorer.NodeActionEvent;
import com.microsoft.tooling.msservices.serviceexplorer.NodeActionListener;
import com.microsoft.tooling.msservices.serviceexplorer.azure.webapp.base.WebAppBaseNode;
import com.microsoft.tooling.msservices.serviceexplorer.azure.webapp.base.WebAppBaseState;

public class DeploymentSlotNode extends WebAppBaseNode implements DeploymentSlotNodeView {
    private static final String ACTION_SWAP_WITH_PRODUCTION = "Swap with production";
    private static final String LABEL = "Slot";
    private final DeploymentSlotNodePresenter presenter;
    protected final String webAppId;
    protected final String slotName;

    public DeploymentSlotNode(final String slotId, final String webAppId, final DeploymentSlotModule parent,
                              final String name, final String state, final String os, final String subscriptionId,
                              final String hostName) {
        super(slotId, name, LABEL, parent, subscriptionId, hostName, os, state);
        this.webAppId = webAppId;
        this.slotName = name;
        this.presenter = new DeploymentSlotNodePresenter();
        this.presenter.onAttachView(this);
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
            createBackgroundActionListener("Stopping Deployment Slot", () -> stop()));
        addAction(ACTION_START, createBackgroundActionListener("Starting Deployment Slot", () -> start()));
        addAction(ACTION_RESTART,
            createBackgroundActionListener("Restarting Deployment Slot", () -> restart()));
        addAction(ACTION_SWAP_WITH_PRODUCTION,
            createBackgroundActionListener("Swapping with Production", () -> swapWithProduction()));
        addAction(ACTION_OPEN_IN_BROWSER, new NodeActionListener() {
            @Override
            protected void actionPerformed(NodeActionEvent e) {
                DefaultLoader.getUIHelper().openInBrowser("http://" + hostName);
            }
        });

        super.loadActions();
    }

    @Override
    protected void onNodeClick(NodeActionEvent e) {
        // RefreshableNode refresh itself when the first time being clicked.
        // The deployment slot node is just a single node for the time being.
        // Override the function to do noting to disable the auto refresh functionality.
    }

    private void start() {
        try {
            presenter.onStartDeploymentSlot(this.subscriptionId, this.webAppId, this.slotName);
        } catch (IOException e) {
            e.printStackTrace();
            // TODO: Error handling
        }
    }

    private void stop() {
        try {
            presenter.onStopDeploymentSlot(this.subscriptionId, this.webAppId, this.slotName);
        } catch (IOException e) {
            e.printStackTrace();
            // TODO: Error handling
        }
    }

    private void restart() {
        try {
            presenter.onRestartDeploymentSlot(this.subscriptionId, this.webAppId, this.slotName);
        } catch (IOException e) {
            e.printStackTrace();
            // TODO: Error handling
        }
    }

    private void swapWithProduction() {
        try {
            presenter.onSwapWithProduction(this.subscriptionId, this.webAppId, this.slotName);
        } catch (IOException e) {
            e.printStackTrace();
            // TODO: Error handling
        }
    }

    @Override
    protected void refreshItems() {
        try {
            presenter.onRefreshNode(this.subscriptionId, this.webAppId, this.slotName);
        } catch (Exception e) {
            e.printStackTrace();
            // TODO: Error handling
        }
    }
}
