/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.database.mysql;

import com.microsoft.azure.toolkit.intellij.database.RegionComboBox;
import com.microsoft.azure.toolkit.lib.Azure;
import com.microsoft.azure.toolkit.lib.common.form.AzureFormInput;
import com.microsoft.azure.toolkit.lib.common.form.AzureValidationInfo;
import com.microsoft.azure.toolkit.lib.mysql.AzureMySql;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class MySQLRegionValidator implements AzureFormInput.Validator {

    private static final String REGION_UNAVAILABLE_MESSAGE = "Currently, the service is not available in this location for your subscription.";
    private final RegionComboBox input;

    @Override
    public AzureValidationInfo doValidate() {
        try {
            if (Azure.az(AzureMySql.class).subscription(input.getSubscription().getId()).checkRegionAvailability(input.getValue())) {
                return AzureValidationInfo.success(input);
            }
            return AzureValidationInfo.error(REGION_UNAVAILABLE_MESSAGE, input);
        } catch (final RuntimeException e) {
            return AzureValidationInfo.error(e.getMessage(), input);
        }
    }
}
