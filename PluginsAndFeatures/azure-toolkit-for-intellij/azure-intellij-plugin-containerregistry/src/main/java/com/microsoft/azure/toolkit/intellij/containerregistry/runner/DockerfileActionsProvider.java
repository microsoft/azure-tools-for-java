/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */
package com.microsoft.azure.toolkit.intellij.containerregistry.runner;

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.vfs.VirtualFile;
import com.microsoft.applicationinsights.core.dependencies.javaxannotation.Nonnull;
import com.microsoft.applicationinsights.core.dependencies.javaxannotation.Nullable;

public interface DockerfileActionsProvider {
    @Nullable
    AnAction[] getActions(@Nonnull final VirtualFile dockerfile);

    default int getPriority() {
        return 10;
    }
}
