/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.azuresdk.service;

import com.microsoft.azure.toolkit.intellij.azuresdk.model.AzureSdkFeatureEntity;
import com.microsoft.azure.toolkit.intellij.azuresdk.model.AzureSdkServiceEntity;

import java.util.Arrays;
import java.util.List;

public class AzureSdkLibraryService {

    public static AzureSdkLibraryService getInstance() {
        return Holder.instance;
    }

    public List<AzureSdkServiceEntity> loadAllServices() {
        final AzureSdkFeatureEntity feature1 = AzureSdkFeatureEntity.builder()
            .name("Service Bus - JMS")
            .description("Feature Description1")
            .build();
        final AzureSdkFeatureEntity feature2 = AzureSdkFeatureEntity.builder()
            .name("Service Bus - XXX")
            .description("Feature Description2")
            .build();
        return Arrays.asList(
            AzureSdkServiceEntity.builder()
                .name("Service Bus")
                .features(Arrays.asList(feature1, feature2))
                .build()
        );
    }

    private static class Holder {
        private static final AzureSdkLibraryService
            instance = new AzureSdkLibraryService();
    }
}
