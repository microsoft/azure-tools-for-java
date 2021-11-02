/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */
package com.microsoft.azure.toolkit.ide.appservice.model;

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
@EqualsAndHashCode
public class ApplicationInsightsConfig {
    private boolean newCreate;
    private String name;
    private String instrumentationKey;

    public ApplicationInsightsConfig(String name) {
        this.newCreate = true;
        this.name = name;
    }

    public ApplicationInsightsConfig(final String name, final String instrumentationKey) {
        this.newCreate = false;
        this.name = name;
        this.instrumentationKey = instrumentationKey;
    }
}
