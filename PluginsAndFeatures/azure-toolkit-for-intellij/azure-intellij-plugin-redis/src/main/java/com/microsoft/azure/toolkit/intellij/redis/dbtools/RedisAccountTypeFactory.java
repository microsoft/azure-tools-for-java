/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.redis.dbtools;

import com.intellij.database.dataSource.url.TypeDescriptor;
import com.intellij.database.dataSource.url.TypesRegistry;
import com.intellij.util.Consumer;
import org.jetbrains.annotations.NotNull;

public class RedisAccountTypeFactory implements TypesRegistry.TypeDescriptorFactory {
    public static final String TYPE_NAME = "redis_cache";
    public static final String CAPTION = "Cache";
    public static final String PARAM_NAME = "cache";

    @Override
    public void createTypeDescriptor(@NotNull Consumer<? super TypeDescriptor> consumer) {
        consumer.consume(TypesRegistry.createTypeDescriptor(TYPE_NAME, ".", CAPTION));
    }
}
