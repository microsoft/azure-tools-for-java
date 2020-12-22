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

package com.microsoft.azure.toolkit.lib.common.form;

import com.microsoft.azuretools.azurecommons.helpers.NotNull;
import org.apache.commons.lang3.ObjectUtils;

import static com.microsoft.intellij.ui.messages.AzureBundle.message;

public interface AzureFormInput<T> extends Validatable {

    String MSG_REQUIRED = message("common.input.validate.fieldRequired");

    T getValue();

    void setValue(final T val);

    @NotNull
    default AzureValidationInfo doValidate() {
        final T value = this.getValue();
        if (this.isRequired() && ObjectUtils.isEmpty(value)) {
            final AzureValidationInfo.AzureValidationInfoBuilder builder = AzureValidationInfo.builder();
            return builder.message(MSG_REQUIRED).input(this).type(AzureValidationInfo.Type.ERROR).build();
        }
        return Validatable.super.doValidate();
    }

    default boolean isRequired() {
        return false;
    }
}
