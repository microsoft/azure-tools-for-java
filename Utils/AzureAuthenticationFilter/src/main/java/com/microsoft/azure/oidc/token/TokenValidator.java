/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.oidc.token;

public interface TokenValidator {

    Boolean validateSignature(Token token);

    Boolean validateAudience(Token token);

    Boolean validateIssuer(Token token);

    Boolean validateIssuedAt(Token token);

    Boolean validateNotBefore(Token token);

    Boolean validateExpiration(Token token);

    Boolean validateCommon(Token token);

}
