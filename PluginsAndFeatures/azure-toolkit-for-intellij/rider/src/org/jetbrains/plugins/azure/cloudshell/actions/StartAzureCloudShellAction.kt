package org.jetbrains.plugins.azure.cloudshell.actions

import com.intellij.ide.BrowserUtil
import com.intellij.notification.Notification
import com.intellij.notification.NotificationListener
import com.intellij.notification.NotificationType
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.progress.Task
import com.microsoft.azure.AzureEnvironment
import com.microsoft.azuretools.authmanage.AdAuthManager
import com.microsoft.azuretools.authmanage.AuthMethodManager
import com.microsoft.azuretools.authmanage.RefreshableTokenCredentials
import org.jetbrains.plugins.azure.cloudshell.AzureCloudShellDeviceAuthenticationContext
import org.jetbrains.plugins.azure.cloudshell.AzureCloudShellNotifications
import org.jetbrains.plugins.azure.cloudshell.rest.*
import org.jetbrains.plugins.terminal.cloud.CloudTerminalProcess
import org.jetbrains.plugins.terminal.cloud.CloudTerminalRunner
import java.net.URI
import javax.swing.event.HyperlinkEvent

class StartAzureCloudShellAction : AnAction() {
    override fun update(e: AnActionEvent?) {
        if (e == null) return

        e.presentation.isEnabled = CommonDataKeys.PROJECT.getData(e.dataContext) != null
                && AuthMethodManager.getInstance().azureManager != null
    }

    override fun actionPerformed(e: AnActionEvent) {
        val project = CommonDataKeys.PROJECT.getData(e.dataContext) ?: return
        val azureManager = AuthMethodManager.getInstance().azureManager ?: return

//        ProgressManager.getInstance().run(object : Task.Backgroundable(project, this.templatePresentation.text) {
//            override fun run(indicator: ProgressIndicator) {
//                indicator.fraction = 0.05
//                indicator.text = "Retrieving Azure subscription details..."
//
//                val selectedSubscriptions = azureManager.subscriptionManager.subscriptionDetails
//                        .asSequence()
//                        .filter { it.isSelected }
//                        .toList()
//
//                indicator.fraction = 1.0
//
//                val step = object : BaseListPopupStep<SubscriptionDetail>("Select subscription to run Azure Cloud Shell", selectedSubscriptions) {
//                    override fun getTextFor(value: SubscriptionDetail?): String {
//                        if (value != null) {
//                            return "${value.subscriptionName} (${value.subscriptionId})"
//                        }
//
//                        return super.getTextFor(value)
//                    }
//
//                    override fun onChosen(selectedValue: SubscriptionDetail, finalChoice: Boolean): PopupStep<*>? {
//                        // TODO
//                        return PopupStep.FINAL_CHOICE
//                    }
//                }
//
//                val popup = JBPopupFactory.getInstance().createListPopup(step)
//                val event = e.inputEvent
//                val c = event?.component
//                if (c != null) {
//                    popup.showUnderneathOf(c)
//                } else {
//                    popup.showInBestPositionFor(e.dataContext)
//                }
//            }
//        })
//return

        ProgressManager.getInstance().run(object : Task.Backgroundable(project, this.templatePresentation.text) {
            override fun run(indicator: ProgressIndicator) {
                indicator.fraction = 0.05
                indicator.text = "Retrieving Azure subscription details..."

                val selectedSubscriptions = azureManager.subscriptionManager.subscriptionDetails
                        .asSequence()
                        .filter { it.isSelected }
                        .toList()

                indicator.fraction = 0.25
                indicator.text = "Retrieving cloud shell preferences..."
                selectedSubscriptions.forEach {

                    val authenticationContext = AzureCloudShellDeviceAuthenticationContext.create(azureManager, it.tenantId)
                    val deviceCode = authenticationContext.acquireDeviceCode()

                    // TODO: prompt

                    val authenticationToken = authenticationContext.acquireTokenByDeviceCode(deviceCode)

                    val tokenCredentials = authenticationContext.tokenCredentialsFor(authenticationToken)

                    // Use custom API's
                    val retrofitClient = azureManager.getRetrofitClient(
                            azureManager.environment.azureEnvironment,
                            AzureEnvironment.Endpoint.RESOURCE_MANAGER,
                            CloudConsoleService::class.java,
                            tokenCredentials)

                    val consoleUserSettingsResponse = retrofitClient.userSettings().execute()
                    if (consoleUserSettingsResponse.isSuccessful) {
                        indicator.fraction = 0.45
                        indicator.text = "Provisioning cloud shell..."

                        // We can provision a shell!
                        val provisionParameters = CloudConsoleProvisionParameters(
                                CloudConsoleProvisionParameters.Properties(consoleUserSettingsResponse.body()!!.properties!!.preferredOsType!!))

                        val provisionResult = retrofitClient.provision(provisionParameters).execute()
                        if (provisionResult.isSuccessful && provisionResult.body()!!.properties!!.provisioningState.equals("Succeeded", true)) {
                            indicator.fraction = 0.65
                            indicator.text = "Requesting cloud shell..."

                            // Cloud Shell URL
                            val shellUrl = provisionResult.body()!!.properties!!.uri!! + "/terminals"

                            // Fetch graph and key vault tokens
                            val credentials = RefreshableTokenCredentials(AdAuthManager.getInstance(), it.tenantId)
                            val graphToken = credentials.getToken(azureManager.environment.azureEnvironment.graphEndpoint())
                            val vaultToken = credentials.getToken("https://" + azureManager.environment.azureEnvironment.keyVaultDnsSuffix().trimStart('.') + "/")

                            // Provision terminal
                            indicator.fraction = 0.75
                            indicator.text = "Requesting cloud shell terminal..."

                            val provisionTerminalParameters = CloudConsoleProvisionTerminalParameters()
                            provisionTerminalParameters.tokens.add(graphToken)
                            provisionTerminalParameters.tokens.add(vaultToken)

                            val provisionTerminalResult = retrofitClient.provisionTerminal(shellUrl, 120, 30, provisionTerminalParameters).execute()
                            if (provisionTerminalResult.isSuccessful && provisionTerminalResult.body()!!.socketUri!!.isNotEmpty()) {
                                // Let's connect!
                                indicator.fraction = 0.85
                                indicator.text = "Connecting to cloud shell terminal..."

                                val socketUri = provisionTerminalResult.body()!!.socketUri!!

                                // Setup connection

                                val socketClient = CloudConsoleTerminalWebSocket(URI(socketUri))
                                socketClient.connectBlocking()

                                val runner = CloudTerminalRunner(project, "Azure Cloud Shell",
                                        CloudTerminalProcess( socketClient.outputStream, socketClient.inputStream))

                                runner.run()

//                                ws.connect()
//
//                                val runner = CloudTerminalRunner(project, "Azure Cloud Shell",
//                                        CloudTerminalProcess( ws.socket!!.outputStream, ws.socket!!.inputStream))
//
//                                runner.run()
//
//                                val terminal = CloudTerminalProvider.getInstance().createTerminal(
//                                        "Azure Cloud Shell",
//                                        project,
//                                        ws.socket!!.inputStream,
//                                        ws.socket!!.outputStream)
//
//                                terminal.attachChild(ws)
//
//                                terminal.
//                                //CloudTerminalRunner
//                                val window = ToolWindowManager.getInstance(project).getToolWindow(TerminalToolWindowFactory.TOOL_WINDOW_ID)
//                                if (window != null && window.isAvailable) {
//                                    TerminalView.getInstance(project).initTerminal()
//                                    window.activate(null)
//                                }
//                                CloudTerminalRunner()
//
//                                val window = ToolWindowManager.getInstance(project).getToolWindow("Terminal")
//                                if (window != null && window.isAvailable) {
//                                    TerminalView.getInstance(project).setFileToOpen(selectedFile)
//                                    window.activate(null)
//                                }

                                return
                            }
                        }
                    }
                }

                // No cloud shell configuration found...
                AzureCloudShellNotifications.notify(project,
                        "Azure",
                        "Failed to start Azure Cloud Shell",
                        "Azure Cloud Shell is not configured in any of your subscriptions. <a href='shellportal'>Configure Azure Cloud Shell</a>",
                        NotificationType.WARNING,
                        object : NotificationListener.Adapter() {
                            override fun hyperlinkActivated(notification: Notification, e: HyperlinkEvent) {
                                if (!project.isDisposed) {
                                    when (e.description) {
                                        "shellportal" -> BrowserUtil.browse("https://shell.azure.com")
                                    }
                                }
                            }
                        })
            }
        })
    }
}