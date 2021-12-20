/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.eclipse.function.launch.model;

import com.microsoft.azure.toolkit.eclipse.common.launch.BaseRunConfiguration;

public class FunctionLocalRunConfiguration extends BaseRunConfiguration {
    private String functionCliPath;
    private String localSettingsJsonPath;

    public String getFunctionCliPath() {
        return functionCliPath;
    }

    public void setFunctionCliPath(String functionCliPath) {
        this.functionCliPath = functionCliPath;
    }

    public String getLocalSettingsJsonPath() {
        return localSettingsJsonPath;
    }

    public void setLocalSettingsJsonPath(String localSettingsJsonPath) {
        this.localSettingsJsonPath = localSettingsJsonPath;
    }
}
