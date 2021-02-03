/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.hdinsight.sdk.common;

import org.apache.http.Header;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public class HttpResponse {
    private final int code;
    private final String message;
    private final List<Header> headers;
    private final String content;

    public HttpResponse(final int code,
                        final String message,
                        final Header[] headers,
                        final String content) {
        this.code = code;
        this.message = message == null ? "" : message;
        this.headers = headers == null ? Collections.emptyList() : Arrays.asList(headers);
        this.content = content == null ? "" : content;
    }

    public int getCode() {
        return code;
    }

    public String getMessage() {
        return message;
    }

    public List<Header> getHeaders() {
        return headers;
    }

    public String findHeader(String headerName) {
        return getHeaders().stream()
                .filter(header -> header.getName().equalsIgnoreCase(headerName))
                // refer to https://www.w3.org/Protocols/rfc2616/rfc2616-sec4.html#sec4.2
                // combine all values by comma
                .map(Header::getValue)
                .collect(Collectors.joining(","));
    }

    public String getContent() {
        return content;
    }

    @Override
    public String toString() {
        return String.format("Response: status [%d], content [%s], headers [%s], body [%s]",
                getCode(),
                getContent(),
                getHeaders().stream()
                        .map(header -> header.getName() + ": " + String.valueOf(header.getValue()))
                        .collect(Collectors.joining("; ")),
                getMessage());
    }
}
