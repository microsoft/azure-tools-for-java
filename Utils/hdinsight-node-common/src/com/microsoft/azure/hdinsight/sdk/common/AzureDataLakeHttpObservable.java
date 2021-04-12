/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.hdinsight.sdk.common;

import com.microsoft.azure.hdinsight.sdk.common.errorresponse.HttpErrorStatus;
import com.microsoft.azuretools.authmanage.CommonSettings;
import com.microsoft.azuretools.azurecommons.helpers.NotNull;
import com.microsoft.azuretools.azurecommons.helpers.Nullable;
import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.NameValuePair;
import org.apache.http.client.methods.HttpRequestBase;
import rx.Observable;

import java.util.Arrays;
import java.util.List;

public class AzureDataLakeHttpObservable extends AzureHttpObservable {
    public AzureDataLakeHttpObservable(@NotNull String tenantId, @NotNull String apiVersion) {
        super(tenantId, apiVersion);
    }

    @NotNull
    @Override
    public String getResourceEndpoint() {
        String endpoint = CommonSettings.getAdEnvironment().dataLakeEndpointResourceId();

        return endpoint != null ? endpoint : "https://datalake.azure.net/";
    }

    @Override
    public Observable<HttpResponse> requestWithHttpResponse(HttpRequestBase httpRequest, HttpEntity entity, List<NameValuePair> parameters, List<Header> addOrReplaceHeaders) {
        return super.requestWithHttpResponse(httpRequest, entity, parameters, addOrReplaceHeaders)
                .onErrorResumeNext(err -> {
                    if (err instanceof HttpErrorStatus) {
                        HttpErrorStatus status = (HttpErrorStatus) err;
                        return Observable.error(
                                new SparkAzureDataLakePoolServiceException(
                                        status.getStatusCode(),
                                        err.getMessage(),
                                        getRequestIdFromHeaders(status.getHeaders()),
                                        httpRequest.getURI()));
                    } else {
                        return Observable.error(err);
                    }
                });
    }

    @NotNull
    public String getRequestIdFromHeaders(@Nullable Header [] headers) {
        if (headers == null) {
            return "";
        }

        Header requestIdHeader =
                Arrays.stream(headers)
                        .filter(header -> header != null
                                && header.getName().equalsIgnoreCase("x-ms-request-id"))
                        .findFirst()
                        .orElse(null);
        return requestIdHeader == null ? "" : requestIdHeader.getValue();
    }
}
