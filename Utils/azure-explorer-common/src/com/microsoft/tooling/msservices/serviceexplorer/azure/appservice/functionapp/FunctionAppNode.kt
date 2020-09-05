/**
 * Copyright (c) 2019-2020 JetBrains s.r.o.
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

package com.microsoft.tooling.msservices.serviceexplorer.azure.appservice.functionapp

import com.microsoft.azuretools.core.mvp.model.functionapp.AzureFunctionAppMvpModel
import com.microsoft.tooling.msservices.components.DefaultLoader
import com.microsoft.tooling.msservices.serviceexplorer.NodeActionEvent
import com.microsoft.tooling.msservices.serviceexplorer.NodeActionListener
import com.microsoft.tooling.msservices.serviceexplorer.azure.AzureNodeActionPromptListener
import com.microsoft.tooling.msservices.serviceexplorer.azure.appservice.functionapp.functions.FunctionNode
import com.microsoft.tooling.msservices.serviceexplorer.azure.webapp.base.WebAppBaseNode
import com.microsoft.tooling.msservices.serviceexplorer.azure.webapp.base.WebAppBaseState
import java.util.logging.Logger

class FunctionAppNode(parent: AzureFunctionAppModule,
                      subscriptionId: String,
                      val functionAppId: String,
                      val functionAppName: String,
                      state: String,
                      hostName: String,
                      os: String)
    : WebAppBaseNode(functionAppId, functionAppName, FUNCTION_LABEL, parent, subscriptionId, hostName, os, state) {

    companion object {
        private const val FUNCTION_LABEL = "Function"

        private const val PROGRESS_MESSAGE_DELETE_FUNCTION_APP = "Deleting Function App '%s'..."

        private const val ICON_FUNCTION_APP_RUNNING = "FunctionAppRunning.svg"
        private const val ICON_FUNCTION_APP_STOPPED = "FunctionAppStopped.svg"

        private val deletingFunctionAppPromptMessage = StringBuilder()
                .appendln("This operation will delete Function App '%s'.")
                .append("Are you sure you want to continue?")
                .toString()
    }

    private val logger = Logger.getLogger(FunctionAppNode::class.java.name)

    private val presenter = FunctionAppNodePresenter<FunctionAppNode>()

    override fun getStartActionListener(): NodeActionListener =
            createBackgroundActionListener("Starting Function App") { startFunctionApp() }

    override fun getStopActionListener(): NodeActionListener =
            createBackgroundActionListener("Stopping Function App") { stopFunctionApp() }

    override fun getRestartActionListener(): NodeActionListener =
            createBackgroundActionListener("Restarting Function App") { restartFunctionApp() }

    override fun getShowPropertiesActionListener(): NodeActionListener = OpenProperties()

    override fun getOpenInBrowserActionListener(): NodeActionListener = OpenInBrowserAction()

    override fun getDeleteActionListener(): NodeActionListener = DeleteFunctionAppAction()

    init {
        loadActions()
        presenter.onAttachView(this@FunctionAppNode)
    }

    override fun getIcon(os: String?, label: String?, state: WebAppBaseState?): String =
            if (state == WebAppBaseState.STOPPED) ICON_FUNCTION_APP_STOPPED
            else ICON_FUNCTION_APP_RUNNING

    override fun refreshItems() {
        val functions = AzureFunctionAppMvpModel.listFunctionsForAppWithId(subscriptionId, functionAppId)

        for (function in functions) {
            val functionId = function.id()
            val functionName = function.name()
            val isEnabled = function.isEnabled()

            addChildNode(FunctionNode(this, subscriptionId, isEnabled, functionId, functionName))
        }
    }

    override fun renderNode(nodeState: WebAppBaseState) {
        when (nodeState) {
            WebAppBaseState.RUNNING -> {
                state = nodeState
                setIconPath(ICON_FUNCTION_APP_RUNNING)
            }
            WebAppBaseState.STOPPED -> {
                state = nodeState
                setIconPath(ICON_FUNCTION_APP_STOPPED)
            }
        }
    }

    private fun createBackgroundActionListener(actionName: String, action: () -> Unit) =
            object : NodeActionListener() {
                override fun actionPerformed(event: NodeActionEvent) {
                    DefaultLoader.getIdeHelper().runInBackground(null, actionName, false, true, "$actionName...", action)
                }
            }

    private fun stopFunctionApp() =
            try { presenter.onStopFunctionApp(subscriptionId, functionAppId) }
            catch (t: Throwable) { logger.warning("Error while stopping Function App with Id: $functionAppId: $t") }

    private fun startFunctionApp() =
            try { presenter.onStartFunctionApp(subscriptionId, functionAppId) }
            catch(t: Throwable) { logger.warning("Error while starting Function App with Id: $functionAppId: $t") }

    private fun restartFunctionApp() =
            try { presenter.onRestartFunctionApp(subscriptionId, functionAppId) }
            catch(t: Throwable) { logger.warning("Error while restarting Function App with Id: $functionAppId: $t") }

    private inner class DeleteFunctionAppAction : AzureNodeActionPromptListener(
            this@FunctionAppNode,
            String.format(deletingFunctionAppPromptMessage, functionAppName),
            String.format(PROGRESS_MESSAGE_DELETE_FUNCTION_APP, functionAppName)) {

        override fun azureNodeAction(event: NodeActionEvent?) {
            try {
                AzureFunctionAppMvpModel.deleteFunctionApp(subscriptionId, functionAppId)
                DefaultLoader.getIdeHelper().invokeLater {
                    getParent().removeNode(subscriptionId, functionAppId, this@FunctionAppNode)
                }
            } catch (t: Throwable) {
                DefaultLoader.getUIHelper().logError(t.message, t)
            }
        }

        override fun onSubscriptionsChanged(e: NodeActionEvent?) { }
    }

    private inner class OpenInBrowserAction : NodeActionListener() {
        override fun actionPerformed(e: NodeActionEvent?) {
            DefaultLoader.getUIHelper().openInBrowser("http://$hostName")
        }
    }

    private inner class OpenProperties : NodeActionListener() {
        override fun actionPerformed(e: NodeActionEvent?) {
            DefaultLoader.getUIHelper().openFunctionAppProperties(this@FunctionAppNode)
        }
    }
}
