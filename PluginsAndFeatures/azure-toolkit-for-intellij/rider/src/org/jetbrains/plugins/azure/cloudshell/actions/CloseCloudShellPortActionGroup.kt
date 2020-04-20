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

import com.intellij.openapi.actionSystem.*
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.progress.PerformInBackgroundOption
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.Task
import com.jetbrains.rider.util.idea.getComponent
import org.jetbrains.plugins.azure.RiderAzureBundle
import org.jetbrains.plugins.azure.cloudshell.CloudShellComponent

class CloseCloudShellPortActionGroup : ActionGroup() {

    private val logger = Logger.getInstance(OpenCloudShellPortAction::class.java)

    override fun update(e: AnActionEvent) {
        val project = CommonDataKeys.PROJECT.getData(e.dataContext)
        val cloudShellComponent = project?.getComponent<CloudShellComponent>()

        e.presentation.isEnabled = CommonDataKeys.PROJECT.getData(e.dataContext) != null
                && cloudShellComponent != null
                && cloudShellComponent.activeConnector() != null
                && cloudShellComponent.activeConnector()!!.openPreviewPorts.any()
    }

    override fun getChildren(e: AnActionEvent?): Array<AnAction> {
        if (e?.dataContext == null) return emptyArray()

        val project = CommonDataKeys.PROJECT.getData(e.dataContext)
        val cloudShellComponent = project?.getComponent<CloudShellComponent>() ?: return emptyArray()
        val currentConnector = cloudShellComponent.activeConnector() ?: return emptyArray()

        if (!currentConnector.openPreviewPorts.any()) return emptyArray()

        val actions = currentConnector.openPreviewPorts.sorted()
                .map { CloseCloudShellPortAction(it) }
                .toMutableList<AnAction>()

        actions.add(Separator())
        actions.add(CloseAllCloudShellPortsAction)

        return actions.toTypedArray()
    }

    override fun hideIfNoVisibleChildren(): Boolean {
        return true
    }
}

private class CloseCloudShellPortAction(val port: Int) : AnAction(port.toString()) {
    private val logger = Logger.getInstance(CloseCloudShellPortAction::class.java)

    override fun update(e: AnActionEvent) {
        e.presentation.isEnabled = true
    }

    override fun actionPerformed(e: AnActionEvent) {
        val project = CommonDataKeys.PROJECT.getData(e.dataContext) ?: return
        val activeConnector = project.getComponent<CloudShellComponent>().activeConnector() ?: return

        ApplicationManager.getApplication().invokeLater {
            object : Task.Backgroundable(
                    project,
                    RiderAzureBundle.message("progress.cloud_shell.close_port.closing_preview_port", port),
                    true,
                    PerformInBackgroundOption.DEAF
            ) {
                override fun run(indicator: ProgressIndicator) {
                    logger.info("Closing preview port $port in Azure Cloud Shell...")
                    activeConnector.closePreviewPort(port)
                }
            }.queue()
        }
    }
}

private object CloseAllCloudShellPortsAction : AnAction("Close all") {
    private val logger = Logger.getInstance(CloseCloudShellPortAction::class.java)

    override fun update(e: AnActionEvent) {
        e.presentation.isEnabled = true
    }

    override fun actionPerformed(e: AnActionEvent) {
        val project = CommonDataKeys.PROJECT.getData(e.dataContext) ?: return
        val activeConnector = project.getComponent<CloudShellComponent>().activeConnector() ?: return

        ApplicationManager.getApplication().invokeLater {
            object : Task.Backgroundable(
                    project,
                    RiderAzureBundle.message("progress.cloud_shell.close_port.closing_all_preview_ports"),
                    true,
                    PerformInBackgroundOption.DEAF
            ) {
                override fun run(indicator: ProgressIndicator) {
                    logger.info("Closing all preview ports in Azure Cloud Shell...")
                    val ports = activeConnector.openPreviewPorts.toIntArray()
                    for (port in ports) {
                        logger.info("Closing port: $port")
                        activeConnector.closePreviewPort(port)
                    }
                }
            }.queue()
        }
    }
}
