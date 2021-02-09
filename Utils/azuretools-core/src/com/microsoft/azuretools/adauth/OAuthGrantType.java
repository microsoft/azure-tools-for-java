/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azuretools.adauth;

class OAuthGrantType {
    public final static String AuthorizationCode = "authorization_code";
    public final static String RefreshToken = "refresh_token";
    public final static String ClientCredentials = "client_credentials";
    public final static String Saml11Bearer = "urn:ietf:params:oauth:grant-type:saml1_1-bearer";
    public final static String Saml20Bearer = "urn:ietf:params:oauth:grant-type:saml2-bearer";
    public final static String JwtBearer = "urn:ietf:params:oauth:grant-type:jwt-bearer";
    public final static String Password = "password";
}
