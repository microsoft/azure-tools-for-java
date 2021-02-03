/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.oidc.token.signature.impl;

import com.microsoft.azure.oidc.exception.PreconditionException;
import com.microsoft.azure.oidc.token.signature.Signature;
import com.microsoft.azure.oidc.token.signature.SignatureFactory;

public final class SimpleSignatureFactory implements SignatureFactory {
    private static final SignatureFactory INSTANCE = new SimpleSignatureFactory();

    @Override
    public Signature createSignature(final String value) {
        if (value == null) {
            throw new PreconditionException("Required parameter is null");
        }
        return new SimpleSignature(value);
    }

    public static SignatureFactory getInstance() {
        return INSTANCE;
    }
}
