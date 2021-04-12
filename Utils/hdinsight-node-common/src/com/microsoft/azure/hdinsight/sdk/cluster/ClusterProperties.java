/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.hdinsight.sdk.cluster;

import com.microsoft.azuretools.azurecommons.helpers.Nullable;

import java.util.List;

public class ClusterProperties {
    private String clusterVersion;
    private String osType;
    private ClusterDefinition clusterDefinition;
    private ComputeProfile computeProfile;
    private String provisioningState;
    private String clusterState;
    private String createdDate;
    private QuotaInfo quotaInfo;
    private List<ConnectivityEndpoint> connectivityEndpoints;
    private SecurityProfile securityProfile;

    public String getClusterVersion(){
        return clusterVersion;
    }

    public String getOsType(){
        return osType;
    }

    public String getProvisioningState(){
        return provisioningState;
    }

    public String getClusterState(){
        return clusterState;
    }

    public ClusterDefinition getClusterDefinition() {
        return clusterDefinition;
    }

    public ComputeProfile getComputeProfile(){
        return computeProfile;
    }

    @Nullable
    public SecurityProfile getSecurityProfile(){
        return securityProfile;
    }

    public String getCreatedDate(){
        return createdDate;
    }

    public QuotaInfo getQuotaInfo(){
        return quotaInfo;
    }

    public List<ConnectivityEndpoint> getConnectivityEndpoints(){
        return connectivityEndpoints;
    }
}
