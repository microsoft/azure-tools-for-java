/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.legacy.function.runner.component.table;

import com.microsoft.azure.toolkit.intellij.legacy.appservice.table.AppSettingsTable;
import lombok.Getter;
import lombok.Setter;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;

public class FunctionAppSettingsTable extends AppSettingsTable {
    public static final String AZURE_WEB_JOB_STORAGE_KEY = "AzureWebJobsStorage";
    public static final String FUNCTIONS_WORKER_RUNTIME_KEY = "FUNCTIONS_WORKER_RUNTIME";
    public static final String FUNCTIONS_WORKER_RUNTIME_VALUE = "java";
    public static final String AZURE_WEB_JOB_STORAGE_VALUE = "";
    @Getter
    @Setter
    private String localSettingPath;

    public FunctionAppSettingsTable(String localSettingPath) {
        super();
        this.localSettingPath = localSettingPath;
    }

    public void loadLocalSetting() {
        final Map<String, String> appSettings = FunctionAppSettingsTableUtils.getAppSettingsFromLocalSettingsJson(new File(localSettingPath));
        setAppSettings(appSettings);
    }

    public Path getLocalSettingsPath() {
        return Paths.get(localSettingPath);
    }

    public void loadRequiredSettings() {
        final Map<String, String> appSettingsMap = getAppSettings();
        if (!appSettingsMap.containsKey(FunctionAppSettingsTable.FUNCTIONS_WORKER_RUNTIME_KEY)) {
            this.addRequiredAppSettings(FunctionAppSettingsTable.FUNCTIONS_WORKER_RUNTIME_KEY, FunctionAppSettingsTable.FUNCTIONS_WORKER_RUNTIME_VALUE);
        }
        if (!appSettingsMap.containsKey(FunctionAppSettingsTable.AZURE_WEB_JOB_STORAGE_KEY)) {
            this.addRequiredAppSettings(FunctionAppSettingsTable.AZURE_WEB_JOB_STORAGE_KEY, FunctionAppSettingsTable.AZURE_WEB_JOB_STORAGE_VALUE);
        }
    }
}
