/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.connector.explorer.node;

import com.intellij.openapi.actionSystem.DataProvider;
import com.microsoft.azure.toolkit.lib.common.action.IActionGroup;

import javax.annotation.Nullable;

public interface IAzureProjectExplorerNode extends DataProvider {
    @Nullable
    IActionGroup getActionGroup();

    default void triggerClickAction(Object event) {

    }

    default void triggerDoubleClickAction(Object event) {

    }
}
