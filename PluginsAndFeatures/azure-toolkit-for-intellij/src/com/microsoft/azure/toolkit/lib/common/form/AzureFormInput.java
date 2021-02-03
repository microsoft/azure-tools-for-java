/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
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
