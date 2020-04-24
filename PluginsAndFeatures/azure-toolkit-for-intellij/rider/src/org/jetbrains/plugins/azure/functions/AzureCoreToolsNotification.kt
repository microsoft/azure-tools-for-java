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

package org.jetbrains.plugins.azure.functions

import com.intellij.ide.util.PropertiesComponent
import com.intellij.notification.*
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.startup.StartupActivity
import com.jetbrains.rd.platform.util.lifetime
import com.jetbrains.rd.util.reactive.adviseOnce
import com.jetbrains.rider.model.RunnableProjectKind
import com.jetbrains.rider.model.runnableProjectsModel
import com.jetbrains.rider.projectView.solution
import com.microsoft.intellij.configuration.AzureRiderSettings
import org.jetbrains.plugins.azure.RiderAzureBundle.message
import org.jetbrains.plugins.azure.functions.coreTools.FunctionsCoreToolsInfoProvider
import org.jetbrains.plugins.azure.functions.coreTools.FunctionsCoreToolsManager

// TODO: FIX_VERSION: Replace with [SolutionLoadNotification] when async notifications are merged into 202
class AzureCoreToolsNotification : StartupActivity {
    companion object {
        private val notificationGroup = NotificationGroup(
                displayId = "AzureFunctions",
                displayType = NotificationDisplayType.BALLOON,
                isLogByDefault = true,
                toolWindowId = null,
                icon = null
        )
    }

    override fun runActivity(project: Project) {
        val properties = PropertiesComponent.getInstance()
        if (!properties.getBoolean(AzureRiderSettings.PROPERTY_FUNCTIONS_CORETOOLS_CHECK_UPDATES, true))
            return

        project.solution.runnableProjectsModel.projects.adviseOnce(project.lifetime.createNested()) { runnableProjects ->
            if (runnableProjects.none { it.kind == RunnableProjectKind.AzureFunctions }) return@adviseOnce

            ApplicationManager.getApplication().executeOnPooledThread {
                val funcCoreToolsInfo = FunctionsCoreToolsInfoProvider.retrieve()
                val allowPrerelease = properties.getBoolean(AzureRiderSettings.PROPERTY_FUNCTIONS_CORETOOLS_ALLOW_PRERELEASE)

                val local = FunctionsCoreToolsManager.determineVersion(funcCoreToolsInfo?.coreToolsPath)
                val remote = FunctionsCoreToolsManager.determineLatestRemote(allowPrerelease)

                if (local == null || (remote != null && local < remote)) {

                    val notification =
                            if (local == null) createInstallNotification(project, allowPrerelease)
                            else createUpdateNotification(project, allowPrerelease)

                    Notifications.Bus.notify(notification, project)
                }
            }
        }
    }

    private fun createInstallNotification(project: Project, allowPreRelease: Boolean): Notification {
        val title = message("notification.function_app.core_tools.install.title")
        val description = message("notification.function_app.core_tools.install.description")
        val notification = notificationGroup.createNotification(
                title = title,
                subtitle = null,
                content = description,
                type = NotificationType.INFORMATION
        )

        notification.appendDownloadCoreToolsAction(
                project = project,
                actionName = message("notification.function_app.core_tools.action.install"),
                allowPreRelease = allowPreRelease
        )

        return notification
    }

    private fun createUpdateNotification(project: Project, allowPreRelease: Boolean): Notification {
        val title = message("notification.function_app.core_tools.update.title")
        val description = message("notification.function_app.core_tools.update.description")

        val notification = notificationGroup.createNotification(
                title = title,
                subtitle = null,
                content = description,
                type = NotificationType.INFORMATION
        )

        notification.appendDownloadCoreToolsAction(
                project = project,
                actionName = message("notification.function_app.core_tools.action.update"),
                allowPreRelease = allowPreRelease
        )
        notification.appendDisableUpdatesCheckAction(project = project)

        return notification
    }

    private fun Notification.appendDownloadCoreToolsAction(project: Project, actionName: String, allowPreRelease: Boolean) {
        addAction(NotificationAction.createSimple(actionName) {
            downloadCoreTools(project, allowPreRelease)
            expire()
        })
    }

    private fun Notification.appendDisableUpdatesCheckAction(project: Project) {
        addAction(NotificationAction.createSimple(message("notification.function_app.core_tools.action.disable_updates")) {
            disableNotification(project)
            expire()
        })
    }

    private fun downloadCoreTools(project: Project, allowPreRelease: Boolean) {
        ProgressManager.getInstance().run(
                FunctionsCoreToolsManager.downloadLatestRelease(project, allowPreRelease) { path ->
                    PropertiesComponent.getInstance().setValue(AzureRiderSettings.PROPERTY_FUNCTIONS_CORETOOLS_PATH, path)
                })
    }

    private fun disableNotification(project: Project) {
        PropertiesComponent.getInstance(project).setValue(AzureRiderSettings.PROPERTY_FUNCTIONS_CORETOOLS_CHECK_UPDATES, false)
    }
}
