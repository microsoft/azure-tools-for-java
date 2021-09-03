/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */
package com.microsoft.azure.toolkit.intellij.storage.component;

import com.azure.core.management.exception.ManagementException;
import com.microsoft.azure.toolkit.intellij.common.ValidationDebouncedTextInput;
import com.microsoft.azure.toolkit.lib.Azure;
import com.microsoft.azure.toolkit.lib.common.entity.CheckNameAvailabilityResultEntity;
import com.microsoft.azure.toolkit.lib.common.form.AzureValidationInfo;
import com.microsoft.azure.toolkit.lib.storage.service.AzureStorageAccount;
import lombok.Getter;
import lombok.Setter;
import org.apache.commons.lang3.StringUtils;

import javax.annotation.Nonnull;
import java.util.regex.Pattern;

public class AccountNameTextField extends ValidationDebouncedTextInput {

    private static final Pattern PATTERN = Pattern.compile("[a-z0-9]{3,24}");
    @Setter
    private int minLength = 3;
    @Setter
    private int maxLength = 24;
    @Getter
    private String subscriptionId;

    public void setSubscriptionId(String subscriptionId) {
        if (!StringUtils.equals(subscriptionId, this.subscriptionId)) {
            this.subscriptionId = subscriptionId;
        }
    }

    @Override
    @Nonnull
    public AzureValidationInfo doValidateValue() {
        if (StringUtils.isBlank(subscriptionId)) {
            return AzureValidationInfo.UNINITIALIZED;
        }
        final AzureValidationInfo info = super.doValidateValue();
        if (!AzureValidationInfo.OK.equals(info)) {
            return info;
        }
        final String value = this.getValue();
        // validate length
        if (StringUtils.length(value) < minLength || StringUtils.length(value) > maxLength) {
            return AzureValidationInfo.builder().input(this)
                .message(String.format("Server name must be at least %s characters and at most %s characters.", minLength, maxLength))
                .type(AzureValidationInfo.Type.ERROR).build();
        }
        // validate special character
        if (!PATTERN.matcher(value).matches()) {
            return AzureValidationInfo.builder().input(this)
                .message("The field can contain only lowercase letters and numbers. Name must be between 3 and 24 characters.")
                .type(AzureValidationInfo.Type.ERROR).build();
        }
        // validate availability
        CheckNameAvailabilityResultEntity resultEntity;
        try {
            resultEntity = Azure.az(AzureStorageAccount.class).checkNameAvailability(subscriptionId, this.getValue());
        } catch (ManagementException e) {
            return AzureValidationInfo.builder().input(this).message(e.getMessage()).type(AzureValidationInfo.Type.ERROR).build();
        }
        if (!resultEntity.isAvailable()) {
            String message = resultEntity.getUnavailabilityReason();
            if ("AlreadyExists".equalsIgnoreCase(resultEntity.getUnavailabilityReason())) {
                message = "The specified storage account name is already taken.";
            }
            return AzureValidationInfo.builder().input(this).message(message).type(AzureValidationInfo.Type.ERROR).build();
        }
        return AzureValidationInfo.OK;
    }

    @Override
    public boolean isRequired() {
        return true;
    }

}
