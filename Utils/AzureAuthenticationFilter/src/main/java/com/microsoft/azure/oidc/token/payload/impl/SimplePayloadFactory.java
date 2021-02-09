/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.oidc.token.payload.impl;

import com.microsoft.azure.oidc.exception.PreconditionException;
import com.microsoft.azure.oidc.token.payload.Payload;
import com.microsoft.azure.oidc.token.payload.PayloadFactory;

public final class SimplePayloadFactory implements PayloadFactory {
    private static final PayloadFactory INSTANCE = new SimplePayloadFactory();

    @Override
    public Payload createPayload(String header, String body) {
        if (header == null || body == null) {
            throw new PreconditionException("Required parameter is null");
        }
        return new SimplePayload(header, body);
    }

    public static PayloadFactory getInstance() {
        return INSTANCE;
    }
}
