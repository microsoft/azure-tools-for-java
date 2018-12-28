/**
 * Copyright (c) 2018 JetBrains s.r.o.
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

package com.microsoft.tooling.msservices.serviceexplorer.azure.database

import com.microsoft.azuretools.azurecommons.helpers.AzureCmdException
import com.microsoft.tooling.msservices.serviceexplorer.AzureRefreshableNode
import com.microsoft.tooling.msservices.serviceexplorer.Node

class AzureDatabaseModule(parent: Node) : AzureRefreshableNode(MODULE_ID, BASE_MODULE_NAME, parent, ICON_PATH) {

    companion object {
        private val MODULE_ID = AzureDatabaseModule::class.java.name
        private const val ICON_PATH = "Database.svg"
        private const val BASE_MODULE_NAME = "SQL Databases"
        private const val ERROR_DELETE_SQL_SERVER = "An error occurred while attempting to delete the SQL Server: %s"
    }

    private val presenter = AzureDatabaseModulePresenter()

    init {
        presenter.onAttachView(this@AzureDatabaseModule)
    }

    @Throws(AzureCmdException::class)
    override fun refreshItems() {
        presenter.onModuleRefresh()
    }

    override fun removeNode(subscriptionId: String, sqlServerId: String, node: Node) {
        try {
            removeDirectChildNode(node)
        } catch (e: Throwable) {
            throw RuntimeException(String.format(ERROR_DELETE_SQL_SERVER, e))
        }
    }

    override fun onError(message: String) {}

    override fun onErrorWithException(message: String, ex: Exception) {}
}
