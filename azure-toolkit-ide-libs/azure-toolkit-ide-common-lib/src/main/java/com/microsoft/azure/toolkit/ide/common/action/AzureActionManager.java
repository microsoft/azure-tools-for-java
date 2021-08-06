/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.ide.common.action;

import com.microsoft.azure.toolkit.lib.common.messager.AzureMessager;
import lombok.Getter;

import java.util.function.Consumer;

public abstract class AzureActionManager {

    @Getter
    private static AzureActionManager instance;

    protected static void register(AzureActionManager manager) {
        if (instance != null) {
            AzureMessager.getDefaultMessager().warning("ActionManager is already registered", null);
            return;
        }
        instance = manager;
    }

    public abstract <T> void registerAction(String id, Action<T> action);

    public <T> void registerAction(String id, Consumer<T> action) {
        this.registerAction(id, new Action<>(action));
    }

    public abstract <T> Action<T> getAction(String id);

    public abstract void registerGroup(String id, ActionGroup group);

    public abstract ActionGroup getGroup(String id);
}
