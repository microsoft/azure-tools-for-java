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

package com.microsoft.azure.hdinsight.sdk.common;

import com.microsoft.azuretools.azurecommons.helpers.NotNull;
import com.microsoft.azuretools.azurecommons.helpers.Nullable;
import org.apache.http.Header;
import org.apache.http.client.config.CookieSpecs;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.message.BasicHeader;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class OAuthTokenHttpObservable extends HttpObservable {
    public static final String TOKEN_HEADER_NAME = "Authorization";

    @NotNull
    private String accessToken;

    public OAuthTokenHttpObservable() {
        this("");
    }


    public OAuthTokenHttpObservable(@NotNull String accessToken) {
        super();
        this.accessToken = accessToken;

        setDefaultRequestConfig(RequestConfig.custom()
                .setCookieSpec(CookieSpecs.DEFAULT)
                .build());

        setHttpClient(HttpClients.custom()
                .useSystemProperties()
                .setDefaultCookieStore(getCookieStore())
                .setDefaultRequestConfig(getDefaultRequestConfig())
                .build());
    }

    @NotNull
    public String getAccessToken() throws IOException {
        return accessToken;
    }

    @Nullable
    @Override
    public Header[] getDefaultHeaders() throws IOException {
        Header[] defaultHeaders = super.getDefaultHeaders();
        List<Header> headers = defaultHeaders == null ?
                new ArrayList<>() :
                Arrays.stream(defaultHeaders)
                      .filter(header -> !header.getName().equals(TOKEN_HEADER_NAME))
                      .collect(Collectors.toList());

        headers.add(new BasicHeader(TOKEN_HEADER_NAME, "Bearer " + getAccessToken()));

        return headers.toArray(new Header[0]);
    }
}
