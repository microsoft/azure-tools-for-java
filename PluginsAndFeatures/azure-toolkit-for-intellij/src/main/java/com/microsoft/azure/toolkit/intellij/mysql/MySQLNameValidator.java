/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */
package com.microsoft.azure.toolkit.intellij.mysql;

import com.microsoft.azure.CloudException;
import com.microsoft.azure.toolkit.intellij.database.ServerNameTextField;
import com.microsoft.azure.toolkit.lib.Azure;
import com.microsoft.azure.toolkit.lib.common.form.AzureValidationInfo;
import com.microsoft.azure.toolkit.lib.mysql.service.AzureMySql;

import java.util.function.Function;

public class MySQLNameValidator implements Function<ServerNameTextField, AzureValidationInfo> {

    @Override
    public AzureValidationInfo apply(ServerNameTextField textField) {
        final String value = textField.getValue();
        // validate availability
        try {
            if (!Azure.az(AzureMySql.class).subscription(textField.getSubscriptionId()).checkNameAvailability(value)) {
                return AzureValidationInfo.builder().input(textField).message(value + " already existed.").type(AzureValidationInfo.Type.ERROR).build();
            }
        } catch (CloudException e) {
            return AzureValidationInfo.builder().input(textField).message(e.getMessage()).type(AzureValidationInfo.Type.ERROR).build();
        }
        return AzureValidationInfo.OK;
    }
}
