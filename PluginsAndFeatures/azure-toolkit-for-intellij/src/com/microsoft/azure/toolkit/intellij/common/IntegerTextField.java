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

package com.microsoft.azure.toolkit.intellij.common;

import com.intellij.ui.components.JBTextField;
import com.microsoft.azure.toolkit.lib.common.form.AzureValidationInfo;
import com.microsoft.azuretools.azurecommons.helpers.NotNull;
import lombok.Getter;
import lombok.Setter;
import org.apache.commons.lang.StringUtils;

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
            return AzureValidationInfo.builder().input(this).type(AzureValidationInfo.Type.ERROR).message("Value should be an integer").build();
        } else if ((minValue != null && value < minValue) || (maxValue != null && value > maxValue)) {
            return AzureValidationInfo.builder().input(this).type(AzureValidationInfo.Type.ERROR)
                                      .message(String.format("Value should be in range [%s,%s]", minValue, maxValue)).build();
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
