/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.common;

import com.intellij.ui.components.JBTextField;
import com.microsoft.azure.toolkit.lib.common.form.AzureValidationInfo;
import com.microsoft.azuretools.azurecommons.helpers.NotNull;
import lombok.Getter;
import lombok.Setter;
import org.apache.commons.lang.StringUtils;

import javax.swing.*;

import static com.microsoft.intellij.ui.messages.AzureBundle.message;

public class IntegerTextField extends JBTextField implements AzureFormInputComponent<Integer> {

    @Setter
    private boolean isRequired;
    @Setter
    @Getter
    private Integer minValue;
    @Getter
    @Setter
    private Integer maxValue;

    @Override
    public Integer getValue() {
        final String text = getText();
        return (StringUtils.isNotEmpty(text) && StringUtils.isNumeric(text)) ? Integer.valueOf(getText()) : null;
    }

    @Override
    public void setValue(final Integer val) {
        setText(val == null ? StringUtils.EMPTY : String.valueOf(val));
    }

    @NotNull
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
        Integer value = getValue();
        if (value == null) {
            return AzureValidationInfo.builder().input(this).type(AzureValidationInfo.Type.ERROR).message(message(
                    "common.integer.validate.notInteger")).build();
        } else if ((minValue != null && value < minValue) || (maxValue != null && value > maxValue)) {
            return AzureValidationInfo.builder().input(this).type(AzureValidationInfo.Type.ERROR)
                                      .message(message("common.integer.validate.invalidValue", minValue, maxValue)).build();
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
