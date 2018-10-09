package org.jetbrains.plugins.azure.cloudshell.controlchannel

import com.google.gson.Gson
import com.google.gson.annotations.SerializedName
import com.intellij.ide.actions.ShowFilePathAction
import com.intellij.notification.Notification
import com.intellij.notification.NotificationListener
import com.intellij.notification.NotificationType
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.fileChooser.FileChooser
import com.intellij.openapi.fileChooser.FileChooserDescriptor
import com.intellij.openapi.fileChooser.FileChooserFactory
import com.intellij.openapi.progress.PerformInBackgroundOption
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.Task
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import org.jetbrains.plugins.azure.cloudshell.AzureCloudShellNotifications
import org.jetbrains.plugins.azure.cloudshell.rest.CloudConsoleService
import java.io.File
import java.io.IOException
import java.util.*
import javax.swing.event.HyperlinkEvent

class DownloadControlMessageHandler(
        private val gson: Gson,
        private val project: Project,
        private val cloudConsoleService: CloudConsoleService,
        private val cloudConsoleBaseUrl: String)
    : ControlMessageHandler {

    companion object {
        private val logger = Logger.getInstance(DownloadControlMessageHandler::class.java)
        private val contentDispositionHeaderRegex = "(?<=filename=\").*?(?=\")".toRegex()
    }

    override fun handle(jsonControlMessage: String) {
        val message = gson.fromJson(jsonControlMessage, DownloadControlMessage::class.java)

        ApplicationManager.getApplication().invokeLater {
            val descriptor = FileChooserDescriptor(false, true, false, false, false, false)
            descriptor.title = "Save file from Azure Cloud Shell to folder"

            FileChooserFactory.getInstance().createPathChooser(descriptor, project, null)
                    .choose(null, object : FileChooser.FileChooserConsumer {
                        override fun consume(files: List<VirtualFile>) {
                            val targetPath = files[0]

                            object : Task.Backgroundable(project, "Downloading file...", true, PerformInBackgroundOption.DEAF) {
                                override fun run(indicator: ProgressIndicator) {
                                    val downloadResult = cloudConsoleService.downloadFileFromTerminal(cloudConsoleBaseUrl + message.fileUri).execute()
                                    if (!downloadResult.isSuccessful) {
                                        logger.error("Error downloading file from cloud terminal. Response received from API: ${downloadResult.code()} ${downloadResult.message()} - ${downloadResult.errorBody()?.string()}")
                                    }

                                    val matchResult = contentDispositionHeaderRegex.find(downloadResult.raw().header("Content-Disposition")
                                            ?: "")
                                    val fileName = if (matchResult != null) {
                                        matchResult.value
                                                .replace('/', '-')
                                                .replace('\\', '-')
                                    } else {
                                        "cloudshell-" + UUID.randomUUID()
                                    }

                                    ApplicationManager.getApplication().invokeLater {
                                        ApplicationManager.getApplication().runWriteAction {
                                            try {
                                                val targetFile = targetPath.createChildData(this, fileName)
                                                val outputStream = targetFile.getOutputStream(this)
                                                downloadResult.body()!!.byteStream().copyTo(outputStream)
                                                outputStream.close()

                                                AzureCloudShellNotifications.notify(project,
                                                        "Azure",
                                                        "File downloaded - $fileName",
                                                        "The file $fileName was downloaded from Azure Cloud Shell. <a href='show'>Show file</a>",
                                                        NotificationType.INFORMATION,
                                                        object : NotificationListener.Adapter() {
                                                            override fun hyperlinkActivated(notification: Notification, e: HyperlinkEvent) {
                                                                if (!project.isDisposed) {
                                                                    when (e.description) {
                                                                        "show" -> ShowFilePathAction.openFile(File(targetFile.presentableUrl))
                                                                    }
                                                                }
                                                            }
                                                        })
                                            } catch (e: IOException) {
                                                logger.error(e)
                                            }
                                        }
                                    }
                                }
                            }.queue()
                        }

                        override fun cancelled() {}
                    })
        }
    }

    private data class DownloadControlMessage(
            @SerializedName("audience")
            val audience : String,

            @SerializedName("fileUri")
            val fileUri : String
    )
}