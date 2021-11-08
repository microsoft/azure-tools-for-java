/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.database.sqlserver.common;

import com.azure.core.management.exception.ManagementException;
import com.microsoft.azure.toolkit.intellij.database.RegionComboBox;
import com.microsoft.azure.toolkit.lib.Azure;
import com.microsoft.azure.toolkit.lib.common.form.AzureFormInput;
import com.microsoft.azure.toolkit.lib.common.form.AzureValidationInfo;
import com.microsoft.azure.toolkit.lib.sqlserver.AzureSqlServer;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class SqlServerRegionValidator implements AzureFormInput.Validator {
    private static final String REGION_UNAVAILABLE_MESSAGE = "Your subscription does not have access to create a server "
        + "in the selected region. For the latest information about region availability for your subscription, go to "
        + "aka.ms/sqlcapacity. Please try another region or create a support ticket to request access.";
    private final RegionComboBox input;

    @Override
    public AzureValidationInfo doValidate() {
        try {
            final AzureSqlServer service = Azure.az(AzureSqlServer.class);
            if (service.checkRegionCapability(input.getSubscription().getId(), input.getValue().getName())) {
                return AzureValidationInfo.success(input);
            }
        } catch (final ManagementException e) {
            return AzureValidationInfo.error(e.getMessage(), input);
        }
        return AzureValidationInfo.error(REGION_UNAVAILABLE_MESSAGE, input);
    }
}
