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
import com.intellij.openapi.actionSystem.impl.ActionButton
import com.intellij.util.ui.AbstractTableCellEditor
import java.awt.Component
import java.awt.Dimension
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import java.awt.event.MouseEvent.*
import javax.swing.JTable
import javax.swing.table.TableCellRenderer

class ActionButtonTableCellRenderer : TableCellRenderer, AbstractTableCellEditor() {
    class CellMouseEvent(val row: Int, val column: Int, val event: MouseEvent) {
        fun isSameCell(otherRow: Int, otherColumn: Int): Boolean = (row == otherRow && column == otherColumn)
        fun isSameCell(other: CellMouseEvent?): Boolean = (other != null && isSameCell(other.row, other.column))
    }

    inner class RenderCellMouseListener(private val table: JTable) : MouseAdapter() {
        private var lastRow = -1
        private var lastColumn = -1

        private fun getActionCell(row: Int, column: Int): ActionButtonTableCellRenderer? {
            if (row < 0 || column < 0) {
                return null
            }

            return table.getCellRenderer(row, column) as? ActionButtonTableCellRenderer
        }

        private fun dispatchMouseEvent(event: MouseEvent?) {
            val mouseRow = table.rowAtPoint(event?.point ?: return)
            val mouseColumn = table.columnAtPoint(event.point ?: return)

            val currentActionCell = getActionCell(mouseRow, mouseColumn)

            currentEvent = if (currentActionCell != null && currentActionCell == this@ActionButtonTableCellRenderer) {
                CellMouseEvent(mouseRow, mouseColumn, event)
            } else {
                null
            }

            // need to force repainting here, since the RenderCell is not a real component with repainting
            table.repaint(table.getCellRect(lastRow, lastColumn, false))
            table.repaint(table.getCellRect(mouseRow, mouseColumn, false))

            lastRow = mouseRow
            lastColumn = mouseColumn
        }

        override fun mouseMoved(e: MouseEvent?) = dispatchMouseEvent(e)
        override fun mouseEntered(e: MouseEvent?) = dispatchMouseEvent(e)
        override fun mouseExited(e: MouseEvent?) = dispatchMouseEvent(e)
    }

    private var prevEvent: CellMouseEvent? = null
    var currentEvent: CellMouseEvent? = null
        set(value) {
            prevEvent = field
            field = value
        }

    private var mouseListener: RenderCellMouseListener? = null

    // Cell Render APIs
    override fun getTableCellRendererComponent(table: JTable?, value: Any?, isSelected: Boolean, hasFocus: Boolean, row: Int, column: Int): Component {
        if (table != null) {
            synchronized(table) {
                if (!(mouseListener in table.mouseListeners && mouseListener in table.mouseMotionListeners)) {
                    // Clean some old exists
                    table.removeMouseListener(mouseListener)
                    table.removeMouseMotionListener(mouseListener)

                    mouseListener = this.RenderCellMouseListener(table)

                    // Add the listener both
                    table.addMouseListener(mouseListener)
                    table.addMouseMotionListener(mouseListener)
                }
            }
        }

        // Reuse the editor cell component but with mouse events to update visual effects
        return getTableCellEditorComponent(table, value, isSelected, row, column).apply {
            val lastEvent = prevEvent
            val event = currentEvent?.event

            if ((lastEvent?.isSameCell(row, column) == true || currentEvent?.isSameCell(row, column) == true) &&
                    event != null) {
                // Hit the last or current events cell in scanning
                val buttonEventId =
                        when (event.id) {
                            MOUSE_MOVED ->
                                when {
                                    currentEvent?.isSameCell(lastEvent) == true -> MOUSE_ENTERED
                                    currentEvent?.isSameCell(row, column) == true -> MOUSE_ENTERED
                                    else -> MOUSE_EXITED
                                }

                            MOUSE_ENTERED, MOUSE_EXITED ->
                                if (currentEvent?.isSameCell(row, column) == true) {
                                    event.id
                                } else {
                                    MOUSE_EXITED
                                }

                            else -> return@apply  // Not dispatch other events
                        }

                val buttonEvent = MouseEvent(
                        this,
                        buttonEventId,
                        event.`when`,
                        event.modifiers,
                        event.x,
                        event.y,
                        event.xOnScreen,
                        event.yOnScreen,
                        event.clickCount,
                        event.isPopupTrigger,
                        event.button
                )

                // The mouse events will trigger repainting
                dispatchEvent(buttonEvent)
            }
        }
    }

    // Cell Editor APIs
    private lateinit var actionButton: ActionButton
    override fun getTableCellEditorComponent(table: JTable?, value: Any?, isSelected: Boolean, row: Int, column: Int): Component {
        val action = value as AnAction

        actionButton = ActionButton(
                action, action.templatePresentation.clone(), "Table $table cell $row:$column", Dimension(13, 13))

        table?.columnModel?.getColumn(column)?.apply {
            preferredWidth = 24
            maxWidth = preferredWidth
        }

        return actionButton
    }

    override fun getCellEditorValue(): Any {
        return actionButton.action
    }
}