/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.connector.database.component;

import com.microsoft.azure.toolkit.intellij.common.AzureComboBox;
import com.microsoft.azure.toolkit.intellij.connector.Password;

import javax.annotation.Nonnull;
import java.util.Arrays;
import java.util.List;

public class PasswordSaveComboBox extends AzureComboBox<Password.SaveType> {

    public PasswordSaveComboBox() {
        super(false);
        this.refreshItems();
    }

    @Override
    @Nonnull
    protected List<? extends Password.SaveType> loadItems() {
        return Arrays.asList(Password.SaveType.values());
    }

    @Override
    protected String getItemText(Object item) {
        if (item instanceof Password.SaveType) {
            return ((Password.SaveType) item).title();
        }
        return super.getItemText(item);
    }

    @Override
    public boolean isRequired() {
        return true;
    }
}
