/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.ide.common;

import com.microsoft.azure.toolkit.ide.common.component.Node;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public interface IExplorerNodeProvider {

    default int getOrder() {
        return 0;
    }

    @Nullable
    default Object getRoot() {
        return null;
    }

    default boolean isAzureService() {
        return true;
    }

    boolean accept(@Nonnull Object data, @Nullable Node<?> parent, ViewType type);

    @Nullable
    Node<?> createNode(@Nonnull Object data, @Nullable Node<?> parent, @Nonnull Manager manager);

    interface Manager {
        @Nonnull
        Node<?> createNode(@Nonnull Object data, Node<?> parent, ViewType type);
    }

    enum ViewType {
        APP_CENTRIC, TYPE_CENTRIC
    }
}
