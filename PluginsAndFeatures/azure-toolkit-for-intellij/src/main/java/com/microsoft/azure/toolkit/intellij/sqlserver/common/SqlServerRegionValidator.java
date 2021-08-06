/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.sqlserver.common;

import com.azure.core.management.exception.ManagementException;
import com.microsoft.azure.toolkit.intellij.database.RegionComboBox;
import com.microsoft.azure.toolkit.lib.Azure;
import com.microsoft.azure.toolkit.lib.common.form.AzureValidationInfo;
import com.microsoft.azure.toolkit.lib.sqlserver.service.AzureSqlServer;

import java.util.function.Function;

public class SqlServerRegionValidator implements Function<RegionComboBox, AzureValidationInfo> {

    private static final String REGION_UNAVAILABLE_MESSAGE = "Your subscription does not have access to create a server "
            + "in the selected region. For the latest information about region availability for your subscription, go to "
            + "aka.ms/sqlcapacity. Please try another region or create a support ticket to request access.";

    @Override
    public AzureValidationInfo apply(RegionComboBox comboBox) {
        AzureSqlServer service = Azure.az(AzureSqlServer.class);
        try {
            if (service.checkRegionCapability(comboBox.getSubscription().getId(), comboBox.getValue().getName())) {
                return AzureValidationInfo.OK;
            }
        } catch (ManagementException e) {
            return AzureValidationInfo.builder().input(comboBox).message(e.getMessage()).type(AzureValidationInfo.Type.ERROR).build();
        }
        final AzureValidationInfo.AzureValidationInfoBuilder builder = AzureValidationInfo.builder();
        return builder.input(comboBox).message(REGION_UNAVAILABLE_MESSAGE).type(AzureValidationInfo.Type.ERROR).build();
    }
}
