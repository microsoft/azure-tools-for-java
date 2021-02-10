/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.oidc.configuration.key.impl;

import com.microsoft.azure.oidc.common.timestamp.TimeStamp;
import com.microsoft.azure.oidc.configuration.key.Key;
import com.microsoft.azure.oidc.configuration.key.KeyFactory;
import com.microsoft.azure.oidc.configuration.key.exponent.Exponent;
import com.microsoft.azure.oidc.configuration.key.modulus.Modulus;
import com.microsoft.azure.oidc.exception.PreconditionException;

public final class SimpleKeyFactory implements KeyFactory {
    private static final KeyFactory INSTANCE = new SimpleKeyFactory();

    @Override
    public Key createKey(final TimeStamp notBefore, final Modulus secret, final Exponent exponent) {
        if (notBefore == null || secret == null || exponent == null) {
            throw new PreconditionException("Required parameter is null");
        }
        return new SimpleKey(notBefore, secret, exponent);
    }

    public static KeyFactory getInstance() {
        return INSTANCE;
    }
}
