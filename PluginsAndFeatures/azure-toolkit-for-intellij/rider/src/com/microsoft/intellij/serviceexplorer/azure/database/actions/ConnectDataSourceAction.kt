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

import com.intellij.database.autoconfig.DataSourceDetector
import com.intellij.database.autoconfig.DataSourceRegistry
import com.intellij.openapi.project.Project
import com.microsoft.azuretools.authmanage.AuthMethodManager
import com.microsoft.azuretools.ijidea.actions.AzureSignInAction
import com.microsoft.intellij.AzurePlugin
import com.microsoft.tooling.msservices.serviceexplorer.Node
import com.microsoft.tooling.msservices.serviceexplorer.NodeActionEvent
import com.microsoft.tooling.msservices.serviceexplorer.NodeActionListener

abstract class ConnectDataSourceAction(protected val node: Node) : NodeActionListener() {

    public override fun actionPerformed(e: NodeActionEvent) {
        val project = node.project as? Project ?: return

        try {
            if (!AzureSignInAction.doSignIn(AuthMethodManager.getInstance(), project)) return

            val registry = DataSourceRegistry(project)

            val builder = populateConnectionBuilder(registry.builder)

            if (builder != null) {
                builder.commit()
                registry.showDialog()
            }
        } catch (ex: Throwable) {
            AzurePlugin.log("Error connecting to database", ex)
            throw RuntimeException("Error connecting to database", ex)
        }
    }

    abstract fun populateConnectionBuilder(builder: DataSourceDetector.Builder): DataSourceDetector.Builder?

    protected class AzureSqlConnectionStringBuilder {
        companion object {
            const val defaultDriverClass = "com.microsoft.sqlserver.jdbc.SQLServerDriver"

            fun build(host: String) : String {
                return "jdbc:sqlserver://$host:1433;encrypt=true;trustServerCertificate=false;hostNameInCertificate=*.database.windows.net;loginTimeout=30;"
            }

            fun build(host: String, database: String) : String {
                return "jdbc:sqlserver://$host:1433;encrypt=true;trustServerCertificate=false;hostNameInCertificate=*.database.windows.net;loginTimeout=30;database=$database;"
            }
        }
    }
}

