/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.common;

import com.intellij.ui.components.fields.ExtendableTextField;
import lombok.Getter;
import lombok.Setter;

import javax.swing.*;

public class AzureTextInput extends ExtendableTextField
    implements AzureFormInputComponent<String>, TextDocumentListenerAdapter {
    @Getter
    @Setter
    private boolean required;
    @Getter
    @Setter
    private Validator validator;

    public AzureTextInput() {
        super();
        this.getDocument().addDocumentListener(this);
    }

    @Override
    public String getValue() {
        return this.getText();
    }

    @Override
    public void setValue(final String val) {
        this.setText(val);
    }

    @Override
    public JComponent getInputComponent() {
        return this;
    }
}
