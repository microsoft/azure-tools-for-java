/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.cosmos.dbtools;

import com.intellij.database.dataSource.url.DataInterchange;
import com.intellij.database.dataSource.url.TypesRegistry;
import com.intellij.database.dataSource.url.ui.UrlPropertiesPanel;
import com.intellij.util.Consumer;
import com.microsoft.azure.toolkit.lib.cosmos.model.DatabaseAccountKind;
import org.apache.commons.lang3.reflect.FieldUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Field;
import java.util.List;

public class MongoCosmosDbAccountTypeFactory implements TypesRegistry.TypeDescriptorFactory {
    private static final String TYPE_NAME = "cosmos_account_mongo";
    private static final String CAPTION = "Account";
    private static final String PARAM_NAME = "account";

    public MongoCosmosDbAccountTypeFactory() {
        makeAccountShowAtTop();
    }

    @SuppressWarnings("unchecked")
    private static void makeAccountShowAtTop() {
        try {
            final Field HEADS = FieldUtils.getField(UrlPropertiesPanel.class, "HEADS", true);
            final List<String> heads = (List<String>) FieldUtils.readStaticField(HEADS, true);
            if (!heads.contains(PARAM_NAME)) {
                final Object[] old = heads.toArray();
                heads.set(0, PARAM_NAME);
                for (int i = 0; i < old.length - 1; i++) {
                    heads.set(i + 1, (String) old[i]);
                }
            }
            System.out.println(heads.size());
        } catch (final Throwable ignored) {
        }
    }

    public void createTypeDescriptor(@NotNull Consumer<? super TypesRegistry.TypeDescriptor> consumer) {
        consumer.consume(new TypesRegistry.BaseTypeDescriptor(TYPE_NAME, ".", CAPTION) {
            @NotNull
            protected TypesRegistry.@NotNull ParamEditor createFieldImpl(@NotNull String caption, @Nullable String configuration, @NotNull DataInterchange interchange) {
                return new AzureCosmosDbAccountParamEditor(DatabaseAccountKind.MONGO_DB, formatFieldCaption(CAPTION), interchange);
            }
        });
    }
}
