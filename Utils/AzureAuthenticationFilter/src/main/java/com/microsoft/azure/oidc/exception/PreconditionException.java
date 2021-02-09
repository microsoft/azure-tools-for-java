/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.oidc.exception;

public final class PreconditionException extends NullPointerException {
    private static final long serialVersionUID = 1L;

    public PreconditionException() {
        super();
    }

    public PreconditionException(String message) {
        super(message);
    }
}
