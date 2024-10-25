/*
 * Copyright 2018-2024 JetBrains s.r.o. and contributors. Use of this source code is governed by the MIT license.
 */

package com.microsoft.azure.toolkit.intellij.cloudshell

import com.intellij.openapi.Disposable
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import com.microsoft.azure.toolkit.intellij.cloudshell.terminal.AzureCloudProcessTtyConnector

@Service(Service.Level.PROJECT)
class CloudShellService: Disposable {
    companion object {
        fun getInstance(project: Project) = project.service<CloudShellService>()
    }

    private val connectors = mutableListOf<AzureCloudProcessTtyConnector>()

    fun registerConnector(connector: AzureCloudProcessTtyConnector) {
        connectors.add(connector)
    }

    fun unregisterConnector(connector: AzureCloudProcessTtyConnector) {
        connectors.remove(connector)
    }

    fun activeConnector() = connectors.firstOrNull { it.isConnected }

    override fun dispose() {
    }
}