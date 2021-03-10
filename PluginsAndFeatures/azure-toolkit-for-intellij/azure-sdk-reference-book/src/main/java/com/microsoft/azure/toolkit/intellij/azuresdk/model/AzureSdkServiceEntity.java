/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.azuresdk.model;

import lombok.Builder;
import lombok.Getter;
import lombok.ToString;

import java.util.List;

@Builder
@Getter
@ToString(of = "name", includeFieldNames = false)
public class AzureSdkServiceEntity {
    private final String name;
    private final List<AzureSdkFeatureEntity> features;
}
