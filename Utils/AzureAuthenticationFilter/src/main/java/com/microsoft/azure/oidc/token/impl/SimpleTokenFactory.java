/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.oidc.token.impl;

import java.util.List;

import com.microsoft.azure.oidc.common.algorithm.Algorithm;
import com.microsoft.azure.oidc.common.id.ID;
import com.microsoft.azure.oidc.common.issuer.Issuer;
import com.microsoft.azure.oidc.common.name.Name;
import com.microsoft.azure.oidc.common.timestamp.TimeStamp;
import com.microsoft.azure.oidc.exception.PreconditionException;
import com.microsoft.azure.oidc.token.Token;
import com.microsoft.azure.oidc.token.TokenFactory;
import com.microsoft.azure.oidc.token.email.Email;
import com.microsoft.azure.oidc.token.payload.Payload;
import com.microsoft.azure.oidc.token.signature.Signature;

final class SimpleTokenFactory implements TokenFactory {
    private static final TokenFactory INSTANCE = new SimpleTokenFactory();

    @Override
    public Token createToken(final Name keyName, final Algorithm algorithm, final TimeStamp issuedAt,
            final TimeStamp notBefore, final TimeStamp expiration, final ID userID, final List<Email> userEmails,
            final Issuer issuer, final ID audience, final Payload payload, final Signature signature) {
        if (keyName == null || algorithm == null || issuedAt == null || notBefore == null || expiration == null
                || userID == null || userEmails == null || issuer == null || audience == null || payload == null
                || signature == null) {
            throw new PreconditionException("Required parameter is null");
        }
        return new SimpleToken(keyName, algorithm, issuedAt, notBefore, expiration, userID, userEmails, issuer,
                audience, payload, signature);
    }

    public static TokenFactory getInstance() {
        return INSTANCE;
    }
}
