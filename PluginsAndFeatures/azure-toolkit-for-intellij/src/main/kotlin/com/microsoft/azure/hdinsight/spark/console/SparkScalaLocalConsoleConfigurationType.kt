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

import com.intellij.execution.configurations.ConfigurationTypeUtil
import org.jetbrains.plugins.scala.console.configuration.ScalaConsoleConfigurationType
import org.jetbrains.plugins.scala.console.configuration.ScalaConsoleRunConfigurationFactory

class SparkScalaLocalConsoleConfigurationType : ScalaConsoleConfigurationType() {
    companion object {
        @JvmStatic
        val instance by lazy { ConfigurationTypeUtil.findConfigurationType(SparkScalaLocalConsoleConfigurationType::class.java) }
    }
    fun sparkLocalConfFactory(): ScalaConsoleRunConfigurationFactory = SparkScalaLocalConsoleRunConfigurationFactory(this)

    override fun getDisplayName(): String = "Spark Local Console(Scala)"

    override fun getConfigurationTypeDescription(): String = "Apache Spark local console(Scala) run configurations"

    override fun getId(): String = "SparkScalaLocalConsoleRunConfiguration"
}
