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

import com.intellij.openapi.actionSystem.AnAction
import com.intellij.util.ui.ColumnInfo
import javax.swing.table.TableCellEditor
import javax.swing.table.TableCellRenderer

open class UniqueColumnNameTableSchema(val columns: Array<UniqueColumnInfo<out Any>>) {
    open inner class RowDescriptor(vararg kvPairs: Pair<String, Any?>)
        : AbstractMap<String, Any?>() {

        init {
            // validation check for Key existing in column definitions
            for (columnName in kvPairs.map { it.first }) {
                if (columnName !in columns.map { it.name })
                throw NoSuchElementException("Can't found column name '$columnName' in defined schema columns")
            }
        }

        private val entriesSet = kvPairs.map {
                object : Map.Entry<String, Any?> {
                    override val key = it.first
                    override val value = it.second
                }
            }.toSet()

        override val entries: Set<Map.Entry<String, Any?>>
            get() = entriesSet

    }

    interface HideableHeaderColumn {
        var doesHide: Boolean
    }

    abstract class UniqueColumnInfo<T>(name: String) : ColumnInfo<RowDescriptor, T>(name)

    open class PlainColumnInfo(name: String) : UniqueColumnInfo<String>(name) {
        override fun valueOf(jobDesc: RowDescriptor?): String? = jobDesc?.get(name)?.toString()
    }

    class ActionColumnInfo(name: String) : UniqueColumnInfo<AnAction>(name), HideableHeaderColumn {
        override var doesHide: Boolean = true

        // Share a renderer per column
        private val renderer = ActionButtonTableCellRenderer()

        override fun valueOf(jobDesc: RowDescriptor?): AnAction? = jobDesc?.get(name) as? AnAction

        override fun getRenderer(jobDesc: RowDescriptor?): TableCellRenderer? = valueOf(jobDesc)?.let { renderer }

        override fun getEditor(jobDesc: RowDescriptor?): TableCellEditor? = valueOf(jobDesc)?.let { renderer }

        override fun getColumnClass(): Class<*> = AnAction::class.java

        override fun isCellEditable(item: RowDescriptor?): Boolean = true
    }
}