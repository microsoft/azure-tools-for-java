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

import com.intellij.util.ui.ColumnInfo
import com.intellij.util.ui.SortableColumnModel
import com.microsoft.azure.Page
import com.microsoft.azure.PagedList
import javax.swing.RowSorter
import javax.swing.table.AbstractTableModel

class LivyBatchJobTableModel(private val schema: UniqueColumnNameTableSchema? = null)
    : AbstractTableModel(), SortableColumnModel {
    interface JobPage : Page<UniqueColumnNameTableSchema.RowDescriptor> {
        override fun items(): List<UniqueColumnNameTableSchema.RowDescriptor>?
    }

    class JobPagedList : PagedList<UniqueColumnNameTableSchema.RowDescriptor>() {
        var firstJobPage : JobPage? = null
            set(value) {
                field = value

                clear()
                setCurrentPage(value)
            }

        override fun nextPage(nextPageLink: String?): Page<UniqueColumnNameTableSchema.RowDescriptor>? =
                fetchNextPage?.invoke(nextPageLink)

        var fetchNextPage: ((nexPageLink: String?) -> JobPage?)? = null

        // Override by an empty method since we won't like to load all jobs at once.
        // And all results of size(), toArray(), lastIndexOf() are the currently cached
        // jobs.
        override fun loadAll() { }
    }

    val pagedJobs = JobPagedList()

    /**
     * Methods from abstract class [AbstractTableModel] and interface [javax.swing.table.TableModel]
     */

    override fun getRowCount(): Int = pagedJobs.size

    override fun getColumnName(column: Int): String =
            if ((columnInfos[column] as? UniqueColumnNameTableSchema.HideableHeaderColumn)?.doesHide == true)
                ""
            else
                columnInfos[column].name

    override fun getColumnCount(): Int = columnInfos.size

    override fun getColumnClass(columnIndex: Int): Class<*> = columnInfos[columnIndex].columnClass

    override fun isCellEditable(rowIndex: Int, columnIndex: Int): Boolean {
        return columnInfos[columnIndex].isCellEditable(getJobDescriptor(rowIndex))
    }

    override fun getValueAt(rowIndex: Int, columnIndex: Int): Any? =
            schema?.columns?.get(columnIndex)?.valueOf(getJobDescriptor(rowIndex))

    /**
     * Methods to support sort operations interface [SortableColumnModel]
     */

    override fun isSortable(): Boolean = true

    override fun getColumnInfos(): Array<ColumnInfo<Any, Any>> =
            schema?.columns?.filterIsInstance<ColumnInfo<Any, Any>>()?.toTypedArray() ?: arrayOf()

    override fun setSortable(ignored: Boolean) { }

    override fun getDefaultSortKey(): RowSorter.SortKey? = null

    // For the row object as a job descriptor
    override fun getRowValue(row: Int): Any = getJobDescriptor(row)

    fun getJobDescriptor(row: Int): UniqueColumnNameTableSchema.RowDescriptor = pagedJobs[row]
}