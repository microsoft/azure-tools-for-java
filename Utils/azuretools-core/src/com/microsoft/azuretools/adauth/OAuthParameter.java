/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azuretools.adauth;

class OAuthParameter {
    public static final String ResponseType = "response_type";
    public static final String GrantType = "grant_type";
    public static final String ClientId = "client_id";
    public static final String ClientSecret = "client_secret";
    public static final String ClientAssertion = "client_assertion";
    public static final String ClientAssertionType = "client_assertion_type";
    public static final String RefreshToken = "refresh_token";
    public static final String RedirectUri = "redirect_uri";
    public static final String Resource = "resource";
    public static final String Code = "code";
    public static final String Scope = "scope";
    public static final String Assertion = "assertion";
    public static final String RequestedTokenUse = "requested_token_use";
    public static final String Username = "username";
    public static final String Password = "password";

    public static final String FormsAuth = "amr_values";
    public static final String LoginHint = "login_hint"; // login_hint is not standard oauth2 parameter
    public static final String CorrelationId = OAuthHeader.CorrelationId; // correlation id is not standard oauth2 parameter
    public static final String Prompt = "prompt"; // prompt is not standard oauth2 parameter
}
