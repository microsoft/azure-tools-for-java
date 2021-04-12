/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.link.mysql;

import com.microsoft.azure.toolkit.intellij.common.AzureComboBox;

import java.util.Arrays;
import java.util.List;

public class PasswordSaveComboBox extends AzureComboBox<PasswordSaveType> {

    public PasswordSaveComboBox() {
        super(false);
        this.refreshItems();
    }

    @Override
    protected List<? extends PasswordSaveType> loadItems() {
        return Arrays.asList(PasswordSaveType.values());
    }

    @Override
    protected String getItemText(Object item) {
        if (item instanceof PasswordSaveType) {
            return ((PasswordSaveType) item).getName();
        }
        return super.getItemText(item);
    }

    @Override
    public boolean isRequired() {
        return true;
    }
}
