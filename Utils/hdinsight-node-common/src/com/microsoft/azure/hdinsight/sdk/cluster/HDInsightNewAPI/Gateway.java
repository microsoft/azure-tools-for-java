/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.hdinsight.sdk.cluster.HDInsightNewAPI;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public class Gateway {
    @JsonProperty("restAuthCredential.isEnabled")
    private String isEnabled;

    @JsonProperty("restAuthCredential.password")
    private String password;

    @JsonProperty("restAuthCredential.username")
    private String username;

    public String getIsEnabled(){
        return isEnabled;
    }

    public String getPassword(){
        return password;
    }

    public String getUsername(){
        return username;
    }
}
