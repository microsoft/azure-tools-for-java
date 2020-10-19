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
import com.microsoft.azure.toolkit.lib.common.utils.Debouncer;
import com.microsoft.azure.toolkit.lib.common.utils.TailingDebouncer;

public class ValidationDebouncedTextInput extends AzureTextInput {
    protected static final int DEBOUNCE_DELAY = 500;
    protected AzureValidationInfo validationInfo;
    private final Debouncer validator;

    public ValidationDebouncedTextInput() {
        super();
        this.validator = new TailingDebouncer(() -> this.validationInfo = this.doValidateValue(), DEBOUNCE_DELAY);
    }

    protected AzureValidationInfo doValidateValue() {
        return super.doValidate();
    }

    @Override
    public AzureValidationInfo doValidate() {
        if (this.validator.isPending()) {
            return AzureValidationInfo.PENDING;
        } else if (this.validationInfo == null) {
            this.validationInfo = this.doValidateValue();
        }
        return this.validationInfo;
    }

    protected void revalidateValue() {
        this.validator.debounce();
    }

    public void onDocumentChanged() {
        this.revalidateValue();
    }
}
