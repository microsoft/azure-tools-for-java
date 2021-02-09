/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
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
