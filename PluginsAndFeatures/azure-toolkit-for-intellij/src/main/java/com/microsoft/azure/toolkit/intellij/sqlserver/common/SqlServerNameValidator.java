/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */
package com.microsoft.azure.toolkit.intellij.sqlserver.common;

import com.azure.core.management.exception.ManagementException;
import com.microsoft.azure.toolkit.intellij.database.ServerNameTextField;
import com.microsoft.azure.toolkit.lib.Azure;
import com.microsoft.azure.toolkit.lib.common.entity.CheckNameAvailabilityResultEntity;
import com.microsoft.azure.toolkit.lib.common.form.AzureValidationInfo;
import com.microsoft.azure.toolkit.lib.sqlserver.service.AzureSqlServer;

import java.util.function.Function;

public class SqlServerNameValidator implements Function<ServerNameTextField, AzureValidationInfo> {

    @Override
    public AzureValidationInfo apply(ServerNameTextField textField) {
        final String value = textField.getValue();
        // validate availability
        CheckNameAvailabilityResultEntity resultEntity;
        try {
            resultEntity = Azure.az(AzureSqlServer.class).checkNameAvailability(textField.getSubscriptionId(), value);
        } catch (ManagementException e) {
            return AzureValidationInfo.builder().input(textField).message(e.getMessage()).type(AzureValidationInfo.Type.ERROR).build();
        }
        if (!resultEntity.isAvailable()) {
            String message = resultEntity.getUnavailabilityReason();
            if ("AlreadyExists".equalsIgnoreCase(resultEntity.getUnavailabilityReason())) {
                message = "The specified server name is already in use.";
            }
            return AzureValidationInfo.builder().input(textField).message(message).type(AzureValidationInfo.Type.ERROR).build();
        }
        return AzureValidationInfo.OK;
    }
}
