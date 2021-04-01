/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.oidc.token.email.impl;

import com.microsoft.azure.oidc.exception.PreconditionException;
import com.microsoft.azure.oidc.token.email.Email;
import com.microsoft.azure.oidc.token.email.EmailFactory;

public final class SimpleEmailFactory implements EmailFactory {
    private static final EmailFactory INSTANCE = new SimpleEmailFactory();

    @Override
    public Email createEmail(String value) {
        if (value == null) {
            throw new PreconditionException("Required parameter is null");
        }
        return new SimpleEmail(value);
    }

    public static EmailFactory getInstance() {
        return INSTANCE;
    }
}
