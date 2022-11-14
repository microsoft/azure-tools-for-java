/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */
package com.microsoft.azure.toolkit.ide.appservice.model;

import com.microsoft.azure.toolkit.lib.applicationinsights.workspace.LogAnalyticsWorkspaceConfig;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class ApplicationInsightsConfig {
    private boolean newCreate;
    @EqualsAndHashCode.Include
    private String name;
    @EqualsAndHashCode.Include
    private String instrumentationKey;
    private LogAnalyticsWorkspaceConfig workspaceConfig;

    public ApplicationInsightsConfig(String name) {
        this.newCreate = true;
        this.name = name;
        this.workspaceConfig = LogAnalyticsWorkspaceConfig.builder().newCreate(true)
                .name(String.format("workspace-%s", name)).build();
    }

    public ApplicationInsightsConfig(final String name, final String instrumentationKey) {
        this.newCreate = false;
        this.name = name;
        this.instrumentationKey = instrumentationKey;
        this.workspaceConfig = LogAnalyticsWorkspaceConfig.builder().newCreate(true)
                .name(String.format("workspace-%s", name)).build();
    }
}
