/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.hdinsight.sdk.common.livy.interactive.exceptions;

import com.microsoft.azure.hdinsight.sdk.common.HDIException;

public class LivyInteractiveException extends HDIException {
    public LivyInteractiveException(String message) {
        super(message);
    }

    public LivyInteractiveException(String message, Throwable throwable) {
        super(message, throwable);
    }
}
