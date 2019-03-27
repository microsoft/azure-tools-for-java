/**
 * Copyright (c) 2019 JetBrains s.r.o.
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

package org.jetbrains.plugins.azure.functions

import com.intellij.ide.util.PropertiesComponent
import com.intellij.notification.*
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.startup.StartupActivity
import com.jetbrains.rd.util.reactive.adviseOnce
import com.jetbrains.rdclient.util.idea.createLifetime
import com.jetbrains.rider.model.RunnableProjectKind
import com.jetbrains.rider.model.runnableProjectsModel
import com.jetbrains.rider.projectView.solution
import com.microsoft.intellij.configuration.AzureRiderSettings
import org.jetbrains.plugins.azure.functions.coreTools.FunctionsCoreToolsInfoProvider
import org.jetbrains.plugins.azure.functions.coreTools.FunctionsCoreToolsManager
import javax.swing.event.HyperlinkEvent

class AzureCoreToolsNotification : StartupActivity {
    companion object {
        private val notificationGroup = NotificationGroup("AzureFunctions", NotificationDisplayType.BALLOON, true, null, null)
    }

    override fun runActivity(project: Project) {
        project.solution.runnableProjectsModel.projects.adviseOnce(project.createLifetime()) { runnableProjects ->
            if (runnableProjects.none { it.kind == RunnableProjectKind.AzureFunctions }) return@adviseOnce

            val funcCoreToolsInfo = FunctionsCoreToolsInfoProvider.build()

            val local = FunctionsCoreToolsManager.determineVersion(funcCoreToolsInfo?.coreToolsPath)
            val remote = FunctionsCoreToolsManager.determineLatestRemote()

            if (local == null || remote != null && local < remote) {
                val title = if (local == null) {
                    "Install Azure Functions Core Tools"
                } else {
                    "Update Azure Functions Core Tools"
                }

                val description = if (local == null) {
                    "<a href='install'>Install the Azure Functions Core Tools</a> to develop, run and debug Azure Functions locally."
                } else {
                    "A newer version of the Azure Functions Core Tools is available. <a href='install'>Update now</a>"
                }

                val notification = notificationGroup.createNotification(
                        title, null, description,
                        NotificationType.INFORMATION,
                        object : NotificationListener.Adapter() {
                            override fun hyperlinkActivated(notification: Notification, e: HyperlinkEvent) {
                                if (!project.isDisposed) {
                                    when (e.description) {
                                        "install" -> {
                                            ProgressManager.getInstance().run(
                                                    FunctionsCoreToolsManager.downloadLatestRelease(project) {
                                                        PropertiesComponent.getInstance().setValue(
                                                                AzureRiderSettings.PROPERTY_FUNCTIONS_CORETOOLS_PATH, it)
                                                        notification.expire()
                                                    })
                                        }
                                    }
                                }
                            }
                        })

                Notifications.Bus.notify(notification, project)
            }
        }
    }
}