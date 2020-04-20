/**
 * Copyright (c) 2019-2020 JetBrains s.r.o.
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

package org.jetbrains.plugins.azure.cloudshell.actions

import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.progress.PerformInBackgroundOption
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.Task
import com.intellij.openapi.ui.InputValidator
import com.intellij.openapi.ui.Messages
import com.jetbrains.rider.util.idea.getComponent
import org.jetbrains.plugins.azure.RiderAzureBundle
import org.jetbrains.plugins.azure.cloudshell.CloudShellComponent

class OpenCloudShellPortAction : AnAction() {
    private val logger = Logger.getInstance(OpenCloudShellPortAction::class.java)

    override fun update(e: AnActionEvent) {
        val project = CommonDataKeys.PROJECT.getData(e.dataContext)
        val cloudShellComponent = project?.getComponent<CloudShellComponent>()

        e.presentation.isEnabled = project != null
                && cloudShellComponent?.activeConnector() != null
    }

    override fun actionPerformed(e: AnActionEvent) {
        val project = CommonDataKeys.PROJECT.getData(e.dataContext) ?: return
        val activeConnector = project.getComponent<CloudShellComponent>().activeConnector() ?: return

        val port = Messages.showInputDialog(project,
                RiderAzureBundle.message("dialog.cloud_shell.open_port.message"),
                RiderAzureBundle.message("dialog.cloud_shell.open_port.title"),
                null,
                null,
                PreviewPortInputValidator.INSTANCE)?.toIntOrNull() ?: return

        ApplicationManager.getApplication().invokeLater {
            object : Task.Backgroundable(
                    project,
                    RiderAzureBundle.message("progress.cloud_shell.open_port.opening_preview_port", port),
                    true,
                    PerformInBackgroundOption.DEAF
            ) {
                override fun run(indicator: ProgressIndicator) {
                    logger.info("Opening preview port $port in Azure Cloud Shell...")
                    activeConnector.openPreviewPort(port, true)
                }
            }.queue()
        }
    }

    private class PreviewPortInputValidator : InputValidator {
        companion object {
            val INSTANCE = PreviewPortInputValidator()
        }

        override fun checkInput(input: String?): Boolean {
            return try {
                val port = input?.toIntOrNull()

                port != null && ((port in 1025..8079) || (port in 8091..49151))
            } catch(e: Exception) {
                false
            }
        }

        override fun canClose(input: String?): Boolean {
            return checkInput(input)
        }
    }
}
