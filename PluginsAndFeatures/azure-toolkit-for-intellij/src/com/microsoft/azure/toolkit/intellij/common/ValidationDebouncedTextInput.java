/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.common;

import com.intellij.icons.AllIcons;
import com.intellij.ui.AnimatedIcon;
import com.microsoft.azure.toolkit.lib.common.form.AzureValidationInfo;
import com.microsoft.azure.toolkit.lib.common.utils.Debouncer;
import com.microsoft.azure.toolkit.lib.common.utils.TailingDebouncer;

public class ValidationDebouncedTextInput extends AzureTextInput {
    protected static final int DEBOUNCE_DELAY = 500;
    protected AzureValidationInfo validationInfo;
    private final Debouncer validator;
    private final Extension spinner = Extension.create(new AnimatedIcon.Default(), "validating", null);
    private final Extension invalid = Extension.create(AllIcons.General.BalloonError, "invalid", null);
    private final Extension valid = Extension.create(AllIcons.General.InspectionsOK, "valid", null);

    public ValidationDebouncedTextInput() {
        super();
        this.validator = new TailingDebouncer(() -> this.validationInfo = this.doValidateValue(), DEBOUNCE_DELAY);
    }

    protected AzureValidationInfo doValidateValue() {
        return super.doValidate();
    }

    @Override
    public AzureValidationInfo doValidate() {
        AzureValidationInfo info = this.validationInfo;
        if (this.validator.isPending()) {
            info = AzureValidationInfo.PENDING;
        } else if (this.validationInfo == null) {
            this.validationInfo = this.doValidateValue();
            info = this.validationInfo;
        }
        if (info == AzureValidationInfo.PENDING) {
            this.setExtensions(spinner);
        } else if (info == AzureValidationInfo.OK) {
            this.setExtensions(valid);
        } else if (info != AzureValidationInfo.UNINITIALIZED) {
            this.setExtensions(invalid);
        }
        return info;
    }

    protected void revalidateValue() {
        this.validator.debounce();
    }

    public void onDocumentChanged() {
        this.revalidateValue();
    }
}
