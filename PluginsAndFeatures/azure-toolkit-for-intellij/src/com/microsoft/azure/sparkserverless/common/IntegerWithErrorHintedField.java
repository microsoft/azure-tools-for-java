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

package com.microsoft.azure.sparkserverless.common;

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
