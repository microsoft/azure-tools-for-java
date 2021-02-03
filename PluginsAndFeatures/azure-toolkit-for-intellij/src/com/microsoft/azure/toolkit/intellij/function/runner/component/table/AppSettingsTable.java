/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.function.runner.component.table;

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
