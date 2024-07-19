/*
 * Copyright 2018-2024 JetBrains s.r.o. and contributors. Use of this source code is governed by the MIT license.
 */

package com.microsoft.azure.toolkit.intellij.cloudshell.actions

import com.intellij.openapi.actionSystem.*
import com.microsoft.azure.toolkit.intellij.cloudshell.CloudShellService

class CloseCloudShellPortActionGroup : ActionGroup() {
    override fun update(e: AnActionEvent) {
        val project = e.project
        if (project == null) {
            e.presentation.isEnabled = false
            return
        }

        val activeConnector = CloudShellService.getInstance(project).activeConnector()

        e.presentation.isHideGroupIfEmpty = true
        e.presentation.isEnabled = activeConnector != null && activeConnector.hasPreviewPorts()
    }

    override fun getChildren(e: AnActionEvent?): Array<AnAction> {
        val project = e?.project ?: return emptyArray()
        val activeConnector = CloudShellService.getInstance(project).activeConnector() ?: return emptyArray()

        if (!activeConnector.hasPreviewPorts()) return emptyArray()

        val actions = activeConnector.getPreviewPorts()
            .map { CloseCloudShellPortAction(it) }
            .toMutableList<AnAction>()

        actions.add(Separator())
        actions.add(CloseAllCloudShellPortsAction())

        return actions.toTypedArray()
    }

    override fun getActionUpdateThread() = ActionUpdateThread.BGT
}