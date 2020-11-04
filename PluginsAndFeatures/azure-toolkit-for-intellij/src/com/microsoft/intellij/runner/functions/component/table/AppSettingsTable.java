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

import com.intellij.ui.table.JBTable;

import javax.swing.*;
import java.awt.*;
import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;

public class AppSettingsTable extends JBTable {

    private String localSettingPath;
    private AppSettingModel appSettingModel = new AppSettingModel();

    public AppSettingsTable(String localSettingPath) {
        super();
        this.localSettingPath = localSettingPath;
        this.setModel(appSettingModel);
        this.setCellSelectionEnabled(true);
        this.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        this.setAutoscrolls(true);
        this.setMinimumSize(new Dimension(-1, 100));
        this.setPreferredScrollableViewportSize(null);
    }

    public void loadRequiredSettings() {
        appSettingModel.loadRequiredAttributes();
    }

    public void loadLocalSetting() {
        final Map<String, String> appSettings = AppSettingsTableUtils.getAppSettingsFromLocalSettingsJson(new File(localSettingPath));
        setAppSettings(appSettings);
    }

    public void addAppSettings(String key, String value) {
        final int index = appSettingModel.addAppSettings(key, value);
        this.refresh();
        scrollToRow(index);
    }

    public void addAppSettings(Map<String, String> appSettingMap) {
        appSettingMap.entrySet().stream().forEach(entry -> addAppSettings(entry.getKey(), entry.getValue()));
        this.refresh();
        scrollToRow(0);
    }

    public void removeAppSettings(int row) {
        appSettingModel.removeAppSettings(row);
        this.refresh();
    }

    public void setAppSettings(Map<String, String> appSettingMap) {
        clear();
        addAppSettings(appSettingMap);
    }

    public void clear() {
        appSettingModel.clear();
        this.refresh();
    }

    public String getSelectedKey() {
        return appSettingModel.getAppSettingsKey(getSelectedRow());
    }

    public Map<String, String> getAppSettings() {
        return appSettingModel.getAppSettings();
    }

    public Path getLocalSettingsPath() {
        return Paths.get(localSettingPath);
    }

    public boolean isEmpty() {
        return appSettingModel.getRowCount() == 0;
    }

    private void scrollToRow(int target) {
        scrollRectToVisible(getCellRect(target, 0, true));
    }

    private void refresh() {
        this.setSize(-1, getRowHeight() * getRowCount());
        this.repaint();
    }
}
