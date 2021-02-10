/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azuretools.adauth;

import org.codehaus.jackson.annotate.JsonIgnoreProperties;
import org.codehaus.jackson.annotate.JsonProperty;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
class IdToken {
    @JsonProperty
    String aud;

    @JsonProperty(IdTokenClaim.Issuer)
    String issuer;

    @JsonProperty
    String iat;

    @JsonProperty
    String nbf;

    @JsonProperty
    String exp;

    @JsonProperty
    String altsecid;

    @JsonProperty
    String[] amr;

    @JsonProperty(IdTokenClaim.FamilyName)
    String familyName;

    @JsonProperty(IdTokenClaim.GivenName)
    String givenName;

//    @JsonProperty
//    String idp;

    @JsonProperty
    String in_corp;

    @JsonProperty
    String ipaddr;

    @JsonProperty
    String name;

    @JsonProperty(IdTokenClaim.ObjectId)
    String objectId;

    @JsonProperty
    String onprem_sid;

    @JsonProperty(IdTokenClaim.PasswordExpiration)
    String passwordExpiration;

    @JsonProperty(IdTokenClaim.PasswordChangeUrl)
    String passwordChangeUrl;

    @JsonProperty(IdTokenClaim.Subject)
    String subject;

    @JsonProperty(IdTokenClaim.TenantId)
    String tenantId;

    @JsonProperty
    String unique_name;

    @JsonProperty(IdTokenClaim.UPN)
    String upn;

    @JsonProperty
    String ver;

    @JsonProperty(IdTokenClaim.Email)
    String email;

    @JsonProperty(IdTokenClaim.IdentityProvider)
    String identityProvider;

    @JsonProperty
    List<String> wids;
}
