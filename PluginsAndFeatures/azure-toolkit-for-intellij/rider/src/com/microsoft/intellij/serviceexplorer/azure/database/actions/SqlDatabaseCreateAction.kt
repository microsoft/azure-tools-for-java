/**
 * Copyright (c) 2018-2020 JetBrains s.r.o.
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

package com.microsoft.intellij.serviceexplorer.azure.database.actions

import com.intellij.openapi.project.Project
import com.intellij.openapi.rd.defineNestedLifetime
import com.microsoft.azuretools.core.mvp.model.database.AzureSqlServerMvpModel
import com.microsoft.intellij.ui.forms.sqldatabase.CreateSqlDatabaseOnServerDialog
import com.microsoft.tooling.msservices.helpers.Name
import com.microsoft.tooling.msservices.serviceexplorer.NodeActionEvent
import com.microsoft.tooling.msservices.serviceexplorer.NodeActionListener
import com.microsoft.tooling.msservices.serviceexplorer.azure.database.sqlserver.SqlServerNode

@Name("New SQL Database")
class SqlDatabaseCreateAction(private val sqlServerNode: SqlServerNode) : NodeActionListener() {

    override fun actionPerformed(event: NodeActionEvent?) {
        event ?: return

        val project = sqlServerNode.project as? Project ?: return

        // TODO: This need to be fixed! We must not block UI thread when making a curl request
        //  Need to be executed on a background thread or any other way (move inside dialog for example)
        val sqlServer = AzureSqlServerMvpModel.getSqlServerById(sqlServerNode.subscriptionId, sqlServerNode.sqlServerId)
        val createSqlDatabaseForm =
                CreateSqlDatabaseOnServerDialog(project.defineNestedLifetime(), project, sqlServer, Runnable { sqlServerNode.load(true) })
        createSqlDatabaseForm.show()
    }

    override fun getIconPath(): String = "AddEntity.svg"
}