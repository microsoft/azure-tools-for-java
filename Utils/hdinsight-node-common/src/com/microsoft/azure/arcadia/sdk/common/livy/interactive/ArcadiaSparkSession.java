/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.arcadia.sdk.common.livy.interactive;

import com.microsoft.azure.arcadia.sdk.common.ArcadiaSparkHttpObservable;
import com.microsoft.azure.hdinsight.sdk.common.livy.interactive.SparkSession;

import java.net.URI;

public class ArcadiaSparkSession extends SparkSession {
    private final ArcadiaSparkHttpObservable http;

    public ArcadiaSparkSession(final String name, final URI baseUrl, final String tenantId) {
        super(name, baseUrl);
        this.http = new ArcadiaSparkHttpObservable(tenantId);
    }

    @Override
    public ArcadiaSparkHttpObservable getHttp() {
        return http;
    }
}
