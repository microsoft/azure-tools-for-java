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
import com.microsoft.azure.management.appservice.WebAppBase
import com.microsoft.tooling.msservices.components.DefaultLoader
import com.microsoft.tooling.msservices.serviceexplorer.AzureRefreshableNode
import com.microsoft.tooling.msservices.serviceexplorer.Node
import com.microsoft.tooling.msservices.serviceexplorer.NodeAction
import org.slf4j.Logger

abstract class DeploymentSlotModuleBase<TSlot : DeploymentSlotBase<TSlot>>(
    moduleId: String,
    parent: Node,
    val subscriptionId: String,
    val app: WebAppBase,
    val isDeploymentSlotsSupported: Boolean,
    private val logger: Logger
) : AzureRefreshableNode(moduleId, MODULE_NAME, parent, SLOT_ICON_PATH), DeploymentSlotModuleView<TSlot> {

    companion object {
        private const val MODULE_NAME = "Deployment Slots"
        private const val SLOT_ICON_PATH = "Slot.svg"
    }

    abstract val presenter: DeploymentSlotModulePresenterBase<TSlot, DeploymentSlotModuleView<TSlot>>

    override fun getNodeActions(): MutableList<NodeAction> {
        val nodeActions = super.getNodeActions()
        val action = getNodeActionByName("New Deployment Slot")
            ?: return nodeActions

        if (!isDeploymentSlotsSupported)
            action.isEnabled = false

        return nodeActions
    }

    override fun refreshItems() {
        try {
            presenter.onRefreshDeploymentSlotModule(subscriptionId, app.id())
        } catch (t: Throwable) {
            t.printStackTrace()
            logger.error(t.message)
        }
    }

    override fun removeNode(sid: String, id: String, node: Node) {
        try {
            presenter.onDeleteDeploymentSlot(sid, app.id(), id)
            removeDirectChildNode(node)
        } catch (t: Throwable) {
            DefaultLoader.getUIHelper()
                .logError("An error occurred while attempting to delete the Deployment Slot", t)
        }
    }
}
