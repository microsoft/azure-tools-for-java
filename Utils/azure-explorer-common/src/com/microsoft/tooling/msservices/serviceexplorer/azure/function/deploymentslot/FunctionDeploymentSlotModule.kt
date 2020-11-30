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

package com.microsoft.tooling.msservices.serviceexplorer.azure.function.deploymentslot

import com.microsoft.azure.management.appservice.FunctionApp
import com.microsoft.azure.management.appservice.FunctionDeploymentSlot
import com.microsoft.tooling.msservices.serviceexplorer.Node
import com.microsoft.tooling.msservices.serviceexplorer.azure.appservice.slot.DeploymentSlotModuleBase
import com.microsoft.tooling.msservices.serviceexplorer.azure.appservice.slot.DeploymentSlotModulePresenterBase
import com.microsoft.tooling.msservices.serviceexplorer.azure.appservice.slot.DeploymentSlotModuleView
import com.microsoft.tooling.msservices.serviceexplorer.azure.function.FunctionModule
import org.slf4j.LoggerFactory

class FunctionDeploymentSlotModule(
    parent: Node,
    subscriptionId: String,
    app: FunctionApp,
    isDeploymentSlotsSupported: Boolean
) :
    DeploymentSlotModuleBase<FunctionDeploymentSlot>(FunctionModule::class.java.name, parent, subscriptionId, app, isDeploymentSlotsSupported, logger),
    DeploymentSlotModuleView<FunctionDeploymentSlot> {

    companion object {
        private val logger = LoggerFactory.getLogger(FunctionDeploymentSlotModule::class.java)
    }

    private val myPresenter: DeploymentSlotModulePresenterBase<FunctionDeploymentSlot, DeploymentSlotModuleView<FunctionDeploymentSlot>>
        get() = FunctionDeploymentSlotModulePresenter()

    override val presenter: DeploymentSlotModulePresenterBase<FunctionDeploymentSlot, DeploymentSlotModuleView<FunctionDeploymentSlot>> =
        myPresenter

    init {
        presenter.onAttachView(this)
    }

    override fun renderDeploymentSlots(slots: List<FunctionDeploymentSlot>) {
        slots.forEach { slot: FunctionDeploymentSlot ->
            val slotNode = FunctionDeploymentSlotNode(
                appId = slot.parent().id(),
                appName = slot.parent().name(),
                parent = this,
                slotId = slot.id(),
                slotName = slot.name(),
                state = slot.state(),
                os = slot.operatingSystem().toString(),
                subscriptionId = subscriptionId,
                hostName = slot.defaultHostName()
            )
            addChildNode(slotNode)
        }
    }
}
