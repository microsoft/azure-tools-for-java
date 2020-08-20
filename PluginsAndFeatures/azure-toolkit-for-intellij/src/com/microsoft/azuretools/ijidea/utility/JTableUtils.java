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
import javax.swing.table.JTableHeader;
import javax.swing.table.TableColumn;
import javax.swing.table.TableModel;
import java.awt.event.*;
import java.util.Objects;

public class JTableUtils {
    public static void enableBatchSelection(JTable table, int checkboxColumnIndex) {
        final TableColumn column = table.getColumnModel().getColumn(checkboxColumnIndex);
        final CheckBoxModelListener listener = new CheckBoxModelListener(table, checkboxColumnIndex);
        column.setHeaderValue(true);
        column.setHeaderRenderer(new ThreeStateCheckBoxRenderer());
        table.getModel().addTableModelListener(listener);
    }

    private static class CheckBoxModelListener extends MouseAdapter implements TableModelListener {
        private final JTable table;
        private final int colIndex;
        private boolean changingHeaderValue = false;

        public CheckBoxModelListener(final JTable table, final int columnIndex) {
            super();
            this.table = table;
            this.colIndex = columnIndex;
            final JTableHeader header = table.getTableHeader();
            header.addMouseListener(this);
        }

        @Override
        public void tableChanged(final TableModelEvent e) {
            final int column = e.getColumn();
            final TableModel model = (TableModel) e.getSource();
            if (column == this.colIndex && !this.changingHeaderValue) {
                int numSelected = 0;
                for (int row = 0; row < model.getRowCount(); row++) {
                    if ((Boolean) model.getValueAt(row, this.colIndex)) {
                        numSelected++;
                    }
                }
                this.updateHeaderState(numSelected);
            }
        }

        private void updateHeaderState(final int numSelected) {
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

        @Override
        public void mouseClicked(MouseEvent e) {
            final int columnIndex = this.table.columnAtPoint(e.getPoint());

            if (columnIndex == this.colIndex) {
                final TableColumn checkboxColumn = table.getColumnModel().getColumn(this.colIndex);
                final Object value = checkboxColumn.getHeaderValue();
                final boolean checked = Objects.nonNull(value) && (boolean) value;
                final int rowCount = table.getModel().getRowCount();
                this.changingHeaderValue = true;
                checkboxColumn.setHeaderValue(!checked);
                for (int rowIndex = 0; rowIndex < rowCount; rowIndex++) {
                    table.getModel().setValueAt(!checked, rowIndex, this.colIndex);
                }
                this.changingHeaderValue = false;
            }
            this.table.getTableHeader().repaint();
        }
    }
}
