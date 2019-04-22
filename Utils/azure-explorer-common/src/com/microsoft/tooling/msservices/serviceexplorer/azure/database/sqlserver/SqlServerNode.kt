/**
 * Copyright (c) 2018-2019 JetBrains s.r.o.
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

package com.microsoft.tooling.msservices.serviceexplorer.azure.database.sqlserver

import com.microsoft.azuretools.azurecommons.helpers.AzureCmdException
import com.microsoft.azuretools.core.mvp.model.database.AzureSqlDatabaseMvpModel
import com.microsoft.azuretools.core.mvp.model.database.AzureSqlServerMvpModel
import com.microsoft.tooling.msservices.components.DefaultLoader
import com.microsoft.tooling.msservices.serviceexplorer.NodeActionEvent
import com.microsoft.tooling.msservices.serviceexplorer.RefreshableNode
import com.microsoft.tooling.msservices.serviceexplorer.azure.AzureNodeActionPromptListener
import com.microsoft.tooling.msservices.serviceexplorer.azure.database.AzureDatabaseModule
import com.microsoft.tooling.msservices.serviceexplorer.azure.database.sqldatabase.SqlDatabaseNode

class SqlServerNode(parent: AzureDatabaseModule,
                    override val subscriptionId: String,
                    override val sqlServerId: String,
                    override val sqlServerName: String,
                    override var state: String)
    : RefreshableNode(sqlServerId, sqlServerName, parent, SQL_SERVER_ICON, true), SqlServerVirtualInterface {

    companion object {
        private const val SQL_SERVER_ICON = "SqlServer.svg"
        private const val SQL_DATABASE_MASTER = "master"

        private const val ACTION_DELETE = "Delete"
        private const val PROGRESS_MESSAGE_DELETE_SQL_SERVER = "Deleting SQL Server '%s'"
        private const val PROMPT_MESSAGE_DELETE_SQL_SERVER =
                "This operation will delete SQL Server '%s'.\nAre you sure you want to continue?"

        private const val ICON_ACTION_DELETE = "Discard.svg"
    }

    init {
        loadActions()
    }

    override fun onError(message: String) {
    }

    override fun onErrorWithException(message: String, ex: Exception) {
    }

    override fun loadActions() {
        addAction(ACTION_DELETE, ICON_ACTION_DELETE, DeleteSqlServerAction())
        super.loadActions()
    }

    @Throws(AzureCmdException::class)
    override fun refreshItems() {
        val sqlDatabasesList = AzureSqlDatabaseMvpModel.listSqlDatabasesByServerId(this.subscriptionId, this.sqlServerId)

        for (sqlDatabase in sqlDatabasesList) {
            if (sqlDatabase.name() == SQL_DATABASE_MASTER) continue

            addChildNode(SqlDatabaseNode(
                    this,
                    subscriptionId,
                    sqlDatabase.id(),
                    sqlDatabase.name(),
                    sqlDatabase.status()))
        }
    }

    private inner class DeleteSqlServerAction internal constructor()
        : AzureNodeActionPromptListener(
            this@SqlServerNode,
            String.format(PROMPT_MESSAGE_DELETE_SQL_SERVER, sqlServerName),
            String.format(PROGRESS_MESSAGE_DELETE_SQL_SERVER, sqlServerName)) {

        override fun azureNodeAction(event: NodeActionEvent?) {
            try {
                AzureSqlServerMvpModel.deleteSqlServer(subscriptionId, sqlServerId)
                DefaultLoader.getIdeHelper().invokeLater {
                    getParent().removeNode(subscriptionId, sqlServerId, this@SqlServerNode)
                }
            } catch (e: Throwable) {
                DefaultLoader.getUIHelper().logError(e.message, e)
            }
        }

        override fun onSubscriptionsChanged(e: NodeActionEvent?) {
        }
    }
}
