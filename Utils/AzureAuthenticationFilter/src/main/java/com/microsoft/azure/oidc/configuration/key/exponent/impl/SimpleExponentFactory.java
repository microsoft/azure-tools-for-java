/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.oidc.configuration.key.exponent.impl;

import com.microsoft.azure.oidc.configuration.key.exponent.Exponent;
import com.microsoft.azure.oidc.configuration.key.exponent.ExponentFactory;
import com.microsoft.azure.oidc.exception.PreconditionException;

public final class SimpleExponentFactory implements ExponentFactory {
    private static final ExponentFactory INSTANCE = new SimpleExponentFactory();

    @Override
    public Exponent createKeyExponent(final String value) {
        if (value == null) {
            throw new PreconditionException("Required parameter is null");
        }
        return new SimpleExponent(value);
    }

    public static ExponentFactory getInstance() {
        return INSTANCE;
    }
}
