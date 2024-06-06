/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.ide.appservice.function.coretools;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.microsoft.azure.toolkit.lib.Azure;
import com.microsoft.azure.toolkit.lib.common.exception.AzureToolkitRuntimeException;
import org.apache.http.HttpEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;

import javax.annotation.Nullable;
import java.io.IOException;
import java.util.Objects;


public class ReleaseService {
    private final static String FUNCTIONS_CORE_TOOLS_FEED_URL = "https://aka.ms/func-core-tools-feed";
    private final String FAILED_TO_GET_FEED_INFO = "failed to get release feed info";
    private final static ReleaseService instance = new ReleaseService();

    public static ReleaseService getInstance() {
        return instance;
    }

    @Nullable
    public ReleaseFeedData getReleaseFeedData() {
        final CloseableHttpClient client = HttpClients.custom().setSSLContext(Azure.az().config().getSslContext()).build();
        final HttpRequestBase request = new HttpGet(FUNCTIONS_CORE_TOOLS_FEED_URL);
        final ObjectMapper jsonMapper = new JsonMapper()
                .configure(JsonParser.Feature.ALLOW_COMMENTS, true)
                .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        try (final CloseableHttpResponse response = client.execute(request)) {
            final HttpEntity entity = response.getEntity();
            if (Objects.nonNull(entity)) {
                return jsonMapper.readValue(entity.getContent(), ReleaseFeedData.class);
            }
        } catch (final IOException e) {
            throw new AzureToolkitRuntimeException(FAILED_TO_GET_FEED_INFO, e);
        }
        return null;
    }
}
