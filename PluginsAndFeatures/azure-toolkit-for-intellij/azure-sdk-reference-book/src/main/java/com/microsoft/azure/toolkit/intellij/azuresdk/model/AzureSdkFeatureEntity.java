/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.azuresdk.model;

import lombok.Builder;
import lombok.Getter;

import javax.annotation.Nonnull;
import java.util.List;

@Builder
@Getter
public class AzureSdkFeatureEntity {
    private final String name;
    private final String service;
    private final String description;
    @Nonnull
    private final List<AzureSdkPackageEntity> clientPackages;
    @Nonnull
    private final List<AzureSdkPackageEntity> managementPackages;

    public String toString() {
        return this.name;
    }
}
