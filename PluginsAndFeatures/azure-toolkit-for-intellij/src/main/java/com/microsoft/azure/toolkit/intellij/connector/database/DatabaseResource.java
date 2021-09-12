/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.connector.database;

import com.intellij.openapi.project.Project;
import com.microsoft.azure.toolkit.intellij.common.AzureFormJPanel;
import com.microsoft.azure.toolkit.intellij.connector.Password;
import com.microsoft.azure.toolkit.intellij.connector.PasswordStore;
import com.microsoft.azure.toolkit.intellij.connector.lib.Resource;
import com.microsoft.azure.toolkit.intellij.connector.lib.ResourceDefinition;
import com.microsoft.azure.toolkit.lib.database.JdbcUrl;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.jdom.Attribute;
import org.jdom.Element;

import javax.annotation.Nonnull;
import java.util.Arrays;
import java.util.List;
import java.util.function.Supplier;

@Setter
@Getter
@RequiredArgsConstructor
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class DatabaseResource implements Resource<Database> {

    private final Database database;
    private final Definition definition;

    @Override
    public Database getData() {
        return database;
    }

    @Override
    public String getId() {
        return DigestUtils.md5Hex(this.database.getId());
    }

    @Override
    public String getName() {
        return this.database.getName();
    }

    public String getTitle() {
        return "Azure Database";
    }

    @RequiredArgsConstructor
    public static class Definition implements ResourceDefinition<Database> {
        public static final Definition SQL_SERVER = new Definition("Microsoft.Sql", "SQL Server", DatabaseResourcePanel::sqlServer);
        public static final Definition AZURE_MYSQL = new Definition("Microsoft.DBforMySQL", "Azure Database for MySQL", DatabaseResourcePanel::mysql);

        public static List<Definition> values() {
            return Arrays.asList(AZURE_MYSQL, SQL_SERVER);
        }

        @Getter
        private final String name;
        @Getter
        private final String title;
        private final Supplier<AzureFormJPanel<Database>> panelSupplier;

        @Override
        public Resource<Database> define(Database resource) {
            return new DatabaseResource(resource, this);
        }

        @Override
        public AzureFormJPanel<Database> getResourcesPanel(@Nonnull String type, Project project) {
            return this.panelSupplier.get();
        }

        @Override
        public boolean write(@Nonnull final Element resourceEle, @Nonnull final Resource<Database> r) {
            final DatabaseResource resource = (DatabaseResource) r;
            final String defName = resource.getDefName();
            final Database database = resource.getData();
            final Password.SaveType saveType = database.getPassword().saveType();
            resourceEle.setAttribute(new Attribute(Resource.FIELD_ID, resource.getId()));
            resourceEle.addContent(new Element("azureResourceId").addContent(database.getId()));
            resourceEle.addContent(new Element("url").setText(database.getJdbcUrl().toString()));
            resourceEle.addContent(new Element("username").setText(database.getUsername()));
            resourceEle.addContent(new Element("passwordSave").setText(saveType.name()));
            final char[] password = database.getPassword().password();
            final String storedPassword = PasswordStore.loadPassword(defName, resource.getId(), database.getUsername(), saveType);
            if (ArrayUtils.isNotEmpty(password) && !StringUtils.equals(String.valueOf(password), storedPassword)) {
                PasswordStore.savePassword(defName, resource.getId(), database.getUsername(), database.getPassword().password(), saveType);
            }
            return true;
        }

        @Override
        public Resource<Database> read(@Nonnull Element resourceEle) {
            final String id = resourceEle.getChildTextTrim("azureResourceId");
            final Database db = Database.fromId(id);
            return new DatabaseResource(db, this);
        }

        public void read(@Nonnull final Element resourceEle, @Nonnull final DatabaseResource resource) {
            final String defName = resource.getDefName();
            final Database database = resource.getDatabase();
            database.setJdbcUrl(JdbcUrl.from(resourceEle.getChildTextTrim("url")));
            database.setUsername(resourceEle.getChildTextTrim("username"));
            database.setPassword(new Password().saveType(Password.SaveType.valueOf(resourceEle.getChildTextTrim("passwordSave"))));
            if (database.getPassword().saveType() == Password.SaveType.FOREVER) {
                PasswordStore.migratePassword(database.getId(), database.getUsername(), defName, database.getId(), database.getUsername());
            }
            final String savedPassword = PasswordStore.loadPassword(defName, database.getId(), database.getUsername(), database.getPassword().saveType());
            if (StringUtils.isNotBlank(savedPassword)) {
                database.getPassword().password(savedPassword.toCharArray());
            }
        }
    }
}
