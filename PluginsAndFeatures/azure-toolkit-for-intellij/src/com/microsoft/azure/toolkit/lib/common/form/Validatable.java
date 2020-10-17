package com.microsoft.azure.toolkit.lib.common.form;

import com.microsoft.azuretools.azurecommons.helpers.Nullable;

import java.util.Objects;

public interface Validatable {
    default AzureValidationInfo doValidate() {
        final Validator validator = this.getValidator();
        if (Objects.nonNull(validator)) {
            return validator.doValidate();
        }
        return AzureValidationInfo.OK;
    }

    @Nullable
    default Validator getValidator() {
        return null;
    }

    @FunctionalInterface
    interface Validator {
        AzureValidationInfo doValidate();
    }
}
