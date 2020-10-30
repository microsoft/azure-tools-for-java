package com.microsoft.azure.toolkit.intellij.common;

import com.intellij.ui.components.JBTextField;
import com.microsoft.azure.toolkit.lib.common.form.AzureFormInput;
import com.microsoft.azure.toolkit.lib.common.form.AzureValidationInfo;
import com.microsoft.azuretools.azurecommons.helpers.NotNull;
import com.microsoft.azuretools.azurecommons.helpers.Nullable;
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
        AzureValidationInfo result = AzureFormInputComponent.super.doValidate();
        if (result.getType() == AzureValidationInfo.Type.ERROR) {
            return result;
        }
        final String input = getText();
        if (StringUtils.isEmpty(input)) {
            return AzureValidationInfo.OK;
        }
        Integer value = getValue();
        if (value == null) {
            return AzureValidationInfo.builder().input(this).type(AzureValidationInfo.Type.ERROR).message("Value should be integer").build();
        } else if ((minValue != null && value < minValue) || (maxValue != null && value > maxValue)) {
            return AzureValidationInfo.builder().input(this).type(AzureValidationInfo.Type.ERROR)
                                      .message(String.format("Value should be in rang [%s,%s]", minValue, maxValue)).build();
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
