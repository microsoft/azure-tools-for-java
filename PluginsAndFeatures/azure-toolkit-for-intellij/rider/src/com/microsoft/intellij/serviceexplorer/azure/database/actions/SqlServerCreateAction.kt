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
package com.microsoft.intellij.serviceexplorer.azure.database.actions

import com.intellij.openapi.project.Project
import com.jetbrains.rdclient.util.idea.defineNestedLifetime
import com.microsoft.azuretools.authmanage.AuthMethodManager
import com.microsoft.azuretools.ijidea.actions.AzureSignInAction
import com.microsoft.intellij.ui.forms.sqlserver.CreateSqlServerDialog
import com.microsoft.tooling.msservices.helpers.Name
import com.microsoft.tooling.msservices.serviceexplorer.NodeActionEvent
import com.microsoft.tooling.msservices.serviceexplorer.NodeActionListener
import com.microsoft.tooling.msservices.serviceexplorer.azure.database.AzureDatabaseModule

@Name("New SQL Server")
class SqlServerCreateAction(private val databasesModule: AzureDatabaseModule) : NodeActionListener() {

    override fun actionPerformed(event: NodeActionEvent?) {
        val project = databasesModule.project as? Project ?: return
        if (!AzureSignInAction.doSignIn(AuthMethodManager.getInstance(), project)) return

        val createSqlServerForm =
                CreateSqlServerDialog(project.defineNestedLifetime(), project, Runnable { databasesModule.load(true) })

        createSqlServerForm.show()
    }
}