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

import com.intellij.ui.components.JBScrollPane
import com.intellij.ui.table.JBTable
import com.intellij.util.ui.ColumnInfo
import com.intellij.util.ui.UIUtil
import com.microsoft.azure.hdinsight.common.mvc.IdeaSettableControlView
import com.microsoft.azure.hdinsight.spark.ui.livy.batch.LivyBatchJobTableModel.*
import java.awt.Component
import java.awt.Graphics
import java.awt.Point
import javax.swing.ListSelectionModel.SINGLE_SELECTION
import javax.swing.table.TableCellEditor
import javax.swing.table.TableCellRenderer

abstract class LivyBatchJobTableViewport : IdeaSettableControlView<LivyBatchJobTableViewport.Model> {
    data class Model(
            var tableModel: LivyBatchJobTableModel = LivyBatchJobTableModel(),
            var firstJobPage: JobPage? = null
    )

    interface Control {
        /**
         * Event handler for Job table row selected
         *
         * @param jobSelected selected job descriptor, null for deselected
         */
        fun onJobSelected(jobSelected: UniqueColumnNameTableSchema.RowDescriptor?)


        /**
         * Event handler for Job table to get next page
         *
         * @param nextPageLink next page link to get, null for end of pages
         */
        fun onNextPage(nextPageLink: String?): JobPage?
    }

    abstract val viewportControl: Control

    inner class LivyBatchJobTable: JBTable(LivyBatchJobTableModel()) {
        init {
            this.setSelectionMode(SINGLE_SELECTION)
            selectionModel.addListSelectionListener {
                val jobSelected = if (selectedRow >= 0) jobTableModel.getJobDescriptor(selectedRow) else null

                viewportControl.onJobSelected(jobSelected)
            }
        }

        val jobTableModel: LivyBatchJobTableModel
            get() = model as? LivyBatchJobTableModel
                    ?: throw IllegalArgumentException("LivyBatchJobTable only supports LivyBatchJobTableModel")


        fun getColumnInfoAt(column: Int): ColumnInfo<Any, Any> = jobTableModel.columnInfos[column]

        // Override getCellEditor method since no need to edit the job table, but needs to perform actions
        override fun getCellEditor(row: Int, column: Int): TableCellEditor {
            return getColumnInfoAt(column).getEditor(jobTableModel.getJobDescriptor(row))
                    ?: super.getCellEditor(row, column)
        }

        override fun getCellRenderer(row: Int, column: Int): TableCellRenderer {
            return getColumnInfoAt(column).getRenderer(jobTableModel.getJobDescriptor(row))
                    ?: super.getCellRenderer(row, column)
        }

        override fun paint(g: Graphics) {
            super.paint(g)
            UIUtil.fixOSXEditorBackground(this)
        }
    }

    private val table = LivyBatchJobTable()
    private val scrollableTable = JBScrollPane(table).apply {
        viewport.addChangeListener { _ ->
            val viewRect = viewport.viewRect
            val firstRowInView = table.rowAtPoint(Point(0, viewRect.y))
            if (firstRowInView < 0) {
                return@addChangeListener
            }

            val lastTableRow = getModel(Model::class.java).tableModel.rowCount - 1
            val lastRowInView = table.rowAtPoint(Point(0, viewRect.y + viewRect.height - 1)).takeIf { it >=0 }
                ?: lastTableRow

            if (lastRowInView == lastTableRow) {
                (table.model as? LivyBatchJobTableModel)?.pagedJobs?.apply {
                    if (hasNextPage()) {
                        loadNextPage()
                    }
                }
            }
        }
    }

    val component: Component = scrollableTable

    override fun setDataInDispatch(from: Model) {
        if (table.model != from.tableModel) {
            (table.model as? LivyBatchJobTableModel)?.pagedJobs?.fetchNextPage = null

            table.model = from.tableModel.apply {
                // TODO: improve the pagination user experiences by async
                // Currently, the PagedList implementation will fetch the following pages as much as possible
                // after set the firstJobPage. For better user experiences, the pages loading can be async
                // and one by one.
                pagedJobs.fetchNextPage = { viewportControl.onNextPage(it) }
                pagedJobs.firstJobPage = from.firstJobPage
            }
        }
    }

    override fun getData(to: Model) {
        to.tableModel = table.jobTableModel
    }
}
