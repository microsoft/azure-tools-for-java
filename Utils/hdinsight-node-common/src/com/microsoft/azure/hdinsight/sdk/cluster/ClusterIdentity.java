/**
 * Copyright (c) Microsoft Corporation
 * <p/>
 * All rights reserved.
 * <p/>
 * MIT License
 * <p/>
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated
 * documentation files (the "Software"), to deal in the Software without restriction, including without limitation
 * the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and
 * to permit persons to whom the Software is furnished to do so, subject to the following conditions:
 * <p/>
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of
 * the Software.
 * <p/>
 * THE SOFTWARE IS PROVIDED *AS IS*, WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO
 * THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT,
 * TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
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
