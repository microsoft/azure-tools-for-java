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
                                        getRequestIdFromHeaders(status.getHeaders())));
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
