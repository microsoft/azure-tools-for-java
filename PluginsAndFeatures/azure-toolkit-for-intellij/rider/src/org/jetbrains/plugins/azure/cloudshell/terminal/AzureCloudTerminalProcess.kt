package org.jetbrains.plugins.azure.cloudshell.terminal

import org.jetbrains.plugins.azure.cloudshell.rest.CloudConsoleTerminalWebSocket
import org.jetbrains.plugins.terminal.cloud.CloudTerminalProcess

class AzureCloudTerminalProcess(private val socketClient: CloudConsoleTerminalWebSocket)
    : CloudTerminalProcess(socketClient.outputStream, socketClient.inputStream) {
    override fun destroy() {
        socketClient.close()
        super.destroy()
    }
}