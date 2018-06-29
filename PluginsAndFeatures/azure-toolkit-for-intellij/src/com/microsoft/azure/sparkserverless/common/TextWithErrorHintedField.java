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

import com.intellij.ui.DocumentAdapter;
import com.microsoft.azuretools.azurecommons.helpers.NotNull;
import com.microsoft.azuretools.azurecommons.helpers.Nullable;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import java.util.Set;
import java.util.regex.Pattern;

public class TextWithErrorHintedField extends JTextField implements Validatable {
    @Nullable
    private String errorMessage;
    @Nullable
    private Set<String> notAllowedValues;
    @NotNull
    private final ErrorMessageTooltip errorMessageTooltip = new ErrorMessageTooltip(this);
    @NotNull
    private Pair<Pattern, String> patternAndErrorMessagePair = Pair.of(Pattern.compile("^[a-zA-Z0-9_-]+"),
            "Only alphanumeric characters, underscores and hyphens are allowed");

    public TextWithErrorHintedField() {
        super();
        this.getDocument().addDocumentListener(new DocumentAdapter() {
            @Override
            protected void textChanged(DocumentEvent e) {
                errorMessageTooltip.setVisible(TextWithErrorHintedField.this);
            }
        });
    }

    @Nullable
    protected String validateTextField() {
        if (StringUtils.isEmpty(getText())) {
            setErrorMessage("A string is expected");
        } else if (patternAndErrorMessagePair != null && patternAndErrorMessagePair.getLeft() != null &&
                !patternAndErrorMessagePair.getLeft().matcher(getText()).matches()) {
            if (patternAndErrorMessagePair.getRight() != null) {
                setErrorMessage(patternAndErrorMessagePair.getRight());
            } else {
                setErrorMessage(String.format("%s does not match the pattern %s", getText(),
                        patternAndErrorMessagePair.getLeft().toString()));
            }
        } else if (notAllowedValues != null && notAllowedValues.contains(getText())) {
            setErrorMessage(String.format("%s already exists", getText()));
        } else {
            setErrorMessage(null);
        }
        return getErrorMessage();
    }

    @Nullable
    @Override
    public String getErrorMessage() {
        return errorMessage;
    }

    private void setErrorMessage(@Nullable String errorMessage) {
        this.errorMessage = errorMessage;
    }

    public void setNotAllowedValues(@Nullable Set<String> notAllowedValues) {
        this.notAllowedValues = notAllowedValues;
    }

    public void setPatternAndErrorMessage(@Nullable Pair<Pattern, String> patternAndErrorMessagePair) {
            this.patternAndErrorMessagePair = patternAndErrorMessagePair;
    }

    @Override
    public boolean isLegal() {
        return StringUtils.isEmpty(validateTextField());
    }
}
