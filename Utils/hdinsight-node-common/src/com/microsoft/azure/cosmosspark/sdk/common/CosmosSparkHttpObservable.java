/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.cosmosspark.sdk.common;

import com.microsoft.azure.hdinsight.sdk.common.AzureHttpObservable;
import com.microsoft.azure.hdinsight.sdk.common.azure.serverless.AzureSparkServerlessAccount;
import com.microsoft.azuretools.azurecommons.helpers.NotNull;
import org.apache.http.Header;
import org.apache.http.message.BasicHeader;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class CosmosSparkHttpObservable extends AzureHttpObservable {
    public static final String KOBO_ACCOUNT_HEADER_NAME = "x-ms-kobo-account-name";

    private AzureSparkServerlessAccount adlAccount;

    public CosmosSparkHttpObservable(@NotNull String tenantId, @NotNull AzureSparkServerlessAccount adlAccount) {
        super(tenantId, "");
        this.adlAccount = adlAccount;
    }

    @Override
    public Header[] getDefaultHeaders() throws IOException {
        Header[] defaultHeaders = super.getDefaultHeaders();
        List<Header> headers = Arrays.stream(defaultHeaders)
                      .filter(header -> !header.getName().equals(KOBO_ACCOUNT_HEADER_NAME))
                      .collect(Collectors.toList());

        headers.add(new BasicHeader(KOBO_ACCOUNT_HEADER_NAME, adlAccount.getName()));

        return headers.toArray(new Header[0]);
    }
}
