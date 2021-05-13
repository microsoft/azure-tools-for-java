/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */
package com.microsoft.azure.toolkit.intellij.appservice;

import com.microsoft.azure.toolkit.intellij.common.ValidationDebouncedTextInput;
import com.microsoft.azure.toolkit.lib.common.form.AzureValidationInfo;
import com.microsoft.azure.toolkit.lib.common.model.Subscription;
import com.microsoft.azuretools.azurecommons.helpers.NotNull;
import com.microsoft.intellij.util.ValidationUtils;

import java.util.Objects;

public class AppNameInput extends ValidationDebouncedTextInput {
    private Subscription subscription;

    public void setSubscription(Subscription subscription) {
        if (!Objects.equals(subscription, this.subscription)) {
            this.subscription = subscription;
            this.revalidateValue();
        }
    }

    @NotNull
    public AzureValidationInfo doValidateValue() {
        final AzureValidationInfo info = super.doValidateValue();
        if (info == AzureValidationInfo.OK) {
            try {
                ValidationUtils.validateAppServiceName(subscription != null ? subscription.getId() : null, this.getValue());
            } catch (final IllegalArgumentException e) {
                final AzureValidationInfo.AzureValidationInfoBuilder builder = AzureValidationInfo.builder();
                return builder.input(this).message(e.getMessage()).type(AzureValidationInfo.Type.ERROR).build();
            }
        }
        return info;
    }
}
