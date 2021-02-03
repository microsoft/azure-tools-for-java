/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.oidc.configuration.key.modulus.impl;

import com.microsoft.azure.oidc.configuration.key.modulus.Modulus;
import com.microsoft.azure.oidc.configuration.key.modulus.ModulusFactory;
import com.microsoft.azure.oidc.exception.PreconditionException;

public final class SimpleModulusFactory implements ModulusFactory {
    private static final ModulusFactory INSTANCE = new SimpleModulusFactory();

    @Override
    public Modulus createKeyValue(final String value) {
        if (value == null) {
            throw new PreconditionException("Required parameter is null");
        }
        return new SimpleModulus(value);
    }

    public static ModulusFactory getInstance() {
        return INSTANCE;
    }
}
