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

package com.microsoft.azure.hdinsight.spark.ui.livy.batch

import com.intellij.openapi.Disposable
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.application.runWriteAction
import com.intellij.openapi.command.undo.UndoUtil
import com.intellij.openapi.editor.EditorFactory
import com.intellij.openapi.editor.EditorKind
import com.microsoft.azure.hdinsight.common.mvc.IdeaSettableControlView
import javax.swing.JComponent
import javax.swing.JSplitPane
import javax.swing.JSplitPane.HORIZONTAL_SPLIT

abstract class LivyBatchJobViewer : Disposable, IdeaSettableControlView<LivyBatchJobViewer.Model> {
    data class Model(
            var tableViewportModel: LivyBatchJobTableViewport.Model = LivyBatchJobTableViewport.Model(),
            var jobDetail: String? = null
    )

    interface Control : LivyBatchJobTableViewport.Control

    private val jobDetailNotSetMessage = "<Click the job item row to get details>"

    val jobTableViewport : LivyBatchJobTableViewport = object : LivyBatchJobTableViewport() {
        override val viewportControl: Control by lazy { jobViewerControl }
    }

    private val jobDetailDocument = EditorFactory.getInstance().createDocument(jobDetailNotSetMessage).apply {
        UndoUtil.disableUndoFor(this)
    }
    private val jobDetailViewer = EditorFactory.getInstance().createViewer(jobDetailDocument, null, EditorKind.MAIN_EDITOR)

    abstract val jobViewerControl : Control

    open fun refreshActionPerformed(anActionEvent: AnActionEvent?) {}

    private val jobTableViewerWithToolBar = LivyBatchJobTableViewerWithToolBar(jobTableViewport, this::refreshActionPerformed)

    val component: JComponent by lazy {
        JSplitPane(HORIZONTAL_SPLIT, jobTableViewerWithToolBar, jobDetailViewer.component).apply {
            dividerSize = 6
            dividerLocation = 600
        }
    }

    override fun dispose() {
        EditorFactory.getInstance().releaseEditor(jobDetailViewer)
    }

    override fun getData(to: Model) {
        jobTableViewport.getData(to.tableViewportModel)
        to.jobDetail = jobDetailDocument.text
    }

    override fun setDataInDispatch(from: Model) {
        jobTableViewport.setDataInDispatch(from.tableViewportModel)

        runWriteAction {
            jobDetailDocument.setText(from.jobDetail ?: jobDetailNotSetMessage)
        }
    }
}