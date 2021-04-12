/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.oidc.common.name.impl;

import com.microsoft.azure.oidc.common.name.Name;
import com.microsoft.azure.oidc.common.name.NameFactory;
import com.microsoft.azure.oidc.exception.PreconditionException;

public final class SimpleNameFactory implements NameFactory {
    private static final NameFactory INSTANCE = new SimpleNameFactory();

    @Override
    public Name createKeyName(final String name) {
        if (name == null) {
            throw new PreconditionException("Required parameter is null");
        }
        return new SimpleName(name);
    }

    public static NameFactory getInstance() {
        return INSTANCE;
    }
}
