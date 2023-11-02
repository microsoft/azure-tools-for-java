/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */
package com.microsoft.azure.toolkit.intellij.database.connection;

import com.azure.resourcemanager.resources.fluentcore.arm.ResourceId;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.microsoft.azure.toolkit.ide.common.action.ResourceCommonActionsContributor;
import com.microsoft.azure.toolkit.ide.common.store.AzureStoreManager;
import com.microsoft.azure.toolkit.intellij.connector.AzureServiceResource;
import com.microsoft.azure.toolkit.lib.common.action.AzureActionManager;
import com.microsoft.azure.toolkit.lib.common.messager.AzureMessager;
import com.microsoft.azure.toolkit.lib.database.JdbcUrl;
import com.microsoft.azure.toolkit.lib.database.entity.IDatabase;
import com.microsoft.azure.toolkit.lib.database.entity.IDatabaseServer;
import lombok.Getter;
import lombok.Setter;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import static com.microsoft.azure.toolkit.intellij.database.connection.DatabaseConnectionUtils.ACCESS_DENIED_ERROR_CODE;

@Setter
@Getter
public class SqlDatabaseResource<T extends IDatabase> extends AzureServiceResource<T> {

    @Nonnull
    private final Database database;

    @Override
    public String getName() {
        return database.getServerName() + "/" + database.getName();
    }

    public SqlDatabaseResource(@Nonnull T database, @Nullable String username, @Nonnull Definition<T> definition) {
        super(database, definition);
        this.database = new Database(ResourceId.fromString(database.getId()).parent().id(), database.getName());
        this.database.setUsername(username);
    }

    public SqlDatabaseResource(@Nonnull String id, @Nonnull String username, @Nonnull Definition<T> definition) {
        super(id, definition);
        final ResourceId resourceId = ResourceId.fromString(id);
        this.database = new Database(resourceId.parent().id(), resourceId.name());
        this.database.setUsername(username);
    }

    @Override
    public void navigate(AnActionEvent event) {
        final ResourceId parent = ResourceId.fromString(this.getDataId()).parent();
        final IDatabaseServer<T> server = (IDatabaseServer<T>) this.getDefinition().getResource(this.getDataId()).getServer();
        AzureActionManager.getInstance().getAction(ResourceCommonActionsContributor.SHOW_PROPERTIES).handle(server, event);
    }

    @Nullable
    public String loadPassword() {
        final char[] password = getPassword();
        if (ArrayUtils.isNotEmpty(password)) {
            return String.valueOf(password);
        }
        final String defName = this.getDefinition().getName();
        final String saved = AzureStoreManager.getInstance().getSecureStore().loadPassword(defName, this.getDataId(), this.getUsername());
        if (StringUtils.isBlank(saved)) {
            return null;
        }
        final DatabaseConnectionUtils.ConnectResult result = DatabaseConnectionUtils.connectWithPing(this.getJdbcUrl(), this.getUsername(), saved);
        if (StringUtils.isNotBlank(saved) && result.isConnected()) {
            return saved;
        }
        if (result.getErrorCode() != ACCESS_DENIED_ERROR_CODE) {
            AzureMessager.getMessager().warning(result.getMessage(), "Azure Resource Connector");
        }
        return null;
    }

    public JdbcUrl getJdbcUrl() {
        return database.getJdbcUrl();
    }

    public String getUsername() {
        return database.getUsername();
    }

    public char[] getPassword() {
        return database.getPassword();
    }

    @Nonnull
    public Database getDatabase() {
        return database;
    }

    public void setJdbcUrl(JdbcUrl url) {
        this.database.setJdbcUrl(url);
    }

    public void setPassword(char[] password) {
        this.database.setPassword(password);
    }

    public void setUsername(String username) {
        this.database.setUsername(username);
    }

    @Override
    public boolean canConnectSilently() {
        return false;
    }
}
