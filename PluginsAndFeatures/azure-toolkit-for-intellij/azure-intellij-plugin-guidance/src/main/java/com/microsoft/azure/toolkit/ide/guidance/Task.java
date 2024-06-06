/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.ide.guidance;

import com.intellij.openapi.Disposable;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public interface Task extends Disposable {

    void execute() throws Exception;

    default void prepare() {
    }

    default boolean isReady() {
        return true;
    }

    default boolean isDone() {
        return false;
    }

    default boolean isToSkip() {
        return false;
    }

    @Nonnull
    String getName();

    @Nullable
    default String getDescription() {
        return this.getName() + "desc";
    }

    @Nullable
    default String getDoc() {
        return null;
    }

    @Override
    default void dispose() {

    }
}
