package org.jetbrains.plugins.azure.cloudshell

import com.intellij.openapi.components.ProjectComponent
import org.jetbrains.plugins.azure.cloudshell.terminal.AzureCloudProcessTtyConnector

class CloudShellComponent : ProjectComponent {
    private val connectors = mutableListOf<AzureCloudProcessTtyConnector>()

    fun registerConnector(connector: AzureCloudProcessTtyConnector) {
        connectors.add(connector)
    }

    fun unregisterConnector(connector: AzureCloudProcessTtyConnector) {
        connectors.remove(connector)
    }

    fun activeConnector() : AzureCloudProcessTtyConnector? {
        return connectors.firstOrNull { it.isConnected }
    }
}