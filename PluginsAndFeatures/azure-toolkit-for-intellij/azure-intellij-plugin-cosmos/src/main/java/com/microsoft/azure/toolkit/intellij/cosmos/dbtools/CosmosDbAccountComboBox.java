/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.cosmos.dbtools;

import com.microsoft.azure.toolkit.intellij.dbtools.DbUtilsComboBoxBase;
import com.microsoft.azure.toolkit.lib.Azure;
import com.microsoft.azure.toolkit.lib.common.cache.CacheManager;
import com.microsoft.azure.toolkit.lib.cosmos.AzureCosmosService;
import com.microsoft.azure.toolkit.lib.cosmos.CosmosDBAccount;
import com.microsoft.azure.toolkit.lib.cosmos.model.DatabaseAccountKind;
import lombok.RequiredArgsConstructor;

import javax.annotation.Nullable;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@RequiredArgsConstructor
public class CosmosDbAccountComboBox extends DbUtilsComboBoxBase<CosmosDBAccount> {
    private final DatabaseAccountKind kind;

    @Nullable
    @Override
    protected CosmosDBAccount doGetDefaultValue() {
        return CacheManager.getUsageHistory(CosmosDBAccount.class)
                .peek(v -> Objects.isNull(kind) || Objects.equals(kind, v.getKind()));
    }

    @Override
    protected List<CosmosDBAccount> load() {
        return Azure.az(AzureCosmosService.class).getDatabaseAccounts(kind).stream()
                .filter(m -> !m.isDraftForCreating()).collect(Collectors.toList());
    }
}

