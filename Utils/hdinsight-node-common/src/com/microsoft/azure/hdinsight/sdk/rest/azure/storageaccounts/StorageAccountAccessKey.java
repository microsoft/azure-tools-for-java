/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.hdinsight.sdk.rest.azure.storageaccounts;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.microsoft.azuretools.azurecommons.helpers.NotNull;

@JsonIgnoreProperties(ignoreUnknown = true)
public class StorageAccountAccessKey {
    private String keyName;
    private String value;
    private String permissions;
    public String getKeyName(){
        return keyName;
    }

    public void setKeyName(@NotNull String keyName){
        this.keyName = keyName;
    }

    public String getValue(){
        return value;
    }

    public void setValue(@NotNull String value){
        this.value = value;
    }

    public String getPermissions(){
        return permissions;
    }

    public void setPermissions(@NotNull String permissions){
        this.permissions = permissions;
    }

}
