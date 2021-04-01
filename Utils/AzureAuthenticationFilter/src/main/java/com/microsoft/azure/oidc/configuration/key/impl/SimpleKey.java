/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.oidc.configuration.key.impl;

import com.microsoft.azure.oidc.common.timestamp.TimeStamp;
import com.microsoft.azure.oidc.configuration.key.Key;
import com.microsoft.azure.oidc.configuration.key.exponent.Exponent;
import com.microsoft.azure.oidc.configuration.key.modulus.Modulus;
import com.microsoft.azure.oidc.exception.PreconditionException;

final class SimpleKey implements Key {
    private final TimeStamp notBefore;
    private final Modulus secret;
    private final Exponent exponent;

    public SimpleKey(final TimeStamp notBefore, final Modulus secret, final Exponent exponent) {
        if (notBefore == null || secret == null || exponent == null) {
            throw new PreconditionException("Required parameter is null");
        }
        this.notBefore = notBefore;
        this.secret = secret;
        this.exponent = exponent;
    }

    @Override
    public TimeStamp getNotBefore() {
        return notBefore;
    }

    @Override
    public Modulus getSecret() {
        return secret;
    }

    @Override
    public Exponent getExponent() {
        return exponent;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((exponent == null) ? 0 : exponent.hashCode());
        result = prime * result + ((secret == null) ? 0 : secret.hashCode());
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
        SimpleKey other = (SimpleKey) obj;
        if (exponent == null) {
            if (other.exponent != null)
                return false;
        } else if (!exponent.equals(other.exponent))
            return false;
        if (secret == null) {
            if (other.secret != null)
                return false;
        } else if (!secret.equals(other.secret))
            return false;
        return true;
    }
}
