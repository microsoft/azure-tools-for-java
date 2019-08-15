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
package com.microsoft.azure.arcadia.serverexplore

import com.intellij.execution.RunManager
import com.intellij.openapi.project.Project
import com.microsoft.azure.arcadia.serverexplore.arcadianode.ArcadiaSparkOps
import com.microsoft.intellij.rxjava.IdeaSchedulers
import com.microsoft.azure.hdinsight.common.logger.ILogger
import com.microsoft.azure.hdinsight.spark.actions.SparkAppSubmitContext
import com.microsoft.azure.hdinsight.spark.actions.SparkDataKeys.*
import com.microsoft.azure.hdinsight.spark.run.configuration.ArcadiaSparkConfigurationFactory
import com.microsoft.azure.hdinsight.spark.run.configuration.ArcadiaSparkConfigurationType
import org.codehaus.plexus.util.ExceptionUtils
import com.microsoft.azure.hdinsight.spark.actions.ArcadiaSparkSelectAndSubmitAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.Presentation


class ArcadiaSparkOpsCtrl(val ops: ArcadiaSparkOps) : ILogger {
    init {
        ops.arcadiaSubmitAction
                .observeOn(IdeaSchedulers(null).dispatchUIThread())
                .subscribe({ computeNodePair ->
                    val compute = computeNodePair.left
                    val node = computeNodePair.right
                    log().info(String.format("Submit message received. cluster: %s, node: %s",
                            compute, node))

                    (node.project as? Project)?.apply {
                        try {
                            val runManager = RunManager.getInstance(this)
                            val batchConfigSettings = runManager.getConfigurationSettingsList(ArcadiaSparkConfigurationType)
                            val runConfigName = "[Spark on Arcadia]" + compute.title
                            val runConfigSetting = batchConfigSettings.stream()
                                    .filter { settings -> settings.configuration.name.startsWith(runConfigName) }
                                    .findFirst()
                                    .orElseGet { runManager.createConfiguration(runConfigName, ArcadiaSparkConfigurationFactory(ArcadiaSparkConfigurationType)) }

                            var context = SparkAppSubmitContext()
                                    .putData(RUN_CONFIGURATION_SETTING, runConfigSetting)
                                    .putData(CLUSTER, compute)

                            val actionPresentation = Presentation("Submit Job")
                            actionPresentation.description = "Submit specified Spark application into the remote cluster"

                            val event = AnActionEvent.createFromDataContext(
                                    String.format("Spark on Arcadia %s context menu", compute.title),
                                    actionPresentation,
                                    context)

                            ArcadiaSparkSelectAndSubmitAction().actionPerformed(event)
                        } catch (ex: Exception) {
                            log().warn(ex.message)
                        }
                    }
                }, { ex -> log().warn(ExceptionUtils.getStackTrace(ex)) })
    }
}