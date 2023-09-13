/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.cosmos.dbtools;

import com.intellij.database.dataSource.url.DataInterchange;
import com.intellij.database.dataSource.url.TypesRegistry;
import com.intellij.openapi.util.NlsContexts;
import com.intellij.util.Consumer;
import com.microsoft.azure.toolkit.lib.cosmos.model.DatabaseAccountKind;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nullable;

public class CassandraCosmosDbAccountTypeFactory implements TypesRegistry.TypeDescriptorFactory {
    private static final String TYPE_NAME = "cosmos_account_cassandra";
    private static final String CAPTION = "Account";
    private static final String PARAM_NAME = "account";

    @Override
    public void createTypeDescriptor(@NotNull Consumer<? super TypesRegistry.TypeDescriptor> consumer) {
        consumer.consume(new TypesRegistry.BaseTypeDescriptor(TYPE_NAME, ".", CAPTION) {
            @Override
            protected @NotNull TypesRegistry.ParamEditor createFieldImpl(@NlsContexts.Label @NotNull String s, @Nullable String s1, @NotNull DataInterchange dataInterchange) {
                return new AzureCosmosDbAccountParamEditor(DatabaseAccountKind.CASSANDRA, formatFieldCaption(CAPTION), dataInterchange);
            }
        });
    }
}
