/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.legacy.appservice.table;

import com.microsoft.azure.toolkit.lib.common.task.AzureTaskManager;
import org.apache.commons.collections4.ListUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;
import javax.swing.table.TableModel;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static com.microsoft.azure.toolkit.intellij.common.AzureBundle.message;

public class AppSettingModel implements TableModel {

    private static final String[] TITLE = {"Key", "Value"};

    private final List<Pair<String, String>> appSettings = new ArrayList<>();
    private final Set<String> requiredKeys = new HashSet<>();
    private final List<TableModelListener> tableModelListenerList = new ArrayList<>();

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
        if (isRowInvalid(row)) {
            return false;
        }
        final Pair<String, String> target = appSettings.get(row);
        return requiredKeys.contains(target.getKey()) && col == 0;
    }

    @Override
    @Nullable
    public Object getValueAt(int row, int col) {
        if (isRowInvalid(row)) {
            return null;
        }
        final Pair<String, String> target = appSettings.get(row);
        return col == 0 ? target.getKey() : target.getValue();
    }

    @Override
    public void setValueAt(Object value, int row, int col) {
        if (value != null && !(value instanceof String)) {
            throw new IllegalArgumentException(message("function.appSettings.validate.illegalType"));
        }
        while (row >= appSettings.size()) {
            appSettings.add(Pair.of("", ""));
        }
        final Pair<String, String> target = appSettings.get(row);
        appSettings.set(row, Pair.of((String) (col == 0 ? value : target.getLeft()), (String) (col == 0 ? target.getRight() : value)));
        fireTableChanged();
    }

    public int addAppSettings(String key, String value) {
        final Pair<String, String> result = Pair.of(key, value);
        final int index = ListUtils.indexOf(appSettings, pair -> StringUtils.equalsIgnoreCase(pair.getKey(), key));
        if (index >= 0) {
            appSettings.set(index, result);
        } else {
            appSettings.add(result);
        }
        fireTableChanged();
        return index > 0 ? index : appSettings.size() - 1;
    }

    public int addRequiredAppSettings(@Nonnull final String key, final String value) {
        this.requiredKeys.add(key);
        return addAppSettings(key, value);
    }

    public void removeAppSettings(int row) {
        if (isRowInvalid(row)) {
            return;
        }
        final Pair<String, String> target = appSettings.get(row);
        if (requiredKeys.contains(target.getKey())) {
            throw new IllegalArgumentException(message("function.appSettings.validate.requiredParameter", target.getKey()));
        }
        appSettings.remove(row);
        fireTableChanged();
    }

    public String getAppSettingsKey(int row) {
        if (isRowInvalid(row)) {
            return null;
        }
        return appSettings.get(row).getKey();
    }

    public Map<String, String> getAppSettings() {
        final Map<String, String> result = new HashMap<>();
        appSettings.forEach(pair -> result.put(pair.getKey(), pair.getValue()));
        return result;
    }

    public void clear() {
        appSettings.clear();
        fireTableChanged();
    }

    public void fireTableChanged() {
        tableModelListenerList.forEach(listener ->
                AzureTaskManager.getInstance().runLater(() -> listener.tableChanged(new TableModelEvent(this))));
    }

    @Override
    public void addTableModelListener(TableModelListener tableModelListener) {
        tableModelListenerList.add(tableModelListener);
    }

    @Override
    public void removeTableModelListener(TableModelListener tableModelListener) {
        tableModelListenerList.remove(tableModelListener);
    }

    private boolean isRowInvalid(int row) {
        return row < 0 || row >= appSettings.size();
    }
}
