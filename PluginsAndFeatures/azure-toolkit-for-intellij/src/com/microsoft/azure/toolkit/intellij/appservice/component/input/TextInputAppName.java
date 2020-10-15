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

package com.microsoft.azure.toolkit.intellij.appservice.component.input;

import com.microsoft.azure.management.resources.Subscription;
import com.microsoft.azure.toolkit.lib.AzureValidationInfo;
import com.microsoft.azure.toolkit.lib.utils.Debouncer;
import com.microsoft.azure.toolkit.lib.utils.TailingDebouncer;
import com.microsoft.azuretools.azurecommons.helpers.NotNull;
import com.microsoft.intellij.util.ValidationUtils;

import java.util.Objects;

public class TextInputAppName extends AzureTextField {

    public static final int DEBOUNCE_DELAY = 500;
    private final Debouncer validator;
    private Subscription subscription;
    private AzureValidationInfo validationInfo;

    public TextInputAppName() {
        super();
        this.validator = new TailingDebouncer(this::revalidateValue, DEBOUNCE_DELAY);
    }

    @Override
    public AzureValidationInfo validateValue() {
        if (this.validator.isPending()) {
            return AzureValidationInfo.PENDING;
        } else if (this.validationInfo == null) {
            this.validationInfo = this.doValidate();
        }
        return this.validationInfo;
    }

    private void revalidateValue() {
        this.validationInfo = this.doValidate();
    }

    public void setSubscription(Subscription subscription) {
        if (!Objects.equals(subscription, this.subscription)) {
            this.subscription = subscription;
            this.validator.debounce();
        }
    }

    public void onDocumentChanged() {
        this.validator.debounce();
    }

    @NotNull
    private AzureValidationInfo doValidate() {
        final AzureValidationInfo info = super.validateValue();
        if (info == AzureValidationInfo.OK) {
            try {
                ValidationUtils.validateAppServiceName(this.subscription.subscriptionId(), this.getValue());
            } catch (final IllegalArgumentException e) {
                return AzureValidationInfo.builder()
                                          .input(this)
                                          .message(e.getMessage())
                                          .type(AzureValidationInfo.Type.ERROR)
                                          .build();
            }
        }
        return info;
    }
}
