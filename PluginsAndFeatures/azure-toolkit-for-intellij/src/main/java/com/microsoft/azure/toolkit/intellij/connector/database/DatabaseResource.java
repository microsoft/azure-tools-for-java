/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.connector.database;

import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.project.Project;
import com.microsoft.azure.toolkit.intellij.common.AzureFormJPanel;
import com.microsoft.azure.toolkit.intellij.connector.Connection;
import com.microsoft.azure.toolkit.intellij.connector.Password;
import com.microsoft.azure.toolkit.intellij.connector.PasswordStore;
import com.microsoft.azure.toolkit.intellij.connector.Resource;
import com.microsoft.azure.toolkit.intellij.connector.database.component.PasswordDialog;
import com.microsoft.azure.toolkit.intellij.connector.spring.SpringSupported;
import com.microsoft.azure.toolkit.lib.common.messager.AzureMessager;
import com.microsoft.azure.toolkit.lib.common.operation.AzureOperationBundle;
import com.microsoft.azure.toolkit.lib.common.task.AzureTaskManager;
import com.microsoft.azure.toolkit.lib.database.JdbcUrl;
import com.microsoft.tooling.msservices.components.DefaultLoader;
import lombok.AccessLevel;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.jdom.Attribute;
import org.jdom.Element;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;

import static com.microsoft.azure.toolkit.intellij.connector.database.DatabaseConnectionUtils.ACCESS_DENIED_ERROR_CODE;

@Setter
@Getter
@RequiredArgsConstructor
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class DatabaseResource implements Resource<Database> {
    @EqualsAndHashCode.Include
    private final Database data;
    private final Definition definition;

    @Override
    public String getDataId() {
        return this.data.getId();
    }

    @Override
    public String getName() {
        return this.data.getFullName();
    }

    public boolean isModified(Resource<Database> dr) {
        final boolean urlModified = !Objects.equals(this.data.getJdbcUrl(), dr.getData().getJdbcUrl());
        final boolean usernameModified = !StringUtils.equals(this.data.getUsername(), dr.getData().getUsername());
        final boolean passwordSaveTypeModified = this.data.getPassword().saveType() != dr.getData().getPassword().saveType();
        return urlModified || usernameModified || passwordSaveTypeModified;
    }

    public Map<String, String> initEnv(@Nonnull final Project project) {
        final Map<String, String> envMap = new HashMap<>();
        envMap.put(String.format("%s_URL", Connection.ENV_PREFIX), this.data.getJdbcUrl().toString());
        envMap.put(String.format("%s_USERNAME", Connection.ENV_PREFIX), this.data.getUsername());
        envMap.put(String.format("%s_PASSWORD", Connection.ENV_PREFIX), loadPassword().or(() -> inputPassword(project)).orElse(""));
        return envMap;
    }

    private Optional<String> loadPassword() {
        final Password password = this.data.getPassword();
        if (Objects.nonNull(password) && password.saveType() == Password.SaveType.NEVER) {
            return Optional.empty();
        }
        final String defName = this.definition.getName();
        if (password.saveType() == Password.SaveType.FOREVER) {
            PasswordStore.migratePassword(this.getDataId(), this.data.getUsername(),
                    defName, this.getDataId(), this.data.getUsername());
        }
        final String saved = PasswordStore.loadPassword(defName, this.getDataId(), this.data.getUsername(), password.saveType());
        if (password.saveType() == Password.SaveType.UNTIL_RESTART && StringUtils.isBlank(saved)) {
            return Optional.empty();
        }
        final DatabaseConnectionUtils.ConnectResult result = DatabaseConnectionUtils.connectWithPing(this.data.getJdbcUrl(), this.data.getUsername(), saved);
        if (StringUtils.isNotBlank(saved) && result.isConnected()) {
            return Optional.of(saved);
        }
        if (result.getErrorCode() != ACCESS_DENIED_ERROR_CODE) {
            AzureMessager.getMessager().warning(result.getMessage(), "Azure Resource Connector");
        }
        return Optional.empty();
    }

    @Nonnull
    private Optional<String> inputPassword(@Nonnull final Project project) {
        final AtomicReference<Password> passwordRef = new AtomicReference<>();
        AzureTaskManager.getInstance().runAndWait(AzureOperationBundle.title("mysql.update_password"), () -> {
            final PasswordDialog dialog = new PasswordDialog(project, this.data);
            if (dialog.showAndGet()) {
                final Password password = dialog.getValue();
                this.data.getPassword().saveType(password.saveType());
                PasswordStore.savePassword(this.definition.getName(),
                        this.getDataId(), this.data.getUsername(), password.password(), password.saveType());
                passwordRef.set(password);
            }
        });
        return Optional.ofNullable(passwordRef.get()).map(c -> String.valueOf(c.password()));
    }

    @Override
    public void navigate(AnActionEvent event) {
        if (Definition.AZURE_MYSQL == this.getDefinition()) {
            DefaultLoader.getUIHelper().openMySQLPropertyView(this.getData().getServerId().id(), Objects.requireNonNull(event.getProject()));
        } else if (Definition.SQL_SERVER == this.getDefinition()) {
            DefaultLoader.getUIHelper().openSqlServerPropertyView(this.getData().getServerId().id(), Objects.requireNonNull(event.getProject()));
        }
    }

    @Getter
    @RequiredArgsConstructor
    public enum Definition implements SpringSupported<Database> {
        SQL_SERVER("Azure.SqlServer", "Azure SQL Server", "/icons/SqlServer/SqlServer.svg", DatabaseResourcePanel::sqlServer),
        AZURE_MYSQL("Azure.MySQL", "Azure MySQL", "/icons/MySQL/MySQL.svg", DatabaseResourcePanel::mysql);

        private final String name;
        private final String title;
        private final String icon;
        @Getter(AccessLevel.NONE)
        private final Supplier<AzureFormJPanel<Database>> panelSupplier;

        @Override
        public Resource<Database> define(Database resource) {
            return new DatabaseResource(resource, this);
        }

        @Override
        public AzureFormJPanel<Database> getResourcePanel(Project project) {
            return this.panelSupplier.get();
        }

        @Override
        public List<Pair<String, String>> getSpringProperties() {
            final List<Pair<String, String>> properties = new ArrayList<>();
            properties.add(Pair.of("spring.datasource.url", String.format("${%s_URL}", Connection.ENV_PREFIX)));
            properties.add(Pair.of("spring.datasource.username", String.format("${%s_USERNAME}", Connection.ENV_PREFIX)));
            properties.add(Pair.of("spring.datasource.password", String.format("${%s_PASSWORD}", Connection.ENV_PREFIX)));
            return properties;
        }

        @Override
        public boolean write(@Nonnull final Element resourceEle, @Nonnull final Resource<Database> r) {
            final DatabaseResource resource = (DatabaseResource) r;
            final String defName = resource.getDefinition().getName();
            final Database database = resource.getData();
            final Password.SaveType saveType = database.getPassword().saveType();
            resourceEle.setAttribute(new Attribute("id", resource.getId()));
            resourceEle.addContent(new Element("dataId").addContent(resource.getDataId()));
            resourceEle.addContent(new Element("url").setText(database.getJdbcUrl().toString()));
            resourceEle.addContent(new Element("username").setText(database.getUsername()));
            resourceEle.addContent(new Element("passwordSave").setText(saveType.name()));
            final char[] password = database.getPassword().password();
            final String storedPassword = PasswordStore.loadPassword(defName, resource.getDataId(), database.getUsername(), saveType);
            if (ArrayUtils.isNotEmpty(password) && !StringUtils.equals(String.valueOf(password), storedPassword)) {
                PasswordStore.savePassword(defName, resource.getDataId(), database.getUsername(), database.getPassword().password(), saveType);
            }
            return true;
        }

        @Override
        public Resource<Database> read(@Nonnull Element resourceEle) {
            final String dataId = resourceEle.getChildTextTrim("dataId");
            final Database db = new Database(dataId);
            final String defName = this.getName();
            db.setJdbcUrl(JdbcUrl.from(resourceEle.getChildTextTrim("url")));
            db.setUsername(resourceEle.getChildTextTrim("username"));
            db.setPassword(new Password().saveType(Password.SaveType.valueOf(resourceEle.getChildTextTrim("passwordSave"))));
            if (db.getPassword().saveType() == Password.SaveType.FOREVER) {
                PasswordStore.migratePassword(db.getId(), db.getUsername(), defName, db.getId(), db.getUsername());
            }
            final String savedPassword = PasswordStore.loadPassword(defName, db.getId(), db.getUsername(), db.getPassword().saveType());
            if (StringUtils.isNotBlank(savedPassword)) {
                db.getPassword().password(savedPassword.toCharArray());
            }
            return new DatabaseResource(db, this);
        }

        @Override
        public String toString() {
            return this.getTitle();
        }
    }
}
