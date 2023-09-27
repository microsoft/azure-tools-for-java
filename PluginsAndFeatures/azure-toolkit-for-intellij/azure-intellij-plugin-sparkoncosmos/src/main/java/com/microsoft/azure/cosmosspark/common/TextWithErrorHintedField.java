/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.cosmosspark.common;

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
