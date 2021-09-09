/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.ide.common.store;

import lombok.Getter;

import javax.annotation.Nonnull;

public class AzureStoreManager {
    private static AzureStoreManager instance;

    @Getter
    private IMachineStore machineStore;

    @Getter
    private IIdeStore ideStore;

    @Getter
    private ISecureStore secureStore;

    public static synchronized void register(@Nonnull IMachineStore machineStore,
                                             @Nonnull IIdeStore ideStore,
                                             @Nonnull ISecureStore secureStore) {
        getInstance();
        instance.machineStore = machineStore;
        instance.ideStore = ideStore;
        instance.secureStore = secureStore;
    }

    public static AzureStoreManager getInstance() {
        if (instance == null) {
            synchronized (AzureStoreManager.class) {
                if (instance == null) {
                    instance = new AzureStoreManager();
                }
            }
        }
        return instance;
    }
}
