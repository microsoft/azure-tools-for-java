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

import com.intellij.execution.Executor
import com.intellij.execution.configurations.*
import com.intellij.execution.runners.ExecutionEnvironment
import com.intellij.openapi.module.Module
import com.intellij.openapi.module.ModuleManager
import com.intellij.openapi.options.SettingsEditor
import com.intellij.openapi.project.Project
import com.microsoft.azure.hdinsight.common.ClusterManagerEx
import com.microsoft.azure.hdinsight.sdk.cluster.IClusterDetail
import com.microsoft.azure.hdinsight.sdk.common.livy.interactive.SparkSession
import com.microsoft.azure.hdinsight.spark.jobs.JobUtils
import com.microsoft.azure.hdinsight.spark.run.configuration.RemoteDebugRunConfiguration
import java.net.URI

class SparkScalaLivyConsoleRunConfiguration(project: Project,
                                            configurationFactory: SparkScalaLivyConsoleRunConfigurationFactory,
                                            batchRunConfiguration: RemoteDebugRunConfiguration?,
                                            name: String)
    : ModuleBasedConfiguration<RunConfigurationModule>(
        name, batchRunConfiguration?.configurationModule ?: RunConfigurationModule(project), configurationFactory)
{

    var clusterName = batchRunConfiguration?.submitModel?.submissionParameter?.clusterName
            ?: throw RuntimeConfigurationWarning("A Spark Run Configuration should be selected to start a console")

    private lateinit var cluster: IClusterDetail

    override fun getConfigurationEditor(): SettingsEditor<out RunConfiguration> =
            SparkScalaLivyConsoleRunConfigurationEditor()

    override fun getValidModules(): MutableCollection<Module> {
        return ModuleManager.getInstance(project).findModuleByName(project.name)?.let { mutableListOf(it) }
                ?: mutableListOf()
    }

    override fun getState(executor: Executor, env: ExecutionEnvironment): RunProfileState? {
        val session = SparkSession(
                name,
                URI.create(JobUtils.getLivyBaseUri(cluster).toString()),
                cluster.httpUserName,
                cluster.httpPassword)

        return SparkScalaLivyConsoleRunProfileState(SparkScalaConsoleBuilder(project), session)
    }

    override fun checkSettingsBeforeRun() {
        super.checkSettingsBeforeRun()

        cluster = ClusterManagerEx.getInstance().getClusterDetailByName(clusterName)
                .orElseThrow { RuntimeConfigurationError("Can't find the target cluster $clusterName") }
    }
}
