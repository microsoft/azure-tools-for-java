/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.hdinsight.sdk.cluster.HDInsightNewAPI;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Map;

@JsonIgnoreProperties(ignoreUnknown = true)
public class Configurations  {
    @JsonProperty("core-site")
    private Map<String, String> coresite;

    @JsonProperty("clusterIdentity")
    private ClusterIdentity clusterIdentity;

    @JsonProperty("gateway")
    private Gateway gateway;

    public Map<String,String> getCoresite(){
        return coresite;
    }

    public ClusterIdentity getClusterIdentity(){
        return clusterIdentity;
    }

    public Gateway getGateway(){
        return gateway;
    }
}
