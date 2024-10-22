/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.cosmos.dbtools;

import com.intellij.credentialStore.OneTimeString;
import com.intellij.database.dataSource.LocalDataSource;
import com.intellij.database.dataSource.url.DataInterchange;
import com.intellij.database.dataSource.url.FieldSize;
import com.intellij.openapi.actionSystem.AnAction;
import com.microsoft.azure.toolkit.intellij.dbtools.AzureParamsEditorBase;
import com.microsoft.azure.toolkit.intellij.dbtools.NoResourceTipLabel;
import com.microsoft.azure.toolkit.intellij.dbtools.NotSignedInTipLabel;
import com.microsoft.azure.toolkit.lib.common.operation.AzureOperation;
import com.microsoft.azure.toolkit.lib.cosmos.CosmosDBAccount;
import com.microsoft.azure.toolkit.lib.cosmos.model.CosmosDBAccountConnectionString;
import com.microsoft.azure.toolkit.lib.cosmos.model.DatabaseAccountKind;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.annotation.Nonnull;
import java.util.Objects;
import java.util.Optional;

public class AzureCosmosDbAccountParamEditor extends AzureParamsEditorBase<CosmosDbAccountComboBox, CosmosDBAccount> {
    public static final String KEY_COSMOS_ACCOUNT_ID = "AZURE_COSMOS_ACCOUNT";
    public static final String KEY_FROM_AZURE_EXPLORER = "FROM_EXPLORER";
    @Nonnull
    private final DatabaseAccountKind databaseAccountKind;
    @Nullable
    private CosmosDBAccountConnectionString connectionString;

    public AzureCosmosDbAccountParamEditor(
            @Nonnull DatabaseAccountKind databaseAccountKind,
            @Nullable String caption,
            @NotNull DataInterchange interchange,
            AnAction @NotNull ... actions) {
        super(new CosmosDbAccountComboBox(databaseAccountKind), interchange, FieldSize.LARGE, caption, KEY_COSMOS_ACCOUNT_ID, actions);
        this.databaseAccountKind = databaseAccountKind;
        final CosmosDbAccountComboBox comboBox = this.getEditorComponent();
        final String accountId = interchange.getProperty(KEY_COSMOS_ACCOUNT_ID);
        if (StringUtils.isNotBlank(accountId)) {
            comboBox.setByResourceId(accountId);
        }
    }

    @Override
    protected NotSignedInTipLabel createNotSignedInTipLabel() {
        return new CosmosNotSignedInLabel(databaseAccountKind);
    }

    @Override
    protected NoResourceTipLabel createNoResourceTipLabel() {
        return new NoAccountsTipLabel(databaseAccountKind);
    }

    @Override
    @AzureOperation(name = "user/cosmos.select_account_dbtools.account", params = {"value.getName()"}, source = "value")
    protected void setResource(
            @Nonnull DataInterchange interchange,
            @Nullable Object fromBackground,
            @Nullable CosmosDBAccount value,
            @Nullable String oldResourceId,
            @Nullable String newResourceId) {
        this.connectionString = (CosmosDBAccountConnectionString) fromBackground;
        final boolean fromExplorer = Objects.nonNull(interchange.getProperty(KEY_FROM_AZURE_EXPLORER));
        interchange.putProperty(KEY_FROM_AZURE_EXPLORER, null);
        interchange.putProperty(KEY_COSMOS_ACCOUNT_ID, Optional.ofNullable(newResourceId).orElse(NONE));
        if (Objects.isNull(value) || Objects.isNull(connectionString) || StringUtils.equalsIgnoreCase(oldResourceId, newResourceId) && !fromExplorer) {
            return;
        }
        final LocalDataSource dataSource = interchange.getDataSource();
        final String host = connectionString.getHost();
        final String port = String.valueOf(connectionString.getPort());
        final String user = connectionString.getUsername();
        final String password = String.valueOf(connectionString.getPassword());
        LocalDataSource.setUsername(dataSource, user);
        interchange.getCredentials().storePassword(dataSource, new OneTimeString(password));
        this.setUseSsl(true);
        interchange.putProperties(consumer -> {
            consumer.consume("host", host);
            consumer.consume("user", user);
            consumer.consume("port", port);
        });
        this.setUsername(user);
    }

    @Override
    protected Object doBackgroundUpdateForSetResource(@Nullable CosmosDBAccount value) {
        return Optional.ofNullable(value).map(CosmosDBAccount::getCosmosDBAccountPrimaryConnectionString).orElse(null);
    }

    @Override
    protected boolean needsBackgroundUpdateForSetResource() {
        return true;
    }

    @Override
    protected String getHostFromValue(CosmosDBAccount value) {
        return Optional.ofNullable(this.connectionString).map(CosmosDBAccountConnectionString::getHost).orElse(null);
    }
}