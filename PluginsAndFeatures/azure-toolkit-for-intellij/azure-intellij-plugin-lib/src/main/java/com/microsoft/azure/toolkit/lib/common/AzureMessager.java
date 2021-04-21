/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.lib.common;

import javax.annotation.Nonnull;

public abstract class AzureMessager {

    private static AzureMessager instance;

    public static synchronized void register(AzureMessager manager) {
        if (AzureMessager.instance == null) {
            AzureMessager.instance = manager;
        }
    }

    public static AzureMessager getInstance() {
        return AzureMessager.instance;
    }

    public abstract boolean confirm(@Nonnull String title, @Nonnull String message);

    public abstract void alert(@Nonnull String title, @Nonnull String message);

    public abstract void alert(@Nonnull String message);

    public abstract void success(@Nonnull String title, @Nonnull String message);

    public abstract void success(@Nonnull String message);

    public abstract void info(@Nonnull String title, @Nonnull String message);

    public abstract void warning(@Nonnull String title, @Nonnull String message);

    public abstract void error(@Nonnull Throwable throwable);

    public abstract void error(@Nonnull String title, @Nonnull Throwable throwable);
}
