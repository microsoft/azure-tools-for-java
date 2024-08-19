/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.database.dbtools;

import com.intellij.database.dataSource.url.DataInterchange;
import com.intellij.database.dataSource.url.ParamEditor;
import com.intellij.database.dataSource.url.TypeDescriptorUi;
import com.intellij.database.dataSource.url.TypesRegistryUi;
import com.intellij.database.dataSource.url.ui.BaseTypeDescriptor;
import com.intellij.util.Consumer;
import com.microsoft.azure.toolkit.lib.mysql.MySqlServer;
import com.microsoft.azure.toolkit.lib.postgre.PostgreSqlServer;
import com.microsoft.azure.toolkit.lib.sqlserver.MicrosoftSqlServer;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public class DatabaseServerTypeUIFactory implements TypesRegistryUi.TypeDescriptorUiFactory {

    public static final String MYSQL = "az_mysql_server";
    public static final String SQLSERVER = "az_sqlserver_server";
    public static final String POSTGRE = "az_postgre_server";

    @Override
    public void createTypeDescriptor(@NotNull Consumer<? super TypeDescriptorUi> consumer) {
        final BaseTypeDescriptor mysql = new BaseTypeDescriptor(MYSQL) {
            protected ParamEditor createFieldImpl(@Nonnull String caption, @Nullable String configuration, @Nonnull DataInterchange interchange) {
                return new DatabaseServerParamEditor(DatabaseServerClass.MySql, formatFieldCaption("Server"), interchange);
            }
        };
        final BaseTypeDescriptor sqlserver = new BaseTypeDescriptor(SQLSERVER) {
            protected ParamEditor createFieldImpl(@Nonnull String caption, @Nullable String configuration, @Nonnull DataInterchange interchange) {
                return new DatabaseServerParamEditor(DatabaseServerClass.MsSql, formatFieldCaption("Server"), interchange);
            }
        };
        final BaseTypeDescriptor postgre = new BaseTypeDescriptor(POSTGRE) {
            protected ParamEditor createFieldImpl(@Nonnull String caption, @Nullable String configuration, @Nonnull DataInterchange interchange) {
                return new DatabaseServerParamEditor(DatabaseServerClass.Postgres, formatFieldCaption("Server"), interchange);
            }
        };
        consumer.consume(mysql);
        consumer.consume(sqlserver);
        consumer.consume(postgre);
    }
}
