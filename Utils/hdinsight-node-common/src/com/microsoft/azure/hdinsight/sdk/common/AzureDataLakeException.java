/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.hdinsight.sdk.common;

import java.io.IOException;

public class AzureDataLakeException extends IOException {
    private final int statusCode;

    public AzureDataLakeException(final int statusCode, final String s) {
        super(s);
        this.statusCode = statusCode;
    }

    public int getStatusCode() {
        return statusCode;
    }
}
