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

import com.intellij.execution.console.ConsoleHistoryController
import com.intellij.execution.process.ProcessHandler
import com.intellij.execution.ui.RunContentManager
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project

data class SparkConsoleDetail(
        val console: SparkConsole,
        val model: ConsoleHistoryController,
        val processHandler: ProcessHandler?)

object SparkConsoleManager {
    private val allProjectConsoles = mutableMapOf<Project, MutableSet<SparkConsoleDetail>>()

    fun add(console: SparkConsole, model: ConsoleHistoryController, processHandler: ProcessHandler?) {
        val project = console.project

        synchronized(allProjectConsoles) {
            allProjectConsoles.getOrPut(project) { mutableSetOf() }
                    .add(SparkConsoleDetail(console, model, processHandler))
        }
    }

    fun remove(console: SparkConsole) {
        val project = console.project

        synchronized(allProjectConsoles) {
            allProjectConsoles[project]?.removeIf { it.console == console }
        }
    }

    // Get current selected Spark Console with the predicate condition
    fun get(project: Project, predicate: (SparkConsoleDetail) -> Boolean = { true }): SparkConsoleDetail? {
        synchronized(allProjectConsoles) {
            return allProjectConsoles[project]?.asSequence()
                    ?.filter(predicate)
                    ?.find { isSelected(it) }
        }
    }

    fun isSelected(consoleDetail: SparkConsoleDetail?): Boolean {
        val console = consoleDetail?.console ?: return false

        return RunContentManager.getInstance(console.project).selectedContent?.executionConsole == console
    }

    fun get(editor: Editor): SparkConsoleDetail? {
        val project = editor.project ?: return null

        return get(project) { it.console.consoleEditor == editor }
    }
}