/*
 * Copyright (c) Microsoft Corporation
 *
 * All rights reserved.
 *
 * MIT License
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated
 * documentation files (the "Software"), to deal in the Software without restriction, including without limitation
 * the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and
 * to permit persons to whom the Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of
 * the Software.
 *
 * THE SOFTWARE IS PROVIDED *AS IS*, WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO
 * THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT,
 * TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package com.microsoft.azure.hdinsight.spark.console

import com.intellij.execution.configurations.RuntimeConfigurationError
import com.intellij.notification.NotificationGroup
import com.intellij.notification.NotificationType
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.microsoft.azure.hdinsight.common.logger.ILogger
import com.microsoft.azuretools.ijidea.utility.AzureAnAction
import com.microsoft.intellij.activitylog.ActivityLogToolWindowFactory
import java.lang.reflect.InvocationTargetException
import java.lang.reflect.Method


// The Action is a bridge to connect Scala related actions with dependent Scala Plugin actions by reflection
open class RunSparkConsoleActionDelegate(sparkScalaActionClassName: String) : AzureAnAction(), ILogger {
    private val notificationGroup = NotificationGroup.toolWindowGroup("Spark Console", ActivityLogToolWindowFactory.ACTIVITY_LOG_WINDOW)

    private val delegate = SparkScalaPluginDelegate(sparkScalaActionClassName)

    val isEnabled
        get() = delegate.isEnabled && updateMethod != null && actionPerformedMethod != null

    private val updateMethod: Method?
        get() = delegate.getMethod("update", AnActionEvent::class.java)

    private val actionPerformedMethod: Method?
        get() = delegate.getMethod("actionPerformed", AnActionEvent::class.java)

    override fun update(actionEvent: AnActionEvent) {
        val presentation = actionEvent.presentation

        // Hide for the Scala plugin not installed
        presentation.isVisible = isEnabled
        presentation.isEnabled = isEnabled

        if (isEnabled) {
            updateMethod?.invoke(delegate.sparkScalaObj, actionEvent)
        }
    }

    @Throws(RuntimeConfigurationError::class)
    override fun onActionPerformed(actionEvent: AnActionEvent) {
        try {
            actionPerformedMethod?.invoke(delegate.sparkScalaObj, actionEvent)
        } catch (ex: InvocationTargetException) {
            var notification = notificationGroup.createNotification(
                    "Error running Spark Console. \n" + ex.targetException.message,
                    NotificationType.ERROR
            )
            val project = CommonDataKeys.PROJECT.getData(actionEvent.dataContext) ?: return
            notification.notify(project)
        }
    }
}

class RunSparkLocalConsoleActionDelegate : RunSparkConsoleActionDelegate("com.microsoft.azure.hdinsight.spark.console.RunSparkScalaLocalConsoleAction")
class RunSparkLivyConsoleActionDelegate : RunSparkConsoleActionDelegate("com.microsoft.azure.hdinsight.spark.console.RunSparkScalaLivyConsoleAction")
class SparkSendSelectionToConsoleActionDelegate : RunSparkConsoleActionDelegate("com.microsoft.azure.hdinsight.spark.console.SparkSendSelectionToConsoleAction")
