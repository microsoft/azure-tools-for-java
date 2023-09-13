/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.database.dbtools;

import com.intellij.database.dataSource.url.DataInterchange;
import com.intellij.database.dataSource.url.TypesRegistry;
import com.intellij.openapi.util.NlsContexts;
import com.intellij.util.Consumer;
import com.microsoft.azure.toolkit.lib.mysql.MySqlServer;
import com.microsoft.azure.toolkit.lib.postgre.PostgreSqlServer;
import com.microsoft.azure.toolkit.lib.sqlserver.MicrosoftSqlServer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class DatabaseServerTypeFactory implements TypesRegistry.TypeDescriptorFactory {

    public static final String MYSQL = "az_mysql_server";
    public static final String SQLSERVER = "az_sqlserver_server";
    public static final String POSTGRE = "az_postgre_server";

    @Override
    public void createTypeDescriptor(@NotNull Consumer<? super TypesRegistry.TypeDescriptor> consumer) {
        final TypesRegistry.BaseTypeDescriptor mysql = new TypesRegistry.BaseTypeDescriptor(MYSQL, ".", "Server") {
            @Override
            protected TypesRegistry.@NotNull ParamEditor createFieldImpl(@NlsContexts.Label @NotNull String caption, @Nullable String configuration, @NotNull DataInterchange interchange) {
                return new DatabaseServerParamEditor(MySqlServer.class, formatFieldCaption("Server"), interchange);
            }
        };
        final TypesRegistry.BaseTypeDescriptor sqlserver = new TypesRegistry.BaseTypeDescriptor(SQLSERVER, ".", "Server") {
            protected TypesRegistry.@NotNull ParamEditor createFieldImpl(@NlsContexts.Label @NotNull String caption, @Nullable String configuration, @NotNull DataInterchange interchange) {
                return new DatabaseServerParamEditor(MicrosoftSqlServer.class, formatFieldCaption("Server"), interchange);
            }
        };
        final TypesRegistry.BaseTypeDescriptor postgre = new TypesRegistry.BaseTypeDescriptor(POSTGRE, ".", "Server") {
            protected TypesRegistry.@NotNull ParamEditor createFieldImpl(@NlsContexts.Label @NotNull String caption, @Nullable String configuration, @NotNull DataInterchange interchange) {
                return new DatabaseServerParamEditor(PostgreSqlServer.class, formatFieldCaption("Server"), interchange);
            }
        };
        consumer.consume(mysql);
        consumer.consume(sqlserver);
        consumer.consume(postgre);
    }
}
