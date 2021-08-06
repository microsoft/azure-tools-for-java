/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.common;

import com.microsoft.azure.toolkit.lib.common.form.AzureValidationInfo;
import lombok.Getter;
import org.apache.commons.lang3.StringUtils;

import javax.swing.*;
import javax.swing.event.DocumentListener;

public class AzurePasswordFieldInput implements AzureFormInputComponent<String> {

    private JPasswordField delegate;

    @Getter
    private boolean passwordInitialized;

    public AzurePasswordFieldInput(JPasswordField delegate) {
        this.delegate = delegate;
        this.delegate.getDocument().addDocumentListener(generateInitializedListener());
    }


    public AzurePasswordFieldInput(JPasswordField delegate, final boolean passwordInitialized) {
        this.delegate = delegate;
        this.passwordInitialized = passwordInitialized;
    }

    @Override
    public AzureValidationInfo doValidate() {
        if (!isPasswordInitialized()) {
            return AzureValidationInfo.UNINITIALIZED;
        }
        return AzureFormInputComponent.super.doValidate();
    }

    @Override
    public String getValue() {
        char[] password = this.delegate.getPassword();
        return password != null ? String.valueOf(password) : StringUtils.EMPTY;
    }

    @Override
    public void setValue(final String val) {
        this.delegate.setText(val);
    }

    @Override
    public JComponent getInputComponent() {
        return delegate;
    }

    private DocumentListener generateInitializedListener() {
        return new TextDocumentListenerAdapter() {
            @Override
            public void onDocumentChanged() {
                if (!passwordInitialized) {
                    passwordInitialized = true;
                }
            }
        };
    }

}
