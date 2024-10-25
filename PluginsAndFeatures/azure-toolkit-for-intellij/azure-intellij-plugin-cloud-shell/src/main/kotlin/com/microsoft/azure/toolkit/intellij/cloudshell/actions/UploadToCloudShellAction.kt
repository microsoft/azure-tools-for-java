/*
 * Copyright 2018-2024 JetBrains s.r.o. and contributors. Use of this source code is governed by the MIT license.
 */

@file:Suppress("UnstableApiUsage")

package com.microsoft.azure.toolkit.intellij.cloudshell.actions

import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.application.EDT
import com.intellij.openapi.fileChooser.FileChooser
import com.intellij.openapi.fileChooser.FileChooserDescriptor
import com.intellij.openapi.progress.currentThreadCoroutineScope
import com.intellij.openapi.vfs.VirtualFile
import com.microsoft.azure.toolkit.intellij.cloudshell.CloudShellService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class UploadToCloudShellAction : AnAction() {
    override fun update(e: AnActionEvent) {
        val project = e.project
        if (project == null) {
            e.presentation.isEnabled = false
            return
        }

        val activeConnector = CloudShellService.getInstance(project).activeConnector()
        e.presentation.isEnabled = activeConnector != null
    }

    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: return
        val activeConnector = CloudShellService.getInstance(project).activeConnector() ?: return

        currentThreadCoroutineScope().launch(Dispatchers.EDT) {
            val descriptor = FileChooserDescriptor(true, false, false, true, false, true).apply {
                title = "Select File(s) To Upload To Azure Cloud Shell"
            }
            FileChooser.chooseFiles(descriptor, project, null, null, object : FileChooser.FileChooserConsumer {
                override fun consume(files: MutableList<VirtualFile>) = files.forEach {
                    activeConnector.uploadFile(it.name, it)
                }

                override fun cancelled() {
                }
            })
        }
    }

    override fun getActionUpdateThread() = ActionUpdateThread.BGT
}