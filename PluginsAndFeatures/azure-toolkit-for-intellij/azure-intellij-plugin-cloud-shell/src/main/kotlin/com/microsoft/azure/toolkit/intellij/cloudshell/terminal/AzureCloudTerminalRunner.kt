/*
 * Copyright 2018-2024 JetBrains s.r.o. and contributors. Use of this source code is governed by the MIT license.
 */

@file:Suppress("UnstableApiUsage")

package com.microsoft.azure.toolkit.intellij.cloudshell.terminal

import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Disposer
import com.intellij.platform.util.coroutines.namedChildScope
import com.jediterm.terminal.TtyConnector
import com.microsoft.azure.toolkit.intellij.cloudshell.CloudShellService
import com.microsoft.azure.toolkit.intellij.cloudshell.controlchannel.CloudConsoleControlChannelClient
import kotlinx.coroutines.CoroutineScope
import org.apache.http.client.utils.URIBuilder
import org.jetbrains.plugins.terminal.cloud.CloudTerminalProcess
import org.jetbrains.plugins.terminal.cloud.CloudTerminalRunner

class AzureCloudTerminalRunner(
    project: Project,
    private val scope: CoroutineScope,
    private val cloudConsoleBaseUrl: String,
    socketUri: String,
    process: AzureCloudTerminalProcess
) : CloudTerminalRunner(project, "Azure Cloud Shell", process) {
    companion object {
        private val LOG = logger<AzureCloudTerminalRunner>()
    }

    private val controlChannelSocketUrl = "$socketUri/control"

    private val uploadFileToTerminalUrl: String
    private val previewPortBaseUrl: String
    private val resizeTerminalUrl: String

    init {
        val builder = URIBuilder(socketUri).apply {
            scheme = "https"
            path = path.removePrefix("/\$hc").trimEnd('/')
        }
        val uri = builder.toString()
        uploadFileToTerminalUrl = "$uri/upload"
        previewPortBaseUrl = "$cloudConsoleBaseUrl/ports"
        resizeTerminalUrl = "$uri/size"
    }

    override fun createTtyConnector(process: CloudTerminalProcess): TtyConnector {
        val cloudShellService = CloudShellService.getInstance(project)

        LOG.trace("Creating a new tty connector")

        val controlClient = CloudConsoleControlChannelClient(
            controlChannelSocketUrl,
            cloudConsoleBaseUrl,
            project
        )
        controlClient.connect(scope.namedChildScope("CloudConsoleControlChannelClient"))

        val connector = createConnector(process)
        Disposer.register(connector, controlClient)

        cloudShellService.registerConnector(connector)
        Disposer.register(cloudShellService, connector)

        return connector
    }

    private fun createConnector(process: CloudTerminalProcess) = AzureCloudProcessTtyConnector(
        process,
        project,
        scope.namedChildScope("AzureCloudProcessTtyConnector"),
        uploadFileToTerminalUrl,
        previewPortBaseUrl,
        resizeTerminalUrl
    )
}