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
package org.jetbrains.plugins.azure.cloudshell.controlchannel

import com.google.gson.Gson
import com.google.gson.annotations.SerializedName
import com.intellij.ide.actions.RevealFileAction
import com.intellij.notification.Notification
import com.intellij.notification.NotificationListener
import com.intellij.notification.NotificationType
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.fileChooser.FileChooserFactory
import com.intellij.openapi.fileChooser.FileSaverDescriptor
import com.intellij.openapi.progress.PerformInBackgroundOption
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.Task
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.util.PathUtil
import org.jetbrains.plugins.azure.RiderAzureBundle
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

        object : Task.Backgroundable(project, RiderAzureBundle.message("progress.cloud_shell.downloading_file"), true, PerformInBackgroundOption.DEAF) {
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
                    val dialog = FileChooserFactory.getInstance().createSaveFileDialog(
                            FileSaverDescriptor(RiderAzureBundle.message("context_menu.cloud_shell.save_file"),
                                    "", PathUtil.getFileExtension(fileName) ?: ""), project)

                    val targetFile = dialog.save(
                            LocalFileSystem.getInstance().findFileByPath(System.getProperty("user.home")),
                            fileName)

                    if (targetFile != null) {
                        ApplicationManager.getApplication().runWriteAction {
                            try {
                                val virtualFile = targetFile.getVirtualFile(true)
                                val outputStream = virtualFile!!.getOutputStream(this)
                                downloadResult.body()!!.byteStream().copyTo(outputStream)
                                outputStream.close()

                                AzureCloudShellNotifications.notify(project,
                                        RiderAzureBundle.message("common.azure"),
                                        RiderAzureBundle.message("notification.cloud_shell.download_file.subtitle", fileName),
                                        RiderAzureBundle.message("notification.cloud_shell.download_file.message", fileName) +
                                                " <a href='show'>" + RiderAzureBundle.message("notification.cloud_shell.download_file.message.show_file") + "</a>",
                                        NotificationType.INFORMATION,
                                        object : NotificationListener.Adapter() {
                                            override fun hyperlinkActivated(notification: Notification, e: HyperlinkEvent) {
                                                if (!project.isDisposed) {
                                                    when (e.description) {
                                                        "show" -> RevealFileAction.openFile(File(virtualFile.presentableUrl))
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
            }
        }.queue()
    }

    private data class DownloadControlMessage(
            @SerializedName("audience")
            val audience : String,

            @SerializedName("fileUri")
            val fileUri : String
    )
}
