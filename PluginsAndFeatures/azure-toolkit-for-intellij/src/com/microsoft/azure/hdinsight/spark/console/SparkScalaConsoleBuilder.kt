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

import com.intellij.execution.filters.TextConsoleBuilderImpl
import com.intellij.execution.ui.ConsoleView
import com.intellij.openapi.project.Project
import org.jetbrains.plugins.scala.console.ScalaConsoleInfo
import org.jetbrains.plugins.scala.console.ScalaLanguageConsole
import org.jetbrains.plugins.scala.console.ScalaLanguageConsoleView

data class SparkContextValueInfo(val name: String, val master: String, val appId: String)
data class SparkSessionValueInfo(val name: String)

class SparkScalaConsoleBuilder(project: Project) : TextConsoleBuilderImpl(project) {
    public override fun getProject(): Project {
        return super.getProject()
    }

    private val sparkContextRegex = """^Spark context available as '(.+)' \(master = (.+), app id = (.+)\)\.\s*$""".toRegex()
    private val sparkSessionRegex = """^Spark session available as '(.+)'\.\s*$""".toRegex()

    private fun parseSparkContext(msg: String): SparkContextValueInfo? {
        return sparkContextRegex.matchEntire(msg)
                ?.destructured
                ?.let { (name, master, appId) -> SparkContextValueInfo(name, master, appId) }
    }

    private fun parseSparkSession(msg: String): SparkSessionValueInfo? {
        return sparkSessionRegex.matchEntire(msg)
                ?.destructured
                ?.let { (name) -> SparkSessionValueInfo(name) }
    }

    fun getSparkContextDeclareStatement(scVal: String) = "val $scVal: org.apache.spark.SparkContext\n"
    fun getSparkSessionDeclareStatement(sparkVal: String) = "val $sparkVal: org.apache.spark.sql.SparkSession\n"

    override fun getConsole(): ConsoleView {
        val consoleView = SparkScalaLivyConsole(project, "Spark Console (Scala)")

        ScalaConsoleInfo.setIsConsole(consoleView.file, true)
        consoleView.prompt = null
        consoleView.addMessageFilter { line, _ ->
            when {
                sparkContextRegex.matches(line) -> parseSparkContext(line)
                        ?.apply { consoleView.textSent(getSparkContextDeclareStatement(name)) }
                sparkSessionRegex.matches(line) -> parseSparkSession(line)
                        ?.apply { consoleView.textSent(getSparkSessionDeclareStatement(name)) }
            }

            // No highlight result
            null
        }

        return consoleView
    }
}