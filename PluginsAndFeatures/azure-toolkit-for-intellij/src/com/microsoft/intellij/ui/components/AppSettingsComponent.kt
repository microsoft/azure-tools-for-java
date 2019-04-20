/**
 * Copyright (c) 2019 JetBrains s.r.o.
 * <p/>
 * All rights reserved.
 * <p/>
 * MIT License
 * <p/>
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated
 * documentation files (the "Software"), to deal in the Software without restriction, including without limitation
 * the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and
 * to permit persons to whom the Software is furnished to do so, subject to the following conditions:
 * <p/>
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of
 * the Software.
 * <p/>
 * THE SOFTWARE IS PROVIDED *AS IS*, WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO
 * THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT,
 * TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package com.microsoft.intellij.ui.components

import com.intellij.icons.AllIcons
import com.intellij.openapi.actionSystem.ActionToolbarPosition
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.ui.AnActionButton
import com.intellij.ui.ToolbarDecorator
import com.intellij.ui.table.JBTable
import net.miginfocom.swing.MigLayout
import javax.swing.JPanel
import javax.swing.ListSelectionModel
import javax.swing.table.DefaultTableModel

class AppSettingsComponent :
        JPanel(MigLayout("novisualpadding, ins 0, fillx, wrap 1")) {

    companion object {
        private const val BUTTON_EDIT = "Edit"
        private const val BUTTON_REMOVE = "Remove"
        private const val BUTTON_ADD = "Add"
        private const val TABLE_HEADER_VALUE = "Value"
        private const val TABLE_HEADER_KEY = "Key"
        private const val TABLE_LOADING_MESSAGE = "Loading ... "
    }

    private val pnlAppTable: JPanel

    val table = initAppSettingsTable()
    val btnAdd = initAddButton()
    val btnDelete = initDeleteButton()
    var deleteButtonAction = {}
    val btnEdit = initEditButton()

    val cachedAppSettings = mutableMapOf<String, String>()
    val editedAppSettings = mutableMapOf<String, String>()

    init {
        pnlAppTable = ToolbarDecorator
                .createDecorator(table)
                .addExtraActions(btnAdd, btnDelete, btnEdit)
                .setToolbarPosition(ActionToolbarPosition.RIGHT)
                .createPanel()

        apply {
            add(pnlAppTable, "growx")
        }
    }

    private fun initAppSettingsTable(): JBTable {

        val tableModel = DefaultTableModel()
        tableModel.addColumn(TABLE_HEADER_KEY)
        tableModel.addColumn(TABLE_HEADER_VALUE)

        val table = JBTable(tableModel)
        table.emptyText.text = TABLE_LOADING_MESSAGE
        table.rowSelectionAllowed = true
        table.setShowGrid(false)
        table.isStriped = true
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION)

        table.addPropertyChangeListener { event ->
            if ("tableCellEditor" != event.propertyName) return@addPropertyChangeListener

            if (table.isEditing) {
                updateTableActionBtnStatus(true)
                return@addPropertyChangeListener
            }

            editedAppSettings.clear()
            var row = 0
            while (row < tableModel.rowCount) {
                val keyObj = tableModel.getValueAt(row, 0)
                var key = ""
                var value = ""
                if (keyObj != null) {
                    key = keyObj as String
                }
                if (key.isEmpty() || editedAppSettings.containsKey(key)) {
                    tableModel.removeRow(row)
                    continue
                }
                val valueObj = tableModel.getValueAt(row, 1)
                if (valueObj != null) {
                    value = valueObj as String
                }
                editedAppSettings[key] = value
                row++
            }
            updateTableActionBtnStatus(false)
        }

        return table
    }

    private fun initAddButton() =
            object : AnActionButton(BUTTON_ADD, AllIcons.General.Add) {
                override fun actionPerformed(anActionEvent: AnActionEvent) {
                    if (table.isEditing) {
                        table.cellEditor.stopCellEditing()
                    }

                    val tableModel = table.model as DefaultTableModel

                    tableModel.addRow(arrayOf("", ""))
                    table.editCellAt(table.rowCount - 1, 0)
                }
            }

    private fun initDeleteButton() =
            object : AnActionButton(BUTTON_REMOVE, AllIcons.General.Remove) {
                override fun actionPerformed(anActionEvent: AnActionEvent) {
                    val selectedRow = table.selectedRow
                    if (selectedRow == -1) return

                    val tableModel = table.model as DefaultTableModel
                    editedAppSettings.remove(tableModel.getValueAt(selectedRow, 0))
                    tableModel.removeRow(selectedRow)
                    deleteButtonAction()
                }
            }

    private fun initEditButton() =
            object : AnActionButton(BUTTON_EDIT, AllIcons.Actions.Edit) {
                override fun actionPerformed(anActionEvent: AnActionEvent) {
                    val selectedRow = table.selectedRow
                    val selectedCol = table.selectedColumn
                    if (selectedRow == -1 || selectedCol == -1) {
                        return
                    }
                    table.editCellAt(selectedRow, selectedCol)
                }
            }

    private fun updateTableActionBtnStatus(isEditing: Boolean) {
        btnAdd.isEnabled = !isEditing
        btnDelete.isEnabled = !isEditing
        btnEdit.isEnabled = !isEditing
    }
}