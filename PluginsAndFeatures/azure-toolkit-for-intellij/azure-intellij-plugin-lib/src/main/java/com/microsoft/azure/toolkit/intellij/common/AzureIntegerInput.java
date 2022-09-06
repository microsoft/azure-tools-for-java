/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.common;

import com.microsoft.azure.toolkit.lib.common.exception.AzureToolkitRuntimeException;
import com.microsoft.azure.toolkit.lib.common.form.AzureValidationInfo;
import lombok.Getter;
import lombok.Setter;
import org.apache.commons.lang3.StringUtils;

import javax.annotation.Nullable;
import java.util.Objects;

public class AzureIntegerInput extends BaseAzureTextInput<Integer> {

    @Setter
    @Getter
    private Integer minValue;
    @Getter
    @Setter
    private Integer maxValue;

    public AzureIntegerInput() {
        super();
        this.addValidator(this::doValidateValue);
        this.setRequired(true);
    }

    @Nullable
    @Override
    public Integer getValue() {
        final String text = getText();
        if (StringUtils.isBlank(text)) {
            return getDefaultValue();
        }
        try {
            return Integer.parseInt(text);
        } catch (final Exception e) {
            throw new AzureToolkitRuntimeException(String.format("\"%s\" is not an integer", text));
        }
    }

    @Override
    public void setValue(final Integer val) {
        setText(val == null ? StringUtils.EMPTY : String.valueOf(val));
    }

    public AzureValidationInfo doValidateValue() {
        final Integer value = this.getValue();
        if (Objects.isNull(value)) {
            return AzureValidationInfo.none(this);
        }
        if (Objects.nonNull(minValue) && Objects.nonNull(maxValue) && (value < minValue || value > maxValue)) {
            return AzureValidationInfo.error(String.format("Value should be in range [%d, %d]", minValue, maxValue), this);
        } else if (Objects.nonNull(minValue) && value < minValue) {
            return AzureValidationInfo.error(String.format("Value should be >= %d", minValue), this);
        } else if (Objects.nonNull(maxValue) && value > maxValue) {
            return AzureValidationInfo.error(String.format("Value should be <= %d", maxValue), this);
        } else {
            return AzureValidationInfo.success(this);
        }
    }
}
