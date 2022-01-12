/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.ide.common.icon;

import com.microsoft.azure.toolkit.lib.common.messager.AzureMessager;

import javax.annotation.Nonnull;
import javax.swing.*;

public abstract class AzureIconManager {
    private static AzureIconManager manager;

    public static AzureIconManager getInstance() {
        return AzureIconManager.manager;
    }

    public static synchronized void register(@Nonnull AzureIconManager manager) {
        if (AzureIconManager.manager == null) { // not allow overwriting...
            AzureIconManager.manager = manager;
        } else {
            AzureMessager.getMessager().warning("Icon manager has already been registered");
        }
    }

    public abstract Icon getIcon(@Nonnull final AzureIcon icon);
}
