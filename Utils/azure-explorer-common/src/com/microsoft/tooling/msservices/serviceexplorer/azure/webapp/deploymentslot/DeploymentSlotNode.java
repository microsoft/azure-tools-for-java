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
import com.microsoft.azuretools.ActionConstants;
import com.microsoft.azuretools.azurecommons.helpers.Nullable;
import com.microsoft.azuretools.core.mvp.model.webapp.AzureWebAppMvpModel;
import com.microsoft.tooling.msservices.components.DefaultLoader;
import com.microsoft.tooling.msservices.serviceexplorer.AzureActionEnum;
import com.microsoft.tooling.msservices.serviceexplorer.AzureIconSymbol;
import com.microsoft.tooling.msservices.serviceexplorer.BasicActionBuilder;
import com.microsoft.tooling.msservices.serviceexplorer.NodeAction;
import com.microsoft.tooling.msservices.serviceexplorer.NodeActionEvent;
import com.microsoft.tooling.msservices.serviceexplorer.azure.webapp.base.WebAppBaseNode;
import com.microsoft.tooling.msservices.serviceexplorer.azure.webapp.base.WebAppBaseState;

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
        addAction(initActionBuilder(this::start).withAction(AzureActionEnum.START).withBackgroudable(true).build());
        addAction(initActionBuilder(this::stop).withAction(AzureActionEnum.STOP).withBackgroudable(true).build());
        addAction(initActionBuilder(this::restart).withAction(AzureActionEnum.RESTART).withBackgroudable(true).build());
        addAction(initActionBuilder(this::delete).withAction(AzureActionEnum.DELETE).withBackgroudable(true).withPromptable(true).build());
        addAction(initActionBuilder(this::openInBrowser).withAction(AzureActionEnum.OPEN_IN_PORTAL).withBackgroudable(true).build());
        addAction(initActionBuilder(this::showProperties).withAction(AzureActionEnum.SHOW_PROPERTIES).build());
        addAction(ACTION_SWAP_WITH_PRODUCTION, initActionBuilder(this::swap).withBackgroudable(true).build("Swapping"));
        super.loadActions();
    }

    private BasicActionBuilder initActionBuilder(Runnable runnable) {
        return new BasicActionBuilder(runnable)
                .withModuleName(DeploymentSlotModule.MODULE_NAME)
                .withInstanceName(name);
    }

    @Override
    protected void onNodeClick(NodeActionEvent e) {
        // RefreshableNode refresh itself when the first time being clicked.
        // The deployment slot node is just a single node for the time being.
        // Override the function to do noting to disable the auto refresh functionality.
    }

    @Override
    @AzureOperation(name = "refresh deployment slot", type = AzureOperation.Type.ACTION)
    protected void refreshItems() {
        final WebApp app = AzureWebAppMvpModel.getInstance().getWebAppById(subscriptionId, webAppId);
        final DeploymentSlot slot = app.deploymentSlots().getByName(slotName);
        this.renderNode(WebAppBaseState.fromString(slot.state()));
    }

    @AzureOperation(name = ActionConstants.WebApp.DeploymentSlot.START, type = AzureOperation.Type.ACTION)
    private void start() {
        AzureWebAppMvpModel.getInstance().startDeploymentSlot(subscriptionId, webAppId, slotName);
        this.renderNode(WebAppBaseState.RUNNING);
    }

    @AzureOperation(name = ActionConstants.WebApp.DeploymentSlot.STOP, type = AzureOperation.Type.ACTION)
    private void stop() {
        AzureWebAppMvpModel.getInstance().stopDeploymentSlot(subscriptionId, webAppId, slotName);
        this.renderNode(WebAppBaseState.STOPPED);
    }

    @AzureOperation(name = ActionConstants.WebApp.DeploymentSlot.RESTART, type = AzureOperation.Type.ACTION)
    private void restart() {
        AzureWebAppMvpModel.getInstance().restartDeploymentSlot(subscriptionId, webAppId, slotName);
        this.renderNode(WebAppBaseState.RUNNING);
    }

    @AzureOperation(name = ActionConstants.WebApp.DeploymentSlot.DELETE, type = AzureOperation.Type.ACTION)
    private void delete() {
        this.getParent().removeNode(this.getSubscriptionId(), this.getName(), DeploymentSlotNode.this);
    }

    @AzureOperation(name = ActionConstants.WebApp.DeploymentSlot.SWAP, type = AzureOperation.Type.ACTION)
    private void swap() {
        AzureWebAppMvpModel.getInstance().swapSlotWithProduction(subscriptionId, webAppId, slotName);
    }

    @AzureOperation(name = ActionConstants.WebApp.DeploymentSlot.OPEN_IN_BROWSER, type = AzureOperation.Type.ACTION)
    private void openInBrowser() {
        DefaultLoader.getUIHelper().openInBrowser("http://" + this.hostName);
    }

    @AzureOperation(name = ActionConstants.WebApp.DeploymentSlot.SHOW_PROPERTIES, type = AzureOperation.Type.ACTION)
    private void showProperties() {
        DefaultLoader.getUIHelper().openDeploymentSlotPropertyView(DeploymentSlotNode.this);
    }

}
