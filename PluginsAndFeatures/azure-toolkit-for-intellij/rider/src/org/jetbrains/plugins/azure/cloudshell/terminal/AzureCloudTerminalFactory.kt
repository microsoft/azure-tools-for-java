package org.jetbrains.plugins.azure.cloudshell.terminal

import com.intellij.openapi.project.Project
import org.jetbrains.plugins.azure.cloudshell.rest.CloudConsoleService
import org.jetbrains.plugins.azure.cloudshell.rest.CloudConsoleTerminalWebSocket
import java.net.URI

class AzureCloudTerminalFactory {
    companion object {
        fun createTerminalRunner(project: Project,
                                 cloudConsoleService: CloudConsoleService,
                                 cloudConsoleBaseUrl: String,
                                 socketUri: URI): AzureCloudTerminalRunner {
            // Connect terminal web socket
            val terminalSocketClient = CloudConsoleTerminalWebSocket(socketUri)
            terminalSocketClient.connectBlocking()

            // Create runner
            return AzureCloudTerminalRunner(
                    project, cloudConsoleService, cloudConsoleBaseUrl, socketUri, AzureCloudTerminalProcess(terminalSocketClient))
        }
    }
}