/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.function.runner.deploy.ui;

import com.microsoft.azuretools.core.mvp.ui.base.MvpView;

import java.util.Map;

public interface FunctionDeployMvpView extends MvpView {
    void beforeFillAppSettings();

    void fillAppSettings(Map<String, String> appSettings);
}
