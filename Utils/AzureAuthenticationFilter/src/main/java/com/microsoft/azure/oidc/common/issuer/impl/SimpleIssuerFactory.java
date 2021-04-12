/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.oidc.common.issuer.impl;

import com.microsoft.azure.oidc.common.issuer.Issuer;
import com.microsoft.azure.oidc.common.issuer.IssuerFactory;
import com.microsoft.azure.oidc.exception.PreconditionException;

public final class SimpleIssuerFactory implements IssuerFactory {
    private static final IssuerFactory INSTANCE = new SimpleIssuerFactory();

    @Override
    public Issuer createIssuer(final String name) {
        if (name == null) {
            throw new PreconditionException("Required parameter is null");
        }
        return new SimpleIssuer(name);
    }

    public static IssuerFactory getInstance() {
        return INSTANCE;
    }
}
