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

import com.intellij.execution.configurations.RunConfiguration
import com.intellij.openapi.module.ModuleManager
import com.intellij.openapi.project.Project
import com.microsoft.azure.hdinsight.spark.run.SparkLocalRun
import com.microsoft.azure.hdinsight.spark.run.configuration.LivySparkBatchJobRunConfiguration
import org.jetbrains.plugins.scala.console.configuration.ScalaConsoleRunConfigurationFactory

class SparkScalaLocalConsoleRunConfigurationFactory(sparkConsoleType: SparkScalaLocalConsoleConfigurationType)
    : ScalaConsoleRunConfigurationFactory(sparkConsoleType) {
    override fun createTemplateConfiguration(project: Project): RunConfiguration {
        return SparkScalaLocalConsoleRunConfiguration(project, this, "")
    }

    override fun createConfiguration(name: String?, template: RunConfiguration): RunConfiguration {
        // Create a Spark Scala Console run configuration based on Spark Batch run configuration
        val configuration = createTemplateConfiguration(template.project) as SparkScalaLocalConsoleRunConfiguration
        configuration.batchRunConfiguration = template as? LivySparkBatchJobRunConfiguration
                ?: throw UnsupportedOperationException("Spark Local Console doesn't support starting from the configuration ${template.name}(type: ${template.type.displayName})")

        configuration.name = "${template.name} >> Spark Local Console(Scala)"
        val moduleManager = ModuleManager.getInstance(template.project)

        val localClassModule = template.model.localRunConfigurableModel?.classpathModule
        configuration.setModule(localClassModule?.let { moduleManager.findModuleByName(it) }
                ?: SparkLocalRun.defaultModule(template.project))

        return configuration
    }
}
