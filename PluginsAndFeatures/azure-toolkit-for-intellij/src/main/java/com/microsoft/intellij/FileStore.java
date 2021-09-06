/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */
package com.microsoft.intellij;

import com.microsoft.azure.toolkit.ide.common.store.IFileStore;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public class FileStore implements IFileStore {

    @Nullable
    @Override
    public String getProperty(@Nonnull String s, @Nonnull String s1) {
        // TODO: need implemented
        return null;
    }

    @Nullable
    @Override
    public String getProperty(@Nonnull String s, @Nonnull String s1, @Nullable String s2) {
        // TODO: need implemented
        return null;
    }

    @Override
    public void setProperty(@Nonnull String s, @Nonnull String s1, @Nullable String s2) {
        // TODO: need implemented
    }

    @Override
    public void load() {
        // TODO: need implemented
    }
}
