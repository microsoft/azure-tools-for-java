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
package org.jetbrains.plugins.azure.cloudshell.actions

import com.intellij.ide.BrowserUtil
import com.intellij.notification.Notification
import com.intellij.notification.NotificationListener
import com.intellij.notification.NotificationType
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.progress.PerformInBackgroundOption
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.progress.Task
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.popup.JBPopupFactory
import com.intellij.openapi.ui.popup.PopupStep
import com.intellij.openapi.ui.popup.util.BaseListPopupStep
import com.intellij.openapi.wm.ToolWindowManager
import com.microsoft.aad.adal4j.AuthenticationException
import com.microsoft.azure.AzureEnvironment
import com.microsoft.azuretools.authmanage.AdAuthManager
import com.microsoft.azuretools.authmanage.AuthMethodManager
import com.microsoft.azuretools.authmanage.RefreshableTokenCredentials
import com.microsoft.azuretools.authmanage.models.SubscriptionDetail
import com.microsoft.azuretools.sdkmanage.AzureManager
import com.microsoft.rest.credentials.ServiceClientCredentials
import org.jetbrains.plugins.azure.cloudshell.AzureCloudShellNotifications
import org.jetbrains.plugins.azure.cloudshell.rest.CloudConsoleProvisionParameters
import org.jetbrains.plugins.azure.cloudshell.rest.CloudConsoleProvisionTerminalParameters
import org.jetbrains.plugins.azure.cloudshell.rest.CloudConsoleService
import org.jetbrains.plugins.azure.cloudshell.rest.getRetrofitClient
import org.jetbrains.plugins.azure.cloudshell.terminal.AzureCloudTerminalFactory
import org.jetbrains.plugins.terminal.TerminalToolWindowFactory
import org.jetbrains.plugins.terminal.TerminalView
import java.net.URI
import javax.swing.event.HyperlinkEvent

class StartAzureCloudShellAction : AnAction() {
    private val logger = Logger.getInstance(StartAzureCloudShellAction::class.java)

    private val defaultTerminalColumns = 100
    private val defaultTerminalRows = 30

    override fun update(e: AnActionEvent) {
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
                logger.debug("Retrieving Azure subscription details...")

                val selectedSubscriptions = azureManager.subscriptionManager.subscriptionDetails
                        .asSequence()
                        .filter { it.isSelected }
                        .toList()

                logger.debug("Retrieved ${selectedSubscriptions.count()} Azure subscriptions")

                // One option? No popup needed
                if (selectedSubscriptions.count() == 1) {
                    ApplicationManager.getApplication().invokeLater {
                        if (project.isDisposed) return@invokeLater

                        startAzureCloudShell(project, azureManager, selectedSubscriptions.first())
                    }
                    return
                }

                // Multiple options? Popup.
                ApplicationManager.getApplication().invokeLater {
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

                    logger.debug("Showing popup to select Azure subscription")

                    val popup = JBPopupFactory.getInstance().createListPopup(step)
                    popup.showCenteredInCurrentWindow(project)
                }
            }
        }.queue()
    }

    private fun startAzureCloudShell(project: Project, azureManager: AzureManager, subscriptionDetail: SubscriptionDetail) {
        try {
            logger.debug("Start Azure Cloud Shell for subscription ${subscriptionDetail.subscriptionId}; tenant ${subscriptionDetail.tenantId}")

            val authManager = AdAuthManager.getInstance()
            val tokenCredentials = RefreshableTokenCredentials(authManager, subscriptionDetail.tenantId)

            provisionAzureCloudShell(project, azureManager, tokenCredentials, subscriptionDetail)
        } catch (e: AuthenticationException) {
            // Failed to authenticate....
            logger.error("Failed to authenticate Azure Cloud Shell", e)

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
                indicator.isIndeterminate = false

                // Custom Azure API client
                val retrofitClient = azureManager.getRetrofitClient(
                        azureManager.environment.azureEnvironment,
                        AzureEnvironment.Endpoint.RESOURCE_MANAGER,
                        CloudConsoleService::class.java,
                        tokenCredentials)

                // 1. Retrieve cloud shell preferences
                updateIndicator(indicator, 0.05, "Retrieving cloud shell preferences...")

                val consoleUserSettingsResponse = retrofitClient.userSettings().execute()
                if (!consoleUserSettingsResponse.isSuccessful) {
                    reportNoCloudShellConfiguration(project)
                    return
                }

                val preferredOsType = consoleUserSettingsResponse.body()?.properties?.preferredOsType ?: "Linux"

                // 2. Provision cloud shell
                updateIndicator(indicator, 0.35, "Provisioning cloud shell...", "Provisioning cloud shell... (preferred OS type: $preferredOsType)")

                val provisionParameters = CloudConsoleProvisionParameters(
                        CloudConsoleProvisionParameters.Properties(preferredOsType))

                val provisionResult = retrofitClient.provision(provisionParameters).execute()
                val provisionSucceeded = provisionResult.isSuccessful && provisionResult.body()?.properties?.
                        provisioningState?.equals("Succeeded", true) ?: false
                if (!provisionSucceeded) {
                    logger.error("Could not provision cloud shell. ")
                    logger.debug("Response received from API: ${consoleUserSettingsResponse.code()} ${consoleUserSettingsResponse.message()} - ${consoleUserSettingsResponse.errorBody()?.string()}")

                    reportNoCloudShellConfiguration(project)
                    return
                }

                val provisionUrl = provisionResult.body()?.properties?.uri
                if (provisionUrl.isNullOrEmpty())
                {
                    logger.error("Cloud shell URL was empty")
                    logger.debug("Response received from API: ${provisionResult.code()} ${provisionResult.message()} - ${provisionResult.errorBody()?.string()}")

                    reportNoCloudShellConfiguration(project)
                    return
                }

                // 3. Request cloud shell
                updateIndicator(indicator, 0.65, "Requesting cloud shell...")
                val shellUrl = "$provisionUrl/terminals"

                // Fetch graph and key vault tokens
                val credentials = RefreshableTokenCredentials(AdAuthManager.getInstance(), subscriptionDetail.tenantId)
                val graphToken = credentials.getToken(azureManager.environment.azureEnvironment.graphEndpoint())
                val vaultToken = credentials.getToken("https://" + azureManager.environment.azureEnvironment.keyVaultDnsSuffix().trimStart('.') + "/")

                // 4. Provision terminal
                updateIndicator(indicator, 0.75, "Requesting cloud shell terminal...")

                val provisionTerminalParameters = CloudConsoleProvisionTerminalParameters()
                provisionTerminalParameters.tokens.add(graphToken)
                provisionTerminalParameters.tokens.add(vaultToken)

                val provisionTerminalResult = retrofitClient.provisionTerminal(shellUrl, defaultTerminalColumns, defaultTerminalRows, provisionTerminalParameters).execute()
                if (!provisionTerminalResult.isSuccessful) {
                    reportNoCloudShellConfiguration(project)
                    return
                }

                val socketUri = provisionTerminalResult.body()?.socketUri
                if (socketUri.isNullOrEmpty())
                {
                    logger.error("Socket URI was empty")
                    logger.debug("Response received from API: ${provisionTerminalResult.code()} ${provisionTerminalResult.message()} - ${provisionTerminalResult.errorBody()?.string()}")

                    reportNoCloudShellConfiguration(project)
                    return
                }

                // 5. Connect to terminal
                updateIndicator(indicator, 0.85, "Connecting to cloud shell terminal...")

                val runner = AzureCloudTerminalFactory.createTerminalRunner(
                        project, retrofitClient, provisionUrl!!, URI(socketUri))

                ApplicationManager.getApplication().invokeLater {
                    val terminalWindow = ToolWindowManager.getInstance(project)
                            .getToolWindow(TerminalToolWindowFactory.TOOL_WINDOW_ID)

                    terminalWindow.show {
                        // HACK: Because local terminal always opens, we want to make sure it is available before opening cloud terminal
                        ApplicationManager.getApplication().invokeLater {
                            Thread.sleep(500)
                            TerminalView.getInstance(project).createNewSession(runner)
                        }
                    }
                }
            }
        })
    }

    private fun updateIndicator(indicator: ProgressIndicator, fraction: Double, text: String, debugText: String? = null) {
        indicator.fraction = fraction
        indicator.text = text
        logger.debug(debugText ?: text)
    }

    private fun reportNoCloudShellConfiguration(project: Project) {
        // No cloud shell configuration found...
        logger.warn("Azure Cloud Shell is not configured in any Azure subscription.")

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
}