/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.intellij.util;

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
            throw new IllegalArgumentException("value type of checkbox column must be Boolean/boolean!");
        }
        final CheckboxHeaderMouseListener listener = new CheckboxHeaderMouseListener(table, checkboxColumnIndex);
        table.getTableHeader().addMouseListener(listener);
        final TableColumn column = table.getColumnModel().getColumn(checkboxColumnIndex);
        column.setHeaderRenderer(new ThreeStateCheckBoxRenderer());
        column.setHeaderValue(false);
    }

    private static class CheckboxHeaderMouseListener extends MouseAdapter implements TableModelListener {
        private final JTable table;
        private final int colIndex;
        private boolean updatingHeaderValue = false;

        public CheckboxHeaderMouseListener(final JTable table, final int columnIndex) {
            super();
            this.table = table;
            this.colIndex = columnIndex;
            this.table.getModel().addTableModelListener(this);
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

        /**
         * table model changed
         */
        @Override
        public void tableChanged(final TableModelEvent e) {
            if ((e.getColumn() == TableModelEvent.ALL_COLUMNS || e.getColumn() == this.colIndex)
                    && !this.updatingHeaderValue) {
                final TableModel model = this.table.getModel();
                this.updateHeaderValue(model);
            }
        }

        private void updateTableModel(final boolean headerValue) {
            final int rowCount = table.getModel().getRowCount();
            this.updatingHeaderValue = true;
            for (int rowIndex = 0; rowIndex < rowCount; rowIndex++) {
                table.getModel().setValueAt(headerValue, rowIndex, this.colIndex);
            }
            this.updatingHeaderValue = false;
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
    }
}
