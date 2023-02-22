/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.legacy.function.runner.deploy;

import com.microsoft.azure.toolkit.ide.appservice.function.FunctionAppConfig;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.Collections;
import java.util.Map;
import java.util.Optional;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class FunctionDeployModel {
    private String appSettingsKey;
    private String appSettingsHash;
    private String hostJsonPath;
    private String deploymentStagingDirectoryPath;
    private String moduleName;

    private FunctionAppConfig functionAppConfig = FunctionAppConfig.builder().build();

    public Map<String, String> getTelemetryProperties() {
        return Optional.ofNullable(functionAppConfig).map(FunctionAppConfig::getTelemetryProperties).orElse(Collections.emptyMap());
    }
}
