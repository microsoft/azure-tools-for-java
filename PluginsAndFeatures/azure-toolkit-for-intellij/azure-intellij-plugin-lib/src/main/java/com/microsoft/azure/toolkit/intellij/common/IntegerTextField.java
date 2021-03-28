/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.common;

import com.intellij.ui.components.JBTextField;
import com.microsoft.azure.toolkit.lib.common.form.AzureValidationInfo;
import lombok.Getter;
import lombok.Setter;
import org.apache.commons.lang.StringUtils;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.swing.*;

public class IntegerTextField extends JBTextField implements AzureFormInputComponent<Integer> {

    @Setter
    private boolean isRequired;
    @Setter
    @Getter
    private Integer minValue;
    @Getter
    @Setter
    private Integer maxValue;

    @Nullable
    @Override
    public Integer getValue() {
        final String text = getText();
        return (StringUtils.isNotEmpty(text) && StringUtils.isNumeric(text)) ? Integer.valueOf(getText()) : null;
    }

    @Override
    public void setValue(final Integer val) {
        setText(val == null ? StringUtils.EMPTY : String.valueOf(val));
    }

    @Nonnull
    @Override
    public AzureValidationInfo doValidate() {
        if (!this.isEnabled() || !this.isVisible()) {
            return AzureValidationInfo.OK;
        }
        final String input = getText();
        if (StringUtils.isEmpty(input)) {
            if (this.isRequired()) {
                final AzureValidationInfo.AzureValidationInfoBuilder builder = AzureValidationInfo.builder();
                return builder.message(MSG_REQUIRED).input(this).type(AzureValidationInfo.Type.ERROR).build();
            }
            return AzureValidationInfo.OK;
        }
        final Integer value = getValue();
        if (value == null) {
            return AzureValidationInfo.builder().input(this).type(AzureValidationInfo.Type.ERROR).message("Value should be an integer").build();
        } else if ((minValue != null && value < minValue) || (maxValue != null && value > maxValue)) {
            return AzureValidationInfo.builder().input(this).type(AzureValidationInfo.Type.ERROR)
                    .message(String.format("Value should be in range [%d, %d]", minValue, maxValue)).build();
        } else {
            return AzureValidationInfo.OK;
        }
    }

    @Override
    public boolean isRequired() {
        return isRequired;
    }

    @Override
    public JComponent getInputComponent() {
        return this;
    }
}
