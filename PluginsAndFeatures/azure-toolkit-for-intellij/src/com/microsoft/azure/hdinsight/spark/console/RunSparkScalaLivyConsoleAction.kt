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

import com.intellij.execution.RunManagerEx
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.microsoft.azure.hdinsight.spark.run.action.SelectSparkApplicationTypeAction
import com.microsoft.azure.hdinsight.spark.run.action.SparkApplicationType
import com.microsoft.azure.hdinsight.spark.run.configuration.*
import org.jetbrains.plugins.scala.console.ScalaConsoleRunConfigurationFactory

class RunSparkScalaLivyConsoleAction : RunSparkScalaConsoleAction() {
    override val focusedTabIndex: Int
        get() = 1

    override val isLocalRunConfigEnabled: Boolean
        get() = false

    override val consoleRunConfigurationFactory: ScalaConsoleRunConfigurationFactory
        get() = SparkScalaLivyConsoleConfigurationType().confFactory()

    override fun getNewSettingName(): String = "Spark Livy Interactive Session Console(Scala)"

    override fun update(event: AnActionEvent) {
        event.presentation.isEnabled = false
        super.update(event)
        val project = CommonDataKeys.PROJECT.getData(event.dataContext) ?: return
        val selectedConfigSettings = RunManagerEx.getInstanceEx(project).selectedConfiguration
        var runConfig = selectedConfigSettings?.configuration

        event.presentation.isEnabled = when {
            runConfig == null -> SelectSparkApplicationTypeAction.getSelectedSparkApplicationType() != SparkApplicationType.CosmosServerlessSpark
            runConfig.javaClass == CosmosServerlessSparkConfiguration::class.java -> false
            else -> true
        }
    }
}