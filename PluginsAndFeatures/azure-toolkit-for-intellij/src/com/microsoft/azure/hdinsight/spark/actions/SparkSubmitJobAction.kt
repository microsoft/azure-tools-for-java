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

package com.microsoft.azure.hdinsight.spark.actions

import com.intellij.execution.ExecutorRegistry
import com.intellij.execution.RunnerAndConfigurationSettings
import com.intellij.execution.actions.ConfigurationContext
import com.intellij.execution.runners.ExecutionEnvironmentBuilder
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.DataContext
import com.intellij.openapi.actionSystem.ex.ActionManagerEx
import com.microsoft.azure.hdinsight.spark.actions.SparkDataKeys.*
import com.microsoft.azure.hdinsight.spark.run.SparkBatchJobRunExecutor
import com.microsoft.azure.hdinsight.spark.run.action.RunConfigurationActionUtils
import com.microsoft.azure.hdinsight.spark.run.action.SelectSparkApplicationTypeAction
import com.microsoft.azure.hdinsight.spark.run.action.SeqActions
import com.microsoft.azure.hdinsight.spark.run.action.SparkApplicationType
import com.microsoft.azure.hdinsight.spark.run.configuration.LivySparkBatchJobRunConfiguration
import com.microsoft.azuretools.ijidea.utility.AzureAnAction
import com.microsoft.azuretools.telemetry.TelemetryConstants
import com.microsoft.azuretools.telemetrywrapper.Operation
import com.microsoft.azuretools.telemetrywrapper.TelemetryManager


open class SparkSubmitJobAction : AzureAnAction() {
    open fun submitWithPopupMenu(anActionEvent: AnActionEvent) : Boolean {
        return if (SelectSparkApplicationTypeAction.getSelectedSparkApplicationType() == SparkApplicationType.None) {
            val action = ActionManagerEx.getInstance().getAction("Actions.SparkSubmitJobActionGroups")
            action?.actionPerformed(anActionEvent)
            true
        } else {
            false
        }
    }
    override fun onActionPerformed(anActionEvent: AnActionEvent?) {
        if (anActionEvent == null) {
            return
        }
        if (submitWithPopupMenu(anActionEvent)) {
            return
        }

        val operation = TelemetryManager.createOperation(getServiceName(anActionEvent), getOperationName(anActionEvent))
        operation.start()

        val runConfigurationSetting = anActionEvent.dataContext.getData(RUN_CONFIGURATION_SETTING) ?:
                getRunConfigurationFromDataContext(anActionEvent.dataContext) ?: return
        val clusterName = anActionEvent.dataContext.getData(CLUSTER)?.name
        val mainClassName = anActionEvent.dataContext.getData(MAIN_CLASS_NAME)

        submit(runConfigurationSetting, clusterName, mainClassName, operation)
    }

    override fun update(event: AnActionEvent) {
        super.update(event)

        event.presentation.isEnabledAndVisible = getRunConfigurationFromDataContext(event.dataContext) != null
    }

    private fun getRunConfigurationFromDataContext(dataContext: DataContext): RunnerAndConfigurationSettings? {
        val configContext = ConfigurationContext.getFromContext(dataContext)

        val existingConfig = configContext.findExisting()
        return if (existingConfig != null && existingConfig is LivySparkBatchJobRunConfiguration) existingConfig else configContext.configuration
    }

    private fun submit(runConfigurationSetting: RunnerAndConfigurationSettings,
                       clusterName: String?,
                       mainClassName: String?,
                       operation: Operation) {
        val executor = ExecutorRegistry.getInstance().getExecutorById(SparkBatchJobRunExecutor.EXECUTOR_ID)

        runConfigurationSetting.isEditBeforeRun = true

        val runConfiguration = runConfigurationSetting.configuration as? LivySparkBatchJobRunConfiguration ?: return
        val model = runConfiguration.model
        model.focusedTabIndex = 1   // Select remote job submission tab

        if (clusterName != null) {
            model.submitModel.submissionParameter.clusterName = clusterName     // Select the cluster
            model.isClusterSelectionEnabled = false
        } else {
            model.isClusterSelectionEnabled = true
        }

        if (mainClassName != null) {
            model.submitModel.submissionParameter.clusterName = mainClassName
        }

        model.isLocalRunConfigEnabled = false   // Disable local run configuration tab

        val environment = ExecutionEnvironmentBuilder.create(executor, runConfigurationSetting).build()
        RunConfigurationActionUtils.runEnvironmentProfileWithCheckSettings(environment, operation)

        // Restore for common run configuration editor
        runConfigurationSetting.isEditBeforeRun = false
        model.isLocalRunConfigEnabled = true
        if (clusterName != null) {
            model.isClusterSelectionEnabled = true
        }
    }

    override fun getServiceName(event: AnActionEvent): String? {
        return SelectSparkApplicationTypeAction.getSelectedSparkApplicationType().value
    }

    override fun getOperationName(event: AnActionEvent?): String {
        return TelemetryConstants.RUN_REMOTE_SPARK_JOB
    }
}

class LivySparkSelectAndSubmitAction : SeqActions("Actions.SelectHDInsightSparkType", "Actions.SubmitSparkApplicationAction")
class CosmosSparkSelectAndSubmitAction : SeqActions("Actions.SelectCosmosSparkType", "Actions.SubmitSparkApplicationAction")
class CosmosServerlessSparkSelectAndSubmitAction : SeqActions("Actions.SelectCosmosServerlessSparkType", "Actions.SubmitSparkApplicationAction")
class ArisSparkSelectAndSubmitAction : SeqActions("Actions.SelectArisSparkType", "Actions.SubmitSparkApplicationAction")