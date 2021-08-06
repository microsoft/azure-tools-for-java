/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.cosmosspark.common;

import com.intellij.openapi.options.ConfigurationException;
import com.intellij.ui.DocumentAdapter;
import com.intellij.ui.components.fields.IntegerField;
import com.microsoft.azuretools.azurecommons.helpers.NotNull;
import com.microsoft.azuretools.azurecommons.helpers.Nullable;

import javax.swing.event.DocumentEvent;

public class IntegerWithErrorHintedField extends IntegerField implements Validatable {
    @NotNull
    private final ErrorMessageTooltip errorMessageTooltip = new ErrorMessageTooltip(this);

    public IntegerWithErrorHintedField() {
        super();
        this.getDocument().addDocumentListener(new DocumentAdapter() {
            @Override
            protected void textChanged(DocumentEvent e) {
                errorMessageTooltip.setVisible(IntegerWithErrorHintedField.this);
            }
        });
    }

    @Override
    public boolean isLegal() {
        return getValueEditor().isValid(getValue());
    }

    @Nullable
    @Override
    public String getErrorMessage() {
        try {
            this.validateContent();
        } catch (ConfigurationException ex) {
            return ex.getMessage();
        }
        return null;
    }

}
