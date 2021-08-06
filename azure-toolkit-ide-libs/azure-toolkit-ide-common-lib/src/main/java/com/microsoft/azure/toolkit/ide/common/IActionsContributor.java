/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.ide.common;

import com.microsoft.azure.toolkit.ide.common.action.AzureActionManager;

public interface IActionsContributor {
    default void registerActions(AzureActionManager am) {
    }

    default void registerGroups(AzureActionManager am) {
    }

    default void registerHandlers(AzureActionManager am) {
    }

    default int getZOrder() {
        return 0;
    }
}
