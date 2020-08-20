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

package com.microsoft.azuretools.ijidea.utility;

import com.intellij.profile.codeInspection.ui.table.ThreeStateCheckBoxRenderer;

import javax.swing.*;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;
import javax.swing.table.*;
import java.awt.event.*;
import java.util.Objects;

public class JTableUtils {
    public static void enableBatchSelection(JTable table, int checkboxColumnIndex) {
        final Class<?> columnClass = table.getModel().getColumnClass(checkboxColumnIndex);
        if (columnClass != Boolean.class && columnClass != boolean.class) {
            throw new AssertionError("value type of checkbox column must be Boolean/boolean!");
        }
        final TableColumn column = table.getColumnModel().getColumn(checkboxColumnIndex);
        final BatchSelectableTableModelListener listener = new BatchSelectableTableModelListener(table, checkboxColumnIndex);
        table.getModel().addTableModelListener(listener);
        column.setHeaderRenderer(new ThreeStateCheckBoxRenderer());
        column.setHeaderValue(false);
    }

    private static class BatchSelectableTableModelListener extends MouseAdapter implements TableModelListener {
        private final JTable table;
        private final int colIndex;
        private boolean updatingHeaderValue = false;

        public BatchSelectableTableModelListener(final JTable table, final int columnIndex) {
            super();
            this.table = table;
            this.colIndex = columnIndex;
            final JTableHeader header = table.getTableHeader();
            header.addMouseListener(this);
        }

        /**
         * table model changed
         */
        @Override
        public void tableChanged(final TableModelEvent e) {
            if ((e.getColumn() == TableModelEvent.ALL_COLUMNS || e.getColumn() == this.colIndex) && !this.updatingHeaderValue) {
                final TableModel model = this.table.getModel();
                this.updateHeaderValue(model);
            }
        }

        /**
         * table header clicked
         */
        @Override
        public void mouseClicked(MouseEvent e) {
            final int columnIndex = this.table.columnAtPoint(e.getPoint());
            if (columnIndex == this.colIndex) {
                final TableColumn checkboxColumn = this.table.getColumnModel().getColumn(this.colIndex);
                final Object value = checkboxColumn.getHeaderValue();
                final boolean checked = Objects.nonNull(value) && (boolean) value;
                checkboxColumn.setHeaderValue(!checked);
                this.updateTableModel(!checked);
                this.table.getTableHeader().repaint();
            }
        }

        private void updateHeaderValue(final TableModel model) {
            int numSelected = 0;
            for (int row = 0; row < model.getRowCount(); row++) {
                if ((Boolean) model.getValueAt(row, this.colIndex)) {
                    numSelected++;
                }
            }
            final TableColumn column = table.getColumnModel().getColumn(this.colIndex);
            if (numSelected == this.table.getModel().getRowCount()) {
                column.setHeaderValue(true);
            } else if (numSelected == 0) {
                column.setHeaderValue(false);
            } else {
                column.setHeaderValue(null);
            }
            this.table.getTableHeader().repaint();
        }

        private void updateTableModel(final boolean headerValue) {
            final int rowCount = table.getModel().getRowCount();
            this.updatingHeaderValue = true;
            for (int rowIndex = 0; rowIndex < rowCount; rowIndex++) {
                table.getModel().setValueAt(headerValue, rowIndex, this.colIndex);
            }
            this.updatingHeaderValue = false;
        }
    }
}
