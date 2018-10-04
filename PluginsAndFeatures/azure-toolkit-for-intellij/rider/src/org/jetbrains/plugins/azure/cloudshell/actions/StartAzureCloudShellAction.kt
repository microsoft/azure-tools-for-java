package org.jetbrains.plugins.azure.cloudshell.actions

import com.intellij.ide.BrowserUtil
import com.intellij.notification.Notification
import com.intellij.notification.NotificationListener
import com.intellij.notification.NotificationType
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.progress.PerformInBackgroundOption
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.progress.Task
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.popup.JBPopupFactory
import com.intellij.openapi.ui.popup.PopupStep
import com.intellij.openapi.ui.popup.util.BaseListPopupStep
import com.microsoft.aad.adal4j.AuthenticationException
import com.microsoft.azure.AzureEnvironment
import com.microsoft.azuretools.authmanage.AdAuthManager
import com.microsoft.azuretools.authmanage.AuthMethodManager
import com.microsoft.azuretools.authmanage.RefreshableTokenCredentials
import com.microsoft.azuretools.authmanage.models.SubscriptionDetail
import com.microsoft.azuretools.sdkmanage.AzureManager
import com.microsoft.rest.credentials.ServiceClientCredentials
import org.jetbrains.plugins.azure.cloudshell.AzureCloudShellNotifications
import org.jetbrains.plugins.azure.cloudshell.AzureCloudTerminalProcess
import org.jetbrains.plugins.azure.cloudshell.rest.*
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

        object : Task.Backgroundable(project, "Retrieving Azure subscription details...", true, PerformInBackgroundOption.DEAF)
        {
            override fun run(indicator: ProgressIndicator)
            {
                val selectedSubscriptions = azureManager.subscriptionManager.subscriptionDetails
                        .asSequence()
                        .filter { it.isSelected }
                        .toList()

                // One option? No popup needed
                if (selectedSubscriptions.count() == 1) {
                    ApplicationManager.getApplication().invokeLater {
                        if (project.isDisposed) return@invokeLater

                        startAzureCloudShell(project, azureManager, selectedSubscriptions.first())
                    }
                    return
                }

                // Multiple options? Popup.
                ApplicationManager.getApplication().invokeLater{
                    if (project.isDisposed) return@invokeLater

                    val step = object : BaseListPopupStep<SubscriptionDetail>("Select subscription to run Azure Cloud Shell", selectedSubscriptions) {
                        override fun getTextFor(value: SubscriptionDetail?): String {
                            if (value != null) {
                                return "${value.subscriptionName} (${value.subscriptionId})"
                            }

                            return super.getTextFor(value)
                        }

                        override fun onChosen(selectedValue: SubscriptionDetail, finalChoice: Boolean): PopupStep<*>? {
                            doFinalStep {
                                startAzureCloudShell(project, azureManager, selectedValue)
                            }
                            return PopupStep.FINAL_CHOICE
                        }
                    }

                    val popup = JBPopupFactory.getInstance().createListPopup(step)
                    popup.showCenteredInCurrentWindow(project)
                }
            }
        }.queue()
    }

    private fun startAzureCloudShell(project: Project, azureManager: AzureManager, subscriptionDetail: SubscriptionDetail) {
        try {
            val authManager = AdAuthManager.getInstance()
            val tokenCredentials = RefreshableTokenCredentials(authManager, subscriptionDetail.tenantId)

            provisionAzureCloudShell(project, azureManager, tokenCredentials, subscriptionDetail)
        } catch (e: AuthenticationException) {
            // Failed to authenticate....
            AzureCloudShellNotifications.notify(project,
                    "Azure",
                    "Failed to authenticate Azure Cloud Shell",
                    "Authentication was unsuccessful.",
                    NotificationType.WARNING,
                    null)
        }
    }

    private fun provisionAzureCloudShell(project: Project, azureManager: AzureManager, tokenCredentials: ServiceClientCredentials, subscriptionDetail: SubscriptionDetail) {
        ProgressManager.getInstance().run(object : Task.Backgroundable(project, this.templatePresentation.text) {
            override fun run(indicator: ProgressIndicator) {
                indicator.fraction = 0.05
                indicator.text = "Retrieving cloud shell preferences..."

                // Use custom API's
                val retrofitClient = azureManager.getRetrofitClient(
                        azureManager.environment.azureEnvironment,
                        AzureEnvironment.Endpoint.RESOURCE_MANAGER,
                        CloudConsoleService::class.java,
                        tokenCredentials)

                val consoleUserSettingsResponse = retrofitClient.userSettings().execute()
                if (consoleUserSettingsResponse.isSuccessful) {
                    indicator.fraction = 0.35
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
                        val credentials = RefreshableTokenCredentials(AdAuthManager.getInstance(), subscriptionDetail.tenantId)
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
                                    AzureCloudTerminalProcess(socketClient))

                            runner.run()

                            return
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