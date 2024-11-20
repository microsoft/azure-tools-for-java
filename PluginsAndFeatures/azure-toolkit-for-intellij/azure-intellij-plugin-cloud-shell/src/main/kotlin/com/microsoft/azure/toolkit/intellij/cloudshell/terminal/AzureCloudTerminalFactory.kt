/*
 * Copyright 2018-2024 JetBrains s.r.o. and contributors. Use of this source code is governed by the MIT license.
 */

@file:Suppress("UnstableApiUsage")

package com.microsoft.azure.toolkit.intellij.cloudshell.terminal

import com.intellij.ide.ui.IdeUiService
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import kotlinx.coroutines.CoroutineScope
import java.net.URI

@Service(Service.Level.PROJECT)
class AzureCloudTerminalFactory(private val project: Project, private val scope: CoroutineScope) {
    companion object {
        fun getInstance(project: Project) = project.service<AzureCloudTerminalFactory>()
    }

    fun createTerminalRunner(cloudConsoleBaseUrl: String, socketUri: String): AzureCloudTerminalRunner {
        val terminalSocketClient = CloudConsoleTerminalWebSocket(URI(socketUri))
        IdeUiService.getInstance().sslSocketFactory?.let {
            // Inject IDEA SSL socket factory
            terminalSocketClient.setSocketFactory(it)
        }

        terminalSocketClient.connectBlocking()

        return AzureCloudTerminalRunner(
            project,
            scope,
            cloudConsoleBaseUrl,
            socketUri,
            AzureCloudTerminalProcess(terminalSocketClient)
        )
    }
}