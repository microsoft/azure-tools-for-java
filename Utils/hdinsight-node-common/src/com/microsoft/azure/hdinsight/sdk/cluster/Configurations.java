/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.hdinsight.sdk.cluster;

import com.google.gson.annotations.SerializedName;
import com.microsoft.azuretools.azurecommons.helpers.NotNull;
import com.microsoft.azuretools.azurecommons.helpers.Nullable;

import java.util.Map;

public class Configurations  {

    @SerializedName("core-site")
    private Map<String, String> coresite;
    private ClusterIdentity clusterIdentity;
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

    public void setCoresite(@Nullable Map<String, String> coresite) {
        this.coresite = coresite;
    }

    public void setClusterIdentity(@Nullable ClusterIdentity clusterIdentity) {
        this.clusterIdentity = clusterIdentity;
    }

    public void setGateway(@Nullable Gateway gateway) {
        this.gateway = gateway;
    }
}
