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

import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.ui.Messages
import com.microsoft.azuretools.telemetry.TelemetryConstants
import com.microsoft.azuretools.telemetrywrapper.Operation
import org.jetbrains.plugins.scala.console.configuration.ScalaConsoleRunConfigurationFactory

class RunSparkScalaLocalConsoleAction : RunSparkScalaConsoleAction() {
    private var isMockFs: Boolean = false

    override val selectedMenuActionId: String
        get() = "Actions.SparkRunLocalConsoleActionGroups"

    override val isLocalRunConfigEnabled: Boolean
        get() = true

    override val focusedTabIndex: Int
        get() = 0

    override val consoleRunConfigurationFactory: ScalaConsoleRunConfigurationFactory
        get() = SparkScalaLocalConsoleConfigurationType().sparkLocalConfFactory(isMockFs)

    override fun getNewSettingName(): String = "Spark Local Console(Scala)"

    override fun getOperationName(event: AnActionEvent?): String = TelemetryConstants.RUN_SPARK_LOCAL_CONSOLE

    override fun onActionPerformed(event: AnActionEvent, operation: Operation?): Boolean {
        val project = CommonDataKeys.PROJECT.getData(event.dataContext) ?: return true

        isMockFs = Messages.YES == Messages.showYesNoDialog(
                        project,
                        "Do you want to use a mocked file system?",
                        "Setting file system",
                        Messages.getQuestionIcon())

        return super.onActionPerformed(event, operation)
    }
}