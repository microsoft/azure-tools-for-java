package org.jetbrains.plugins.azure.cloudshell.terminal

import com.intellij.openapi.project.Project
import org.jetbrains.plugins.azure.cloudshell.rest.CloudConsoleService
import org.jetbrains.plugins.azure.cloudshell.rest.CloudConsoleTerminalWebSocket
import java.net.URI

class AzureCloudTerminalFactory {
    companion object {
        fun createTerminalRunner(project: Project, cloudConsoleService: CloudConsoleService, socketUri: URI): AzureCloudTerminalRunner {
            // Connect web socket
            val socketClient = CloudConsoleTerminalWebSocket(socketUri)
            socketClient.connectBlocking()

            // Create runner
            return AzureCloudTerminalRunner(
                    project, cloudConsoleService, socketUri, AzureCloudTerminalProcess(socketClient))
        }
    }
}