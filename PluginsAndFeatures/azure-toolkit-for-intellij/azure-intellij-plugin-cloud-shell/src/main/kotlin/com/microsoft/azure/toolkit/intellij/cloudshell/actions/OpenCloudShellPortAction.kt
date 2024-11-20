/*
 * Copyright 2018-2024 JetBrains s.r.o. and contributors. Use of this source code is governed by the MIT license.
 */

@file:Suppress("UnstableApiUsage")

package com.microsoft.azure.toolkit.intellij.cloudshell.actions

import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.progress.currentThreadCoroutineScope
import com.intellij.openapi.ui.Messages
import com.intellij.platform.ide.progress.withBackgroundProgress
import com.microsoft.azure.toolkit.intellij.cloudshell.CloudShellService
import com.microsoft.azure.toolkit.intellij.cloudshell.utils.PreviewPortInputValidator
import kotlinx.coroutines.launch

class OpenCloudShellPortAction : AnAction() {
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

        val port = Messages.showInputDialog(
            "Configure port to preview (in ranges [1025-8079] and [8091-49151]):",
            "Configure Port To Preview",
            null,
            null,
            PreviewPortInputValidator()
        )?.toIntOrNull() ?: return

        currentThreadCoroutineScope().launch {
            withBackgroundProgress(project, "Opening preview port $port in Azure Cloud Shell...") {
                activeConnector.openPreviewPort(port, true)
            }
        }
    }

    override fun getActionUpdateThread() = ActionUpdateThread.BGT
}