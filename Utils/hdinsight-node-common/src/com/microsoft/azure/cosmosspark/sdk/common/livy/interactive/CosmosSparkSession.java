/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.cosmosspark.sdk.common.livy.interactive;

import com.microsoft.azure.cosmosspark.sdk.common.CosmosSparkHttpObservable;
import com.microsoft.azure.hdinsight.sdk.common.HttpObservable;
import com.microsoft.azure.hdinsight.sdk.common.azure.serverless.AzureSparkServerlessAccount;
import com.microsoft.azure.hdinsight.sdk.common.livy.interactive.SparkSession;

import java.net.URI;

public class CosmosSparkSession extends SparkSession {
    private final CosmosSparkHttpObservable http;

    public CosmosSparkSession(final String name,
                              final URI baseUrl,
                              final String tenantId,
                              final AzureSparkServerlessAccount adlAccount) {
        super(name, baseUrl);
        this.http = new CosmosSparkHttpObservable(tenantId, adlAccount);
    }

    @Override
    public HttpObservable getHttp() {
        return this.http;
    }
}
