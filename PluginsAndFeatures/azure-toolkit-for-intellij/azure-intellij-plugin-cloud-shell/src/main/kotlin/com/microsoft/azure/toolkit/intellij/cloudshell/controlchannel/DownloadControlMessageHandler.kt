/*
 * Copyright 2018-2024 JetBrains s.r.o. and contributors. Use of this source code is governed by the MIT license.
 */

package com.microsoft.azure.toolkit.intellij.cloudshell.controlchannel

import com.intellij.notification.Notification
import com.intellij.notification.NotificationType
import com.intellij.openapi.application.EDT
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.fileChooser.FileChooserFactory
import com.intellij.openapi.fileChooser.FileSaverDescriptor
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.platform.ide.progress.withBackgroundProgress
import com.microsoft.azure.toolkit.intellij.cloudshell.actions.RevealFileAction
import com.microsoft.azure.toolkit.intellij.cloudshell.rest.CloudConsoleService
import io.ktor.util.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.io.path.moveTo

@Service(Service.Level.PROJECT)
class DownloadControlMessageHandler(
    private val project: Project,
    private val scope: CoroutineScope
) {
    companion object {
        fun getInstance(project: Project): DownloadControlMessageHandler = project.service()
        private val LOG = logger<DownloadControlMessageHandler>()
    }

    fun handle(cloudConsoleBaseUrl: String, message: ControlMessage) = scope.launch(Dispatchers.Default) {
        if (message.fileUri.isNullOrEmpty()) {
            LOG.warn("File URI is empty in the control message")
            return@launch
        }

        val fileUrl = cloudConsoleBaseUrl + message.fileUri
        val file = withBackgroundProgress(project, "Downloading file...") {
            val service = CloudConsoleService.getInstance(project)
            return@withBackgroundProgress service.downloadFileFromTerminal(fileUrl)
        }
        val fileName = file.fileName.toString()

        val targetFile = withContext(Dispatchers.EDT) {
            val dialog = FileChooserFactory
                .getInstance()
                .createSaveFileDialog(
                    FileSaverDescriptor("Save File From Azure Cloud Shell", "", file.extension),
                    project
                )
            dialog.save(
                LocalFileSystem.getInstance().findFileByPath(System.getProperty("user.home")),
                fileName
            )
        }

        if (targetFile != null) {
            val targetFilePath = targetFile.file.toPath()
            file.moveTo(targetFilePath, overwrite = true)

            Notification(
                "Azure CloudShell",
                "File downloaded - $fileName",
                "The file $fileName was downloaded from Azure Cloud Shell.",
                NotificationType.INFORMATION
            )
                .addAction(RevealFileAction(targetFilePath))
                .notify(project)
        }
    }
}