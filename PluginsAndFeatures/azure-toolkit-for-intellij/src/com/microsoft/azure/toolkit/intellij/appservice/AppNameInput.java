/*
 * Copyright (c) Microsoft Corporation
 *
 * All rights reserved.
 *
 * MIT License
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated
 * documentation files (the "Software"), to deal in the Software without restriction, including without limitation
 * the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and
 * to permit persons to whom the Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of
 * the Software.
 *
 * THE SOFTWARE IS PROVIDED *AS IS*, WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO
 * THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT,
 * TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package com.microsoft.azure.toolkit.intellij.appservice;

import com.microsoft.azure.management.resources.Subscription;
import com.microsoft.azure.toolkit.intellij.common.ValidationDebouncedTextInput;
import com.microsoft.azure.toolkit.lib.common.form.AzureValidationInfo;
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
                ValidationUtils.validateAppServiceName(subscription != null ? subscription.subscriptionId() : null, this.getValue());
            } catch (final IllegalArgumentException e) {
                final AzureValidationInfo.AzureValidationInfoBuilder builder = AzureValidationInfo.builder();
                return builder.input(this).message(e.getMessage()).type(AzureValidationInfo.Type.ERROR).build();
            }
        }
        return info;
    }
}
