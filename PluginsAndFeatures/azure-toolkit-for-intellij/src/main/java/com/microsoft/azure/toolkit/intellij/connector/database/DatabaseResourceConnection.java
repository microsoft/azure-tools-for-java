/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.connector.database;

import com.intellij.openapi.project.Project;
import com.microsoft.azure.toolkit.intellij.connector.Connection;
import com.microsoft.azure.toolkit.intellij.connector.ConnectionDefinition;
import com.microsoft.azure.toolkit.intellij.connector.ModuleResource;
import com.microsoft.azure.toolkit.intellij.connector.Password;
import com.microsoft.azure.toolkit.intellij.connector.PasswordStore;
import com.microsoft.azure.toolkit.intellij.connector.Resource;
import com.microsoft.azure.toolkit.intellij.connector.ResourceDefinition;
import com.microsoft.azure.toolkit.intellij.connector.database.component.PasswordDialog;
import com.microsoft.azure.toolkit.lib.common.bundle.AzureString;
import com.microsoft.azure.toolkit.lib.common.messager.AzureMessager;
import com.microsoft.azure.toolkit.lib.common.operation.AzureOperationBundle;
import com.microsoft.azure.toolkit.lib.common.task.AzureTaskManager;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;

import javax.annotation.Nonnull;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

import static com.microsoft.azure.toolkit.intellij.connector.database.DatabaseConnectionUtils.ACCESS_DENIED_ERROR_CODE;

public class DatabaseResourceConnection extends Connection<Database, String> {

    public DatabaseResourceConnection(DatabaseResource resource, ModuleResource consumer, Definition definition) {
        super(resource, consumer, definition);
    }

    protected Map<String, String> initEnv(@Nonnull final Project project) {
        final Map<String, String> envMap = new HashMap<>();
        final Database database = this.resource.getData();
        envMap.put(this.getEnvPrefix() + "URL", database.getJdbcUrl().toString());
        envMap.put(this.getEnvPrefix() + "USERNAME", database.getUsername());
        envMap.put(this.getEnvPrefix() + "PASSWORD", loadPassword(resource).or(() -> inputPassword(project, resource)).orElse(""));
        return envMap;
    }

    private static Optional<String> loadPassword(@Nonnull final Resource<Database> resource) {
        final Database database = resource.getData();
        if (Objects.nonNull(database.getPassword()) && database.getPassword().saveType() == Password.SaveType.NEVER) {
            return Optional.empty();
        }
        final String defName = resource.getDefinition().getName();
        if (database.getPassword().saveType() == Password.SaveType.FOREVER) {
            PasswordStore.migratePassword(database.getId(), database.getUsername(),
                    defName, database.getId(), database.getUsername());
        }
        final String saved = PasswordStore.loadPassword(defName, database.getId(), database.getUsername(), database.getPassword().saveType());
        if (database.getPassword().saveType() == Password.SaveType.UNTIL_RESTART && StringUtils.isBlank(saved)) {
            return Optional.empty();
        }
        final DatabaseConnectionUtils.ConnectResult result = DatabaseConnectionUtils.connectWithPing(database.getJdbcUrl(), database.getUsername(), saved);
        if (StringUtils.isNotBlank(saved) && result.isConnected()) {
            return Optional.of(saved);
        }
        if (result.getErrorCode() != ACCESS_DENIED_ERROR_CODE) {
            AzureMessager.getMessager().warning(result.getMessage(), "Azure Resource Connector");
        }
        return Optional.empty();
    }

    @Nonnull
    private static Optional<String> inputPassword(@Nonnull final Project project, @Nonnull final Resource<Database> resource) {
        final AtomicReference<Password> passwordRef = new AtomicReference<>();
        final AzureString title = AzureOperationBundle.title("mysql.update_password");
        final Database database = resource.getData();
        AzureTaskManager.getInstance().runAndWait(title, () -> {
            final PasswordDialog dialog = new PasswordDialog(project, resource);
            if (dialog.showAndGet()) {
                final Password password = dialog.getData();
                database.getPassword().saveType(password.saveType());
                PasswordStore.savePassword(resource.getDefinition().getName(),
                        database.getId(), database.getUsername(), password.password(), password.saveType());
                passwordRef.set(password);
            }
        });
        return Optional.ofNullable(passwordRef.get()).map(c -> String.valueOf(c.password()));
    }

    @Getter
    public static class Definition extends ConnectionDefinition<Database, String> {
        public static final Definition MODULE_MYSQL = new Definition(DatabaseResource.Definition.AZURE_MYSQL, ModuleResource.Definition.IJ_MODULE);
        public static final Definition MODULE_SQL = new Definition(DatabaseResource.Definition.SQL_SERVER, ModuleResource.Definition.IJ_MODULE);

        private static final String PROMPT_TITLE = "Azure Resource Connector";

        public Definition(ResourceDefinition<Database> rd, ResourceDefinition<String> cd) {
            super(rd, cd);
        }

        @Nonnull
        @Override
        public Connection<Database, String> define(Resource<Database> resource, Resource<String> consumer) {
            return new DatabaseResourceConnection((DatabaseResource) resource, (ModuleResource) consumer, this);
        }

        @Override
        protected boolean isModified(Database db, Database edb) {
            final boolean urlModified = !Objects.equals(db.getJdbcUrl(), edb.getJdbcUrl());
            final boolean usernameModified = !StringUtils.equals(db.getUsername(), edb.getUsername());
            final boolean passwordSaveTypeModified = db.getPassword().saveType() != edb.getPassword().saveType();
            return urlModified || usernameModified || passwordSaveTypeModified;
        }
    }
}
