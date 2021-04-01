/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.hdinsight.sdk.common;

import com.microsoft.azuretools.azurecommons.helpers.Nullable;

import java.net.URI;

public class SparkAzureDataLakePoolServiceException extends AzureDataLakeException {
    @Nullable
    private String requestId;

    @Nullable
    private URI requestUri;

    public SparkAzureDataLakePoolServiceException(final int statusCode, final String s, @Nullable final String requestId, @Nullable URI uri) {
        super(statusCode, s);
        this.requestId = requestId;
        this.requestUri = uri;
    }

    @Nullable
    public String getRequestId() {
        return requestId;
    }

    @Nullable
    public URI getRequestUri() {
        return requestUri;
    }

    public void setRequestId(@Nullable String requestId) {
        this.requestId = requestId;
    }
}
