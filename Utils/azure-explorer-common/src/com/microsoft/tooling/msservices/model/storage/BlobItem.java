/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.tooling.msservices.model.storage;


import com.microsoft.azuretools.azurecommons.helpers.NotNull;

public interface BlobItem {
    public static enum BlobItemType {
        BlobFile,
        BlobDirectory
    }

    @NotNull
    String getName();

    @NotNull
    String getUri();

    @NotNull
    String getContainerName();

    @NotNull
    String getPath();

    @NotNull
    BlobItemType getItemType();
}
