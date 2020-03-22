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

class OAuthReservedClaim {
    public static final String Code = "code";
    public static final String TokenType = "token_type";
    public static final String AccessToken = "access_token";
    public static final String RefreshToken = "refresh_token";
    public static final String Resource = "resource";
    public static final String IdToken = "id_token";
    public static final String CreatedOn = "created_on";
    public static final String ExpiresOn = "expires_on";
    public static final String ExpiresIn = "expires_in";
    public static final String NotBefore = "not_before";
    public static final String Error = "error";
    public static final String ErrorSubcode = "error_subcode";
    public static final String ErrorDescription = "error_description";
    public static final String ErrorCodes = "error_codes";
}
