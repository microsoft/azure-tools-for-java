/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.oidc.configuration.endpoint.impl;

import com.microsoft.azure.oidc.configuration.endpoint.EndPoint;
import com.microsoft.azure.oidc.configuration.endpoint.EndPointFactory;
import com.microsoft.azure.oidc.exception.PreconditionException;

public final class SimpleEndPointFactory implements EndPointFactory {
    private static final EndPointFactory INSTANCE = new SimpleEndPointFactory();

    @Override
    public EndPoint createEndPoint(String name) {
        if (name == null) {
            throw new PreconditionException("Required parameter is null");
        }
        return new SimpleEndPoint(name);
    }

    public static EndPointFactory getInstance() {
        return INSTANCE;
    }
}
