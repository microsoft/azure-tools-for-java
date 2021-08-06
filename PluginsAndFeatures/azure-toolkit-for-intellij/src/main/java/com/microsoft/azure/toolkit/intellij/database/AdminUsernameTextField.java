/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */
package com.microsoft.azure.toolkit.intellij.database;

import com.microsoft.azure.toolkit.intellij.common.ValidationDebouncedTextInput;
import com.microsoft.azure.toolkit.lib.common.form.AzureValidationInfo;
import com.microsoft.azuretools.azurecommons.helpers.NotNull;
import lombok.Getter;
import lombok.Setter;
import org.apache.commons.lang3.StringUtils;

import java.util.regex.Pattern;

public class AdminUsernameTextField extends ValidationDebouncedTextInput {

    private static final int MIN_LENGTH = 1;
    private static final int MAX_LENGTH = 16;
    private static final Pattern PATTERN = Pattern.compile("^[a-zA-Z][a-zA-Z0-9]*");

    private static final String INVALID_LENGTH_MESSAGE = "Admin username must be at least 1 characters and at most 16 characters.";
    private static final String INVALID_ALPHANUMERIC_MESSAGE = "Admin username must only contain characters and numbers.";
    private static final String[] INVALID_USERNAMES = new String[]{"azure_superuser", "admin", "administrator", "root", "guest", "public"};
    private static final String INVALID_USERNAMES_MESSAGE =
            "Admin login name cannot be 'azure_superuser', 'admin', 'administrator', 'root', 'guest' or 'public'.";
    private static final String INVALID_LANGUAGE_MESSAGE = "Your login name is a SQL Identifier or a built-in database users or role. " +
            "Make sure it doesn’t contain whitespaces, Unicode characters, or nonalphabetic characters, and doesn’t begin with numbers or symbols.";

    @Getter
    @Setter
    private boolean valueInitialized;

    /**
     * Admin username must be at least 1 characters and at most 16 characters.
     * Admin username must only contain characters and numbers.
     * Admin login name cannot be 'azure_superuser', 'admin', 'administrator', 'root', 'guest' or 'public'
     */
    @NotNull
    public AzureValidationInfo doValidateValue() {
        if (!isValueInitialized()) {
            return AzureValidationInfo.UNINITIALIZED;
        }
        final AzureValidationInfo info = super.doValidateValue();
        if (!AzureValidationInfo.OK.equals(info)) {
            return info;
        }
        final String value = this.getValue();
        // validate length
        if (StringUtils.length(value) < MIN_LENGTH || StringUtils.length(value) > MAX_LENGTH) {
            final AzureValidationInfo.AzureValidationInfoBuilder builder = AzureValidationInfo.builder();
            return builder.input(this).message(INVALID_LENGTH_MESSAGE).type(AzureValidationInfo.Type.ERROR).build();
        }
        // validate special character
        if (!StringUtils.isAlphanumeric(value)) {
            final AzureValidationInfo.AzureValidationInfoBuilder builder = AzureValidationInfo.builder();
            return builder.input(this).message(INVALID_ALPHANUMERIC_MESSAGE).type(AzureValidationInfo.Type.ERROR).build();
        }
        // validate special character
        if (!PATTERN.matcher(value).matches()) {
            final AzureValidationInfo.AzureValidationInfoBuilder builder = AzureValidationInfo.builder();
            return builder.input(this).message(INVALID_LANGUAGE_MESSAGE).type(AzureValidationInfo.Type.ERROR).build();
        }
        // validate special admin username
        if (StringUtils.equalsAnyIgnoreCase(value, INVALID_USERNAMES)) {
            final AzureValidationInfo.AzureValidationInfoBuilder builder = AzureValidationInfo.builder();
            return builder.input(this).message(INVALID_USERNAMES_MESSAGE).type(AzureValidationInfo.Type.ERROR).build();
        }
        return AzureValidationInfo.OK;
    }

    @Override
    public boolean isRequired() {
        return true;
    }

}
