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

import com.microsoft.azuretools.adauth.AuthException;
import com.microsoft.azuretools.adauth.PromptBehavior;
import com.microsoft.azuretools.authmanage.AuthMethodManager;
import com.microsoft.azuretools.azurecommons.helpers.NotNull;
import com.microsoft.azuretools.azurecommons.helpers.Nullable;
import com.microsoft.azuretools.sdkmanage.AzureManager;
import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.NameValuePair;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpRequestBase;
import rx.Observable;

import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

import static com.microsoft.azure.hdinsight.sdk.storage.adlsgen2.ADLSGen2FSOperation.PERMISSIONS_HEADER;
import static com.microsoft.azure.hdinsight.sdk.storage.adlsgen2.ADLSGen2FSOperation.UMASK_HEADER;

public class ADLSGen2OAuthHttpObservable extends OAuthTokenHttpObservable {
    private static final String resource = "https://storage.azure.com/";
    private String tenantId;

    public ADLSGen2OAuthHttpObservable(String tenantId) {
        this.tenantId = tenantId;
    }

    @Override
    public String getAccessToken() throws IOException {
        AzureManager azureManager = AuthMethodManager.getInstance().getAzureManager();

        // not signed in
        if (azureManager == null) {
            throw new AuthException("Not signed in. Can't send out the request.");
        }

        return azureManager.getAccessToken(tenantId, resource, PromptBehavior.Auto);
    }

    @Override
    public Observable<CloseableHttpResponse> request(@NotNull final HttpRequestBase httpRequest,
                                                     final @Nullable HttpEntity entity,
                                                     final @Nullable List<NameValuePair> parameters,
                                                     final @Nullable List<Header> addOrReplaceHeaders) {
        // Filter out set permission related headers since they are not supported in request with OAuth
        List<Header> filteredHeaders = addOrReplaceHeaders;
        if (filteredHeaders != null) {
            filteredHeaders =
                    filteredHeaders.stream()
                                   .filter(header -> !header.getName().equalsIgnoreCase(PERMISSIONS_HEADER)
                                           && !header.getName().equalsIgnoreCase(UMASK_HEADER))
                                   .collect(Collectors.toList());
        }

        return super.request(httpRequest, entity, parameters, filteredHeaders);
    }
}
