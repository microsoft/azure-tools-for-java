/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.hdinsight.sdk.common.livy.interactive.exceptions;

public class SessionNotStartException extends LivyInteractiveException {
    public SessionNotStartException(String message) {
        super(message);
    }

    public SessionNotStartException(String message, Throwable throwable) {
        super(message, throwable);
    }
}
