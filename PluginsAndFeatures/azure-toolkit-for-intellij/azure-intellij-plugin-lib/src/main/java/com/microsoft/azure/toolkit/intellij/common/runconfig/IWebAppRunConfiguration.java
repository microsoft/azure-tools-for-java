/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.common.runconfig;

import com.intellij.openapi.module.Module;

import java.util.Map;

public interface IWebAppRunConfiguration {
    void setApplicationSettings(Map<String, String> env);

    Module getModule();
}
