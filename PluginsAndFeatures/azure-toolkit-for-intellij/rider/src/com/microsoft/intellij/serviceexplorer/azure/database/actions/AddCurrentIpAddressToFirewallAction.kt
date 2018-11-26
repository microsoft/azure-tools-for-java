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

import com.intellij.notification.NotificationType
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.progress.PerformInBackgroundOption
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.progress.Task
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.InputValidator
import com.intellij.openapi.ui.Messages
import com.intellij.openapi.util.IconLoader
import com.microsoft.azure.management.sql.SqlServer
import com.microsoft.azuretools.authmanage.AuthMethodManager
import com.microsoft.azuretools.ijidea.actions.AzureSignInAction
import com.microsoft.intellij.runner.db.AzureDatabaseMvpModel
import com.microsoft.tooling.msservices.serviceexplorer.Node
import com.microsoft.tooling.msservices.serviceexplorer.NodeActionEvent
import com.microsoft.tooling.msservices.serviceexplorer.NodeActionListener
import com.microsoft.tooling.msservices.serviceexplorer.azure.database.sqldatabase.SqlDatabaseNode
import com.microsoft.tooling.msservices.serviceexplorer.azure.database.sqlserver.SqlServerNode
import org.jetbrains.plugins.azure.cloudshell.AzureCloudShellNotifications
import org.jetbrains.plugins.azure.util.PublicIpAddressProvider
import sun.net.util.IPAddressUtil
import java.time.Clock
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

abstract class AddCurrentIpAddressToFirewallAction(private val node: Node) : NodeActionListener() {
    public override fun actionPerformed(e: NodeActionEvent) {
        val project = node.project as? Project ?: return

        if (!AzureSignInAction.doSignIn(AuthMethodManager.getInstance(), project)) return

        // Get server details
        val databaseServerNode = when (node) {
            is SqlDatabaseNode -> node.parent as SqlServerNode
            is SqlServerNode -> node
            else -> return
        }

        val sqlServer = AzureDatabaseMvpModel.getSqlServerById(databaseServerNode.subscriptionId, databaseServerNode.sqlServerId)

        // Fetch current IP address
        ProgressManager.getInstance().run(object : Task.Backgroundable(project, "Retrieving current public IP address...", true, PerformInBackgroundOption.DEAF) {
            override fun run(indicator: ProgressIndicator) {
                val publicIpAddressResult = PublicIpAddressProvider.retrieveCurrentPublicIpAddress()

                if (publicIpAddressResult != PublicIpAddressProvider.Result.none) {
                    ApplicationManager.getApplication().invokeLater {
                        requestAddFirewallRule(project, sqlServer, publicIpAddressResult)
                    }
                } else {
                    AzureCloudShellNotifications.notify(project,
                            "Could not retrieve current public IP address",
                            "An error occurred while retrieving current public IP address",
                            "Try adding your IP address from the Azure management portal.",
                            NotificationType.ERROR)
                }
            }
        })
    }

    private fun requestAddFirewallRule(project: Project, sqlServer: SqlServer, publicIpAddressResult: PublicIpAddressProvider.Result) {
        val ipAddressInput = Messages.showInputDialog(project,
                "Add firewall rule for IP address:",
                "Add firewall rule",
                IconLoader.getIcon("/icons/Database.svg"),
                publicIpAddressResult.ipv4address,
                IpAddressInputValidator.INSTANCE)

        if (ipAddressInput.isNullOrEmpty()) return

        val ipAddressForRule = ipAddressInput!!.trim()

        // Add IP address
        ProgressManager.getInstance().run(object : Task.Backgroundable(project, "Adding firewall rule for current public IP address...", true, PerformInBackgroundOption.DEAF) {
            override fun run(indicator: ProgressIndicator) {
                val currentFirewallRules = sqlServer.firewallRules().list()

                if (!currentFirewallRules.any {
                            it.startIPAddress() == ipAddressForRule
                                    || it.endIPAddress() == ipAddressForRule }) {

                    val formatter = DateTimeFormatter.ofPattern("yyyy_MM_dd_HH_mm_ss")
                    val now = LocalDateTime.now(Clock.systemUTC())

                    sqlServer.firewallRules()
                            .define("ClientIPAddress_${now.format(formatter)}")
                            .withIPAddressRange(ipAddressForRule, ipAddressForRule)
                            .create()
                }

                AzureCloudShellNotifications.notify(project,
                        "Added firewall rule",
                        "Firewall rule for your current public IP address has been added",
                        "You can now connect to your Sql Database from within the IDE.",
                        NotificationType.INFORMATION)
            }
        })
    }

    private class IpAddressInputValidator() : InputValidator {
        companion object {
            val INSTANCE = IpAddressInputValidator()
        }

        override fun checkInput(input: String?): Boolean {
            return !input.isNullOrEmpty() && IPAddressUtil.isIPv4LiteralAddress(input)
        }

        override fun canClose(input: String?): Boolean {
            return checkInput(input)
        }
    }
}