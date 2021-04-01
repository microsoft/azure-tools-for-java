/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.hdinsight.sdk.rest.azure.storageaccounts.api;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.microsoft.azure.hdinsight.sdk.rest.azure.storageaccounts.StorageAccountAccessKey;
import com.microsoft.azuretools.azurecommons.helpers.NotNull;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public class PostListKeysResponse {
    private List<StorageAccountAccessKey> keys;
    public List<StorageAccountAccessKey> getKeys(){
        return  keys;
    }

    public void setKeys(@NotNull List<StorageAccountAccessKey> keys){
        this.keys = keys;
    }
}
