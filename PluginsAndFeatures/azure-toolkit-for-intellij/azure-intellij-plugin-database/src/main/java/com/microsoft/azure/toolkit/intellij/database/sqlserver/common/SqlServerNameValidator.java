/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */
package com.microsoft.azure.toolkit.intellij.database.sqlserver.common;

import com.microsoft.azure.CloudException;
import com.microsoft.azure.toolkit.intellij.database.ServerNameTextField;
import com.microsoft.azure.toolkit.lib.Azure;
import com.microsoft.azure.toolkit.lib.common.entity.CheckNameAvailabilityResultEntity;
import com.microsoft.azure.toolkit.lib.common.form.AzureFormInput;
import com.microsoft.azure.toolkit.lib.common.form.AzureValidationInfo;
import com.microsoft.azure.toolkit.lib.sqlserver.AzureSqlServer;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;

import java.util.regex.Pattern;

@RequiredArgsConstructor
public class SqlServerNameValidator implements AzureFormInput.Validator {
    private static final Pattern PATTERN = Pattern.compile("^[a-z0-9][a-z0-9-]+[a-z0-9]$");
    private static final int MIN_LENGTH = 3;
    private static final int MAX_LENGTH = 63;
    private final ServerNameTextField input;

    @Override
    public AzureValidationInfo doValidate() {
        final String value = input.getValue();
        if (StringUtils.length(value) < MIN_LENGTH || StringUtils.length(value) > MAX_LENGTH) { // validate length
            return AzureValidationInfo.builder().input(input)
                .message(String.format("Server name must be at least %s characters and at most %s characters.", MIN_LENGTH, MAX_LENGTH))
                .type(AzureValidationInfo.Type.ERROR).build();
        } else if (!PATTERN.matcher(value).matches()) { // validate special character
            return AzureValidationInfo.builder().input(input)
                .message("Your server name can contain only lowercase letters, numbers, and '-', but can't start or end with '-'.")
                .type(AzureValidationInfo.Type.ERROR).build();
        }
        try { // validate availability
            final CheckNameAvailabilityResultEntity availability =
                Azure.az(AzureSqlServer.class).checkNameAvailability(input.getSubscription().getId(), value);
            if (!availability.isAvailable()) {
                String message = availability.getUnavailabilityReason();
                if ("AlreadyExists".equalsIgnoreCase(message)) {
                    message = String.format("name \"%s\" is already in use.", value);
                }
                return AzureValidationInfo.error(message, input);
            }
        } catch (final CloudException e) {
            return AzureValidationInfo.error(e.getMessage(), input);
        }
        return AzureValidationInfo.success(input);
    }
}
