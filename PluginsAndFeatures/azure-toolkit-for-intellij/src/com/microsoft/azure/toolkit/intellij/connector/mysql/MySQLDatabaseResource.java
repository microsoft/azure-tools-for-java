/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.connector.mysql;

import com.intellij.openapi.project.Project;
import com.microsoft.azure.management.resources.fluentcore.arm.ResourceId;
import com.microsoft.azure.toolkit.intellij.common.AzureFormJPanel;
import com.microsoft.azure.toolkit.intellij.connector.Password;
import com.microsoft.azure.toolkit.intellij.connector.PasswordStore;
import com.microsoft.azure.toolkit.intellij.connector.Resource;
import com.microsoft.azure.toolkit.intellij.connector.ResourceDefinition;
import com.microsoft.azuretools.azurecommons.helpers.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.jdom.Attribute;
import org.jdom.Element;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

@Setter
@Getter
@Builder
@AllArgsConstructor
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public final class MySQLDatabaseResource implements Resource {
    private final Definition definition = Definition.AZURE_MYSQL;
    private final String databaseName;
    private final ResourceId serverId;

    private JdbcUrl jdbcUrl;
    private String username;
    private Password password;
    private String envPrefix;

    public MySQLDatabaseResource(@Nonnull final String serverId, @Nullable final String databaseName) {
        this.databaseName = databaseName;
        this.serverId = ResourceId.fromString(serverId);
    }

    public MySQLDatabaseResource(@Nonnull final String databaseId) {
        final ResourceId dbId = ResourceId.fromString(databaseId);
        this.serverId = dbId.parent();
        this.databaseName = dbId.name();
    }

    public ResourceId getServerId() {
        return this.serverId;
    }

    @NotNull
    @Override
    public String getBizId() {
        return serverId.id() + "/databases/" + databaseName;
    }

    @Getter
    @RequiredArgsConstructor
    public enum Definition implements ResourceDefinition<MySQLDatabaseResource> {
        AZURE_MYSQL("Microsoft.DBforMySQL", "Azure Database for MySQL");
        private final String type;
        private final String name;

        @Override
        public AzureFormJPanel<MySQLDatabaseResource> getResourcesPanel(String type, final Project project) {
            return new MySQLDatabaseResourcePanel();
        }

        @Override
        public boolean write(Element resourceEle, MySQLDatabaseResource resource) {
            resourceEle.setAttribute(new Attribute(Resource.FIELD_ID, resource.getId()));
            resourceEle.addContent(new Element("azureResourceId").addContent(resource.getBizId()));
            resourceEle.addContent(new Element("url").setText(resource.jdbcUrl.toString()));
            resourceEle.addContent(new Element("username").setText(resource.username));
            resourceEle.addContent(new Element("passwordSave").setText(resource.password.saveType().name()));
            if (ArrayUtils.isNotEmpty(resource.password.password())) {
                PasswordStore.savePassword(resource.getId(), resource.username, resource.password.password(), resource.password.saveType());
            }
            return true;
        }

        @Nullable
        public MySQLDatabaseResource read(Element resourceEle) {
            final MySQLDatabaseResource resource = new MySQLDatabaseResource(resourceEle.getChildTextTrim("azureResourceId"));
            resource.setJdbcUrl(JdbcUrl.from(resourceEle.getChildTextTrim("url")));
            resource.setUsername(resourceEle.getChildTextTrim("username"));
            resource.setPassword(new Password().saveType(Password.SaveType.valueOf(resourceEle.getChildTextTrim("passwordSave"))));
            final String password = PasswordStore.loadPassword(resource.getId(), resource.getUsername(), resource.password.saveType());
            if (StringUtils.isNotBlank(password)) {
                resource.password.password(password.toCharArray());
            }
            return resource;
        }

        public String toString() {
            return this.getName();
        }
    }
}
