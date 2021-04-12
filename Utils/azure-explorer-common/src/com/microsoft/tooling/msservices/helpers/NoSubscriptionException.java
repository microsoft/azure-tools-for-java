/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.tooling.msservices.helpers;

public class NoSubscriptionException extends Exception {
    public NoSubscriptionException(String message) {
        super(message);
    }
}
