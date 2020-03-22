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

import com.microsoft.azuretools.azurecommons.helpers.NotNull;
import com.microsoft.azuretools.azurecommons.helpers.Nullable;
import com.microsoft.azuretools.azurecommons.helpers.AzureCmdException;

import javax.net.ssl.HttpsURLConnection;

public interface RestServiceManager {
    interface HttpsURLConnectionProvider {
        @NotNull
        HttpsURLConnection getSSLConnection(@NotNull String managementUrl,
                                            @NotNull String path,
                                            @NotNull ContentType contentType)
                throws AzureCmdException;
    }

    enum ContentType {
        Json("application/json"),
        Xml("application/xml"),
        Text("text/plain");

        @NotNull
        private final String value;

        ContentType(@NotNull String value) {
            this.value = value;
        }

        @Override
        public String toString() {
            return value;
        }
    }

    @NotNull
    String executeRequest(@NotNull String managementUrl,
                          @NotNull String path,
                          @NotNull ContentType contentType,
                          @NotNull String method,
                          @Nullable String postData,
                          @NotNull HttpsURLConnectionProvider sslConnectionProvider)
            throws AzureCmdException;

    @NotNull
    HttpsURLConnection getSSLConnection(@NotNull String managementUrl,
                                        @NotNull String path,
                                        @NotNull ContentType contentType)
            throws AzureCmdException;
}