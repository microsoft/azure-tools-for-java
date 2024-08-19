/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.database.dbtools;

import com.microsoft.azure.toolkit.lib.database.entity.IDatabaseServer;
import com.microsoft.azure.toolkit.lib.mysql.MySqlServer;
import com.microsoft.azure.toolkit.lib.postgre.PostgreSqlServer;
import com.microsoft.azure.toolkit.lib.sqlserver.MicrosoftSqlServer;
import lombok.Getter;

public enum DatabaseServerClass {
    MySql(MySqlServer.class),
    MsSql(MicrosoftSqlServer.class),
    Postgres(PostgreSqlServer.class);

    @Getter
    private final Class<? extends IDatabaseServer<?>> clazz;

    DatabaseServerClass(Class<? extends IDatabaseServer<?>> clazz) {
        this.clazz = clazz;
    }

    public String getServerName() {
        return switch (this) {
            case MySql -> "MySQL";
            case MsSql -> "Microsoft SQL";
            case Postgres -> "PostgreSQL";
            default -> "Database";
        };
    }

    @javax.annotation.Nullable
    public String getServiceName() {
        return switch (this) {
            case MySql -> "mysql";
            case MsSql -> "sqlserver";
            case Postgres -> "postgre";
            default -> null;
        };
    }
}
