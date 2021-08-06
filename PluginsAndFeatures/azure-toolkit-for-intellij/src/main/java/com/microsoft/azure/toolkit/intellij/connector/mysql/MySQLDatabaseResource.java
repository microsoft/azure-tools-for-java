/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.connector.mysql;

import com.microsoft.azure.toolkit.intellij.connector.database.DatabaseResource;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.jdom.Element;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

@EqualsAndHashCode(onlyExplicitlyIncluded = true, callSuper = true)
public class MySQLDatabaseResource extends DatabaseResource {

    public MySQLDatabaseResource(@Nonnull final String serverId, @Nullable final String databaseName) {
        super(Definition.AZURE_MYSQL.getType(), serverId, databaseName);
    }

    public MySQLDatabaseResource(@Nonnull final String databaseId) {
        super(Definition.AZURE_MYSQL.getType(), databaseId);
    }

    @Override
    public String getTitle() {
        return Definition.AZURE_MYSQL.getTitle();
    }

    @Getter
    @RequiredArgsConstructor
    public enum Definition implements DatabaseDefinition {
        AZURE_MYSQL("Microsoft.DBforMySQL", "Azure Database for MySQL");
        private final String type;
        private final String title;

        @Override
        public MySQLDatabaseResource read(@Nonnull final Element resourceEle) {
            final MySQLDatabaseResource resource = new MySQLDatabaseResource(resourceEle.getChildTextTrim("azureResourceId"));
            DatabaseDefinition.super.read(resourceEle, resource);
            return resource;
        }

    }
}
