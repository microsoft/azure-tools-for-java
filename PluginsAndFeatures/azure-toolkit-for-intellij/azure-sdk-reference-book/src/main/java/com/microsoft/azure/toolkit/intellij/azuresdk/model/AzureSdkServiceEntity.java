/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.azuresdk.model;

import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Builder
@Getter
public class AzureSdkServiceEntity {
    private final String name;
    private final List<AzureSdkFeatureEntity> features;

    public String toString() {
        return this.name;
    }
}
