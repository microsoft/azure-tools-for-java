/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.database.dbtools;

import com.microsoft.azure.toolkit.intellij.dbtools.DbUtilsComboBoxBase;
import com.microsoft.azure.toolkit.lib.Azure;
import com.microsoft.azure.toolkit.lib.common.cache.CacheManager;
import com.microsoft.azure.toolkit.lib.database.entity.IDatabaseServer;
import com.microsoft.azure.toolkit.lib.mysql.AzureMySql;
import com.microsoft.azure.toolkit.lib.postgre.AzurePostgreSql;
import com.microsoft.azure.toolkit.lib.sqlserver.AzureSqlServer;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.List;
import java.util.stream.Collectors;

@Getter
@RequiredArgsConstructor
public class SqlDbServerComboBox extends DbUtilsComboBoxBase<IDatabaseServer<?>> {
    @Nonnull
    private final DatabaseServerClass databaseServerClass;

    @Nullable
    @Override
    protected IDatabaseServer<?> doGetDefaultValue() {
        return CacheManager.getUsageHistory(databaseServerClass.getClazz()).peek();
    }

    @Override
    protected List<IDatabaseServer<?>> load() {
        final List<? extends IDatabaseServer<?>> allServers = switch (databaseServerClass) {
            case MySql -> Azure.az(AzureMySql.class).servers();
            case MsSql -> Azure.az(AzureSqlServer.class).servers();
            case Postgres -> Azure.az(AzurePostgreSql.class).servers();
        };

        return getRunningServers(allServers);
    }

    private List<IDatabaseServer<?>> getRunningServers(List<? extends IDatabaseServer<?>> allServers) {
        return allServers.stream().filter(s -> s.getFormalStatus().isRunning()).collect(Collectors.toList());
    }
}
