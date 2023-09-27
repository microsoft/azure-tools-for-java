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
import com.intellij.execution.ui.ConsoleViewContentType
import com.intellij.openapi.module.Module
import org.jetbrains.plugins.scala.console.ScalaConsoleInfo
import org.jetbrains.plugins.scala.console.ScalaLanguageConsole

class SparkScalaLivyConsole(module: Module) : ScalaLanguageConsole(module), SparkConsole {
    override fun indexCodes(codes: String) {
        super.textSent(codes)
    }

    override fun print(text: String, contentType: ConsoleViewContentType){
        super.print(text,contentType)
        SparkConsoleExecuteAction.UPDATE_PROMPT()
    }

    override fun attachToProcess(processHandler: ProcessHandler) {
        super.attachToProcess(processHandler)

        // Add to Spark Console Manager
        val controller = ConsoleHistoryController.getController(this)
            ?: throw RuntimeException("Can't find controller for Livy console from console history controller")

        // Remove self from ScalaConsoleInfo amend method ScalaConsoleInfo.disposeConsole(this)
        ScalaConsoleInfo.addConsole(this,controller,processHandler)

        SparkConsoleManager.add(this, controller, processHandler)
    }
}
