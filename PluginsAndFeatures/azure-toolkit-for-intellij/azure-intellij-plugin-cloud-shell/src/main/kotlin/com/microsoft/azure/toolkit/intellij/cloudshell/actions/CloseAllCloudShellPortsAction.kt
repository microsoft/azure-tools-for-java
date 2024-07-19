/*
 * Copyright 2018-2024 JetBrains s.r.o. and contributors. Use of this source code is governed by the MIT license.
 */

@file:Suppress("UnstableApiUsage")

package com.microsoft.azure.toolkit.intellij.cloudshell.actions

import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.progress.currentThreadCoroutineScope
import com.intellij.platform.ide.progress.withBackgroundProgress
import com.microsoft.azure.toolkit.intellij.cloudshell.CloudShellService
import kotlinx.coroutines.launch

class CloseAllCloudShellPortsAction : AnAction("Close all") {
    override fun update(e: AnActionEvent) {
        e.presentation.isEnabled = true
    }

    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: return
        val activeConnector = CloudShellService.getInstance(project).activeConnector() ?: return

        currentThreadCoroutineScope().launch {
            withBackgroundProgress(project, "Closing all preview ports in Azure Cloud Shell...") {
                activeConnector.closePreviewPorts()
            }
        }
    }

    override fun getActionUpdateThread() = ActionUpdateThread.BGT
}