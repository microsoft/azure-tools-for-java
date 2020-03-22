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
