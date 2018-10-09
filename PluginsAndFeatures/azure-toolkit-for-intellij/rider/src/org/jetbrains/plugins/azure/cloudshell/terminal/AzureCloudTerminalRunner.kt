package org.jetbrains.plugins.azure.cloudshell.terminal

import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.Project
import com.jediterm.terminal.ProcessTtyConnector
import com.jediterm.terminal.TtyConnector
import org.apache.http.client.utils.URIBuilder
import org.jetbrains.plugins.azure.cloudshell.rest.CloudConsoleService
import org.jetbrains.plugins.terminal.cloud.CloudTerminalProcess
import org.jetbrains.plugins.terminal.cloud.CloudTerminalRunner
import java.io.IOException
import java.net.URI
import java.nio.charset.Charset

class AzureCloudTerminalRunner(project: Project,
                               private val cloudConsoleService: CloudConsoleService,
                               socketUri: URI,
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

            override fun getName(): String {
                return "Connector: $pipeName"
            }

            override fun isConnected(): Boolean {
                return true
            }
        }
    }
}