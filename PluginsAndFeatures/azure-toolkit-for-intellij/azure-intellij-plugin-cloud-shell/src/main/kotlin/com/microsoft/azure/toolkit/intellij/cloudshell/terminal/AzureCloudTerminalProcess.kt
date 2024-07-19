/*
 * Copyright 2018-2024 JetBrains s.r.o. and contributors. Use of this source code is governed by the MIT license.
 */

package com.microsoft.azure.toolkit.intellij.cloudshell.terminal

import org.jetbrains.plugins.terminal.cloud.CloudTerminalProcess

class AzureCloudTerminalProcess(private val socketClient: CloudConsoleTerminalWebSocket) :
    CloudTerminalProcess(socketClient.outputStream, socketClient.inputStream) {
    override fun destroy() {
        socketClient.close()
        super.destroy()
    }
}