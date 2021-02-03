/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.hdinsight.sdk.common.errorresponse;

import com.microsoft.azuretools.azurecommons.helpers.NotNull;
import com.microsoft.azuretools.azurecommons.helpers.Nullable;
import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpException;

import java.util.Arrays;
import java.util.stream.Collectors;

public class HttpErrorStatus extends HttpException {
    private int statusCode;

    @Nullable
    private Header[] headers;

    @Nullable
    private HttpEntity entity;

    public HttpErrorStatus(
            int statusCode,
            @NotNull String message,
            @Nullable Header[] headers,
            @Nullable HttpEntity entity) {
        super(message);
        this.statusCode = statusCode;
        this.headers = headers;
        this.entity = entity;
    }

    public int getStatusCode() {
        return statusCode;
    }

    @Nullable
    public Header[] getHeaders() {
        return headers;
    }

    @Nullable
    public HttpEntity getEntity() {
        return entity;
    }

    public String getErrorDetails() {
        StringBuilder sb = new StringBuilder();
        sb.append("Status Code: " + getStatusCode() + "\n");
        if (getHeaders() != null) {
            String headersString = Arrays.stream(getHeaders())
                    .map(header -> "\t" + header.getName() + ": " + header.getValue())
                    .collect(Collectors.joining("\n"));
            sb.append("Headers:\n" + headersString + "\n");
        }
        sb.append("Error message: " + getMessage());
        return sb.toString();
    }

    @Override
    public String toString() {
        return super.toString() + " with details --\n" + getErrorDetails();
    }
}
