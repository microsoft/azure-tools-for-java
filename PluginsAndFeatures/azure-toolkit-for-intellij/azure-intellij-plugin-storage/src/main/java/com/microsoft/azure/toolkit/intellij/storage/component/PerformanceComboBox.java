/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.storage.component;

import com.intellij.ui.components.fields.ExtendableTextComponent;
import com.microsoft.azure.toolkit.intellij.common.AzureComboBox;
import com.microsoft.azure.toolkit.lib.Azure;
import com.microsoft.azure.toolkit.lib.common.operation.AzureOperation;
import com.microsoft.azure.toolkit.lib.storage.model.Performance;
import com.microsoft.azure.toolkit.lib.storage.AzureStorageAccount;

import javax.annotation.Nonnull;
import java.util.Collections;
import java.util.List;

public class PerformanceComboBox extends AzureComboBox<Performance> {

    @Nonnull
    @Override
    @AzureOperation(
            name = "storage|account.performance.list.supported",
            type = AzureOperation.Type.SERVICE
    )
    protected List<? extends Performance> loadItems() {
        return Azure.az(AzureStorageAccount.class).listSupportedPerformances();
    }

    @Override
    protected String getItemText(Object item) {
        return item instanceof Performance ? ((Performance) item).getName() : super.getItemText(item);
    }


    @Nonnull
    @Override
    protected List<ExtendableTextComponent.Extension> getExtensions() {
        return Collections.emptyList();
    }
}
