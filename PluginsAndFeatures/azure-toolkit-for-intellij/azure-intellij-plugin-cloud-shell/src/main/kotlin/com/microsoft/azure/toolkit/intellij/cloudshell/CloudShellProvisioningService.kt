/*
 * Copyright 2018-2024 JetBrains s.r.o. and contributors. Use of this source code is governed by the MIT license.
 */

package com.microsoft.azure.toolkit.intellij.cloudshell

import com.azure.core.credential.TokenRequestContext
import com.azure.identity.implementation.util.ScopeUtil
import com.intellij.notification.Notification
import com.intellij.notification.NotificationType
import com.intellij.openapi.application.EDT
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.ToolWindowManager
import com.intellij.platform.ide.progress.withBackgroundProgress
import com.intellij.platform.util.progress.withProgressText
import com.microsoft.azure.toolkit.intellij.cloudshell.rest.*
import com.microsoft.azure.toolkit.intellij.cloudshell.terminal.AzureCloudTerminalFactory
import com.microsoft.azure.toolkit.lib.Azure
import com.microsoft.azure.toolkit.lib.auth.Account
import com.microsoft.azure.toolkit.lib.auth.AzureAccount
import com.microsoft.azure.toolkit.lib.common.model.Subscription
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.reactor.awaitSingleOrNull
import kotlinx.coroutines.withContext
import org.jetbrains.plugins.terminal.TerminalToolWindowFactory
import org.jetbrains.plugins.terminal.TerminalToolWindowManager
import java.net.URI
import kotlin.time.Duration.Companion.milliseconds

@Service(Service.Level.PROJECT)
class CloudShellProvisioningService(private val project: Project) {
    companion object {
        fun getInstance(project: Project) = project.service<CloudShellProvisioningService>()
        private val LOG = logger<CloudShellProvisioningService>()
    }

    private val defaultTerminalColumns = 100
    private val defaultTerminalRows = 30

    suspend fun provision(subscription: Subscription) = withBackgroundProgress(project, "Provisioning cloud shell...") {
        try {
            val account = Azure.az(AzureAccount::class.java).account()
            if (!account.isLoggedIn) {
                showUnsuccessfulNotification("Account is not logged in.")
                return@withBackgroundProgress
            }

            val accessTokenResult = setAccessToken(account, subscription.tenantId)
            if (!accessTokenResult) return@withBackgroundProgress

            val resourceManagerEndpoint = account.environment.resourceManagerEndpoint

            val settings = getSettings(resourceManagerEndpoint)
            if (settings == null) {
                showUnsuccessfulNotification("Unable to retrieve cloud shell settings.")
                return@withBackgroundProgress
            }

            val provisionResult = provision(resourceManagerEndpoint, settings)
            if (provisionResult == null) {
                showUnsuccessfulNotification("Unable to provision cloud shell.")
                return@withBackgroundProgress
            }

            val provisionUrl = provisionResult.properties.uri
            if (provisionUrl.isNullOrEmpty()) {
                LOG.error("Cloud shell URL was empty")
                return@withBackgroundProgress
            }

            val provisionTerminalResult = provisionTerminal(provisionUrl, account, subscription.tenantId)
            if (provisionTerminalResult == null) {
                showUnsuccessfulNotification("Unable to provision cloud shell terminal.")
                return@withBackgroundProgress
            }

            val socketUri = provisionTerminalResult.socketUri?.trimEnd('/')
            if (socketUri.isNullOrEmpty()) {
                LOG.error("Socket URI was empty")
                return@withBackgroundProgress
            }

            connectShellToTheTerminal(provisionUrl, socketUri)

        } catch (t: Throwable) {
            LOG.error("Unable to provision cloud shell", t)
            showUnsuccessfulNotification("")
        }
    }

    private suspend fun setAccessToken(account: Account, tenantId: String): Boolean {
        val accessToken = CloudShellAccessTokenService.getInstance().getAccessToken(tenantId, account)
        if (accessToken == null) {
            LOG.warn("Unable to get access token")
            return false
        }

        val cloudConsoleService = CloudConsoleService.getInstance(project)
        cloudConsoleService.setAccessToken(accessToken, tenantId)

        return true
    }

    private suspend fun getSettings(resourceManagerEndpoint: String): CloudConsoleUserSettings? {
        val cloudConsoleService = CloudConsoleService.getInstance(project)
        val settings = withProgressText("Retrieving cloud shell preferences...") {
            cloudConsoleService.getUserSettings(resourceManagerEndpoint)
        }
        if (settings == null) {
            LOG.warn("Azure Cloud Shell is not configured in any Azure subscription")
            return null
        }

        return settings
    }

    private suspend fun provision(
        resourceManagerEndpoint: String,
        settings: CloudConsoleUserSettings
    ): CloudConsoleProvisionResult? {
        val cloudConsoleService = CloudConsoleService.getInstance(project)
        val preferredOsType = settings.properties?.preferredOsType ?: "Linux"
        val provisionParameters = CloudConsoleProvisionParameters(
            CloudConsoleProvisionParameterProperties(preferredOsType)
        )
        val provisionResult = withProgressText("Provisioning cloud shell...") {
            cloudConsoleService.provision(
                resourceManagerEndpoint,
                settings.properties?.preferredLocation,
                provisionParameters
            )
        }

        if (provisionResult == null || provisionResult.properties.provisioningState != "Succeeded") {
            LOG.warn("Unable to provision cloud shell")
            return null
        }

        return provisionResult
    }

    private suspend fun provisionTerminal(
        provisionUrl: String,
        account: Account,
        tenantId: String
    ): CloudConsoleProvisionTerminalResult? {
        val shellUrl = "$provisionUrl/terminals"

        val keyVaultDnsSuffix = "https://" + account.environment.keyVaultDnsSuffix.trimStart('.') + "/"
        val vaultScopes = ScopeUtil.resourceToScopes(keyVaultDnsSuffix)
        val vaultToken = account
            .getTenantTokenCredential(tenantId)
            .getToken(TokenRequestContext().apply { addScopes(*vaultScopes) })
            .awaitSingleOrNull()
            ?.token

        if (vaultToken == null) {
            LOG.warn("Unable to obtain a vault token")
            return null
        }

        val shellUri = URI(shellUrl)
        val referrer = "${shellUri.scheme}://${shellUri.host}/\$hc${shellUri.path}"
        val provisionTerminalParameters = CloudConsoleProvisionTerminalParameters(
            listOf(vaultToken)
        )
        val cloudConsoleService = CloudConsoleService.getInstance(project)
        val provisionTerminalResult = withProgressText("Requesting cloud shell terminal...") {
            cloudConsoleService.provisionTerminal(
                shellUrl,
                defaultTerminalColumns,
                defaultTerminalRows,
                referrer,
                provisionTerminalParameters,
            )
        }
        if (provisionTerminalResult == null) {
            LOG.warn("Unable to provision cloud shell terminal")
            return null
        }

        return provisionTerminalResult
    }

    private suspend fun connectShellToTheTerminal(provisionUrl: String, socketUri: String) =
        withProgressText("Connecting to cloud shell terminal...") {
            val runner = AzureCloudTerminalFactory
                .getInstance(project)
                .createTerminalRunner(provisionUrl, socketUri)

            val terminalWindow = ToolWindowManager.getInstance(project)
                .getToolWindow(TerminalToolWindowFactory.TOOL_WINDOW_ID)
                ?: return@withProgressText

            withContext(Dispatchers.EDT) {
                terminalWindow.show()

                delay(500.milliseconds)

                TerminalToolWindowManager
                    .getInstance(project).createNewSession(runner)
            }
        }

    private suspend fun showUnsuccessfulNotification(message: String) = withContext(Dispatchers.EDT) {
        Notification(
            "Azure CloudShell",
            "Unable to provision cloud shell",
            message,
            NotificationType.WARNING
        )
            .notify(project)
    }
}