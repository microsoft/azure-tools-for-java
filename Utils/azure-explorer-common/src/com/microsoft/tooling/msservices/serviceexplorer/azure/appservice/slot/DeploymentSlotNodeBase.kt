/**s
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Copyright (c) 2020-2021 JetBrains s.r.o.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.tooling.msservices.serviceexplorer.azure.appservice.slot

import com.microsoft.azure.management.appservice.DeploymentSlotBase
import com.microsoft.azure.toolkit.lib.common.operation.AzureOperation
import com.microsoft.azuretools.ActionConstants
import com.microsoft.azuretools.azurecommons.helpers.Nullable
import com.microsoft.tooling.msservices.components.DefaultLoader
import com.microsoft.tooling.msservices.serviceexplorer.*
import com.microsoft.tooling.msservices.serviceexplorer.azure.webapp.base.WebAppBaseNode
import com.microsoft.tooling.msservices.serviceexplorer.azure.webapp.base.WebAppBaseState
import com.microsoft.tooling.msservices.serviceexplorer.azure.webapp.deploymentslot.DeploymentSlotModule

abstract class DeploymentSlotNodeBase<TSlot : DeploymentSlotBase<TSlot>>(
    val appId: String,
    val appName: String,
    parent: DeploymentSlotModuleBase<TSlot>,
    val slotId: String,
    val slotName: String,
    state: String,
    os: String,
    subscriptionId: String,
    hostName: String
) : WebAppBaseNode(slotId, slotName, SLOT_LABEL, parent, subscriptionId, hostName, os, state),
    DeploymentSlotNodeView {

    companion object {
        private const val SLOT_LABEL = "Slot"
        private const val ACTION_SWAP_WITH_PRODUCTION_NAME = "Swap with production"
    }

    abstract val presenter: DeploymentSlotNodePresenterBase<TSlot, DeploymentSlotNodeView>

    init {
        loadActions()
    }

    override fun getNodeActions(): List<NodeAction?>? {
        val swapAction = getNodeActionByName(ACTION_SWAP_WITH_PRODUCTION_NAME)
        if (swapAction != null)
            swapAction.isEnabled = state == WebAppBaseState.RUNNING

        return super.getNodeActions()
    }

    abstract fun openDeploymentSlotPropertyAction()

    override fun getShowPropertiesActionListener(): NodeActionListener {
        return initActionBuilder { openDeploymentSlotPropertyAction() }
            .withAction(AzureActionEnum.SHOW_PROPERTIES)
            .withBackgroudable(true)
            .build(AzureActionEnum.SHOW_PROPERTIES.doingName)
    }

    override fun getOpenInBrowserActionListener(): NodeActionListener {
        return initActionBuilder { openInBrowser() }
            .withAction(AzureActionEnum.OPEN_IN_BROWSER)
            .withBackgroudable(true)
            .build(AzureActionEnum.OPEN_IN_BROWSER.doingName)
    }

    override fun getStartActionListener(): NodeActionListener {
        return initActionBuilder { this.start() }
            .withAction(AzureActionEnum.START)
            .withBackgroudable(true)
            .build(AzureActionEnum.START.doingName)
    }

    override fun getRestartActionListener(): NodeActionListener {
        return initActionBuilder { this.restart() }
            .withAction(AzureActionEnum.RESTART)
            .withBackgroudable(true)
            .build(AzureActionEnum.RESTART.doingName)
    }

    override fun getStopActionListener(): NodeActionListener {
        return initActionBuilder { this.stop() }
            .withAction(AzureActionEnum.STOP)
            .withBackgroudable(true)
            .build(AzureActionEnum.STOP.doingName)
    }

    override fun getDeleteActionListener(): NodeActionListener {
        return initActionBuilder(Runnable { delete() })
            .withAction(AzureActionEnum.DELETE)
            .withBackgroudable(true)
            .withPromptable(true)
            .build(AzureActionEnum.DELETE.doingName)
    }

    @AzureOperation(name = "webapp|deployment.refresh", params = ["@slotName", "@webAppName"], type = AzureOperation.Type.ACTION)
    override fun refreshItems() {
        try {
            presenter.onRefreshNode(subscriptionId, appId, slotName)
        } catch (e: Exception) {
            e.printStackTrace()
            DefaultLoader.getUIHelper().logError("Error while refreshing Deployment Slot '$slotName'", e)
        }
    }

    override fun onNodeClick(e: NodeActionEvent?) {
        // RefreshableNode refresh itself when the first time being clicked.
        // The deployment slot node is just a single node for the time being.
        // Override the function to do noting to disable the auto refresh functionality.
    }

    override fun getIconSymbol(): @Nullable AzureIconSymbol? {
        val isLinux = OS_LINUX.equals(os, ignoreCase = true)
        val running = WebAppBaseState.RUNNING == state
        val updating = WebAppBaseState.UPDATING == state
        return if (isLinux) {
            if (running) AzureIconSymbol.DeploymentSlot.RUNNING_ON_LINUX else if (updating) AzureIconSymbol.DeploymentSlot.UPDATING_ON_LINUX else AzureIconSymbol.DeploymentSlot.STOPPED_ON_LINUX
        } else {
            if (running) AzureIconSymbol.DeploymentSlot.RUNNING else if (updating) AzureIconSymbol.DeploymentSlot.UPDATING else AzureIconSymbol.DeploymentSlot.STOPPED
        }
    }

    override fun loadActions() {
        super.loadActions()
        addAction(ACTION_SWAP_WITH_PRODUCTION_NAME,
            initActionBuilder(Runnable { swap() }).withBackgroudable(true)
                .build("Swapping")
        )
    }

    private fun initActionBuilder(runnable: Runnable): BasicActionBuilder {
        return BasicActionBuilder(runnable)
            .withModuleName(DeploymentSlotModule.MODULE_NAME)
            .withInstanceName(name)
    }

    @AzureOperation(name = ActionConstants.WebApp.DeploymentSlot.START, type = AzureOperation.Type.ACTION)
    private fun start() {
        presenter.onStartDeploymentSlot(subscriptionId, appId, slotName)
        renderNode(WebAppBaseState.RUNNING)
    }

    @AzureOperation(name = ActionConstants.WebApp.DeploymentSlot.STOP, type = AzureOperation.Type.ACTION)
    private fun stop() {
        presenter.onStopDeploymentSlot(subscriptionId, appId, slotName)
        renderNode(WebAppBaseState.STOPPED)
    }

    @AzureOperation(name = ActionConstants.WebApp.DeploymentSlot.RESTART, type = AzureOperation.Type.ACTION)
    private fun restart() {
        presenter.onRestartDeploymentSlot(subscriptionId, appId, slotName)
        renderNode(WebAppBaseState.RUNNING)
    }

    @AzureOperation(name = ActionConstants.WebApp.DeploymentSlot.DELETE, type = AzureOperation.Type.ACTION)
    private fun delete() {
        getParent().removeNode(getSubscriptionId(), getName(), this)
    }

    @AzureOperation(name = ActionConstants.WebApp.DeploymentSlot.SWAP, type = AzureOperation.Type.ACTION)
    private fun swap() {
        presenter.onSwapWithProduction(subscriptionId, appId, slotName)
    }

    @AzureOperation(name = ActionConstants.WebApp.DeploymentSlot.OPEN_IN_BROWSER, type = AzureOperation.Type.ACTION)
    private fun openInBrowser() {
        DefaultLoader.getUIHelper().openInBrowser("https://$hostName")
    }

    @AzureOperation(name = ActionConstants.WebApp.DeploymentSlot.SHOW_PROPERTIES, type = AzureOperation.Type.ACTION)
    private fun showProperties() {
        openDeploymentSlotPropertyAction()
    }
}
