/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.hdinsight.sdk.rest.azure.storageaccounts.api;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.microsoft.azure.hdinsight.sdk.rest.azure.storageaccounts.RemoteFile;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public class GetRemoteFilesResponse {
    @JsonProperty(value = "paths")
    private List<RemoteFile> remoteFiles;

    public List<RemoteFile> getRemoteFiles(){
        return remoteFiles;
    }
}
