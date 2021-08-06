/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.lib.common;

public class OperationNotSupportedException extends RuntimeException {
    public OperationNotSupportedException() {
        super();
    }

    public OperationNotSupportedException(final String message) {
        super(message);
    }
}
