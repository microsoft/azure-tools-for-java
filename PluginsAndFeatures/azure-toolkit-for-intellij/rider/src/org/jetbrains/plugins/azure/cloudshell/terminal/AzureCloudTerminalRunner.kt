/**
 * Copyright (c) 2018-2020 JetBrains s.r.o.
 * <p/>
 * All rights reserved.
 * <p/>
 * MIT License
 * <p/>
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated
 * documentation files (the "Software"), to deal in the Software without restriction, including without limitation
 * the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and
 * to permit persons to whom the Software is furnished to do so, subject to the following conditions:
 * <p/>
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of
 * the Software.
 * <p/>
 * THE SOFTWARE IS PROVIDED *AS IS*, WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO
 * THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT,
 * TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package org.jetbrains.plugins.azure.cloudshell.terminal

import com.intellij.ide.BrowserUtil
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.jediterm.terminal.TtyConnector
import com.jetbrains.rider.util.idea.getComponent
import okhttp3.MediaType
import okhttp3.MultipartBody
import okhttp3.RequestBody
import org.apache.http.client.utils.URIBuilder
import org.jetbrains.plugins.azure.RiderAzureBundle.message
import org.jetbrains.plugins.azure.cloudshell.CloudShellComponent
import org.jetbrains.plugins.azure.cloudshell.controlchannel.CloudConsoleControlChannelWebSocket
import org.jetbrains.plugins.azure.cloudshell.rest.CloudConsoleService
import org.jetbrains.plugins.terminal.cloud.CloudTerminalProcess
import org.jetbrains.plugins.terminal.cloud.CloudTerminalRunner
import java.net.URI

class AzureCloudTerminalRunner(project: Project,
                               private val cloudConsoleService: CloudConsoleService,
                               private val cloudConsoleBaseUrl: String,
                               socketUri: URI,
                               process: AzureCloudTerminalProcess)
    : CloudTerminalRunner(project, message("terminal.cloud_shell.runner.pipe_name"), process) {

    companion object {
        private val logger = Logger.getInstance(AzureCloudTerminalRunner::class.java)
    }

    private val resizeTerminalUrl: String
    private val uploadFileToTerminalUrl: String
    private val previewPortBaseUrl: String

    private val controlChannelSocketUrl: String

    init {
        val builder = URIBuilder(socketUri)
        builder.scheme = "https"
        builder.path = builder.path.trimEnd('/')

        resizeTerminalUrl = builder.toString() + "/size"
        uploadFileToTerminalUrl = builder.toString() + "/upload"
        previewPortBaseUrl = cloudConsoleBaseUrl + "/ports"

        controlChannelSocketUrl = socketUri.toString() + "/control"
    }

    override fun createTtyConnector(process: CloudTerminalProcess): TtyConnector {
        val cloudShellComponent = project.getComponent<CloudShellComponent>()

        // Connect control socket
        // TODO: for now, this does not yet need any other wiring, it's just a convenience for e.g. downloading files
        val controlSocketClient = CloudConsoleControlChannelWebSocket(
                project, URI(controlChannelSocketUrl), cloudConsoleService, cloudConsoleBaseUrl)
        controlSocketClient.connectBlocking()

        // Build TTY
        val connector = object : AzureCloudProcessTtyConnector(process) {
            override fun resizeImmediately() {
                if (pendingTermSize != null) {
                    val resizeResult = cloudConsoleService.resizeTerminal(
                            resizeTerminalUrl, pendingTermSize.width, pendingTermSize.height).execute()

                    if (!resizeResult.isSuccessful) {
                        logger.error("Could not resize cloud terminal. Response received from API: ${resizeResult.code()} ${resizeResult.message()} - ${resizeResult.errorBody()?.string()}")
                    }
                }
            }

            override fun uploadFile(fileName: String, file: VirtualFile) {
                val part = MultipartBody.Part.createFormData(
                        "uploading-file",
                        fileName,
                        RequestBody.create(
                                MediaType.get("application/octet-stream"),
                                file.contentsToByteArray()
                        ))

                val uploadResult = cloudConsoleService.uploadFileToTerminal(
                        uploadFileToTerminalUrl,
                        part).execute()

                if (!uploadResult.isSuccessful) {
                    logger.error("Error uploading file to cloud terminal. Response received from API: ${uploadResult.code()} ${uploadResult.message()} - ${uploadResult.errorBody()?.string()}")
                }
            }

            override fun openPreviewPort(port: Int, openInBrowser: Boolean) {
                val result = cloudConsoleService.openPreviewPort("$previewPortBaseUrl/$port/open").execute()

                if (!result.isSuccessful) {
                    logger.error("Could not open preview port. Response received from API: ${result.code()} ${result.message()} - ${result.errorBody()?.string()}")
                    return
                }

                openPreviewPorts.add(port)

                if (openInBrowser && result.body()?.url != null) {
                    BrowserUtil.open(result.body()!!.url!!)
                }
            }

            override fun closePreviewPort(port: Int) {
                val result = cloudConsoleService.closePreviewPort("$previewPortBaseUrl/$port/open").execute()

                if (!result.isSuccessful) {
                    logger.error("Could not close preview port. Response received from API: ${result.code()} ${result.message()} - ${result.errorBody()?.string()}")
                    return
                }

                openPreviewPorts.remove(port)
            }

            override fun getName(): String = message("terminal.cloud_shell.runner.name")

            override fun close() {
                cloudShellComponent.unregisterConnector(this)
                super.close()
            }
        }

        cloudShellComponent.registerConnector(connector)
        return connector
    }
}