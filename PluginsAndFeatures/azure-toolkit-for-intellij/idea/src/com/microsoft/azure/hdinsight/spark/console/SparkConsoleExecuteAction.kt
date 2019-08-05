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

import com.intellij.execution.console.LanguageConsoleImpl
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys.EDITOR
import com.intellij.openapi.editor.ex.EditorEx
import com.intellij.openapi.project.DumbAware
import com.intellij.openapi.util.TextRange
import com.microsoft.azure.hdinsight.common.logger.ILogger
import com.microsoft.azure.hdinsight.spark.run.action.SelectSparkApplicationTypeAction
import com.microsoft.azuretools.ijidea.utility.AzureAnAction
import com.microsoft.azuretools.telemetrywrapper.Operation
import com.microsoft.intellij.util.runInWriteAction
import java.io.IOException
import java.nio.charset.StandardCharsets.UTF_8

class SparkConsoleExecuteAction() : AzureAnAction(), DumbAware, ILogger {
    override fun update(e: AnActionEvent) {
        val editor = e.getData(EDITOR) ?: return

        if (editor !is EditorEx) {
            e.presentation.isEnabled = false

            return
        }

        val console = SparkConsoleManager.get(editor)
        if (console == null) {
            e.presentation.isEnabled = false

            return
        }

        val isEnabled = !editor.isRendererMode && !(console.processHandler?.isProcessTerminated ?: true)

        e.presentation.isEnabled = isEnabled
    }

    override fun getServiceName(event: AnActionEvent?): String {
        return SelectSparkApplicationTypeAction.getSelectedSparkApplicationType().value
    }

    override fun onActionPerformed(actionEvent: AnActionEvent, operation: Operation?): Boolean {
        val editor = actionEvent.getData(EDITOR) ?: return true

        val consoleDetail = SparkConsoleManager.get(editor) ?: return true
        val outputStream = consoleDetail.processHandler?.processInput ?: return true

        val document = consoleDetail.console.editorDocument
        val text = document.text
        val langConsole = consoleDetail.console as? LanguageConsoleImpl

        runInWriteAction {
            val range = TextRange(0, document.textLength)

            editor.selectionModel.setSelection(range.startOffset, range.endOffset)
            langConsole?.addToHistory(range, langConsole.consoleEditor, true)
            consoleDetail.model.addToHistory(text)

            editor.caretModel.moveToOffset(0)
            editor.document.setText("")
        }

        // Send to process as a whole codes block
        val normalizedCodes = text.trimEnd() + "\n"
        try {
            outputStream.write(normalizedCodes.toByteArray(UTF_8))
            outputStream.flush()
        } catch (e : IOException) {
            log().debug("Write $normalizedCodes to stdin error", e)
        }

        consoleDetail.console.indexCodes(normalizedCodes)
        return true
    }
}