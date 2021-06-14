/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.common.operation;

import com.intellij.CommonBundle;
import com.microsoft.azure.toolkit.lib.common.operation.AzureOperationBundle;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.ResourceBundle;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

public class IntellijAzureOperationTitleProvider implements AzureOperationBundle.Provider {

    private static final Map<String, Optional<ResourceBundle>> libBundles = new ConcurrentHashMap<>();
    private static final Map<String, Optional<ResourceBundle>> intellijBundles = new ConcurrentHashMap<>();
    private static final IntellijAzureOperationTitleProvider INSTANCE = new IntellijAzureOperationTitleProvider();
    public static final String ALL = "<ALL>";

    @Override
    @Nonnull
    public String getMessage(@Nonnull final String key, final Object... params) {
        final String notFound = String.format("!%s!", key);
        final String subGroup = key.split("\\.")[0].replaceAll("\\|", "_");
        final String supGroup = key.split("[|.]")[0];
        final ArrayList<Supplier<String>> suppliers = new ArrayList<>();
        suppliers.add(() -> this.getIjOperationTitle(subGroup, key, params));
        suppliers.add(() -> this.getIjOperationTitle(supGroup, key, params));
        suppliers.add(() -> this.getIjOperationTitle(ALL, key, params));
        suppliers.add(() -> this.getLibOperationTitle(subGroup, key, params));
        suppliers.add(() -> this.getLibOperationTitle(supGroup, key, params));
        suppliers.add(() -> this.getLibOperationTitle(ALL, key, params));
        for (final Supplier<String> supplier : suppliers) {
            final String title = supplier.get();
            if (Objects.nonNull(title)) {
                return title;
            }
        }
        return notFound;
    }

    public String getLibOperationTitle(@Nonnull final String group, @Nonnull final String key, final Object... params) {
        return libBundles.computeIfAbsent(group, k -> {
            final String bundleName = ALL.equals(group) ?
                    "com.microsoft.azure.toolkit.operation.titles" :
                    String.format("com.microsoft.azure.toolkit.operation.titles_%s", group);
            return Optional.ofNullable(getBundle(bundleName));
        }).map(b -> CommonBundle.messageOrNull(b, key, params)).orElse(null);
    }

    public String getIjOperationTitle(@Nonnull final String group, @Nonnull final String key, final Object... params) {
        return libBundles.computeIfAbsent(group, k -> {
            final String bundleName = ALL.equals(group) ?
                    "com.microsoft.azure.toolkit.operation.titles_intellij" :
                    String.format("com.microsoft.azure.toolkit.operation.titles_%s_intellij", group);
            return Optional.ofNullable(getBundle(bundleName));
        }).map(b -> CommonBundle.messageOrNull(b, key, params)).orElse(null);
    }

    @Nullable
    private ResourceBundle getBundle(String bundleName) {
        try {
            return ResourceBundle.getBundle(bundleName);
        } catch (final Exception e) {
            return null;
        }
    }

    public static void register() {
        AzureOperationBundle.register(INSTANCE);
    }
}
