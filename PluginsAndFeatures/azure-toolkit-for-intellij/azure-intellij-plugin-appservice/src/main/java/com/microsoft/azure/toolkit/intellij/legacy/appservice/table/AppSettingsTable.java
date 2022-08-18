/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.legacy.appservice.table;

import com.intellij.ui.table.JBTable;
import lombok.Getter;

import javax.annotation.Nonnull;
import javax.swing.*;
import java.awt.*;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class AppSettingsTable extends JBTable {
    @Getter
    protected final Set<String> removedKeys = new HashSet<>();
    protected final AppSettingModel appSettingModel = new AppSettingModel();

    public AppSettingsTable() {
        super();
        this.setModel(appSettingModel);
        this.setCellSelectionEnabled(true);
        this.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        this.setAutoscrolls(true);
        this.setMinimumSize(new Dimension(-1, 150));
        this.setPreferredScrollableViewportSize(null);
    }

    public int addRequiredAppSettings(@Nonnull final String key, final String value) {
        return appSettingModel.addAppSettings(key, value);
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
        removedKeys.add(appSettingModel.getAppSettingsKey(row));
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
