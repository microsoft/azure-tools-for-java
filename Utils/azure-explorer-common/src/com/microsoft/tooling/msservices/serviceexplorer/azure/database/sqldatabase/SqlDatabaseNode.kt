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

package com.microsoft.tooling.msservices.serviceexplorer.azure.database.sqldatabase

import com.microsoft.azuretools.core.mvp.model.database.AzureSqlDatabaseMvpModel
import com.microsoft.tooling.msservices.components.DefaultLoader
import com.microsoft.tooling.msservices.serviceexplorer.Node
import com.microsoft.tooling.msservices.serviceexplorer.NodeActionEvent
import com.microsoft.tooling.msservices.serviceexplorer.RefreshableNode
import com.microsoft.tooling.msservices.serviceexplorer.azure.AzureNodeActionPromptListener
import com.microsoft.tooling.msservices.serviceexplorer.azure.database.sqlserver.SqlServerNode

class SqlDatabaseNode(parent: SqlServerNode,
                      override val subscriptionId: String,
                      override val sqlDatabaseId: String,
                      override val sqlDatabaseName: String,
                      override var runState: String)
    : Node(sqlDatabaseId, sqlDatabaseName, parent, SQL_DATABASE_ICON, true),
        SqlDatabaseVirtualInterface {

    companion object {
        private const val ACTION_DELETE = "Delete"
        private const val SQL_DATABASE_ICON = "Database.svg"
        private const val DELETE_SQL_DATABASE_PROMPT_MESSAGE = "This operation will delete SQL Database %s.\n" + "Are you sure you want to continue?"
        private const val DELETE_SQL_DATABASE_PROGRESS_MESSAGE = "Deleting SQL Database '%s'"

        private const val ICON_ACTION_DELETE = "Discard.svg"
    }

    init {
        loadActions()
    }

    override fun loadActions() {
        addAction(ACTION_DELETE, ICON_ACTION_DELETE, DeleteSqlDatabaseAction())
        super.loadActions()
    }

    private inner class DeleteSqlDatabaseAction internal constructor()
        : AzureNodeActionPromptListener(
            this@SqlDatabaseNode,
            String.format(DELETE_SQL_DATABASE_PROMPT_MESSAGE, sqlDatabaseName),
            String.format(DELETE_SQL_DATABASE_PROGRESS_MESSAGE, sqlDatabaseName)) {

        override fun azureNodeAction(event: NodeActionEvent?) {
            try {
                AzureSqlDatabaseMvpModel.deleteDatabase(subscriptionId, sqlDatabaseId)

                DefaultLoader.getIdeHelper().invokeLater {
                    val parent = getParent() as RefreshableNode
                    parent.removeAllChildNodes()
                    parent.load(false)
                }
            } catch (e: Throwable) {
                DefaultLoader.getUIHelper().logError(e.message, e)
            }
        }

        override fun onSubscriptionsChanged(e: NodeActionEvent) {
        }
    }
}
