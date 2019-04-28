/**
 * Copyright (c) 2019 JetBrains s.r.o.
 * <p/>
 * All rights reserved.
 * <p/>
 * MIT License
 * <p/>
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated
 * documentation files (the "Software"), to deal in the Software without restriction, including without limitation
 * the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and
 * to permit persons to whom the Software is furnished to do so, subject to the following conditions:
 * <p/>
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of
 * the Software.
 * <p/>
 * THE SOFTWARE IS PROVIDED *AS IS*, WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO
 * THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT,
 * TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package com.microsoft.tooling.msservices.serviceexplorer.azure.appservice.functionapp

import com.microsoft.azuretools.utils.AzureUIRefreshCore
import com.microsoft.azuretools.utils.AzureUIRefreshEvent
import com.microsoft.azuretools.utils.AzureUIRefreshListener
import com.microsoft.tooling.msservices.serviceexplorer.Node
import com.microsoft.tooling.msservices.serviceexplorer.RefreshableNode

class AzureFunctionAppModule(parent: Node) : RefreshableNode(MODULE_ID, BASE_MODULE_NAME, parent, ICON_PATH) {

    companion object {

        const val LISTENER_ID = "FunctionAppModule"

        private val MODULE_ID = AzureFunctionAppModule::class.java.name
        private const val ICON_PATH = "FunctionApp.svg"
        private const val BASE_MODULE_NAME = "Function Apps"
        private const val ERROR_DELETE_AZURE_FUNCTION = "An error occurred while attempting to delete the Azure Function: %s"
    }

    private val presenter = AzureFunctionAppModulePresenter()

    init {
        presenter.onAttachView(this@AzureFunctionAppModule)
        createListener()
    }

    override fun refreshItems() {
        presenter.onModuleRefresh()
    }

    override fun removeNode(subscriptionId: String, sqlServerId: String, node: Node) {
        try {
            removeDirectChildNode(node)
        } catch (t: Throwable) {
            throw RuntimeException(String.format(ERROR_DELETE_AZURE_FUNCTION, t))
        }
    }

    override fun onError(message: String) {}

    override fun onErrorWithException(message: String, ex: Exception) {}

    private fun createListener() {
        val listener = object : AzureUIRefreshListener() {
            override fun run() {
                if (event.opsType == AzureUIRefreshEvent.EventType.SIGNIN ||
                        event.opsType == AzureUIRefreshEvent.EventType.SIGNOUT) {
                    removeAllChildNodes()
                    return
                }

                if (event.`object` == null && event.opsType == AzureUIRefreshEvent.EventType.REFRESH) {
                    load(true)
                    return
                }

                if (event.`object` == null &&
                        (event.opsType == AzureUIRefreshEvent.EventType.UPDATE || event.opsType == AzureUIRefreshEvent.EventType.REMOVE)) {
                    if (hasChildNodes()) {
                        load(true)
                    }
                    return
                }
            }
        }
        AzureUIRefreshCore.addListener(LISTENER_ID, listener)
    }
}