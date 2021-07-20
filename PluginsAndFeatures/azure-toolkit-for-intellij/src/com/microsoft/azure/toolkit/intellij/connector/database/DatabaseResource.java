/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.connector.database;

import com.intellij.openapi.project.Project;
import com.microsoft.azure.management.resources.fluentcore.arm.ResourceId;
import com.microsoft.azure.toolkit.intellij.common.AzureFormJPanel;
import com.microsoft.azure.toolkit.intellij.connector.Password;
import com.microsoft.azure.toolkit.intellij.connector.PasswordStore;
import com.microsoft.azure.toolkit.intellij.connector.Resource;
import com.microsoft.azure.toolkit.intellij.connector.ResourceDefinition;
import com.microsoft.azure.toolkit.lib.common.database.JdbcUrl;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.jdom.Attribute;
import org.jdom.Element;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

@Setter
@Getter
@Builder
@AllArgsConstructor
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public final class DatabaseResource implements Resource {
    private final String type;
    private final String databaseName;
    private final ResourceId serverId;

    private JdbcUrl jdbcUrl;
    private String username;
    private Password password;
    private String envPrefix;

    public DatabaseResource(@Nonnull String type, @Nonnull final String serverId, @Nullable final String databaseName) {
        this.type = type;
        this.databaseName = databaseName;
        this.serverId = ResourceId.fromString(serverId);
    }

    public DatabaseResource(@Nonnull String type, @Nonnull final String databaseId) {
        this.type = type;
        final ResourceId dbId = ResourceId.fromString(databaseId);
        this.serverId = dbId.parent();
        this.databaseName = dbId.name();
    }

    public ResourceId getServerId() {
        return this.serverId;
    }

    @Override
    public String getId() {
        return DigestUtils.md5Hex(this.getDatabaseId());
    }

    @NotNull
    @EqualsAndHashCode.Include
    public String getDatabaseId() {
        return serverId.id() + "/databases/" + databaseName;
    }

    @Override
    public String toString() {
        return String.format("%s database: \"%s/%s\"", Definition.getTitleByType(this.type), this.getServerId().name(), this.databaseName);
    }

    @Getter
    @RequiredArgsConstructor
    public enum Definition implements ResourceDefinition<DatabaseResource> {
        AZURE_MYSQL("Microsoft.DBforMySQL", "Azure Database for MySQL"),
        SQL_SERVER("Microsoft.Sql", "SQL Server");
        private final String type;
        private final String title;

        @Override
        public AzureFormJPanel<DatabaseResource> getResourcesPanel(@NotNull String type, final Project project) {
            return new DatabaseResourcePanel(this);
        }

        @Override
        public boolean write(Element resourceEle, DatabaseResource resource) {
            resourceEle.setAttribute(new Attribute(Resource.FIELD_ID, resource.getId()));
            resourceEle.addContent(new Element("azureResourceId").addContent(resource.getDatabaseId()));
            resourceEle.addContent(new Element("url").setText(resource.jdbcUrl.toString()));
            resourceEle.addContent(new Element("username").setText(resource.username));
            resourceEle.addContent(new Element("passwordSave").setText(resource.password.saveType().name()));
            if (ArrayUtils.isNotEmpty(resource.password.password())) {
                PasswordStore.savePassword(resource.getType(), resource.getId(), resource.username, resource.password.password(), resource.password.saveType());
            }
            return true;
        }

        @Override
        public DatabaseResource read(Element resourceEle) {
            final DatabaseResource resource = new DatabaseResource(resourceEle.getAttributeValue("type"), resourceEle.getChildTextTrim("azureResourceId"));
            resource.setJdbcUrl(JdbcUrl.from(resourceEle.getChildTextTrim("url")));
            resource.setUsername(resourceEle.getChildTextTrim("username"));
            resource.setPassword(new Password().saveType(Password.SaveType.valueOf(resourceEle.getChildTextTrim("passwordSave"))));
            if (resource.password.saveType() == Password.SaveType.FOREVER) {
                PasswordStore.migratePassword(resource.getId(), resource.getUsername(), resource.getType(), resource.getId(), resource.getUsername());
            }
            final String savedPassword = PasswordStore.loadPassword(resource.getType(), resource.getId(), resource.getUsername(), resource.password.saveType());
            if (StringUtils.isNotBlank(savedPassword)) {
                resource.password.password(savedPassword.toCharArray());
            }
            return resource;
        }

        @Override
        public String toString() {
            return this.getTitle();
        }

        public static String getTitleByType(String type) {
            for (Definition definition : Definition.values()) {
                if (StringUtils.equals(definition.getType(), type)) {
                    return definition.getTitle();
                }
            }
            return type;
        }
    }
}
