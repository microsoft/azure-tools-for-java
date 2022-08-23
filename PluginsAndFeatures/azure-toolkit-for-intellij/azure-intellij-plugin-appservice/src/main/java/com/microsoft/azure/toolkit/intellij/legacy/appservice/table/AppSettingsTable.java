/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.legacy.appservice.table;

import com.intellij.ui.table.JBTable;
import com.microsoft.azure.toolkit.ide.appservice.model.AppServiceConfig;
import com.microsoft.azure.toolkit.lib.Azure;
import com.microsoft.azure.toolkit.lib.appservice.AppServiceAppBase;
import com.microsoft.azure.toolkit.lib.appservice.AzureAppService;
import com.microsoft.azure.toolkit.lib.common.task.AzureTask;
import com.microsoft.azure.toolkit.lib.common.task.AzureTaskManager;
import com.microsoft.intellij.CommonConst;
import lombok.Getter;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.StringUtils;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.swing.*;
import java.awt.*;
import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

public class AppSettingsTable extends JBTable {
    @Getter
    protected final Set<String> appSettingsKeyToRemove = new HashSet<>();
    protected final AppSettingModel appSettingModel = new AppSettingModel();
    protected AppServiceConfig config;

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

    public void setAppService(@Nonnull final AppServiceConfig config) {
        final Map<String, String> values = this.getAppSettings();
        if (AppServiceConfig.isSameApp(config, this.config)) {
            return;
        }
        this.config = config;
        AzureTaskManager.getInstance().runInBackground("Loading app settings", () -> {
            this.getEmptyText().setText(CommonConst.LOADING_TEXT);
            this.setEnabled(false);
            this.clear();
            final Map<String, String> remoteAppSettings = StringUtils.isNoneBlank(config.getResourceId()) ?
                    ((AppServiceAppBase<?, ?, ?>) Objects.requireNonNull(Azure.az(AzureAppService.class).getById(config.getResourceId()))).getAppSettings() : Collections.emptyMap();
            AzureTaskManager.getInstance().runLater(() -> {
                AppSettingsTable.this.setAppSettings(remoteAppSettings);
                config.getAppSettingsToRemove().forEach(this::removeAppSettings);
                AppSettingsTable.this.addAppSettings(config.getAppSettings());
                this.getEmptyText().setText("No app settings configured");
                this.setEnabled(true);
            }, AzureTask.Modality.ANY);
        });
    }

    public void addAppSettings(@Nonnull String key, String value) {
        appSettingsKeyToRemove.remove(key);
        final int index = appSettingModel.addAppSettings(key, value);
        this.refresh();
        scrollToRow(index);
    }

    public void addAppSettings(@Nullable final Map<String, String> appSettingMap) {
        if (MapUtils.isEmpty(appSettingMap)) {
            return;
        }
        appSettingMap.entrySet().forEach(entry -> addAppSettings(entry.getKey(), entry.getValue()));
        this.refresh();
        scrollToRow(0);
    }

    public void removeAppSettings(int row) {
        appSettingsKeyToRemove.add(appSettingModel.getAppSettingsKey(row));
        appSettingModel.removeAppSettings(row);
        this.refresh();
    }

    public void removeAppSettings(@Nonnull String key) {
        final int row = appSettingModel.getAppSettingsRow(key);
        this.removeAppSettings(row);
    }

    public void setAppSettings(@Nullable final Map<String, String> appSettingMap) {
        clear();
        if (MapUtils.isNotEmpty(appSettingMap)) {
            addAppSettings(appSettingMap);
        }
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
