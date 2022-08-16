/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.cosmos.dbtools;

import com.intellij.credentialStore.OneTimeString;
import com.intellij.database.dataSource.DataSourceConfigurable;
import com.intellij.database.dataSource.LocalDataSource;
import com.intellij.database.dataSource.url.DataInterchange;
import com.intellij.database.dataSource.url.FieldSize;
import com.intellij.database.dataSource.url.template.UrlEditorModel;
import com.intellij.database.dataSource.url.ui.ParamEditorBase;
import com.intellij.ui.components.JBCheckBox;
import com.microsoft.azure.toolkit.intellij.common.AzureComboBox;
import com.microsoft.azure.toolkit.lib.Azure;
import com.microsoft.azure.toolkit.lib.auth.AzureAccount;
import com.microsoft.azure.toolkit.lib.common.model.AbstractAzResource;
import com.microsoft.azure.toolkit.lib.common.task.AzureTask;
import com.microsoft.azure.toolkit.lib.common.task.AzureTaskManager;
import com.microsoft.azure.toolkit.lib.cosmos.AzureCosmosService;
import com.microsoft.azure.toolkit.lib.cosmos.CosmosDBAccount;
import com.microsoft.azure.toolkit.lib.cosmos.model.CosmosDBAccountConnectionString;
import com.microsoft.azure.toolkit.lib.cosmos.model.DatabaseAccountKind;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.SneakyThrows;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.reflect.FieldUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.annotation.Nonnull;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

public class AzureCosmosDbAccountParamEditor extends ParamEditorBase<AzureCosmosDbAccountParamEditor.CosmosDbAccountComboBox> {
    public static final String KEY_COSMOS_ACCOUNT_ID = "AZURE_COSMOS_ACCOUNT";
    public static final String KEY_USERNAME = "username";
    public static final String NONE = "<NONE>";
    @Getter
    @Setter
    private String text = "";
    private CosmosDBAccount account;
    private CosmosDBAccountConnectionString connectionString;
    private boolean updating;

    public AzureCosmosDbAccountParamEditor(@Nonnull DatabaseAccountKind kind, @NotNull String label, @NotNull DataInterchange interchange) {
        super(new CosmosDbAccountComboBox(kind), interchange, FieldSize.LARGE, label);

        final CosmosDbAccountComboBox combox = this.getEditorComponent();
        interchange.addPersistentProperty(KEY_COSMOS_ACCOUNT_ID);
        interchange.addPersistentProperty(KEY_USERNAME);
        final String initialAccountId = interchange.getProperty(KEY_COSMOS_ACCOUNT_ID);
        if (StringUtils.isNotBlank(initialAccountId)) {
            combox.setValue(new AzureComboBox.ItemReference<>(i -> i.getId().equals(initialAccountId)));
        }

        interchange.addPropertyChangeListener((evt -> onPropertiesChanged(evt.getPropertyName(), evt.getNewValue())), this);
        combox.addValueChangedListener(this::setAccount);
    }

    private void onPropertiesChanged(String propertyName, Object newValue) {
        if (!this.updating && Objects.nonNull(this.connectionString) && StringUtils.isNotEmpty((String) newValue)) {
            if (StringUtils.equals(propertyName, "host") && !Objects.equals(this.connectionString.getHost(), newValue) ||
                StringUtils.equals(propertyName, "port") && !Objects.equals(this.connectionString.getPort() + "", newValue)) {
                this.getEditorComponent().setValue((CosmosDBAccount) null);
                this.setAccount(null);
            }
        }
    }

    private void setAccount(@Nullable CosmosDBAccount account) {
        if (this.updating || Objects.equals(this.account, account)) {
            return;
        }
        final DataInterchange interchange = this.getInterchange();
        this.account = account;
        if (account == null) {
            this.connectionString = null;
            interchange.putProperty(KEY_COSMOS_ACCOUNT_ID, NONE);
            return;
        }
        this.updating = true;
        // AzureActionManager.getInstance().getAction(Action.REQUIRE_AUTH).handle(combox::reloadItems);
        AzureTaskManager.getInstance().runOnPooledThread(() -> {
            this.connectionString = account.getCosmosDBAccountPrimaryConnectionString();
            final LocalDataSource dataSource = interchange.getDataSource();
            final String host = connectionString.getHost();
            final String port = String.valueOf(connectionString.getPort());
            final String user = connectionString.getUsername();
            final String password = String.valueOf(connectionString.getPassword());
            this.text = connectionString.getConnectionString();
            AzureTaskManager.getInstance().runLater(() -> {
                LocalDataSource.setUsername(dataSource, user);
                interchange.getCredentials().storePassword(dataSource, new OneTimeString(password));
                this.setUseSsl(true);
                interchange.putProperties(consumer -> {
                    consumer.consume(KEY_COSMOS_ACCOUNT_ID, account.getId());
                    consumer.consume(KEY_USERNAME, user);
                    consumer.consume("host", host);
                    consumer.consume("user", user);
                    consumer.consume("port", port);
                    this.updating = false;
                });
                this.setUsername(user);
            }, AzureTask.Modality.ANY);
        });
    }

    private void setUsername(String user) {
        final UrlEditorModel model = this.getDataSourceConfigurable().getUrlEditor().getEditorModel();
        model.setParameter("user", user);
        model.commit(true);
    }

    @SneakyThrows
    private void setUseSsl(boolean useSsl) {
        final DataSourceConfigurable configurable = this.getDataSourceConfigurable();
        final JBCheckBox useSSLCheckBox = (JBCheckBox) FieldUtils.readField(configurable.getSshSslPanel(), "myUseSSLJBCheckBox", true);
        useSSLCheckBox.setSelected(useSsl);
    }

    @SneakyThrows
    private DataSourceConfigurable getDataSourceConfigurable() {
        return (DataSourceConfigurable) FieldUtils.readField(this.getInterchange(), "myConfigurable", true);
    }

    @RequiredArgsConstructor
    static class CosmosDbAccountComboBox extends AzureComboBox<CosmosDBAccount> {
        private final DatabaseAccountKind kind;

        @Nonnull
        @Override
        protected List<CosmosDBAccount> loadItems() {
            if (!Azure.az(AzureAccount.class).isLoggedIn()) {
                return Collections.emptyList();
            }
            return Azure.az(AzureCosmosService.class).getDatabaseAccounts(kind).stream()
                .filter(m -> !m.isDraftForCreating()).collect(Collectors.toList());
        }

        @Override
        protected String getItemText(Object item) {
            return Optional.ofNullable(item).map(i -> ((CosmosDBAccount) i)).map(AbstractAzResource::getName).orElse("");
        }
    }
}
