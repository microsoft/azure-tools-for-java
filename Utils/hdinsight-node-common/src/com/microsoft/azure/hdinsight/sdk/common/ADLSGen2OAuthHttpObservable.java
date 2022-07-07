/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.hdinsight.sdk.common;

import com.microsoft.azuretools.authmanage.IdeAzureAccount;
import com.microsoft.azuretools.azurecommons.helpers.NotNull;
import com.microsoft.azuretools.azurecommons.helpers.Nullable;
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
import static java.util.Collections.emptyList;
import static java.util.Optional.ofNullable;

public class ADLSGen2OAuthHttpObservable extends OAuthTokenHttpObservable {
    private static final String resource = "https://storage.azure.com/";
    private String tenantId;

    public ADLSGen2OAuthHttpObservable(String tenantId) {
        this.tenantId = tenantId;
    }

    @Override
    public String getAccessToken() throws IOException {
        return IdeAzureAccount.getInstance().getCredentialForTrack1(tenantId).getToken(resource);
    }

    @Override
    public Observable<CloseableHttpResponse> request(@NotNull final HttpRequestBase httpRequest,
                                                     final @Nullable HttpEntity entity,
                                                     final List<NameValuePair> parameters,
                                                     final List<Header> addOrReplaceHeaders) {
        // Filter out set permission related headers since they are not supported in request with OAuth
        List<Header> filteredHeaders = ofNullable(addOrReplaceHeaders)
            .orElse(emptyList())
            .stream()
            .filter(header -> !header.getName().equalsIgnoreCase(PERMISSIONS_HEADER) && !header.getName().equalsIgnoreCase(UMASK_HEADER))
            .collect(Collectors.toList());

        return super.request(httpRequest, entity, ofNullable(parameters).orElse(emptyList()), filteredHeaders);
    }
}
