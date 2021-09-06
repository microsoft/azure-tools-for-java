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
    private IFileStore fileStore;

    @Getter
    private IApplicationStore appStore;

    @Getter
    private ISecureStore secureStore;

    public static synchronized void register(@Nonnull IFileStore fileStore,
                                             @Nonnull IApplicationStore appStore,
                                             @Nonnull ISecureStore secureStore) {
        getInstance();
        instance.fileStore = fileStore;
        instance.appStore = appStore;
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
