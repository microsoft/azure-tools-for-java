/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.connector.sql;

import com.microsoft.azure.toolkit.intellij.connector.database.DatabaseResource;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.jdom.Element;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

@EqualsAndHashCode(onlyExplicitlyIncluded = true, callSuper = true)
public class SqlServerDatabaseResource extends DatabaseResource {

    public SqlServerDatabaseResource(@Nonnull final String serverId, @Nullable final String databaseName) {
        super(Definition.SQL_SERVER.getType(), serverId, databaseName);
    }

    public SqlServerDatabaseResource(@Nonnull final String databaseId) {
        super(Definition.SQL_SERVER.getType(), databaseId);
    }

    @Override
    public String getTitle() {
        return Definition.SQL_SERVER.getTitle();
    }

    @Getter
    @RequiredArgsConstructor
    public enum Definition implements DatabaseDefinition {
        SQL_SERVER("Microsoft.Sql", "SQL Server");
        private final String type;
        private final String title;

        @Override
        public SqlServerDatabaseResource read(@Nonnull final Element resourceEle) {
            final SqlServerDatabaseResource resource = new SqlServerDatabaseResource(resourceEle.getChildTextTrim("azureResourceId"));
            DatabaseDefinition.super.read(resourceEle, resource);
            return resource;
        }

    }
}
