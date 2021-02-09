/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
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
