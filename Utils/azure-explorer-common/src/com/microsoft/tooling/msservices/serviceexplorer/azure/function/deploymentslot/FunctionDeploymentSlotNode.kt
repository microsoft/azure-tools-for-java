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

import com.microsoft.azure.management.appservice.FunctionDeploymentSlot
import com.microsoft.tooling.msservices.components.DefaultLoader
import com.microsoft.tooling.msservices.serviceexplorer.azure.appservice.slot.DeploymentSlotModuleBase
import com.microsoft.tooling.msservices.serviceexplorer.azure.appservice.slot.DeploymentSlotNodeBase
import com.microsoft.tooling.msservices.serviceexplorer.azure.appservice.slot.DeploymentSlotNodePresenterBase
import com.microsoft.tooling.msservices.serviceexplorer.azure.appservice.slot.DeploymentSlotNodeView

class FunctionDeploymentSlotNode(
    appId: String,
    appName: String,
    parent: DeploymentSlotModuleBase<FunctionDeploymentSlot>,
    slotId: String,
    slotName: String,
    state: String,
    os: String,
    subscriptionId: String,
    hostName: String
) :
    DeploymentSlotNodeBase<FunctionDeploymentSlot>(appId, appName, parent, slotId, slotName, state, os, subscriptionId, hostName),
    DeploymentSlotNodeView {

    private val myPresenter: DeploymentSlotNodePresenterBase<FunctionDeploymentSlot, DeploymentSlotNodeView> =
        FunctionDeploymentSlotNodePresenter()

    override val presenter: DeploymentSlotNodePresenterBase<FunctionDeploymentSlot, DeploymentSlotNodeView>
        get() = myPresenter

    init {
        presenter.onAttachView(this)
    }

    override fun openDeploymentSlotPropertyAction() {
        DefaultLoader.getUIHelper().openDeploymentSlotPropertyView(this)
    }
}
