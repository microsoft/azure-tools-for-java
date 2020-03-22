/*
 * Copyright (c) Microsoft Corporation
 *
 * All rights reserved.
 *
 * MIT License
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated
 * documentation files (the "Software"), to deal in the Software without restriction, including without limitation
 * the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and
 * to permit persons to whom the Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of
 * the Software.
 *
 * THE SOFTWARE IS PROVIDED *AS IS*, WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO
 * THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT,
 * TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
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
