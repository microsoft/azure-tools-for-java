package org.jetbrains.plugins.azure.cloudshell.terminal

import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.Project
import com.jediterm.terminal.ProcessTtyConnector
import com.jediterm.terminal.TtyConnector
import org.apache.http.client.utils.URIBuilder
import org.jetbrains.plugins.azure.cloudshell.rest.CloudConsoleService
import org.jetbrains.plugins.terminal.cloud.CloudTerminalProcess
import org.jetbrains.plugins.terminal.cloud.CloudTerminalRunner
import java.net.URI
import java.nio.charset.Charset

class AzureCloudTerminalRunner(project: Project,
                               private val cloudConsoleService: CloudConsoleService,
                               private val socketUri: URI,
                               process: AzureCloudTerminalProcess)
    : CloudTerminalRunner(project, pipeName, process) {

    companion object {
        private const val pipeName = "Azure Cloud Shell"
    }

    private val logger = Logger.getInstance(AzureCloudTerminalRunner::class.java)
    private val resizeTerminalUrl: String

    init {
        val builder = URIBuilder(socketUri)
        builder.scheme = "https"
        builder.path = builder.path.trimEnd('/') + "/size"
        resizeTerminalUrl = builder.toString()
    }

    override fun createTtyConnector(process: CloudTerminalProcess): TtyConnector {
        return object : ProcessTtyConnector(process, Charset.defaultCharset()) {
            override fun resizeImmediately() {
                if (pendingTermSize != null) {
                    val resizeResult = cloudConsoleService.resizeTerminal(
                            resizeTerminalUrl, pendingTermSize.width, pendingTermSize.height).execute()

                    if (!resizeResult.isSuccessful) {
                        logger.error("Could not resize cloud terminal. Response received from API: ${resizeResult.code()} ${resizeResult.message()} - ${resizeResult.errorBody()?.string()}")
                    }
                }
            }

            override fun getName(): String {
                return "Connector: $pipeName"
            }

            override fun isConnected(): Boolean {
                return true
            }
        }
    }
}