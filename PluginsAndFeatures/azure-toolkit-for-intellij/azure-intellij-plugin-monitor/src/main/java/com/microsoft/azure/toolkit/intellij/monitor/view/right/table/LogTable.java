/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.monitor.view.right.table;

import com.azure.monitor.query.models.LogsTableRow;
import com.intellij.ui.table.JBTable;
import com.microsoft.azure.toolkit.intellij.common.CommonConst;
import lombok.Getter;
import org.apache.commons.lang3.StringUtils;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.swing.*;
import javax.swing.table.TableModel;
import javax.swing.table.TableRowSorter;
import java.awt.*;
import java.util.List;
import java.util.Optional;

public class LogTable extends JBTable {
    @Getter
    private LogTableModel logTableModel = new LogTableModel();
    public LogTable() {
        super();
        this.setModel(logTableModel);
        this.setCellSelectionEnabled(true);
        this.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        this.setAutoscrolls(true);
        this.setAutoResizeMode(JBTable.AUTO_RESIZE_OFF);
        this.setPreferredScrollableViewportSize(null);
        this.setAutoCreateRowSorter(true);
    }

    @Override
    @Nullable
    public String getColumnName(int column) {
        return isValidColumnIndex(column) ? super.getColumnName(column) : null;
    }

    @Nullable
    @Override
    public Object getValueAt(int row, int column) {
        return isValidRowIndex(row) && isValidColumnIndex(column) ? super.getValueAt(row, column) : null;
    }

    public void setModel(List<LogsTableRow> logsTableRows) {
        logTableModel = new LogTableModel(logsTableRows);
        this.setModel(logTableModel);
        this.setColumnWidth();
    }

    public void clearModel() {
        this.logTableModel = new LogTableModel();
        this.setModel(logTableModel);
    }

    public void setLoading(boolean isLoading) {
        this.setEnabled(!isLoading);
        if (isLoading) {
            this.getEmptyText().setText(CommonConst.LOADING_TEXT);
        } else {
            this.getEmptyText().setText("No results found");
        }
    }

    public void filter(String stringToFilter) {
        final RowFilter<TableModel, Object> rf;
        try {
            rf = RowFilter.regexFilter("(?i)" + stringToFilter);
        } catch (final java.util.regex.PatternSyntaxException e) {
            return;
        }
        if (this.getRowSorter() instanceof TableRowSorter) {
            ((TableRowSorter<? extends TableModel>) this.getRowSorter()).setRowFilter(rf);
        }
    }

    private void setColumnWidth() {
        final int columnSize = this.getColumnCount();
        for (int columnIndex = 0; columnIndex < columnSize; columnIndex++) {
            final String columnName = Optional.ofNullable(this.getColumnName(columnIndex)).orElse(StringUtils.EMPTY);
            final int width = Math.max(getStringWidth(columnName), getColumnWidth(columnIndex));
            columnModel.getColumn(columnIndex).setPreferredWidth(width);
        }
    }

    private int getStringWidth(@Nonnull String columnName) {
        final FontMetrics fontMetrics = getFontMetrics(getFont());
        return fontMetrics.stringWidth(columnName) + 20;
    }

    private int getColumnWidth(int index) {
        return columnModel.getColumn(index).getWidth();
    }

    private boolean isValidColumnIndex(int columnIndex) {
        return columnIndex >=0 && columnIndex < logTableModel.getColumnCount();
    }

    private boolean isValidRowIndex(int rowIndex) {
        return rowIndex >=0 && rowIndex < logTableModel.getRowCount();
    }
}
