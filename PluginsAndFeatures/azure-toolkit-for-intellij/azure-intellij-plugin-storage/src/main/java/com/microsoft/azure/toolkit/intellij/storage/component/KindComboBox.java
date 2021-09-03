/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.storage.component;

import com.microsoft.azure.toolkit.intellij.common.AzureComboBox;
import com.microsoft.azure.toolkit.lib.Azure;
import com.microsoft.azure.toolkit.lib.common.operation.AzureOperation;
import com.microsoft.azure.toolkit.lib.storage.model.Kind;
import com.microsoft.azure.toolkit.lib.storage.model.Performance;
import com.microsoft.azure.toolkit.lib.storage.service.AzureStorageAccount;

import javax.annotation.Nonnull;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

public class KindComboBox extends AzureComboBox<Kind> {

    private Performance performance;

    public void setPerformance(Performance performance) {
        if (Objects.equals(performance, this.performance)) {
            return;
        }
        this.performance = performance;
        if (performance == null) {
            this.clear();
            return;
        }
        this.refreshItems();
    }

    @Nonnull
    @Override
    @AzureOperation(
            name = "storage|account.kind.list.supported",
            type = AzureOperation.Type.SERVICE
    )
    protected List<? extends Kind> loadItems() {
        return Objects.isNull(this.performance) ? Collections.emptyList() : Azure.az(AzureStorageAccount.class).listSupportedKinds(this.performance);
    }

    @Override
    protected String getItemText(Object item) {
        return item instanceof Kind ? ((Kind) item).getLabel() : super.getItemText(item);
    }
}
