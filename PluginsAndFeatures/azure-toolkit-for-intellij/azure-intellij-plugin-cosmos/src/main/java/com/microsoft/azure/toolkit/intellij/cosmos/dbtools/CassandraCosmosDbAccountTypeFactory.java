/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.cosmos.dbtools;

import com.intellij.database.dataSource.url.DataInterchange;
import com.intellij.database.dataSource.url.TypesRegistry;
import com.intellij.util.Consumer;
import com.microsoft.azure.toolkit.lib.cosmos.model.DatabaseAccountKind;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class CassandraCosmosDbAccountTypeFactory implements TypesRegistry.TypeDescriptorFactory {
    private static final String TYPE_NAME = "cosmos_account_cassandra";
    private static final String CAPTION = "Account";
    private static final String PARAM_NAME = "account";

    public void createTypeDescriptor(@NotNull Consumer<? super TypesRegistry.TypeDescriptor> consumer) {
        consumer.consume(new TypesRegistry.BaseTypeDescriptor(TYPE_NAME, ".", CAPTION) {
            @NotNull
            protected TypesRegistry.@NotNull ParamEditor createFieldImpl(@NotNull String caption, @Nullable String configuration, @NotNull DataInterchange interchange) {
                return new AzureCosmosDbAccountParamEditor(DatabaseAccountKind.CASSANDRA, formatFieldCaption(CAPTION), interchange);
            }
        });
    }
}
