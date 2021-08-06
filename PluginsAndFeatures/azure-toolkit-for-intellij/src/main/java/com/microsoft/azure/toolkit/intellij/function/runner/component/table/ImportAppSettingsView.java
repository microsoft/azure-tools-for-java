/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.function.runner.component.table;

import com.microsoft.azure.toolkit.lib.appservice.service.IFunctionApp;
import com.microsoft.azuretools.core.mvp.ui.base.MvpView;

import java.util.List;
import java.util.Map;

public interface ImportAppSettingsView extends MvpView {
    void fillFunctionApps(List<IFunctionApp> functionApps);

    void fillFunctionAppSettings(Map<String, String> appSettings);

    void beforeFillAppSettings();
}
