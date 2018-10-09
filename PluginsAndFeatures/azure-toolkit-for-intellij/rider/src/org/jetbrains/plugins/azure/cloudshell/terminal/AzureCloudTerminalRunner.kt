package org.jetbrains.plugins.azure.cloudshell.terminal

import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.jediterm.terminal.TtyConnector
import com.jetbrains.rider.util.idea.getComponent
import okhttp3.MediaType
import okhttp3.MultipartBody
import okhttp3.RequestBody
import org.apache.http.client.utils.URIBuilder
import org.jetbrains.plugins.azure.cloudshell.CloudShellComponent
import org.jetbrains.plugins.azure.cloudshell.rest.CloudConsoleService
import org.jetbrains.plugins.terminal.cloud.CloudTerminalProcess
import org.jetbrains.plugins.terminal.cloud.CloudTerminalRunner
import java.net.URI

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
    private val uploadFileToTerminalUrl: String

    init {
        val builder = URIBuilder(socketUri)
        builder.scheme = "https"
        builder.path = builder.path.trimEnd('/')

        resizeTerminalUrl = builder.toString() + "/size"
        uploadFileToTerminalUrl = builder.toString() + "/upload"
    }

    override fun createTtyConnector(process: CloudTerminalProcess): TtyConnector {
        val cloudShellComponent = project?.getComponent<CloudShellComponent>()

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


            override fun getName(): String {
                return "Connector: $pipeName"
            }

            override fun close() {
                cloudShellComponent?.unregisterConnector(this)
                super.close()
            }
        }

        cloudShellComponent?.registerConnector(connector)
        return connector
    }
}