/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.connector.aad;

import com.microsoft.azure.toolkit.intellij.common.AzureTextInput;
import com.microsoft.azure.toolkit.lib.common.form.AzureValidationInfo;
import org.jetbrains.annotations.NotNull;

import java.net.URI;

/**
 * Component for the callback URL text input. It expects a valid URL with scheme and hostname.
 */
public class AzureCallbackURLInput extends AzureTextInput {
    @NotNull
    @Override
    public AzureValidationInfo doValidate() {
        var value = this.getValue();
        if (value == null || value.isEmpty() || isValid(value)) {
            return super.doValidate();
        }

        return AzureValidationInfo.builder()
                .input(this)
                .message(MessageBundle.message("action.azure.aad.registerApp.callbackURLInvalid"))
                .build();

    }

    static boolean isValid(@NotNull String value) {
        try {
            var uri = URI.create(value);
            return !uri.getScheme().isEmpty() && !uri.getHost().isEmpty();
        } catch (Exception e) {
            return false;
        }
    }
}
