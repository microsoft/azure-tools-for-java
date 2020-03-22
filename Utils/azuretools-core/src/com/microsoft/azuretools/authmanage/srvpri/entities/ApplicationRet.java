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
public class ApplicationRet {

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
    public String allowActAsForAllClients;

    @JsonProperty
    public String appBranding;

    @JsonProperty
    public String appCategory;

    @JsonProperty
    public String appData;

    @JsonProperty
    public UUID appId;

    @JsonProperty
    public String appMetadata;

    @JsonProperty
    public List<String> appRoles;

    @JsonProperty
    public boolean availableToOtherTenants;

    @JsonProperty
    public String displayName;

    @JsonProperty
    public String encryptedMsiApplicationSecret;

    @JsonProperty
    public String errorUrl;

    @JsonProperty
    public String groupMembershipClaims;

    @JsonProperty
    public String homepage;

    @JsonProperty
    public List<String> identifierUris;

    @JsonProperty
    public List<String> keyCredentials;

    @JsonProperty
    public List<String> knownClientApplications;

    @JsonProperty("logo@odata.mediaContentType")
    public String logo_at_odata_mediaContentType;

    @JsonProperty
    public String logoUrl;

    @JsonProperty
    public String logoutUrl;

    @JsonProperty
    public boolean oauth2AllowImplicitFlow;

    @JsonProperty
    public boolean oauth2AllowUrlPathMatching;

    @JsonProperty
    public List<OAuth2PermissionsRet> oauth2Permissions;

    @JsonProperty
    public boolean oauth2RequirePostResponse;

    @JsonProperty
    public List <PasswordCredentials> passwordCredentials;

    @JsonProperty
    public String publicClient;

    @JsonProperty
    public String recordConsentConditions;

    @JsonProperty
    public List <String> replyUrls;

    @JsonProperty
    public List <String> requiredResourceAccess;

    @JsonProperty
    public String samlMetadataUrl;

    @JsonProperty
    public boolean supportsConvergence;
}
