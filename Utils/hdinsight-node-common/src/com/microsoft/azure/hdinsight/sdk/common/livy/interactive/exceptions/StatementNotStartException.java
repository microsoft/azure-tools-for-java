/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.hdinsight.sdk.common.livy.interactive.exceptions;

public class StatementNotStartException extends LivyInteractiveException {
    public StatementNotStartException(String message) {
        super(message);
    }

    public StatementNotStartException(String message, Throwable throwable) {
        super(message, throwable);
    }
}
