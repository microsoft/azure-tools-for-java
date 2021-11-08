/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.database;

import com.microsoft.azure.toolkit.lib.common.form.AzureFormInput;
import com.microsoft.azure.toolkit.lib.common.form.AzureValidationInfo;
import com.microsoft.azure.toolkit.lib.common.model.Region;
import lombok.RequiredArgsConstructor;

import java.util.function.BiFunction;

@RequiredArgsConstructor
public abstract class BaseRegionValidator implements AzureFormInput.Validator {
    private static final String REGION_UNAVAILABLE_MESSAGE = "Currently, the service is not available in this location for your subscription.";
    private final RegionComboBox input;
    private final BiFunction<String, Region, Boolean> checkRegionAvailFunc;

    @Override
    public AzureValidationInfo doValidate() {
        try {
            if (checkRegionAvailFunc != null && checkRegionAvailFunc.apply(input.getSubscription().getId(), input.getValue())) {
                return AzureValidationInfo.success(input);
            }
            return AzureValidationInfo.error(REGION_UNAVAILABLE_MESSAGE, input);
        } catch (final RuntimeException e) {
            return AzureValidationInfo.error(e.getMessage(), input);
        }
    }
}
