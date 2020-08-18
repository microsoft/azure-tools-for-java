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
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.util.TextRange
import com.microsoft.azure.hdinsight.common.logger.ILogger
import com.microsoft.azuretools.ijidea.utility.AzureAnAction
import com.microsoft.azuretools.telemetrywrapper.ErrorType
import com.microsoft.azuretools.telemetrywrapper.EventUtil
import com.microsoft.azuretools.telemetrywrapper.Operation
import com.microsoft.intellij.util.runInWriteAction
import org.jetbrains.plugins.scala.lang.psi.api.ScalaFile
import java.io.IOException
import java.nio.charset.StandardCharsets.UTF_8

class SparkSendSelectionToConsoleAction : AzureAnAction(), ILogger {
    override fun update(actionEvent: AnActionEvent) {
        val presentation = actionEvent.presentation

        fun setEnabled(v: Boolean) {
            presentation.isEnabled = v
            presentation.isVisible = v
        }

        setEnabled(false)

        try {
            val content = actionEvent.dataContext
            val file = (CommonDataKeys.PSI_FILE.getData(content) as? ScalaFile) ?: return

            val editor = CommonDataKeys.EDITOR.getData(content) ?: return
            if (!editor.selectionModel.hasSelection()) {
                return
            }

            val consoleDetail = SparkConsoleManager.get(project = file.projectContext()) ?: return
            if (consoleDetail.console.consoleEditor.isDisposed || consoleDetail.processHandler?.isProcessTerminated != false) {
                return
            }

            setEnabled(true)
        } catch (ex: Exception) {
            log().debug("Send to Apache Spark Console action is Disabled", ex)
        }
    }

    override fun onActionPerformed(actionEvent: AnActionEvent, operation: Operation?): Boolean {
        try {
            val content = actionEvent.dataContext
            val editor = CommonDataKeys.EDITOR.getData(content) ?: return true
            val project = CommonDataKeys.PROJECT.getData(content) ?: return true
            val selectedText = editor.selectionModel.selectedText ?: return true

            SparkConsoleManager.get(project = project)?.apply { sendSelection(this, selectedText, operation) }
        } catch (ignored: RuntimeException) {
        }

        return true
    }

    private fun sendSelection(consoleDetail: SparkConsoleDetail, text: String, operation: Operation?) {
        val outputStream = consoleDetail.processHandler?.processInput ?: return
        val consoleEditor = consoleDetail.console.consoleEditor
        consoleDetail.console.setInputText(text)

        runInWriteAction {
            val range = TextRange(0, consoleDetail.console.editorDocument.textLength)
            consoleEditor.selectionModel.setSelection(range.startOffset, range.endOffset)
            (consoleDetail.console as? LanguageConsoleImpl)?.addToHistory(range, consoleEditor, true)
            consoleDetail.model.addToHistory(text)

            consoleEditor.caretModel.moveToOffset(0)
            consoleEditor.document.setText("")
        }

        try {
            outputStream.write("$text\n".toByteArray(UTF_8))
            outputStream.flush()
        } catch (ex: Exception) {
            val errMsg = "Failed to send codes `$text` to Apache Spark Console"
            log().warn(errMsg, ex)
            EventUtil.logErrorClassNameOnlyWithComplete(operation, ErrorType.systemError, IOException(errMsg, ex), null, null)
        }

        consoleDetail.console.indexCodes(text)
    }
}