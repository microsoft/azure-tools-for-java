/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.redis.dbtools;

import com.intellij.database.dataSource.url.*;
import com.intellij.database.dataSource.url.ui.BaseTypeDescriptor;
import com.intellij.openapi.util.NlsContexts;
import com.intellij.util.Consumer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import static com.microsoft.azure.toolkit.intellij.redis.dbtools.RedisAccountTypeFactory.CAPTION;
import static com.microsoft.azure.toolkit.intellij.redis.dbtools.RedisAccountTypeFactory.TYPE_NAME;

public class RedisCacheTypeUIFactory implements TypesRegistryUi.TypeDescriptorUiFactory {
    @Override
    public void createTypeDescriptor(@NotNull Consumer<? super TypeDescriptorUi> consumer) {
        consumer.consume(new BaseTypeDescriptor(TYPE_NAME) {
            @Override
            protected @NotNull ParamEditor createFieldImpl(@NlsContexts.Label @NotNull String s, @Nullable String s1, @NotNull DataInterchange dataInterchange) {
                return new RedisCacheParamEditor(dataInterchange, FieldSize.LARGE, formatFieldCaption(CAPTION));
            }
        });
    }
}
