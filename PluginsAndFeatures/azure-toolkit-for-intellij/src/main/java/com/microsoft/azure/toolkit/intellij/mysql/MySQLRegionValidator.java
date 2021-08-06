/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.mysql;

import com.microsoft.azure.toolkit.intellij.database.RegionComboBox;
import com.microsoft.azure.toolkit.lib.Azure;
import com.microsoft.azure.toolkit.lib.common.form.AzureValidationInfo;
import com.microsoft.azure.toolkit.lib.mysql.service.AzureMySql;

import java.util.function.Function;

public class MySQLRegionValidator implements Function<RegionComboBox, AzureValidationInfo> {

    private static final String REGION_UNAVAILABLE_MESSAGE = "Currently, the service is not available in this location for your subscription.";

    @Override
    public AzureValidationInfo apply(RegionComboBox comboBox) {

        try {
            if (Azure.az(AzureMySql.class).subscription(comboBox.getSubscription().getId()).checkRegionAvailability(comboBox.getValue())) {
                return AzureValidationInfo.OK;
            }
            final AzureValidationInfo.AzureValidationInfoBuilder builder = AzureValidationInfo.builder();
            return builder.input(comboBox).message(REGION_UNAVAILABLE_MESSAGE).type(AzureValidationInfo.Type.ERROR).build();
        } catch (RuntimeException e) {
            return AzureValidationInfo.builder().input(comboBox).message(e.getMessage()).type(AzureValidationInfo.Type.ERROR).build();
        }
    }
}
