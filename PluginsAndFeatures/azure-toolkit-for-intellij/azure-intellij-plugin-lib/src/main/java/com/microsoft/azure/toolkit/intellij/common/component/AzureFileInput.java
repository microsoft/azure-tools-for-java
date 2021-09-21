/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */
package com.microsoft.azure.toolkit.intellij.common.component;

import com.intellij.openapi.ui.TextFieldWithBrowseButton;
import com.microsoft.azure.toolkit.intellij.common.AzureFormInputComponent;
import lombok.Getter;
import lombok.Setter;

import javax.swing.*;

public class AzureFileInput extends TextFieldWithBrowseButton implements AzureFormInputComponent<String> {
    @Getter
    @Setter
    private boolean required;

    @Override
    public JComponent getInputComponent() {
        return this;
    }

    @Override
    public String getValue() {
        return this.getText();
    }

    @Override
    public void setValue(String val) {
        this.setText(val);
    }
}
