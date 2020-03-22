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
package com.microsoft.azure.hdinsight.spark.uihelper;

import com.microsoft.azuretools.azurecommons.helpers.StringHelper;

import javax.swing.table.AbstractTableModel;
import java.util.ArrayList;

public class InteractiveTableModel extends AbstractTableModel {

    public static final int KEY_INDEX = 0;
    public static final int VALUE_INDEX = 1;
    public static final int HIDDEN_INDEX = 2;

    private String[] columnNames;
    private ArrayList<ConfigurationKeyValueRecord> dataRecords;

    public InteractiveTableModel(String[] columnNames) {
        this.columnNames = columnNames;
        this.dataRecords = new ArrayList<>();
    }

    @Override
    public int getRowCount() {
        return this.dataRecords.size();
    }

    @Override
    public int getColumnCount() {
        return this.columnNames.length;
    }

    @Override
    public Object getValueAt(int rowIndex, int columnIndex) {
        if (rowIndex > getRowCount() - 1 || columnIndex > getColumnCount() - 1) {
            return null;
        }

        ConfigurationKeyValueRecord record = dataRecords.get(rowIndex);
        switch (columnIndex) {
            case KEY_INDEX:
                return record.getKey();
            case VALUE_INDEX:
                return record.getValue();
            default:
                return null;
        }
    }

    @Override
    public String getColumnName(int column) {
        return columnNames[column];
    }

    @Override
    public Class<?> getColumnClass(int columnIndex) {
        switch (columnIndex) {
            case KEY_INDEX:
            case VALUE_INDEX:
                return String.class;
            default:
                return Object.class;
        }
    }

    @Override
    public boolean isCellEditable(int rowIndex, int columnIndex) {
        if (columnIndex == HIDDEN_INDEX) {
            return false;
        }

        return true;
    }

    @Override
    public void setValueAt(Object aValue, int rowIndex, int columnIndex) {
        if (rowIndex > getRowCount() - 1 || columnIndex > getColumnCount() - 1) {
            return;
        }

        ConfigurationKeyValueRecord record = dataRecords.get(rowIndex);
        switch (columnIndex) {
            case KEY_INDEX:
                record.setKey((String) aValue);
                break;
            case VALUE_INDEX:
                record.setValue(aValue);
                break;
            default:
                return;
        }

        fireTableCellUpdated(rowIndex, columnIndex);
    }

    public void removeAllRows() {
        dataRecords.clear();
        fireTableDataChanged();
    }

    public boolean hasEmptyRow() {
        int rowCount = getRowCount();
        if (rowCount == 0) {
            return false;
        }

        ConfigurationKeyValueRecord record = dataRecords.get(getRowCount() - 1);
        if (StringHelper.isNullOrWhiteSpace(record.getKey()) && StringHelper.isNullOrWhiteSpace(record.getValue().toString())) {
            return true;
        }

        return false;
    }

    public void addEmptyRow() {
        dataRecords.add(new ConfigurationKeyValueRecord());
        int rowCount = getRowCount();
        fireTableRowsInserted(rowCount - 1, rowCount - 1);
    }

    public void addRow(String key, Object value) {
        dataRecords.add(new ConfigurationKeyValueRecord(key, value));
        int rowCount = getRowCount();
        fireTableRowsInserted(rowCount - 1, rowCount - 1);
    }
}
