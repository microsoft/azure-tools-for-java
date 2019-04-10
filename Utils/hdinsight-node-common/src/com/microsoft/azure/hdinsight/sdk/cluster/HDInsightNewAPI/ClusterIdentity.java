/*
 * Copyright (c) Microsoft Corporation
 *
 * All rights reserved.
 *
 * MIT License
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated
 * documentation files (the "Software"), to deal in the Software without restriction, including without limitation
 * the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and
 * to permit persons to whom the Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of
 * the Software.
 *
 * THE SOFTWARE IS PROVIDED *AS IS*, WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO
 * THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT,
 * TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
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
