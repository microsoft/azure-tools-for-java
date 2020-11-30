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
import com.microsoft.azuretools.core.mvp.ui.base.MvpPresenter
import com.microsoft.tooling.msservices.serviceexplorer.azure.webapp.base.WebAppBaseState
import java.io.IOException

abstract class DeploymentSlotNodePresenterBase<TSlot : DeploymentSlotBase<TSlot>, TView : DeploymentSlotNodeView> : MvpPresenter<TView>() {

    @Throws(IOException::class)
    abstract fun getStartDeploymentSlotAction(subscriptionId: String, appId: String, slotName: String)

    fun onStartDeploymentSlot(subscriptionId: String, appId: String, slotName: String) {
        getStartDeploymentSlotAction(subscriptionId, appId, slotName)
        val view = mvpView ?: return
        if (!isViewDetached) {
            view.renderNode(WebAppBaseState.RUNNING)
        }
    }

    @Throws(IOException::class)
    abstract fun getStopDeploymentSlotAction(subscriptionId: String, appId: String, slotName: String)

    fun onStopDeploymentSlot(subscriptionId: String, appId: String, slotName: String) {
        getStopDeploymentSlotAction(subscriptionId, appId, slotName)
        val view = mvpView ?: return
        if (!isViewDetached) {
            view.renderNode(WebAppBaseState.STOPPED)
        }
    }

    @Throws(IOException::class)
    abstract fun getRestartDeploymentSlotAction(subscriptionId: String, appId: String, slotName: String)

    fun onRestartDeploymentSlot(subscriptionId: String, appId: String, slotName: String) {
        getRestartDeploymentSlotAction(subscriptionId, appId, slotName)
        val view = mvpView ?: return
        if (!isViewDetached) {
            view.renderNode(WebAppBaseState.RUNNING)
        }
    }

    @Throws(Exception::class)
    abstract fun getDeploymentSlotAction(subscriptionId: String, appId: String, slotName: String): TSlot

    fun onRefreshNode(subscriptionId: String, appId: String, slotName: String) {
        val view = mvpView ?: return
        if (!isViewDetached) {
            val slot = getDeploymentSlotAction(subscriptionId, appId, slotName)
            view.renderNode(WebAppBaseState.fromString(slot.state()))
        }
    }

    @Throws(IOException::class)
    abstract fun onSwapWithProduction(subscriptionId: String, appId: String, slotName: String)
}
