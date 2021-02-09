/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azuretools.adauth;

import org.codehaus.jackson.annotate.JsonIgnoreProperties;
import org.codehaus.jackson.annotate.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
class TokenResponse {

    private final static String CorrelationIdClaim = "correlation_id";

    @JsonProperty(OAuthReservedClaim.TokenType)
    String tokenType;

    @JsonProperty(OAuthReservedClaim.AccessToken)
    String accessToken;

    @JsonProperty(OAuthReservedClaim.RefreshToken)
    String refreshToken;

    @JsonProperty(OAuthReservedClaim.Resource)
    String resource;

    @JsonProperty(OAuthReservedClaim.IdToken)
    String idToken;

    @JsonProperty(OAuthReservedClaim.CreatedOn)
    long createdOn;

    @JsonProperty(OAuthReservedClaim.ExpiresOn)
    long expiresOn;

    @JsonProperty(OAuthReservedClaim.ExpiresIn)
    long expiresIn;

    @JsonProperty(OAuthReservedClaim.NotBefore)
    long notBefore;

    @JsonProperty(OAuthReservedClaim.Error)
    String error;

    @JsonProperty(OAuthReservedClaim.ErrorDescription)
    String errorDescription;

    @JsonProperty(OAuthReservedClaim.ErrorCodes)
    String[] errorCodes;

    @JsonProperty(CorrelationIdClaim)
    String correlationId;

    @JsonProperty(OAuthParameter.Scope)
    String scope;

    @JsonProperty(IdTokenClaim.PasswordExpiration)
    String PasswordExpiration;

    @JsonProperty(IdTokenClaim.PasswordChangeUrl)
    String PasswordChangeUrl;

}
