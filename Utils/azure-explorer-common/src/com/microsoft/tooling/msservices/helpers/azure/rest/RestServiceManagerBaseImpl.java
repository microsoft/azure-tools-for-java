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

package com.microsoft.tooling.msservices.helpers.azure.rest;

import com.google.common.base.Strings;
import com.google.common.io.CharStreams;
import com.microsoft.tooling.msservices.components.DefaultLoader;
import com.microsoft.azuretools.azurecommons.helpers.NotNull;
import com.microsoft.azuretools.azurecommons.helpers.Nullable;
import com.microsoft.azuretools.azurecommons.helpers.AzureCmdException;

import javax.net.ssl.HttpsURLConnection;
import java.io.*;
import java.net.URL;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class RestServiceManagerBaseImpl implements RestServiceManager {
    protected static class HttpResponse {
        private int code;
        private String message;
        private Map<String, List<String>> headers;
        private String content;

        public HttpResponse(int code, @NotNull String message, @NotNull Map<String, List<String>> headers,
                            @NotNull String content) {
            this.code = code;
            this.message = message;
            this.headers = new HashMap<String, List<String>>(headers);
            this.content = content;
        }

        public int getCode() {
            return code;
        }

        @NotNull
        public String getMessage() {
            return message;
        }

        @NotNull
        public Map<String, List<String>> getHeaders() {
            return headers;
        }

        @NotNull
        public String getContent() {
            return content;
        }
    }

    private static final String AZURE_API_VERSION = "2014-06-01";
    private static final String USER_AGENT_HEADER = "User-Agent";
    private static final String TELEMETRY_HEADER = "X-ClientService-ClientTag";
    private static final String X_MS_VERSION_HEADER = "x-ms-version";
    private static final String ACCEPT_HEADER = "Accept";
    private static final String CONTENT_TYPE_HEADER = "Content-Type";
    private static final int DEFAULT_READ_TIMEOUT = 0;

    @NotNull
    public String executeRequest(@NotNull String managementUrl,
                                 @NotNull String path,
                                 @NotNull ContentType contentType,
                                 @NotNull String method,
                                 @Nullable String postData,
                                 @NotNull HttpsURLConnectionProvider sslConnectionProvider)
            throws AzureCmdException {
        try {
            HttpsURLConnection sslConnection = sslConnectionProvider.getSSLConnection(managementUrl, path, contentType);
            HttpResponse response = getResponse(method, postData, sslConnection);
            int code = response.getCode();

            if (code < 200 || code >= 300) {
                throw new AzureCmdException(String.format("Error status code %s: %s", code, response.getMessage()),
                        response.getContent());
            }

            return response.getContent();
        } catch (IOException e) {
            throw new AzureCmdException(e.getMessage(), e);
        }
    }

    @NotNull
    public HttpsURLConnection getSSLConnection(@NotNull String managementUrl,
                                               @NotNull String path,
                                               @NotNull ContentType contentType)
            throws AzureCmdException {
        try {
            URL myUrl = new URL(managementUrl + path);
            HttpsURLConnection conn = (HttpsURLConnection) myUrl.openConnection();
            conn.addRequestProperty(USER_AGENT_HEADER, getPlatformUserAgent());
            conn.addRequestProperty(TELEMETRY_HEADER, getPlatformUserAgent());
            conn.addRequestProperty(X_MS_VERSION_HEADER, AZURE_API_VERSION);
            conn.addRequestProperty(ACCEPT_HEADER, "");
            conn.setReadTimeout(DEFAULT_READ_TIMEOUT);
            if (contentType != null) {
                conn.addRequestProperty(CONTENT_TYPE_HEADER, contentType.toString());
            }
            return conn;
        } catch (IOException e) {
            throw new AzureCmdException(e.getMessage(), e);
        }
    }

    @NotNull
    protected static HttpResponse getResponse(@NotNull final String method,
                                              @Nullable final String postData,
                                              @NotNull HttpsURLConnection sslConnection)
            throws IOException, AzureCmdException {
        try {
            sslConnection.setRequestMethod(method);
            sslConnection.setDoOutput(postData != null);

            if (postData != null) {
                DataOutputStream wr = new DataOutputStream(sslConnection.getOutputStream());

                try {
                    wr.writeBytes(postData);
                    wr.flush();
                } finally {
                    wr.close();
                }
            }

            return getResponse(sslConnection);
        } finally {
            sslConnection.disconnect();
        }
    }

    @NotNull
    private static String getPlatformUserAgent() {
        String version = DefaultLoader.getPluginComponent().getSettings().getPluginVersion();
        return String.format(
                "%s/%s (lang=%s; os=%s; version=%s)",
                DefaultLoader.getPluginComponent().getPluginId(),
                version,
                "Java",
                System.getProperty("os.name"),
                version);
    }

    @NotNull
    private static HttpResponse getResponse(@NotNull HttpsURLConnection sslConnection)
            throws IOException {
        int code = sslConnection.getResponseCode();
        String message = Strings.nullToEmpty(sslConnection.getResponseMessage());
        Map<String, List<String>> headers = sslConnection.getHeaderFields();

        if (headers == null) {
            headers = new HashMap<String, List<String>>();
        }

        InputStream is;
        String content;

        try {
            is = sslConnection.getInputStream();
        } catch (IOException e) {
            is = sslConnection.getErrorStream();
        }

        if (is != null) {
            content = readStream(is);
        } else {
            content = "";
        }

        return new HttpResponse(code, message, headers, content);
    }

    @NotNull
    private static String readStream(@NotNull InputStream is) throws IOException {
        BufferedReader in = new BufferedReader(new InputStreamReader(is));

        try {
            return CharStreams.toString(in);
        } finally {
            in.close();
        }
    }
}
