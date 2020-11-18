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

package com.microsoft.intellij.runner.functions.component.table;

import com.microsoft.tooling.msservices.components.DefaultLoader;
import org.apache.commons.collections4.ListUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;

import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;
import javax.swing.table.TableModel;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class AppSettingModel implements TableModel {

    private static final String[] TITLE = {"Key", "Value"};
    private static final String FUNCTIONS_WORKER_RUNTIME_KEY = "FUNCTIONS_WORKER_RUNTIME";
    private static final String AZURE_WEB_JOB_STORAGE_KEY = "AzureWebJobsStorage";
    private static final String FUNCTIONS_WORKER_RUNTIME_VALUE = "java";
    private static final String AZURE_WEB_JOB_STORAGE_VALUE = "";

    public static final String REQUIRED_APP_SETTINGS_PROMOTION = "%s is a required parameter";

    private List<Pair<String, String>> appSettings = new ArrayList<>();
    private List<TableModelListener> tableModelListenerList = new ArrayList<>();

    public AppSettingModel() {
    }

    @Override
    public int getRowCount() {
        return appSettings.size();
    }

    @Override
    public int getColumnCount() {
        return 2;
    }

    @Override
    public String getColumnName(int col) {
        return TITLE[col];
    }

    @Override
    public Class<?> getColumnClass(int col) {
        return String.class;
    }

    @Override
    public boolean isCellEditable(int row, int col) {
        if (!isRowValid(row)) {
            return false;
        }
        final Pair<String, String> target = appSettings.get(row);
        // Should not modify FUNCTIONS_WORKER_RUNTIME and AzureWebJobsStorage
        return !(FUNCTIONS_WORKER_RUNTIME_KEY.equals(target.getKey()) || (AZURE_WEB_JOB_STORAGE_KEY.equals(target.getKey()) && col == 0));
    }

    @Override
    public Object getValueAt(int row, int col) {
        if (!isRowValid(row)) {
            return null;
        }
        final Pair<String, String> target = appSettings.get(row);
        return col == 0 ? target.getKey() : target.getValue();
    }

    @Override
    public void setValueAt(Object value, int row, int col) {
        if (value != null && !(value instanceof String)) {
            throw new IllegalArgumentException("Illegal value type, only String is supported");
        }
        while (row >= appSettings.size()) {
            appSettings.add(Pair.of("", ""));
        }
        final Pair<String, String> target = appSettings.get(row);
        appSettings.set(row, Pair.of((String) (col == 0 ? value : target.getLeft()), (String) (col == 0 ? target.getRight() : value)));
        fireTableChanged();
    }

    public int addAppSettings(String key, String value) {
        final Pair result = Pair.of(key, value);
        final int index = ListUtils.indexOf(appSettings, pair -> StringUtils.equalsIgnoreCase(pair.getKey(), key));
        if (index >= 0) {
            appSettings.set(index, result);
        } else {
            appSettings.add(result);
        }
        fireTableChanged();
        return index > 0 ? index : appSettings.size() - 1;
    }

    public void removeAppSettings(int row) {
        if (!isRowValid(row)) {
            return;
        }
        final Pair<String, String> target = appSettings.get(row);
        if (FUNCTIONS_WORKER_RUNTIME_KEY.equals(target.getKey()) || AZURE_WEB_JOB_STORAGE_KEY.equals(target.getKey())) {
            throw new IllegalArgumentException(String.format(REQUIRED_APP_SETTINGS_PROMOTION, target.getKey()));
        }
        appSettings.remove(row);
        fireTableChanged();
    }

    public String getAppSettingsKey(int row) {
        if (appSettings == null || !isRowValid(row)) {
            return null;
        }
        return appSettings.get(row).getKey();
    }

    public Map<String, String> getAppSettings() {
        final Map<String, String> result = new HashMap<>();
        appSettings.stream().forEach(pair -> result.put(pair.getKey(), pair.getValue()));
        return result;
    }

    public void clear() {
        appSettings.clear();
        fireTableChanged();
    }

    public void fireTableChanged() {
        tableModelListenerList.stream().forEach(listener -> DefaultLoader.getIdeHelper().invokeLater(() -> listener.tableChanged(new TableModelEvent(this))));
    }

    @Override
    public void addTableModelListener(TableModelListener tableModelListener) {
        tableModelListenerList.add(tableModelListener);
    }

    @Override
    public void removeTableModelListener(TableModelListener tableModelListener) {
        tableModelListenerList.remove(tableModelListener);
    }

    public void loadRequiredAttributes() {
        final Map<String, String> appSettingsMap = getAppSettings();
        if (!appSettingsMap.containsKey(FUNCTIONS_WORKER_RUNTIME_KEY)) {
            appSettings.add(Pair.of(FUNCTIONS_WORKER_RUNTIME_KEY, FUNCTIONS_WORKER_RUNTIME_VALUE));
        }
        if (!appSettingsMap.containsKey(AZURE_WEB_JOB_STORAGE_KEY)) {
            appSettings.add(Pair.of(AZURE_WEB_JOB_STORAGE_KEY, AZURE_WEB_JOB_STORAGE_VALUE));
        }
    }

    private boolean isRowValid(int row) {
        return row >= 0 && row < appSettings.size();
    }
}
