/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.connector.database;

import com.intellij.openapi.project.Project;
import com.microsoft.azure.toolkit.intellij.common.AzureFormJPanel;
import com.microsoft.azure.toolkit.intellij.connector.Password;
import com.microsoft.azure.toolkit.intellij.connector.PasswordStore;
import com.microsoft.azure.toolkit.intellij.connector.Resource;
import com.microsoft.azure.toolkit.intellij.connector.ResourceDefinition;
import com.microsoft.azure.toolkit.lib.database.JdbcUrl;
import lombok.AccessLevel;
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
import java.util.function.Supplier;

@Setter
@Getter
@RequiredArgsConstructor
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class DatabaseResource implements Resource<Database> {
    @EqualsAndHashCode.Include
    private final Database data;
    private final Definition definition;

    @Override
    public String getId() {
        return DigestUtils.md5Hex(this.data.getId());
    }

    @Override
    public String getName() {
        return this.data.getFullName();
    }

    @Getter
    @RequiredArgsConstructor
    public enum Definition implements ResourceDefinition<Database> {
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
        public boolean write(@Nonnull final Element resourceEle, @Nonnull final Resource<Database> r) {
            final DatabaseResource resource = (DatabaseResource) r;
            final String defName = resource.getDefinition().getName();
            final Database database = resource.getData();
            final Password.SaveType saveType = database.getPassword().saveType();
            resourceEle.setAttribute(new Attribute("id", resource.getId()));
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
            final Database db = new Database(id);
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
    }
}
