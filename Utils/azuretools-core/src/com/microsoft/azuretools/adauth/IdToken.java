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
