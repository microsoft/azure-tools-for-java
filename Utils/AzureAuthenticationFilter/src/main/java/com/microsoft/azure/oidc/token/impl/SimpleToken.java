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
import com.microsoft.azure.oidc.token.email.Email;
import com.microsoft.azure.oidc.token.payload.Payload;
import com.microsoft.azure.oidc.token.signature.Signature;

final class SimpleToken implements Token {
    private final Name keyName;
    private final Algorithm algorithm;
    private final TimeStamp issuedAt;
    private final TimeStamp notBefore;
    private final TimeStamp expiration;
    private final Issuer issuer;
    private final ID userID;
    private final List<Email> userEmails;
    private final ID audience;
    private final Payload payload;
    private final Signature signature;

    public SimpleToken(final Name keyName, final Algorithm algorithm, final TimeStamp issuedAt,
            final TimeStamp notBefore, final TimeStamp expiration, final ID userID, final List<Email> userEmails,
            final Issuer issuer, final ID audience, final Payload payload, final Signature signature) {
        if (keyName == null || algorithm == null || issuedAt == null || notBefore == null || expiration == null
                || userID == null || userEmails == null || issuer == null || audience == null || payload == null
                || signature == null) {
            throw new PreconditionException("Required parameter is null");
        }
        if(userEmails.isEmpty()) {
            throw new PreconditionException("Emails list is empty");
        }
        this.keyName = keyName;
        this.algorithm = algorithm;
        this.issuedAt = issuedAt;
        this.notBefore = notBefore;
        this.expiration = expiration;
        this.userID = userID;
        this.userEmails = userEmails;
        this.issuer = issuer;
        this.audience = audience;
        this.payload = payload;
        this.signature = signature;
    }

    @Override
    public Name getKeyName() {
        return keyName;
    }

    @Override
    public Algorithm getAlgorithm() {
        return algorithm;
    }

    @Override
    public TimeStamp getIssuedAt() {
        return issuedAt;
    }

    @Override
    public TimeStamp getNotBefore() {
        return notBefore;
    }

    @Override
    public TimeStamp getExpiration() {
        return expiration;
    }

    @Override
    public Issuer getIssuer() {
        return issuer;
    }

    @Override
    public ID getAudience() {
        return audience;
    }

    @Override
    public ID getUserID() {
        return userID;
    }

    @Override
    public List<Email> getUserEmails() {
        return userEmails;
    }

    @Override
    public String getValue() {
        final StringBuilder builder = new StringBuilder();
        builder.append(payload.getValue());
        builder.append(".");
        builder.append(signature.getValue());
        return builder.toString();
    }

    @Override
    public Payload getPayload() {
        return payload;
    }

    @Override
    public Signature getSignature() {
        return signature;
    }

    @Override
    public String toString() {
        final StringBuilder builder = new StringBuilder();
        builder.append(payload.getValue());
        builder.append(".");
        builder.append(signature.getValue());
        return builder.toString();
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((algorithm == null) ? 0 : algorithm.hashCode());
        result = prime * result + ((audience == null) ? 0 : audience.hashCode());
        result = prime * result + ((expiration == null) ? 0 : expiration.hashCode());
        result = prime * result + ((issuedAt == null) ? 0 : issuedAt.hashCode());
        result = prime * result + ((issuer == null) ? 0 : issuer.hashCode());
        result = prime * result + ((keyName == null) ? 0 : keyName.hashCode());
        result = prime * result + ((notBefore == null) ? 0 : notBefore.hashCode());
        result = prime * result + ((payload == null) ? 0 : payload.hashCode());
        result = prime * result + ((signature == null) ? 0 : signature.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        SimpleToken other = (SimpleToken) obj;
        if (algorithm == null) {
            if (other.algorithm != null)
                return false;
        } else if (!algorithm.equals(other.algorithm))
            return false;
        if (audience == null) {
            if (other.audience != null)
                return false;
        } else if (!audience.equals(other.audience))
            return false;
        if (expiration == null) {
            if (other.expiration != null)
                return false;
        } else if (!expiration.equals(other.expiration))
            return false;
        if (issuedAt == null) {
            if (other.issuedAt != null)
                return false;
        } else if (!issuedAt.equals(other.issuedAt))
            return false;
        if (issuer == null) {
            if (other.issuer != null)
                return false;
        } else if (!issuer.equals(other.issuer))
            return false;
        if (keyName == null) {
            if (other.keyName != null)
                return false;
        } else if (!keyName.equals(other.keyName))
            return false;
        if (notBefore == null) {
            if (other.notBefore != null)
                return false;
        } else if (!notBefore.equals(other.notBefore))
            return false;
        if (payload == null) {
            if (other.payload != null)
                return false;
        } else if (!payload.equals(other.payload))
            return false;
        if (signature == null) {
            if (other.signature != null)
                return false;
        } else if (!signature.equals(other.signature))
            return false;
        return true;
    }
}
