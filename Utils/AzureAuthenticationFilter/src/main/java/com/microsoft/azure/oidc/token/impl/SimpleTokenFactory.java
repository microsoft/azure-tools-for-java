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
