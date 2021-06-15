/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.lib.appservice;

import com.microsoft.azure.toolkit.lib.appservice.model.DiagnosticConfig;
import lombok.Builder;
import lombok.Data;
import lombok.experimental.SuperBuilder;

@Data
@SuperBuilder(toBuilder = true)
public class MonitorConfig {
    private ApplicationInsightsConfig applicationInsightsConfig;
    @Builder.Default
    private DiagnosticConfig diagnosticConfig = DiagnosticConfig.builder().build();
}
