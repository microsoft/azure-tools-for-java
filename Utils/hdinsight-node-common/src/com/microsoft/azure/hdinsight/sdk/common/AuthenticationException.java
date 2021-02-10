/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.hdinsight.sdk.common;

public class AuthenticationException extends HDIException {
    public AuthenticationException(String message, int errorCode) {
        super(message, errorCode);
    }
}
