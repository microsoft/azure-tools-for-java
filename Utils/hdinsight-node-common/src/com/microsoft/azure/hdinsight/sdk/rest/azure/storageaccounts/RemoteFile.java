/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.hdinsight.sdk.rest.azure.storageaccounts;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public class RemoteFile {
    @JsonProperty(value = "name")
    private String name;

    @JsonProperty(value = "isDirectory")
    private boolean isDirectory;

    public String getName() {
        return name;
    }

    public boolean isDirectory() {
        return isDirectory;
    }
}
