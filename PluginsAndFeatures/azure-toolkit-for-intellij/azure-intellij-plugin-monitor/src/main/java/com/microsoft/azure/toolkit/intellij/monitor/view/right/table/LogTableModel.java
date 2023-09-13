/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.monitor.view.right.table;

import com.azure.monitor.query.models.LogsColumnType;
import com.azure.monitor.query.models.LogsTableCell;
import com.azure.monitor.query.models.LogsTableRow;
import com.microsoft.azure.toolkit.lib.common.task.AzureTaskManager;
import lombok.Getter;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.Nls;

import javax.annotation.Nullable;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;
import javax.swing.table.TableModel;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

import static com.microsoft.azure.toolkit.intellij.common.AzureBundle.message;

public class LogTableModel implements TableModel {
    @Getter
    private final List<String> columnNames = new ArrayList<>();
    private final List<LogsColumnType> columnClasses = new ArrayList<>();
    @Getter
    private final List<LogsTableRow> logsTableRows = new ArrayList<>();
    private final List<TableModelListener> tableModelListenerList = new ArrayList<>();


    public LogTableModel() {
    }

    public LogTableModel(List<LogsTableRow> logsTableRows ) {
        this.columnClasses.addAll(logsTableRows.get(0).getRow().stream().map(LogsTableCell::getColumnType).collect(Collectors.toList()));
        this.columnNames.addAll(logsTableRows.get(0).getRow().stream().map(LogsTableCell::getColumnName).collect(Collectors.toList()));
        this.logsTableRows.addAll(logsTableRows);
    }

    @Override
    public int getRowCount() {
        return this.logsTableRows.size();
    }

    @Override
    public int getColumnCount() {
        return this.columnNames.size();
    }

    @Nls
    @Override
    public String getColumnName(int columnIndex) {
        final String columnName = this.columnNames.get(columnIndex);
        if (Objects.equals(this.columnClasses.get(columnIndex), LogsColumnType.DATETIME)) {
            return String.format("%s(UTC)", columnName);
        }
        return columnName;
    }

    @Override
    public Class<?> getColumnClass(int columnIndex) {
        final LogsColumnType type = this.columnClasses.get(columnIndex);
        if (LogsColumnType.BOOL.equals(type)) {
            return Boolean.class;
        }
        if (LogsColumnType.INT.equals(type)) {
            return Integer.class;
        }
        if (LogsColumnType.LONG.equals(type)) {
            return Long.class;
        }
        return String.class;
    }

    @Override
    public boolean isCellEditable(int rowIndex, int columnIndex) {
        return false;
    }

    @Override
    @Nullable
    public Object getValueAt(int rowIndex, int columnIndex) {
        if (isRowInvalid(rowIndex)) {
            return null;
        }
        final LogsColumnType type = this.columnClasses.get(columnIndex);
        if (LogsColumnType.BOOL.equals(type)) {
            return this.logsTableRows.get(rowIndex).getRow().get(columnIndex).getValueAsBoolean();
        }
        if (LogsColumnType.INT.equals(type)) {
            return this.logsTableRows.get(rowIndex).getRow().get(columnIndex).getValueAsInteger();
        }
        if (LogsColumnType.LONG.equals(type)) {
            return this.logsTableRows.get(rowIndex).getRow().get(columnIndex).getValueAsLong();
        }
        if (LogsColumnType.DATETIME.equals(type)) {
            final OffsetDateTime dateTime = this.logsTableRows.get(rowIndex).getRow().get(columnIndex).getValueAsDateTime();
            final DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.n a");
            return Optional.ofNullable(dateTime).map(d -> dateTime.format(dateTimeFormatter)).orElse(StringUtils.EMPTY);
        }
        return this.logsTableRows.get(rowIndex).getRow().get(columnIndex).getValueAsString();
    }

    @Override
    public void setValueAt(Object value, int rowIndex, int columnIndex) {
        if (value != null && !(value instanceof String)) {
            throw new IllegalArgumentException(message("function.appSettings.validate.illegalType"));
        }
        while (rowIndex >= this.logsTableRows.size()) {
            final LogsTableCell newCell = new LogsTableCell(columnNames.get(columnIndex), LogsColumnType.STRING, columnIndex, rowIndex, value);
            final LogsTableRow newRow = new LogsTableRow(rowIndex, List.of(newCell));
            this.logsTableRows.add(newRow);
        }
        this.logsTableRows.get(rowIndex).getRow().add(columnIndex, new LogsTableCell(columnNames.get(columnIndex), LogsColumnType.STRING, columnIndex, rowIndex, value));
        fireTableChanged();
    }

    @Override
    public void addTableModelListener(TableModelListener l) {
        tableModelListenerList.add(l);
    }

    @Override
    public void removeTableModelListener(TableModelListener l) {
        tableModelListenerList.remove(l);
    }

    public void clear() {
        this.columnNames.clear();
        this.logsTableRows.clear();
        fireTableChanged();
    }

    private void fireTableChanged() {
        tableModelListenerList.forEach(listener ->
                AzureTaskManager.getInstance().runLater(() -> listener.tableChanged(new TableModelEvent(this))));
    }

    private boolean isRowInvalid(int row) {
        return row < 0 || row >= this.logsTableRows.size();
    }
}
