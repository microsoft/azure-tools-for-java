/*
 * Copyright 2018-2024 JetBrains s.r.o. and contributors. Use of this source code is governed by the MIT license.
 */

package com.microsoft.azure.toolkit.intellij.cloudshell.actions

import com.intellij.ide.actions.RevealFileAction
import com.intellij.notification.Notification
import com.intellij.notification.NotificationAction
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnActionEvent
import java.nio.file.Path

class RevealFileAction(private val path: Path) : NotificationAction("Show file") {
    override fun actionPerformed(e: AnActionEvent, notification: Notification) {
        RevealFileAction.openFile(path)
    }

    override fun update(e: AnActionEvent) {
        e.presentation.isEnabledAndVisible = e.project != null && RevealFileAction.isSupported()
    }

    override fun getActionUpdateThread() = ActionUpdateThread.BGT
}