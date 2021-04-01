/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.tooling.msservices.helpers.azure.rest;

import com.microsoft.azuretools.azurecommons.helpers.NotNull;
import com.microsoft.azuretools.azurecommons.helpers.Nullable;
import com.microsoft.azuretools.azurecommons.helpers.AzureCmdException;
import com.microsoft.tooling.msservices.helpers.azure.rest.RestServiceManager.ContentType;
import com.microsoft.tooling.msservices.helpers.azure.rest.RestServiceManager.HttpsURLConnectionProvider;

import javax.net.ssl.HttpsURLConnection;

public class AzureAADHelper {
    private static final String AUTHORIZATION_HEADER = "Authorization";

    @NotNull
    public static String executeRequest(@NotNull String managementUrl,
                                        @NotNull String path,
                                        @NotNull ContentType contentType,
                                        @NotNull String method,
                                        @Nullable String postData,
                                        @NotNull String accessToken,
                                        @NotNull RestServiceManager manager)
            throws AzureCmdException {
        HttpsURLConnectionProvider sslConnectionProvider = getHttpsURLConnectionProvider(accessToken, manager);

        return manager.executeRequest(managementUrl, path, contentType, method, postData, sslConnectionProvider);
    }

    @NotNull
    private static HttpsURLConnectionProvider getHttpsURLConnectionProvider(
            @NotNull final String accessToken,
            @NotNull final RestServiceManager manager) {
        return new HttpsURLConnectionProvider() {
            @Override
            @NotNull
            public HttpsURLConnection getSSLConnection(@NotNull String managementUrl,
                                                       @NotNull String path,
                                                       @NotNull ContentType contentType)
                    throws AzureCmdException {
                HttpsURLConnection sslConnection = manager.getSSLConnection(managementUrl, path, contentType);
                sslConnection.addRequestProperty(AUTHORIZATION_HEADER, "Bearer " + accessToken);

                return sslConnection;
            }
        };
    }
}
