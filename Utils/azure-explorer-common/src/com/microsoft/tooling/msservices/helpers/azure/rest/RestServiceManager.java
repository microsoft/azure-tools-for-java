/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
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
