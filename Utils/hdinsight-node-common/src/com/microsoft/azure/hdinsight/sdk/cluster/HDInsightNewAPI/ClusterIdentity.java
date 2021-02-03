/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.hdinsight.sdk.cluster.HDInsightNewAPI;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public class ClusterIdentity{
    @JsonProperty("clusterIdentity.applicationId")
    private String applicationId;

    @JsonProperty("clusterIdentity.certificate")
    private String certificate;

    @JsonProperty("clusterIdentity.aadTenantId")
    private String aadTenantId;

    @JsonProperty("clusterIdentity.resourceUri")
    private String resourceUri;

    @JsonProperty("clusterIdentity.certificatePassword")
    private String certificatePassword;

    public String getClusterIdentityapplicationId(){
        return applicationId;
    }

    public String getClusterIdentitycertificate(){
        return certificate;
    }

    public String getClusterIdentityaadTenantId(){
        return aadTenantId;
    }

    public String getClusterIdentityresourceUri(){
        return resourceUri;
    }

    public String getClusterIdentitycertificatePassword(){
        return certificatePassword;
    }

}
