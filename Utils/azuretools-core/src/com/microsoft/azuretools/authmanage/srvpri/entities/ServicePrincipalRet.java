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

package com.microsoft.azuretools.authmanage.srvpri.entities;

import org.codehaus.jackson.annotate.JsonIgnoreProperties;
import org.codehaus.jackson.annotate.JsonProperty;

import java.util.List;
import java.util.UUID;

/**
 * Created by vlashch on 8/18/16.
 */

// inconming
@JsonIgnoreProperties(ignoreUnknown = true)
public class ServicePrincipalRet {

    @JsonProperty("odata.metadata")
    public String odata_metadata;

    @JsonProperty("odata.type")
    public String odata_type;

    @JsonProperty
    public String objectType;

    @JsonProperty
    public UUID objectId;

    @JsonProperty
    public String deletionTimestamp;

    @JsonProperty
    public boolean accountEnabled;

    @JsonProperty
    public String appBranding;

    @JsonProperty
    public String appCategory;

    @JsonProperty
    public String appData;

    @JsonProperty
    public String appDisplayName;

    @JsonProperty
    UUID appId;

    @JsonProperty
    public String appMetadata;

    @JsonProperty
    public UUID appOwnerTenantId;

    @JsonProperty
    public boolean appRoleAssignmentRequired;

    @JsonProperty
    public List<String> appRoles;

    @JsonProperty
    public String authenticationPolicy;

    @JsonProperty
    public String displayName;

    @JsonProperty
    public String errorUrl;

    @JsonProperty
    public String homepage;

    @JsonProperty
    public List<String> keyCredentials;

    @JsonProperty
    public String logoutUrl;

    @JsonProperty
    public String microsoftFirstParty;

    @JsonProperty
    public List<OAuth2PermissionsRet> oauth2Permissions;

    @JsonProperty
    public List <PasswordCredentials> passwordCredentials;

    @JsonProperty
    public String preferredTokenSigningKeyThumbprint;

    @JsonProperty
    public String publisherName;

    @JsonProperty
    public List <String> replyUrls;

    @JsonProperty
    public String samlMetadataUrl;

    @JsonProperty
    public List <String> servicePrincipalNames;

    @JsonProperty
    public List <String> tags;

    @JsonProperty
    public String useCustomTokenSigningKey;
}
