/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.ide.common;

import com.microsoft.azure.toolkit.ide.common.component.Node;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Optional;

public interface IExplorerNodeProvider {
    @Nullable
    default Node<?> getModuleNode(@Nullable Node<?> parent, @Nonnull Manager manager) {
        return Optional.ofNullable(this.getRoot()).map(r -> manager.createNode(r, parent)).orElse(null);
    }

    default int getOrder() {
        return 0;
    }

    @Nullable
    Object getRoot();

    boolean accept(@Nonnull Object data, @Nullable Node<?> parent);

    @Nullable
    Node<?> createNode(@Nonnull Object data, @Nullable Node<?> parent, @Nonnull Manager manager);

    interface Manager {
        @Nullable
        Node<?> createNode(@Nonnull Object data, Node<?> parent);
    }
}
