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

package com.microsoft.tooling.msservices.serviceexplorer.azure.appservice

import com.microsoft.tooling.msservices.serviceexplorer.Node
import com.microsoft.tooling.msservices.serviceexplorer.RefreshableNode
import com.microsoft.tooling.msservices.serviceexplorer.azure.function.FunctionModule
import com.microsoft.tooling.msservices.serviceexplorer.azure.webapp.WebAppModule

class AzureAppServiceModule(parent: Node) : RefreshableNode(MODULE_ID, BASE_MODULE_NAME, parent, ICON_PATH) {

    companion object {
        private val MODULE_ID = AzureAppServiceModule::class.java.name
        private const val ICON_PATH = "AppService.svg"
        private const val BASE_MODULE_NAME = "App Service"
    }

    private val presenter = AzureAppServiceModulePresenter()

    private val functionAppModule = FunctionModule(this)
    private val webAppModule = WebAppModule(this)

    init {
        presenter.onAttachView(this@AzureAppServiceModule)

        addChildNode(functionAppModule)
        addChildNode(webAppModule)
    }

    override fun refreshItems() {
        addChildNode(functionAppModule)
        addChildNode(webAppModule)

        presenter.onModuleRefresh()
    }
}
