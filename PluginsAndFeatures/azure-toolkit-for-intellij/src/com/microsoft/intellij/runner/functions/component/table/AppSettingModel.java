/**
 * Copyright (c) Microsoft Corporation
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

package com.microsoft.intellij.runner.functions.component.table;


import org.apache.commons.collections4.ListUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;

import javax.swing.event.TableModelListener;
import javax.swing.table.TableModel;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class AppSettingModel implements TableModel {

    private static final String[] TITLE = {"Key", "Value"};

    private List<Pair<String, String>> appSettings = new ArrayList<>();

    @Override
    public int getRowCount() {
        return appSettings.size();
    }

    @Override
    public int getColumnCount() {
        return 2;
    }

    @Override
    public String getColumnName(int i) {
        return TITLE[i];
    }

    @Override
    public Class<?> getColumnClass(int i) {
        return String.class;
    }

    @Override
    public boolean isCellEditable(int i, int i1) {
        return true;
    }

    @Override
    public Object getValueAt(int i, int i1) {
        final Pair<String, String> target = appSettings.get(i);
        return i1 == 0 ? target.getKey() : target.getValue();
    }

    @Override
    public void setValueAt(Object o, int i, int i1) {
        while (i >= appSettings.size()) {
            appSettings.add(Pair.of("", ""));
        }
        final Pair<String, String> target = appSettings.get(i);
        appSettings.set(i, Pair.of((String) (i1 == 0 ? o : target.getLeft()), (String) (i1 == 0 ? target.getRight() : o)));
    }

    public int addAppSettings(String key, String value) {
        final Pair result = Pair.of(key, value);
        final int index = ListUtils.indexOf(appSettings, pair -> StringUtils.equalsIgnoreCase(pair.getKey(), key));
        if (index > 0) {
            appSettings.set(index, result);
        } else {
            appSettings.add(result);
        }
        return index > 0 ? index : appSettings.size() - 1;
    }

    public void removeAppSettings(int row) {
        appSettings.remove(row);
    }

    public String getAppSettingsKey(int row) {
        if (appSettings == null || row >= appSettings.size()) {
            return null;
        }
        return appSettings.get(row).getKey();
    }

    public Map<String, String> getAppSettings() {
        final Map<String, String> result = new HashMap<>();
        appSettings.stream().forEach(pair -> result.put(pair.getKey(), pair.getValue()));
        return result;
    }

    public void setAppSettings(Map<String, String> appSettingMap) {
        appSettings.clear();
        appSettingMap.entrySet().forEach(entry -> appSettings.add(Pair.of(entry.getKey(), entry.getValue())));
    }

    public void clear() {
        appSettings.clear();
    }

    @Override
    public void addTableModelListener(TableModelListener tableModelListener) {
    }

    @Override
    public void removeTableModelListener(TableModelListener tableModelListener) {

    }
}
