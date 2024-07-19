/*
 * Copyright 2018-2024 JetBrains s.r.o. and contributors. Use of this source code is governed by the MIT license.
 */

package com.microsoft.azure.toolkit.intellij.cloudshell.terminal

import com.intellij.ide.BrowserUtil
import com.intellij.notification.Notification
import com.intellij.notification.NotificationType
import com.intellij.openapi.Disposable
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.jediterm.core.util.TermSize
import com.jediterm.terminal.ProcessTtyConnector
import com.microsoft.azure.toolkit.intellij.cloudshell.CloudShellService
import com.microsoft.azure.toolkit.intellij.cloudshell.rest.CloudConsoleService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import org.jetbrains.plugins.terminal.cloud.CloudTerminalProcess
import java.io.IOException
import java.nio.charset.Charset

class AzureCloudProcessTtyConnector(
    process: CloudTerminalProcess,
    private val project: Project,
    private val scope: CoroutineScope,
    private val uploadFileToTerminalUrl: String,
    private val previewPortBaseUrl: String,
    private val resizeTerminalUrl: String
) : ProcessTtyConnector(process, Charset.defaultCharset()), Disposable {
    companion object {
        private val LOG = logger<AzureCloudProcessTtyConnector>()
    }

    private val openPreviewPorts = mutableListOf<Int>()

    fun hasPreviewPorts() = openPreviewPorts.isNotEmpty()

    fun getPreviewPorts() = openPreviewPorts.sorted()

    fun uploadFile(fileName: String, file: VirtualFile) {
        val cloudConsoleService = CloudConsoleService.getInstance(project)
        scope.launch {
            val result = cloudConsoleService.uploadFileToTerminal(uploadFileToTerminalUrl, fileName, file)
            if (!result) {
                LOG.warn("Could not upload file to the cloud terminal")
            }
        }
    }

    fun openPreviewPort(port: Int, openInBrowser: Boolean) {
        val cloudConsoleService = CloudConsoleService.getInstance(project)
        scope.launch {
            try {
                val result = cloudConsoleService.openPreviewPort("$previewPortBaseUrl/$port/open")
                if (result == null) {
                    LOG.warn("Could not open preview port")
                    return@launch
                }

                openPreviewPorts.add(port)

                val url = result.url
                if (openInBrowser && url != null) {
                    BrowserUtil.open(url)
                }
            } catch (t: Throwable) {
                LOG.error("Could not open preview port", t)
                Notification(
                    "Azure CloudShell",
                    "Unable to open preview port",
                    "Unable to open preview port $port",
                    NotificationType.WARNING
                )
                    .notify(project)
            }
        }
    }

    fun closePreviewPort(port: Int) {
        val cloudConsoleService = CloudConsoleService.getInstance(project)
        scope.launch {
            try {
                val result = cloudConsoleService.closePreviewPort("$previewPortBaseUrl/$port/close")
                if (result == null) {
                    LOG.warn("Could not close preview port")
                    Notification(
                        "Azure CloudShell",
                        "Unable to close preview port",
                        "Unable to close preview port $port",
                        NotificationType.WARNING
                    )
                        .notify(project)
                    return@launch
                } else {
                    Notification(
                        "Azure CloudShell",
                        "Preview port was closed",
                        "Preview port $port was closed",
                        NotificationType.INFORMATION
                    )
                        .notify(project)
                }

                openPreviewPorts.remove(port)
            } catch (t: Throwable) {
                LOG.error("Could not open preview port", t)
                Notification(
                    "Azure CloudShell",
                    "Unable to close preview port",
                    "Unable to close preview port $port",
                    NotificationType.WARNING
                )
                    .notify(project)
            }
        }
    }

    fun closePreviewPorts() = openPreviewPorts.toIntArray().forEach {
        closePreviewPort(it)
    }

    override fun resize(termSize: TermSize) {
        val cloudConsoleService = CloudConsoleService.getInstance(project)
        scope.launch {
            val result = cloudConsoleService.resizeTerminal(resizeTerminalUrl, termSize.columns, termSize.rows)
            if (!result) {
                LOG.warn("Could not resize cloud terminal")
            }
        }
    }

    override fun getName() = "Connector: Azure Cloud Shell"

    override fun read(buf: CharArray?, offset: Int, length: Int): Int {
        try {
            return super.read(buf, offset, length)
        } catch (e: IOException) {
            if (shouldRethrowIOException(e)) {
                throw e
            }
        }

        return -1
    }

    override fun write(bytes: ByteArray?) {
        try {
            super.write(bytes)
        } catch (e: IOException) {
            if (shouldRethrowIOException(e)) {
                throw e
            }
        }
    }

    override fun write(string: String?) {
        try {
            super.write(string)
        } catch (e: IOException) {
            if (shouldRethrowIOException(e)) {
                throw e
            }
        }
    }

    private fun shouldRethrowIOException(exception: IOException): Boolean =
        exception.message == null || !exception.message!!.contains("pipe closed", true)

    override fun isConnected() = true

    override fun close() {
        val cloudShellService = CloudShellService.getInstance(project)
        cloudShellService.unregisterConnector(this)
        super.close()
    }

    override fun dispose() {
    }
}