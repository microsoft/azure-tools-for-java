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

import com.microsoft.azure.toolkit.lib.common.form.AzureValidationInfo;
import lombok.Getter;
import lombok.Setter;
import org.apache.commons.lang3.StringUtils;

import javax.swing.*;

public class AzurePasswordFieldInput implements AzureFormInputComponent<String> {

    private JPasswordField delegate;

    @Getter
    @Setter
    private boolean passwordInitialized;

    public AzurePasswordFieldInput(JPasswordField delegate) {
        this.delegate = delegate;
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

}
