/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.connector.aad;

import com.microsoft.azure.toolkit.intellij.common.AzureTextInput;
import com.microsoft.azure.toolkit.lib.common.form.AzureValidationInfo;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;

/**
 * Component for the client ID text input.
 */
public class AzureClientIdInput extends AzureTextInput {
    @NotNull
    @Override
    public AzureValidationInfo doValidate() {
        var value = this.getValue();
        if (value == null || value.isEmpty() || isValid(value)) {
            return super.doValidate();
        }

        return AzureValidationInfo.builder()
                .input(this)
                .message(MessageBundle.message("action.azure.aad.registerApp.clientIdInvalid"))
                .build();
    }

    @SuppressWarnings("ResultOfMethodCallIgnored")
    static boolean isValid(@NotNull String value) {
        if (value.isBlank()) {
            return false;
        }

        try {
            UUID.fromString(value);
            return value.length() == 36;
        } catch (Exception e) {
            return false;
        }
    }
}
