/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.oidc.common.id.impl;

import com.microsoft.azure.oidc.common.id.ID;
import com.microsoft.azure.oidc.common.id.IDFactory;
import com.microsoft.azure.oidc.exception.PreconditionException;

public final class SimpleIDFactory implements IDFactory {
    private static final IDFactory INSTANCE = new SimpleIDFactory();

    @Override
    public ID createID(String value) {
        if (value == null) {
            throw new PreconditionException("Required parameter is null");
        }
        return new SimpleID(value);
    }

    public static IDFactory getInstance() {
        return INSTANCE;
    }
}
