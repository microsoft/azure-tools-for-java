/**
 * Copyright (c) 2018-2020 JetBrains s.r.o.
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

package com.microsoft.intellij.serviceexplorer

import com.google.common.collect.ImmutableList
import com.microsoft.intellij.serviceexplorer.azure.appservice.StartStreamingLogsAction
import com.microsoft.intellij.serviceexplorer.azure.appservice.StopStreamingLogsAction
import com.microsoft.intellij.serviceexplorer.azure.database.actions.*
import com.microsoft.intellij.serviceexplorer.azure.functionapp.actions.FunctionAppCreateAction
import com.microsoft.intellij.serviceexplorer.azure.webapp.actions.WebAppCreateAction
import com.microsoft.tooling.msservices.serviceexplorer.Node
import com.microsoft.tooling.msservices.serviceexplorer.NodeActionListener
import com.microsoft.tooling.msservices.serviceexplorer.azure.appservice.functionapp.AzureFunctionAppModule
import com.microsoft.tooling.msservices.serviceexplorer.azure.appservice.functionapp.FunctionAppNode
import com.microsoft.tooling.msservices.serviceexplorer.azure.database.AzureDatabaseModule
import com.microsoft.tooling.msservices.serviceexplorer.azure.database.sqldatabase.SqlDatabaseNode
import com.microsoft.tooling.msservices.serviceexplorer.azure.database.sqlserver.SqlServerNode
import com.microsoft.tooling.msservices.serviceexplorer.azure.webapp.WebAppModule
import com.microsoft.tooling.msservices.serviceexplorer.azure.webapp.WebAppNode
import java.util.*

class RiderNodeActionsMap : NodeActionsMap() {

    override fun getMap(): Map<Class<out Node>, ImmutableList<Class<out NodeActionListener>>> {
        return node2Actions
    }

    companion object {
        private val node2Actions = HashMap<Class<out Node>, ImmutableList<Class<out NodeActionListener>>>()

        init {
            node2Actions[AzureDatabaseModule::class.java] = ImmutableList.Builder<Class<out NodeActionListener>>()
                    .add(SqlServerCreateAction::class.java)
                    .build()

            node2Actions[SqlServerNode::class.java] = ImmutableList.Builder<Class<out NodeActionListener>>()
                    .add(SqlDatabaseCreateAction::class.java)
                    .add(SqlServerOpenInBrowserAction::class.java)
                    .add(SqlServerAddCurrentIpAddressToFirewallAction::class.java)
                    .add(SqlServerConnectDataSourceAction::class.java)
                    .build()

            node2Actions[SqlDatabaseNode::class.java] = ImmutableList.Builder<Class<out NodeActionListener>>()
                    .add(SqlDatabaseOpenInBrowserAction::class.java)
                    .add(SqlDatabaseAddCurrentIpAddressToFirewallAction::class.java)
                    .add(SqlDatabaseConnectDataSourceAction::class.java)
                    .build()

            node2Actions[WebAppModule::class.java] = ImmutableList.Builder<Class<out NodeActionListener>>()
                    .add(WebAppCreateAction::class.java)
                    .build()

            node2Actions[AzureFunctionAppModule::class.java] = ImmutableList.Builder<Class<out NodeActionListener>>()
                    .add(FunctionAppCreateAction::class.java)
                    .build()

            node2Actions[WebAppNode::class.java] = ImmutableList.Builder<Class<out NodeActionListener>>()
                    .add(StartStreamingLogsAction::class.java)
                    .add(StopStreamingLogsAction::class.java)
                    .build()

            node2Actions[FunctionAppNode::class.java] = ImmutableList.Builder<Class<out NodeActionListener>>()
                    .add(StartStreamingLogsAction::class.java)
                    .add(StopStreamingLogsAction::class.java)
                    .build()
        }
    }
}
