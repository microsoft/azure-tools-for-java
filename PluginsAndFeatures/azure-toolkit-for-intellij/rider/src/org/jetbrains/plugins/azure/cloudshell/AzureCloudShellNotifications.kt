package org.jetbrains.plugins.azure.cloudshell

import com.intellij.notification.*
import com.intellij.openapi.project.Project

class AzureCloudShellNotifications {
    companion object {
        val notificationGroup = NotificationGroup("Azure", NotificationDisplayType.BALLOON, true, null, null)

        fun notify(project: Project, title: String?, subtitle: String?, content: String?, type: NotificationType, listener: NotificationListener?) {
            val notification = notificationGroup.createNotification(
                    title, subtitle, content, type, listener)

            Notifications.Bus.notify(notification, project)
        }
    }
}