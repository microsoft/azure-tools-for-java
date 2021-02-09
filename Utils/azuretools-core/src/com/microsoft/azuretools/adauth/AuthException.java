/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azuretools.adauth;

import java.io.IOException;

public class AuthException extends IOException {
    private static final long serialVersionUID = 1L;
    private final String error;
    private final String errorMessage;

    public AuthException(String error, String errorMessage) {
        super(error);
        this.error = error;
        this.errorMessage = errorMessage;
    }

    public AuthException(String error) {
        super(error);
        this.error = error;
        this.errorMessage = error;
    }

    public AuthException(Throwable cause) {
        super(cause);
        this.error = cause.getMessage();
        this.errorMessage = cause.getMessage();
    }

    AuthException(String message, Throwable cause) {
        super(message, cause);
        this.error = message;
        this.errorMessage = cause.getMessage();
    }

    public String getError() {
        return error;
    }

    public String getErrorMessage() {
        return errorMessage;
    }
}
