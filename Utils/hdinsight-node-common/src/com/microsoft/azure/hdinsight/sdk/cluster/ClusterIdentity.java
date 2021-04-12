/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.hdinsight.sdk.cluster;

import com.google.gson.annotations.SerializedName;
import com.microsoft.azuretools.azurecommons.helpers.Nullable;

public class ClusterIdentity{
    @SerializedName("clusterIdentity.applicationId")
    private String applicationId;

    @SerializedName("clusterIdentity.certificate")
    private String certificate;

    @SerializedName("clusterIdentity.aadTenantId")
    private String aadTenantId;

    @SerializedName("clusterIdentity.resourceUri")
    private String resourceUri;

    @SerializedName("clusterIdentity.certificatePassword")
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

    public void setClusterIdentityapplicationId(@Nullable String applicationId) {
        this.applicationId = applicationId;
    }

    public void setClusterIdentitycertificate(@Nullable String certificate) {
        this.certificate = certificate;
    }

    public void setClusterIdentityaadTenantId(@Nullable String aadTenantId) {
        this.aadTenantId = aadTenantId;
    }

    public void setClusterIdentityresourceUri(@Nullable String resourceUri) {
        this.resourceUri = resourceUri;
    }

    public void setClusterIdentitycertificatePassword(@Nullable String certificatePassword) {
        this.certificatePassword = certificatePassword;
    }
}
