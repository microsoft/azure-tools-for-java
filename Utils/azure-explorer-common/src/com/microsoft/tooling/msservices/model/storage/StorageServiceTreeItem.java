/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.tooling.msservices.model.storage;

import com.microsoft.azuretools.azurecommons.helpers.NotNull;
import com.microsoft.tooling.msservices.model.ServiceTreeItem;

public interface StorageServiceTreeItem extends ServiceTreeItem {
    @NotNull
    String getName();
}
