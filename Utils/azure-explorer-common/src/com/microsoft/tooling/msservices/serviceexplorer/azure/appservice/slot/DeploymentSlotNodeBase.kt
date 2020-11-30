/**
 * Copyright (c) 2020 JetBrains s.r.o.
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

package com.microsoft.tooling.msservices.serviceexplorer.azure.appservice.slot

import com.microsoft.azure.management.appservice.DeploymentSlotBase
import com.microsoft.azuretools.azurecommons.helpers.AzureCmdException
import com.microsoft.tooling.msservices.components.DefaultLoader
import com.microsoft.tooling.msservices.serviceexplorer.Node
import com.microsoft.tooling.msservices.serviceexplorer.NodeAction
import com.microsoft.tooling.msservices.serviceexplorer.NodeActionEvent
import com.microsoft.tooling.msservices.serviceexplorer.NodeActionListener
import com.microsoft.tooling.msservices.serviceexplorer.azure.AzureNodeActionPromptListener
import com.microsoft.tooling.msservices.serviceexplorer.azure.webapp.base.WebAppBaseNode
import com.microsoft.tooling.msservices.serviceexplorer.azure.webapp.base.WebAppBaseState
import java.io.IOException

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

        private const val DELETE_SLOT_PROMPT_MESSAGE =
            "This operation will delete the Deployment Slot: %s.\nAre you sure you want to continue?"

        private const val DELETE_SLOT_PROGRESS_MESSAGE = "Deleting Deployment Slot"
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

    override fun getStartActionListener(): NodeActionListener =
        createBackgroundActionListener("Starting Deployment Slot") { start() }

    override fun getRestartActionListener(): NodeActionListener =
        createBackgroundActionListener("Restarting Deployment Slot") { restart() }

    override fun getStopActionListener(): NodeActionListener =
        createBackgroundActionListener("Stopping Deployment Slot") { stop() }

    abstract fun openDeploymentSlotPropertyAction()

    override fun getShowPropertiesActionListener(): NodeActionListener =
        object : NodeActionListener() {
            @Throws(AzureCmdException::class)
            override fun actionPerformed(e: NodeActionEvent) {
                openDeploymentSlotPropertyAction()
            }
        }

    override fun getOpenInBrowserActionListener(): NodeActionListener =
        object : NodeActionListener() {
            override fun actionPerformed(e: NodeActionEvent) {
                DefaultLoader.getUIHelper().openInBrowser("http://$hostName")
            }
        }

    override fun getDeleteActionListener(): NodeActionListener? {
        return DeleteDeploymentSlotAction(subscriptionId, this, name)
    }

    override fun loadActions() {
        addAction(ACTION_SWAP_WITH_PRODUCTION_NAME, createBackgroundActionListener("Swapping with Production") { swapWithProduction() })
        super.loadActions()
    }

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

    //region Actions

    private fun start() {
        try {
            presenter.onStartDeploymentSlot(subscriptionId, appId, slotName)
        } catch (e: IOException) {
            e.printStackTrace()
            DefaultLoader.getUIHelper().logError("Error while starting Deployment Slot '$slotName'", e)
        }
    }

    private fun stop() {
        try {
            presenter.onStopDeploymentSlot(subscriptionId, appId, slotName)
        } catch (e: IOException) {
            e.printStackTrace()
            DefaultLoader.getUIHelper().logError("Error while stopping Deployment Slot '$slotName'", e)
        }
    }

    private fun restart() {
        try {
            presenter.onRestartDeploymentSlot(subscriptionId, appId, slotName)
        } catch (e: IOException) {
            e.printStackTrace()
            DefaultLoader.getUIHelper().logError("Error while restarting Deployment Slot '$slotName'", e)
        }
    }

    private fun swapWithProduction() {
        try {
            presenter.onSwapWithProduction(subscriptionId, appId, slotName)
        } catch (e: IOException) {
            e.printStackTrace()
            DefaultLoader.getUIHelper().logError("Error while swapping Deployment Slot '$slotName' with production", e)
        }
    }

    private class DeleteDeploymentSlotAction(private val subscriptionId: String, private val node: Node, private val name: String) : AzureNodeActionPromptListener(
        node,
        String.format(DELETE_SLOT_PROMPT_MESSAGE, name),
        DELETE_SLOT_PROGRESS_MESSAGE) {

        override fun azureNodeAction(e: NodeActionEvent) {
            node.parent.removeNode(subscriptionId, name, node)
        }

        override fun onSubscriptionsChanged(e: NodeActionEvent?) { }
    }

    //endregion Actions
}
