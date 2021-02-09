/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.hdinsight.sdk.storage;

import com.microsoft.azuretools.azurecommons.helpers.Nullable;

public interface IHDIStorageAccount {
    String getName();
    StorageAccountType getAccountType();
    String getDefaultContainerOrRootPath();
    String getSubscriptionId();

    @Nullable
    default String getDefaultStorageSchema(){
        return null;
    }
}


