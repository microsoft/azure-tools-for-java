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
package com.microsoft.azuretools.telemetry;

import okhttp3.HttpUrl;
import okhttp3.Interceptor;
import okhttp3.Request;
import okhttp3.Response;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class TelemetryInterceptor implements Interceptor {
    static final String API_VERSION = "api-version";
    static final String SUBSCRIPTIONS = "subscriptions";
    static final String MS_REQUEST_ID = "x-ms-request-id";
    static final String PROVIDERS = "providers";
    static final String RESOURCE_PATH = "resource-path";
    static final String RESPONSE_CODE = "httpCode";
    static final String RESPONSE_MESSAGE = "httpMessage";

    @Override
    public Response intercept(final Chain chain) throws IOException {
        final Request request = chain.request();
        final Response response = chain.proceed(request);
        sendTelemetry(response);
        return response;
    }

    private void sendTelemetry(final Response response) {
        final Map<String, String> properties = new HashMap<>();
        final HttpUrl httpUrl = response.request().url();
        final String objectName = parseProvider(httpUrl, properties);
        if (objectName == null) {
            // Might not bea standard API path. Ignore
            return;
        }
        properties.put(API_VERSION, httpUrl.queryParameter(API_VERSION));
        properties.put(RESPONSE_CODE, String.valueOf(response.code()));
        properties.put(RESPONSE_MESSAGE, response.message());
        parseRequestId(response, properties);
        parseSubscriptionId(httpUrl, properties);
        AppInsightsClient.createByType(AppInsightsClient.EventType.Azure, objectName, response.request().method(), properties);
    }

    private String parseProvider(final HttpUrl httpUrl, final Map<String, String> properties) {
        if (httpUrl.pathSegments().contains(PROVIDERS)) {
            int index = httpUrl.pathSegments().indexOf(PROVIDERS);
            if (index + 1 < httpUrl.pathSegments().size()) {
                String path = httpUrl.encodedPath();
                properties.put(RESOURCE_PATH, path.substring(path.indexOf(PROVIDERS)));
                return httpUrl.pathSegments().get(index + 1);
            }
        }
        return null;
    }

    private void parseRequestId(final Response response, final Map<String, String> properties) {
        final String requestId = response.header(MS_REQUEST_ID);
        if (requestId != null)
            properties.put(MS_REQUEST_ID, requestId);
    }

    private void parseSubscriptionId(final HttpUrl httpUrl, final Map<String, String> properties) {
        if (httpUrl.pathSegments().contains(SUBSCRIPTIONS)) {
            int index = httpUrl.pathSegments().indexOf(SUBSCRIPTIONS);
            if (index + 1 < httpUrl.pathSegments().size())
                properties.put(AppInsightsConstants.SubscriptionId, httpUrl.pathSegments().get(index + 1));
        }
    }
}
